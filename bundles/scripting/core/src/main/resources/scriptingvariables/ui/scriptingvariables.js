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
			lookupVariables($("input[name='form.path']").val());
		});
	});

	function renderContent(variables) {
		var myTable = "<br/><p class='ui-widget-header'>Available base variables</p>"
				+ "<table class='nicetable ui-widget-content'>" + "<tbody id='baseVariables'>"
				+ "<thead><tr><th>Name </th> <th> Class </th> <th> Scope</th></tr></thead>";

		for (var i = 0; i < variables.baseVariables.length; i++) {
			myTable += produceTableRow(variables.baseVariables[i], i);
		}

		myTable += "</table><br/><p class='ui-widget-header'>Available custom variables</p>"
				+ "<table class='nicetable ui-widget-content'>"
				+ "<thead><tr><th>Name </th> <th> Class </th> <th> Scope</th></tr></thead>";

		for (var i = 0; i < variables.customVariables.length; i++) {
			myTable += produceTableRow(variables.customVariables[i], i);
		}
		myTable += "</table>";
		return myTable;
	}

	function lookupVariables(path) {
		$.ajax(appendSelectorToPath(path),
				{
					type: 'GET'
				}
		).success(
				function (data) {
					$('#response').html(renderContent(data));
				}
		).fail(
				function () {
					$('#response').html('No scirpting context available under provided path.');
				}
		);
	}

	function appendSelectorToPath(path) {
		var uriLastPart = path.substring(path.lastIndexOf('/'), path.length + 1);
		if (uriLastPart.indexOf('.updateBVP.') === -1) {
			var uriLastPartWithSelector = uriLastPart.replace('.', '.bvpvars.');
			return path.replace(uriLastPart, uriLastPartWithSelector);
		}
		else {
			return path;
		}
	}

	function produceTableRow(variable, i) {
		return "<tr class='" + (i % 2 === 0 ? "even" : "odd") + " ui-state-default'>"
				+ "<td><code>" + variable.name + "</code></td>"
				+ "<td><code>" + variable.description + "</code></td>"
				+ "<td><code>" + variable.scope + "</code></td>"
				+ "</tr>";
	}
})();

