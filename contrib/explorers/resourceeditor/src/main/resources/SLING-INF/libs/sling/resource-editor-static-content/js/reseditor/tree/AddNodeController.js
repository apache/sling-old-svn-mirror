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
 * Handles the Add Node dialog functionality
 */

//defining the module
org.apache.sling.reseditor.AddNodeController = (function() {

	function AddNodeController(settings, mainController){
		var thatAddNodeController = this;
		this.settings = settings;
		this.mainController = mainController;
		this.lastAddNodeURL = "";
		
		$(document).ready(function() {
			$('#addNodeDialog .submit').click(function(){
				thatAddNodeController.addNode();
			});
			
			var nodeTypeObjects = jQuery.map( mainController.getNodeTypes(), function( nt, i ) {
				return {id: nt, text: nt};
			});

			$("#nodeType").select2({
				placeholder: "Node Type",
				allowClear: true, 
				data: nodeTypeObjects
			})

			function format(element) {
				return "<span><span class=\"search-choice-close\"></span>"+element.text+"</span>";
			}

			var data=[];

			var select2 = $("#resourceType").select2({
				placeholder: "Resource Type",
				allowClear: true, 
				formatResult: format,
				data: data,
				createSearchChoice: function(searchTerm){
					return {id:searchTerm, text:searchTerm};
				}
			}).data("select2");
			
			// To get called on a click in the result list:
			// http://stackoverflow.com/a/15637696/1743551
			select2.onSelect = (function(fn) {
			    return function(data, options) {
			        var target;
			        
			        if (options != null) {
			            target = $(options.target);
			        }
			        
			        if (target && target.hasClass('search-choice-close')) {
			            alert('click!');
			        } else {
			            return fn.apply(this, arguments);
			        }
			    }
			})(select2.onSelect);
		});

		AddNodeController.prototype.addNode = function() {
			var thatAddNodeController = this;
			var nodeName = $("#nodeName").val().trim();
			var nodeType = $("#nodeType").val();
			var resourceType = $("#resourceType").val().trim();
			
			var data = {"_charset_": "utf-8"};
			if ("" != nodeType){
				data["jcr:primaryType"] = nodeType;
			}
			if ("" != resourceType){
				data["sling:resourceType"] = resourceType;
			}
			var targetURL = (this.lastAddNodeURL=="/") ? "/" : this.lastAddNodeURL+"/";
			targetURL = this.mainController.decodeFromHTML(targetURL);
			if ("" != nodeName) {
				targetURL += nodeName;
			}
			if (targetURL=="/"){
				//adding a node without a specified name to the root node 
				targetURL = "/*";
			}
			var encodedTargetURL = this.mainController.encodeURL(targetURL);

			$.ajax({
		  	  type: 'POST',
			  url: encodedTargetURL,
			  dataType: "json",
		  	  data: data
		  	})
			.done(function() {
				$('#addNodeDialog').modal("hide");
				var htmlDecodedLastAddNodeURL = thatAddNodeController.mainController.decodeFromHTML(thatAddNodeController.lastAddNodeURL);
				thatAddNodeController.mainController.redirectTo(htmlDecodedLastAddNodeURL);
			})
			.fail(function(errorJson) {
				$('#addNodeDialog').modal("hide");
				thatAddNodeController.mainController.displayAlert(errorJson);
			});
			
		}
	};
	
	AddNodeController.prototype.openAddNodeDialog = function(resourcePath) {
		$('#addNodeDialog').modal({});
		var contextPath = this.mainController.getContextPath() == "/" && resourcePath=="/" ? "" : this.mainController.getContextPath(); 
		this.lastAddNodeURL = contextPath+resourcePath;
	}
	
	return AddNodeController;
}());