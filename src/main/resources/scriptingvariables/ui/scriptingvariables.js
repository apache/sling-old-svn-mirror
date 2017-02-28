/* 
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function () {
	$(document).ready(function () {
		$('#submitButton').on('click', function (e) {
			e.preventDefault();
			lookupVariables($("input[name='form.path']").val(), $("select[name='form.extension']").val());
		});
	});

	function renderContent(variables) {
		var myTable = "";
		for (var engineIndex = 0; engineIndex < variables.length; engineIndex++) {
		    engineSection = variables[engineIndex];
			myTable += "<br/><div class='ui-widget-header ui-corner-top buttonGroup' style='height: 15px'>"
				+ "<span style='float: left; margin-left: 1em;'>Variables for engine '" + engineSection.engine +"' (extensions: "+ engineSection.extensions.join() +")</span>"
				+ "</div>"
				+ "<table class='nicetable ui-widget'>" + "<tbody>"
				+ "<thead><tr><th class='ui-widget-header'>Name </th> <th class='ui-widget-header'>Class</th></tr></thead>";
			
			for (var variableIndex = 0; variableIndex < engineSection.bindings.length; variableIndex++) {
				myTable += produceTableRow(engineSection.bindings[variableIndex], variableIndex);
			}
			myTable += "</table>";
		}
		return myTable;
	}

	function lookupVariables(path, extension) {
		if (/^\//.test(path)) {
			$.ajax(appendSelectorToPath(path) + "?extension="+extension,
					{
						type: 'GET'
					}
			).success(
					function (data) {
						$('#response').html(renderContent(data));
					}
			).fail(
					function () {
						$('#response').html('No scripting context available under provided path.');
					}
			);
		} else {
			$('#response').html('Invalid path given.');
		}
	}

	function appendSelectorToPath(path) {
		return path + ".SLING_availablebindings.json";
	}

    
	function produceTableRow(variable, i) {
		return "<tr class='" + (i % 2 === 0 ? "even" : "odd") + " ui-state-default'>"
				+ "<td>" + variable.name + "</td>"
				+ "<td><code>" + variable.class + "</code></td>"
				+ "</tr>";
	}
})();

