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
		this.settings = settings;
		this.mainController = mainController;
		this.lastAddNodeURL = "";
		this.dialogShown = false;
		this.showAllNodeTypes = false;
		this.nodeTypeObjects = [];
		this.nodeType="";
		this.nodeNameSubmitable=false; // initially open
		this.resourceTypeSubmitable=true;
		
		var thatAddNodeController = this;
		$(document).ready(function() {
			$('#addNodeDialog .submit').click(function(){
				thatAddNodeController.addNode();
			});
			$('#addNodeDialog').on('shown.bs.modal', function () {
				thatAddNodeController.dialogShown = true;
				$('#nodeName').select2("open");
			})
			$('#addNodeDialog').on('hide.bs.modal', function () {
				thatAddNodeController.dialogShown = false;
			})
			$('#addNodeDialog .info-icon').click(function () {
				$('#addNodeDialog .info-content').slideToggle();
			});
			$('#addNodeDialog .info-content .close').click(function () {
				$('#addNodeDialog .info-content').slideToggle();
			});
			$('#addNodeDialog .nt-toggle').click(function () {
				thatAddNodeController.toggleApplicableNodeTypes();
			});
			$("body").on('keydown', function (e) {
		    	// see http://www.javascripter.net/faq/keycodes.htm
				var aKey = 65;
		    	if (e.ctrlKey && aKey==e.which) { /*ctrl-a*/
		    		if (thatAddNodeController.dialogShown){
		    			thatAddNodeController.addNode();
		    		}
		    	}
			})
		});
	};
	
	AddNodeController.prototype.addNode = function() {
		var thatAddNodeController = this;
		var dialogSubmitable = this.resourceTypeSubmitable && this.nodeNameSubmitable;
		if (dialogSubmitable) {
			var nodeName = $("#nodeName").select2("val");
			var nodeType = $("#nodeType").select2("val");
			var resourceType = $("#resourceType").select2("val");
	
			
			var data = {"_charset_": "utf-8"};
			if ("" != nodeType){
				data["jcr:primaryType"] = nodeType;
			}
			var canAddResourceType = nodeType == "" ? true : this.mainController.ntManager.getNodeType(nodeType).canAddProperty("sling:resourceType", "String");
			if ("" != resourceType && canAddResourceType){
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
	}
	
	AddNodeController.prototype.toggleApplicableNodeTypes = function() {
		if (this.showAllNodeTypes){
			this.showAllNodeTypes=false;
			$('#addNodeDialog .form-group.node-type .nt-dependency-description').text("applicable together with node name");
			$('#addNodeDialog .form-group.node-type .nt-toggle').text("show generally applicable");
		} else {
			this.showAllNodeTypes=true;
			$('#addNodeDialog .form-group.node-type .nt-dependency-description').text("generally applicable");
			$('#addNodeDialog .form-group.node-type .nt-toggle').text("show applicable together with node name");
		}
		var nodeType = mainController.ntManager.getNodeType(this.nodeTypeName);
		var appliCnTypesByNodeName = nodeType.getApplicableCnTypesPerCnDef(false /*include mixins*/);
		var nodeNameList = Object.keys(appliCnTypesByNodeName);
		this.nodeTypeObjects = getNodeTypesByDependenyState.call(this, nodeNameList, appliCnTypesByNodeName, this.nodeTypeObjects);
	}
	
	function getNodeTypesByDependenyState(nodeNameList, appliCnTypesByNodeName, nodeTypeObjects){
		var allAppliCnTypes = getAllApplicableCnTypesSorted(nodeNameList, appliCnTypesByNodeName);
		if (this.showAllNodeTypes){
			return jQuery.map( allAppliCnTypes, function( nt, i ) {
				return {id: nt, text: nt};
			});
		} else {
			return getNodeTypesByNodeName(appliCnTypesByNodeName, nodeTypeObjects, allAppliCnTypes);
		}
	}
	
	function getAllApplicableCnTypesSorted(nodeNameList, appliCnTypesByNodeName){
		var allAppliCnTypes = [];
		for (var nodeNameIndex in nodeNameList) {
			var nodeName = nodeNameList[nodeNameIndex];
			for (var nodeType in appliCnTypesByNodeName[nodeName]){
				if (allAppliCnTypes.indexOf(nodeType)<0){
					allAppliCnTypes.push(nodeType);
				}
			}
		}
		allAppliCnTypes.sort();
		return allAppliCnTypes;
	}
	
	function getNodeTypesByNodeName(appliCnTypesByNodeName, nodeTypeObjects, allAppliCnTypes){
		var nodeName = $("#nodeName").val();
		if ("" === nodeName || typeof appliCnTypesByNodeName[nodeName] === "undefined"){
			return nodeTypeObjects = jQuery.map(allAppliCnTypes, function( nt, i ) {
				return {id: nt, text: nt};
			});
		} else if (typeof appliCnTypesByNodeName[nodeName] != "undefined"){
			var nodeTypes = Object.keys(appliCnTypesByNodeName[nodeName]);
			return nodeTypeObjects = jQuery.map(nodeTypes, function( nt, i ) {
					return {id: nt, text: nt};
				});
		} else if (typeof appliCnTypesByNodeName["*"] != "undefined"){
			var nodeTypes = Object.keys(appliCnTypesByNodeName["*"]);
			return nodeTypeObjects = jQuery.map(nodeTypes, function( nt, i ) {
					return {id: nt, text: nt};
				});
		}
//		wenn node name leer, dann alle nt anzeigen	
	}
	
	AddNodeController.prototype.openAddNodeDialog = function(resourcePath, nodeTypeName) {
		var thatAddNodeController = this;
		var resTypeHelpElement = $('#addNodeDialog .resource-type-not-allowed');
		resTypeHelpElement.hide();
		this.nodeTypeName = nodeTypeName;
		var nodeType = mainController.ntManager.getNodeType(this.nodeTypeName);
		var appliCnTypesByNodeName = nodeType.getApplicableCnTypesPerCnDef(false /*include mixins*/);
		var nodeNameListStar = Object.keys(appliCnTypesByNodeName);
		var nodeHelpElement = $('#addNodeDialog .only-listed-node-names-allowed');
		var indexOfResidualDef = nodeNameListStar.indexOf("*");
		var residualsDefined = indexOfResidualDef >= 0;
		if (residualsDefined){
			nodeNameListStar.splice(indexOfResidualDef, 1);
			nodeHelpElement.parents(".form-group").addClass("has-warning");
			nodeHelpElement.hide();
		} else {
			nodeHelpElement.parents(".form-group").addClass("has-warning");
			nodeHelpElement.show();
		}
		nodeNameListStar.sort();
		nodeNameListStar.unshift("");
		var nodeNameObjects = jQuery.map(nodeNameListStar, function( nt, i ) {
			return {id: nt, text: nt};
		});
		$('#nodeName').select2('data', null);
		$("#nodeName").select2({
			placeholder: "Enter or select a node name",
			allowClear: true, 
			selectOnBlur: true,
			dropdownCssClass: "node_name_dd_container",
			data: nodeNameObjects,
			createSearchChoice: function(searchTerm){
				thatAddNodeController.latestEnteredNodeName = searchTerm;
				return {id:searchTerm, text:searchTerm};
			}
		});
		$("#nodeName").on("select2-open", function(e) {
			thatAddNodeController.nodeNameSubmitable=false;
		});
		$("#nodeName").on("select2-close", function(e) {
			thatAddNodeController.nodeNameSubmitable=true;
		});

		var nodeNameList = Object.keys(appliCnTypesByNodeName);
		nodeNameList.sort();
		thatAddNodeController.nodeTypeObjects = getNodeTypesByDependenyState.call(thatAddNodeController, nodeNameList, appliCnTypesByNodeName, thatAddNodeController.nodeTypeObjects);
		$("#nodeName").on("change", function(e) {
			thatAddNodeController.nodeTypeObjects = getNodeTypesByDependenyState.call(thatAddNodeController, nodeNameList, appliCnTypesByNodeName, thatAddNodeController.nodeTypeObjects);
		});
		
		$("#nodeType").on("change", function(e) {
			var nodeTypeName = $("#nodeType").val();
			var nodeType = mainController.ntManager.getNodeType(nodeTypeName);
			var canAddResourceType = nodeType.canAddProperty("sling:resourceType", "String");
			if (canAddResourceType){
				resTypeHelpElement.hide();
			} else {
				resTypeHelpElement.parents(".form-group").addClass("has-warning");
				resTypeHelpElement.show();
			}
		});
		
		
		$('#nodeType').select2('data', null);
		$("#nodeType").select2({
			placeholder: "Select a node type",
			allowClear: true,  
			dropdownCssClass: "node_type_dd_container",
			selectOnBlur: true,
			data: function() { 
			      return { results: thatAddNodeController.nodeTypeObjects } ; // Use the global variable to populate the list
		    }
		});
		
		var contextPath = this.mainController.getContextPath() == "/" && resourcePath.length>0 && resourcePath.charAt(0)=="/" ? "" : this.mainController.getContextPath(); 
		this.lastAddNodeURL = contextPath+resourcePath;


		$('#resourceType').select2('data', null);
		var contextPath = this.mainController.getContextPath();
		contextPath = "/" === contextPath ? "" : contextPath;
		var url = contextPath+"/libs/sling/resource-editor/content-nodes/resource-types.json";
		$.getJSON(url, function( origData ) {
			var data = jQuery.map( origData, function( n, i ) {
				return ( {id:n, text:n} );
			});
			data.unshift({id:"",text:""});
			var select2 = $("#resourceType").select2({
				placeholder: "Enter or select a resource type",
				allowClear: true, 
				dropdownCssClass: "resource_type_dd_container",
				selectOnBlur: true,
				data: data,
				createSearchChoice: function(searchTerm){
					thatAddNodeController.latestEnteredResType = searchTerm;
					return {id:searchTerm, text:searchTerm};
				}
			}).data("select2");

			$("#resourceType").on("select2-open", function(e) {
				thatAddNodeController.resourceTypeSubmitable=false;
			});
			$("#resourceType").on("select2-close", function(e) {
				thatAddNodeController.resourceTypeSubmitable=true;
			});
			
			$('#addNodeDialog').modal('show');
			
			$('#addNodeDialog').addClass('add-node-finished');
		});
	}
	
	return AddNodeController;
}());
