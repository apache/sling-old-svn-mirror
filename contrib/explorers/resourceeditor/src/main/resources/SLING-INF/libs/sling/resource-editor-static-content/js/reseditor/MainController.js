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
 * The MainController is responsible for every functionality 
 * that is not handled by other, more specific controllers.
 */

//defining the module
org.apache.sling.reseditor.MainController = (function() {

	function MainController(settings, ntManager){
		this.ntManager = ntManager;
		this.settings = settings;
		
		var thisMainController = this;
		
		$(document).ready(function() {
			$('#alertClose').click(function () {
				$("#alert").slideUp(function() {
					thisMainController.adjust_height();
					$('#alertMsg #Message').remove();
				});
			})
		});
	};

	MainController.prototype.getNodeTypes = function(){
		return this.settings.nodeTypes;
	}

	MainController.prototype.getContextPath = function(){
		return this.settings.contextPath;
	}

	MainController.prototype.encodeURL = function(unencodedURL){
		url = encodeURIComponent(unencodedURL);
		return url.replace(/%2F/g, "/");
	}

	MainController.prototype.encodeToHTML = function(unencodedHTML){
		//create a in-memory div, set it's inner text(which jQuery automatically encodes)
		//then grab the encoded contents back out.The div never exists on the page.
		return $('<div/>').text(unencodedHTML).html();
	}

	MainController.prototype.decodeFromHTML = function(encodedHTML){
		return $("<div/>").html(encodedHTML).text();
	}
	
	
	MainController.prototype.adjust_height = function(objectId){
		var login_height = $("#login").outerHeight(true);
		var header_height = $("#header").outerHeight(true);
		var alert_height = $("#alerts").outerHeight(true);
		var footer_height = $("#footer").outerHeight(true);
		var sidebar_margin = $("#sidebar").outerHeight(true)-$("#sidebar").outerHeight(false);
		var usable_height = $(window).height() - login_height - header_height - alert_height - sidebar_margin - 1;
		
	// activate again if the footer is needed	
//	 	var usable_height = $(window).height() - header_height - footer_height - sidebar_margin - 1;
		$("#sidebar").height( usable_height );
		$("#outer_content").height( usable_height );
	}

	MainController.prototype.displayAlert = function(error, resourcePath){
		var thisMainController = this;
		var errorJson = error.responseJSON;
		var encodedTitle = this.encodeToHTML(errorJson.title);
		var encodedMsg = this.encodeToHTML(errorJson["status.message"]);
		var errorMsg = encodedTitle+" ("+"Status "+errorJson["status.code"]+") "+encodedMsg;
		$('#alertMsg').append($("<div id='Message'>").append((resourcePath) ? "'"+resourcePath+"': "+errorMsg : errorMsg));
		$("#alert").slideDown(function() {
			thisMainController.adjust_height();
		});
	}


	MainController.prototype.getNTFromLi = function(li){
		var nt_name = $(li).children("a").find("span span.node-type").text();
	    return this.ntManager.getNodeType(nt_name);	
	}
	
	MainController.prototype.redirectTo = function(unencodedTargetPath){
		var newURIencoded = this.encodeURL(unencodedTargetPath);
  	  	var target = this.settings.contextPath+"/reseditor"+newURIencoded;
  	  	location.href=target+".html";
	}
	
	return MainController;
}());
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
 * The MainController is responsible for every functionality 
 * that is not handled by other, more specific controllers.
 */

//defining the module
org.apache.sling.reseditor.MainController = (function() {

	function MainController(settings, ntManager){
		this.ntManager = ntManager;
		this.settings = settings;
		
		var thisMainController = this;
		
		$(document).ready(function() {
			$('#alertClose').click(function () {
				$("#alert").slideUp(function() {
					thisMainController.adjust_height();
					$('#alertMsg #Message').remove();
				});
			})
		});
	};

	MainController.prototype.getNodeTypes = function(){
		return this.settings.nodeTypes;
	}

	MainController.prototype.getContextPath = function(){
		return this.settings.contextPath;
	}

	MainController.prototype.encodeURL = function(unencodedURL){
		url = encodeURIComponent(unencodedURL);
		return url.replace(/%2F/g, "/");
	}

	MainController.prototype.encodeToHTML = function(unencodedHTML){
		//create a in-memory div, set it's inner text(which jQuery automatically encodes)
		//then grab the encoded contents back out.The div never exists on the page.
		return $('<div/>').text(unencodedHTML).html();
	}

	MainController.prototype.decodeFromHTML = function(encodedHTML){
		return $("<div/>").html(encodedHTML).text();
	}
	
	
	MainController.prototype.adjust_height = function(objectId){
		var login_height = $("#login").outerHeight(true);
		var header_height = $("#header").outerHeight(true);
		var alert_height = $("#alerts").outerHeight(true);
		var footer_height = $("#footer").outerHeight(true);
		var sidebar_margin = $("#sidebar").outerHeight(true)-$("#sidebar").outerHeight(false);
		var usable_height = $(window).height() - login_height - header_height - alert_height - sidebar_margin - 1;
		
	// activate again if the footer is needed	
//	 	var usable_height = $(window).height() - header_height - footer_height - sidebar_margin - 1;
		$("#sidebar").height( usable_height );
		$("#outer_content").height( usable_height );
	}

	MainController.prototype.displayAlert = function(error, resourcePath){
		var thisMainController = this;
		var errorJson = error.responseJSON;
		var encodedTitle = this.encodeToHTML(errorJson.title);
		var encodedMsg = this.encodeToHTML(errorJson["status.message"]);
		var errorMsg = encodedTitle+" ("+"Status "+errorJson["status.code"]+") "+encodedMsg;
		$('#alertMsg').append($("<div id='Message'>").append((resourcePath) ? "'"+resourcePath+"': "+errorMsg : errorMsg));
		$("#alert").slideDown(function() {
			thisMainController.adjust_height();
		});
	}


	MainController.prototype.getNTFromLi = function(li){
		var nt_name = $(li).children("a").find("span span.node-type").text();
	    return this.ntManager.getNodeType(nt_name);	
	}
	
	MainController.prototype.redirectTo = function(unencodedTargetPath){
		var newURIencoded = this.encodeURL(unencodedTargetPath);
  	  	var target = this.settings.contextPath+"/reseditor"+newURIencoded;
  	  	location.href=target+".html";
	}
	
	return MainController;
}());
