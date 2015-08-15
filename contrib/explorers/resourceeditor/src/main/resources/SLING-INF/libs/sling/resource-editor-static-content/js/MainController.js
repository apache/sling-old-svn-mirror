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
			var hasError = typeof thisMainController.settings.errorMessage != "undefined" && thisMainController.settings.errorMessage != "" && thisMainController.settings.errorMessage != null;
			if (hasError){
				thisMainController.displayAlertHtml("Status "+thisMainController.settings.errorStatus+". "+thisMainController.settings.errorMessage);
			}
		});
	};

	// From https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/keys
	if (!Object.keys) {
	  Object.keys = (function() {
	    'use strict';
	    var hasOwnProperty = Object.prototype.hasOwnProperty,
	        hasDontEnumBug = !({ toString: null }).propertyIsEnumerable('toString'),
	        dontEnums = [
	          'toString',
	          'toLocaleString',
	          'valueOf',
	          'hasOwnProperty',
	          'isPrototypeOf',
	          'propertyIsEnumerable',
	          'constructor'
	        ],
	        dontEnumsLength = dontEnums.length;

	    return function(obj) {
	      if (typeof obj !== 'object' && (typeof obj !== 'function' || obj === null)) {
	        throw new TypeError('Object.keys called on non-object');
	      }

	      var result = [], prop, i;

	      for (prop in obj) {
	        if (hasOwnProperty.call(obj, prop)) {
	          result.push(prop);
	        }
	      }

	      if (hasDontEnumBug) {
	        for (i = 0; i < dontEnumsLength; i++) {
	          if (hasOwnProperty.call(obj, dontEnums[i])) {
	            result.push(dontEnums[i]);
	          }
	        }
	      }
	      return result;
	    };
	  }());
	};

	/* adding an indexOf function if it's not available */
	if (!Array.prototype.indexOf) {
	    Array.prototype.indexOf = function (searchElement /*, fromIndex */ ) {
	        "use strict";
	        if (this == null) {
	            throw new TypeError();
	        }
	        var t = Object(this);
	        var len = t.length >>> 0;
	        if (len === 0) {
	            return -1;
	        }
	        var n = 0;
	        if (arguments.length > 1) {
	            n = Number(arguments[1]);
	            if (n != n) { // shortcut for verifying if it's NaN
	                n = 0;
	            } else if (n != 0 && n != Infinity && n != -Infinity) {
	                n = (n > 0 || -1) * Math.floor(Math.abs(n));
	            }
	        }
	        if (n >= len) {
	            return -1;
	        }
	        var k = n >= 0 ? n : Math.max(len - Math.abs(n), 0);
	        for (; k < len; k++) {
	            if (k in t && t[k] === searchElement) {
	                return k;
	            }
	        }
	        return -1;
	    }
	}
	
	MainController.prototype.getNodeTypes = function(){
		return this.settings.nodeTypes;
	}

	MainController.prototype.getContextPath = function(){
		return this.settings.contextPath=="" ? "/" : this.settings.contextPath;
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
		var content_tab_height = $("#content-tabs").outerHeight(true);
		var footer_height = $("#footer").outerHeight(true);
		var sidebar_margin = $("#sidebar").outerHeight(true)-$("#sidebar").outerHeight(false);
		var mainrow_margin = $("#main-row").outerHeight(true)-$("#main-row").outerHeight(false);
		var usable_height = $(window).height() - login_height - header_height - alert_height - sidebar_margin - mainrow_margin - 15;
		
	// activate again if the footer is needed	
//	 	var usable_height = $(window).height() - header_height - footer_height - sidebar_margin - 1;
		$("#sidebar").height( usable_height );
		$("#outer_content").height( usable_height-content_tab_height );
	}

	MainController.prototype.displayAlert = function(error, resourcePath){
		var thisMainController = this;
		var errorJson = error.responseJSON;
		var encodedTitle = this.encodeToHTML(errorJson.title);
		var encodedMsg = this.encodeToHTML(errorJson["status.message"]);
		var errorMsg = encodedTitle+" ("+"Status "+errorJson["status.code"]+") "+encodedMsg;
		this.displayAlertHtml((resourcePath) ? "'"+resourcePath+"': "+errorMsg : errorMsg);
	}

	MainController.prototype.displayAlertHtml = function(html){
		var thisMainController = this;
		$('#alertMsg').append($("<div id='Message'>").append(html));
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
