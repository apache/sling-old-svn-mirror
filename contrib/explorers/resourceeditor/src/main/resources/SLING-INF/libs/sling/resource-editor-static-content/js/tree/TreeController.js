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
 * The TreeController is responsible for the node tree functionality of the Sling Resource Editor
 * that is not specific for a 3rd party library.
 * JSTree-specific functionality is implemented in the JSTreeAdapter instead.
 */

//defining the module
org.apache.sling.reseditor.TreeController = (function() {

	function TreeController(settings, mainController){
		var thatTreeController = this;
		this.settings = settings;
		this.mainController = mainController;

		var addNodeControllerSettings = {};
		this.addNodeController = new org.apache.sling.reseditor.AddNodeController(addNodeControllerSettings, mainController);
		
		$(document).ready(function() {
			$("#tree").on("click", "#root",function(e) {
				var target = $(e.target);
				if (target.hasClass("open-icon")){
					thatTreeController.openNodeTarget(e);
				} else if (target.hasClass("add-icon")){
					thatTreeController.openAddNodeDialog(target.parents("li"));
				}else if (target.hasClass("remove-icon")){
					thatTreeController.deleteSingleNode(target.parents("li"));
				}
			});
			$("#tree").on("dblclick", "#root",function(e) {
				var target = $(e.target);
				if (target.hasClass("jstree-anchor") || target.hasClass("node-type")){
					var id = target.parents("li:first").attr("id");
					thatTreeController.openRenameNodeDialog(id);
				}
			});
			$("#tree-info-icon").on("click", function(e, data) {
				$('#sidebar .info-content-container').slideToggle();
			});
			$("#sidebar .info-content-container .close").on("click", function(e, data) {
				$('#sidebar .info-content-container').slideToggle();
			});
		});
	};

	TreeController.prototype.configureKeyListeners = function(e) {
    	// see http://www.javascripter.net/faq/keycodes.htm
		var del = 46;
		var c = 67;
		var v = 86;

    	if (e.ctrlKey && c==e.which) { /*ctrl-c*/
    		var resourcePath = this.getPathFromLi($(e.target).parents("li"));
    		sessionStorage["resourcePath"] = resourcePath;
    	}
    	if (e.ctrlKey && v==e.which) { /*ctrl-v*/
    		var from = sessionStorage["resourcePath"];
    		var to = this.getPathFromLi($(e.target).parents("li"));
    		this.copy(from, to);
    	}
		switch(e.which) {
		    case del:
	    		treeController.deleteNodes();
		        break;
		    case c:
		    	if (!e.ctrlKey){
		    		this.openAddNodeDialog($(e.target).parents("li"));
		    	}
		        break;
		}
		
	}

	TreeController.prototype.copy = function(from, to) {
		var thisTreeController = this;
		var dest = to+"/";
		var data = {
				":operation": "copy",
				":dest": dest};
		$.post( from, data, function( data ) {
			  $( ".result" ).html( data );
			}, "json")
			.done(function() {
				thisTreeController.mainController.redirectTo(to);
			})
			.fail(function(errorJson) {
				thisTreeController.mainController.displayAlert(errorJson);
			});;
	}

	TreeController.prototype.afterOpen = function(node) {
		$('#'+node.id).addClass("opened");
	}

	TreeController.prototype.beforeClose = function(node) {
		$('#'+node.id).removeClass("opened");
	}

	TreeController.prototype.openNodeTarget = function(e) {
		var url = $(e.target).parent().attr("href");
		url = this.mainController.decodeFromHTML(url);
		url = this.mainController.encodeURL(url);
		location.href=url;
	}

	TreeController.prototype.openRenameNodeDialog = function(id) {
		var liElement = $('#'+id);
		$("#tree").jstree("edit", $('#'+id), this.mainController.decodeFromHTML(liElement.attr("nodename")));
	}
	
	TreeController.prototype.renameNode = function(e, data) {
		var thatTreeController = this;
		var newName = this.mainController.decodeFromHTML(data.text);
		var oldName = data.old;
		if (oldName!==newName){
			var currentURL = this.getPathFromLi($('#'+data.node.id));
			var unencodedURI = currentURL;
			var decodedCurrentURI = this.mainController.decodeFromHTML(unencodedURI);
			var newURI = decodedCurrentURI.replace(oldName, newName);
			currentURL = this.mainController.encodeURL(decodedCurrentURI);
			$.ajax({
		  	  type: 'POST',
			  url: currentURL,
		  	  success: function(server_data) {
		  		  thatTreeController.mainController.redirectTo(newURI);
			  },
		  	  error: function(errorJson) {
		  		  thatTreeController.mainController.displayAlert(errorJson);
			  },
			  dataType: "json",
			  contentType : 'application/x-www-form-urlencoded; charset=UTF-8',
		  	  data: { 
		  		":operation": "move",
		  		"_charset_": "utf-8",
		  		":dest": newURI
		  		  }
		  	});
		}
	}
	
	TreeController.prototype.getPathElements = function(resourcePath){
		var pathSuffix = ".html";
		var pathEndsWithPathSuffix = resourcePath.substring(resourcePath.length-pathSuffix.length) == pathSuffix;
		var resourcePathWithoutSuffix = (pathEndsWithPathSuffix) ? resourcePath.substring(0,resourcePath.length-pathSuffix.length) : resourcePath; 
		var currentNodePath = this.mainController.encodeToHTML(resourcePathWithoutSuffix);
		return currentNodePath.substring(1).split("/");
	}
	
	TreeController.prototype.getSelectorFromPath = function(path){
		var paths = path.substring(1).split("/");
		return "#tree > ul [nodename='"+paths.join("'] > ul > [nodename='")+"']";
	}

	TreeController.prototype.getPathFromLi = function(li){
		var path = $(li).parentsUntil(".root").andSelf().map(
				function() {
					return this.tagName == "LI"
							? $(this).attr("nodename") 
							: null;
				}
			).get().join("/");
		return "" == path ? "/" : path;
	};

	TreeController.prototype.getURLEncodedPathFromLi = function(li){
		var htmlEncodedPath = this.getPathFromLi(li);
		var htmlDecodedPath = this.mainController.decodeFromHTML(htmlEncodedPath);
		return this.mainController.encodeURL(htmlDecodedPath);
	};

	TreeController.prototype.openElement = function(root, paths) {
		var thisTreeController = this;
		var pathElementName = paths.shift();
		var pathElementLi = root.children("[nodename='"+pathElementName+"']");
		if (pathElementLi.length === 0){
			alert("Couldn't find "+pathElementName+" under the path "+this.getPathFromLi(root.parent()));
		} else {
			$('#tree').jstree('open_node', pathElementLi,
					function(){
						if (paths.length>0){
							thisTreeController.openElement($("#"+pathElementLi.attr('id')).children("ul"), paths);
						} else  {
							$('#tree').jstree('select_node', pathElementLi.attr('id'), 'true');
					        var target = $('#'+pathElementLi.attr('id')+' a:first');
					        target.focus();
						}
					}
				);
		}
	}

	TreeController.prototype.get_uri_from_li = function(li, extension){
		var path = this.getPathFromLi(li);
		path = this.mainController.decodeFromHTML(path);
		path = this.mainController.encodeURL(path);
		return this.settings.contextPath+"/reseditor"+path+extension;
	}

	TreeController.prototype.deleteNodes = function() {
		var thatTreeController = this;
		var lastDeletedLI;
		var selectedIds = $("#tree").jstree('get_selected');
		var firstId = selectedIds[0];
		var parentLi = $('#'+firstId).parents('li');
		var parentPath = this.getURLEncodedPathFromLi(parentLi);
		var otherPathsToDelete = [];
		var otherPathsToDeleteDecoded = [];
		for (var i=0; i<selectedIds.length; i++){
			var id = selectedIds[i];
			var li = $('#'+id);
			var resourcePathToDelete = this.getPathFromLi(li);
			otherPathsToDelete.push(resourcePathToDelete);
			var decodedResourcePath = this.mainController.decodeFromHTML(resourcePathToDelete);
			otherPathsToDeleteDecoded.push(decodedResourcePath);
		}
		var confirmationMsg = "You are about to delete '"+otherPathsToDelete+"' and all its sub nodes. Are you sure?";
		bootbox.confirm(confirmationMsg, function(result) {
			if (result){
					//http://www.jstree.com/api/#/?q=delete&f=delete_node.jstree
			    	$.ajax({
			        	  type: 'POST',
						  url: parentPath,
			        	  success: function(server_data) {
							var tree = $('#tree').jstree(true);
							for (var i=0; i<selectedIds.length; i++){
								var id = selectedIds[i];
								tree.delete_node(id);
							}
			      		  },
			        	  error: function(errorJson) {
			        		thatTreeController.mainController.displayAlert(errorJson);
			      		  },
			      		  traditional: true,
			      		  dataType: "json",
						  contentType : 'application/x-www-form-urlencoded; charset=UTF-8',
			        	  data: { 
			        		  ":operation": "delete",
			  		  		  "_charset_": "utf-8",
			            	  ":applyTo": otherPathsToDeleteDecoded        		
			        	  }
			        });
			}
		});
	}

	TreeController.prototype.deleteSingleNode = function(li) {
		var thatTreeController = this;
		var resourcePathToDelete = this.getPathFromLi(li);
		var confirmationMsg = "You are about to delete '"+resourcePathToDelete+"' and all its sub nodes. Are you sure?";
		var decodedResourcePath = this.mainController.decodeFromHTML(resourcePathToDelete);
		var encodedResourcePathToDelete = this.mainController.encodeURL(decodedResourcePath);
		var sendDeletePost = function(result) {
			if (result){
		    	$.ajax({
		        	  type: 'POST',
					  url: encodedResourcePathToDelete,
		        	  success: function(server_data) {
		        		var id = li.attr("id");
						var tree = $('#tree').jstree(true);
						tree.delete_node(id);
		      		  },
		        	  error: function(errorJson) {
		        		thatTreeController.mainController.displayAlert(errorJson);
		      		  },
					  dataType: "json",
		        	  data: { 
		        		  ":operation": "delete"
		        	  }
		        });
			}
		};
		bootbox.confirm(confirmationMsg, sendDeletePost);
	}

	TreeController.prototype.openAddNodeDialog = function(li) {
		var thatTreeController = this;
		var resourcePath = this.getPathFromLi(li);
		var nodeTypeName = li.attr("nodetype");
		this.addNodeController.openAddNodeDialog(resourcePath, nodeTypeName);
	}

	/*
	function isModifierPressed(e){
		return (e.shiftKey || e.altKey || e.ctrlKey);
	}
	*/

	return TreeController;
}());
