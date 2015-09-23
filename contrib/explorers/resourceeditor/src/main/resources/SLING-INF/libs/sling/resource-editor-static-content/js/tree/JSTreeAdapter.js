/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// creating the namespace
var org = org || {};
org.apache = org.apache || {};
org.apache.sling = org.apache.sling || {};
org.apache.sling.reseditor = org.apache.sling.reseditor || {};


/*
 JSTreeAdapter - It adapts the JSTree library for the use in the Sling Resource Editor.
 This JSTreeAdapter contains as less logic as needed to configure the JSTree for the Sling Resource Editor. For 
 everything that goes beyond that and contains more functionality, the other Sling Resource Editor controllers are called.
*/

//defining the module
org.apache.sling.reseditor.JSTreeAdapter = (function() {

	function JSTreeAdapter(settings, treeController, mainController){
		this.settings = settings;
		this.treeController = treeController;
		this.mainController = mainController;

		var thisJSTreeAdapter = this;

$(document).ready(function() {
	$(window).resize( function() {
		thisJSTreeAdapter.mainController.adjust_height();
	});
	
	var scrollToPathFinished=false;
	
	thisJSTreeAdapter.mainController.adjust_height();
	
	
	// TO CREATE AN INSTANCE
	// select the tree container using jQuery
	$("#tree")
	.bind("loaded.jstree", function (event, data) {
		var pathElements = treeController.getPathElements(settings.resourcePath);
		
		if (pathElements.length >= 1 && pathElements[0] != "") {
			treeController.openElement($("#tree > ul > li[nodename=''] > ul"), pathElements);
		}
		
		// position the info-icon
		$('#tree-info-icon').show();
		$('#root i:first').before($('#tree-info-icon'));
	})
	// call `.jstree` with the options object
	.jstree({
		"core"      : {
		    "check_callback" : true,
		    multiple: true,
			animation: 600,
			'dblclick_toggle': false,
			'data' : {
				'url' : function (liJson) {
					// initial call for the root element
					if (liJson.id === '#'){
						return settings.contextPath+"/reseditor/.rootnodes.json";
					} else {
						// the li the user clicked on.
						var li = $('#'+liJson.id);
						return treeController.get_uri_from_li(li,".nodes.json");
					}
				},
			    'data' : function (node) {
			        return { 'id' : node.id };
			      }
			}
		},
		"ui"      : {
			"select_limit" : 2
		},
		"crrm"      : {
			"move" : {
				"always_copy" : false,
		        "check_move"  : function (m) {
			        // you find the member description here
			        // http://www.jstree.com/documentation/core.html#_get_move
		        	// TODO refactor to the new jsTree version
			        var src_li = m.o;
			        var src_nt = mainController.getNTFromLi(src_li);
			        var src_nodename = src_li.attr("nodename");
			        
			        var new_parent_ul = m.np.children("ul");
			        var calculated_position = m.cp;
			        var liAlreadySelected = new_parent_ul.length==0 && m.np.prop("tagName").toUpperCase() == 'LI';
			        var dest_li = liAlreadySelected ? m.np : new_parent_ul.children("li:eq("+(calculated_position-1)+")");
			        var dest_nt = mainController.getNTFromLi(dest_li);
					var result;
					if (dest_nt != null){ 
						result = dest_nt.canAddChildNode(src_nodename, src_nt);
					}
                    return result;
                  }
			}
		},
		"dnd" : {
			"drop_finish" : function () {
				console.log("drop");
			},
			"drag_finish" : function (data) {
				console.log("drag");
			}
		},
		// the `plugins` array allows you to configure the active plugins on this instance
		"plugins" : [ "themes", "ui", "core", "hotkeys", "crrm", "dnd"]
    }).bind("rename_node.jstree", function (e, data) {
    	treeController.renameNode(e, data);
    }).bind("move_node.jstree", function (e, data) {
    	// see http://www.jstree.com/documentation/core ._get_move()
    	// TODO refactor to the new jsTree version
    	var src_li = data.rslt.o;
    	var src_path = ""+settings.contextPath+src_li.children("a").attr("target");
    	var dest_li = data.rslt.np; // new parent .cr - same as np, but if a root node is created this is -1
    	var dest_li_path = dest_li.children("a").attr("target") == "/" ? "" : dest_li.children("a").attr("target");
    	var dest_path = ""+settings.contextPath+dest_li_path+"/"+src_li.attr("nodename");
    	var original_parent = data.rslt.op;
    	var is_copy = data.rslt.cy;
    	var position = data.rslt.cp;
    	$.ajax({
      	  type: 'POST',
		  url: src_path,
		  dataType: "json",
      	  success: function(server_data) {
        		var target = ""+settings.contextPath+dest_path;
            	location.href=target+".reseditor.html";
    		  },
      	  error: function(errorJson) {
      			displayAlert(errorJson);
    		  },
      	  data: { 
       		":operation": "move",
//          	":order": position,
      		":dest": dest_path
      		  }
      	});
    }).on('hover_node.jstree', function (event, nodeObj) {
        //noop
    }).on('keydown.jstree', 'a.jstree-anchor', function (e) {
    	treeController.configureKeyListeners(e);
    }).on('select_node.jstree', function (e, data) {
    	//noop
    	;
    }).on('after_open.jstree', function(e, data){
    	treeController.afterOpen(data.node);
    }).on('close_node.jstree', function(e, data){
    	treeController.beforeClose(data.node);
    });
});

	};
	return JSTreeAdapter;
}());
