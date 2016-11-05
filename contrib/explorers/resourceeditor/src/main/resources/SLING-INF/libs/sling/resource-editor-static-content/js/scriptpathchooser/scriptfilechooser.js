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

/*
 * Represents the functionality for the file chooser within the script path chooser.
 */

// creating the namespace
var org = org || {};
org.apache = org.apache || {};
org.apache.sling = org.apache.sling || {};
org.apache.sling.sitebuilder = org.apache.sling.sitebuilder || {};

//defining the module
org.apache.sling.sitebuilder.Scriptfilechooser = (function() {

	function Scriptfilechooser(){
		this.selectedScriptResourcePath = "";
		var thisScriptfilechooser = this;
		$('#page-script-chooser-row .files').on("click", "td", function(e) {
			thisScriptfilechooser.selectedPageScriptPath = $(this).text();
			var selectedPageScriptFilePath = thisScriptfilechooser.selectedScriptResourcePath+"/"+$(this).text()
			$('#selected-script-path-row input[name="selectedScriptFilePath"]').val(selectedPageScriptFilePath);
			$('#selected-script-path-row input[name="resourceSuperType"]').val(thisScriptfilechooser.selectedScriptResourcePath);
		});
	}
	Scriptfilechooser.prototype.setSelectedScriptResourcePath = function(selectedScriptResourcePath) {
		this.selectedScriptResourcePath = selectedScriptResourcePath;
	}

	return Scriptfilechooser;
}());