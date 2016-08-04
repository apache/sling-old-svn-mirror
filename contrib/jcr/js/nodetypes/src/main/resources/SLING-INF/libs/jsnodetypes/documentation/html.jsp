<%--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
--%>

<!DOCTYPE html>
<%@ page session="false"%>
<%@ page isELIgnored="false"%>
<%@ page import="javax.jcr.*,org.apache.sling.api.resource.Resource"%>
<%@ taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0"%>
<sling:defineObjects />
<html lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">


<script type="text/javascript" src="../js/jsnodetypes.js"></script>

<style type="text/css">
#main {
    border: 1px dashed;
    margin: 0 auto;
    padding: 10px;
    width: 1000px;
}
input {
	margin: 10px 0;
}
.label {
	margin: 5px;
}
#ntNames {
	white-space: pre-wrap;
}
#ntJson {
	display: inline;
}
.code.block-code {
	display: block;
}
.code {
	border: 1px solid black;
	display: inline-block;
	vertical-align: top;
	margin-top: 10px;
}
.code pre {
	margin: 5px;
}
.nodeTypeMethods {
	display: inline-block;
	margin: 0 10px 10px;
	width: 350px;
}
.doc {
	margin-bottom: 15px;
}
.parameter {
	margin-left: 10px;
}

</style>

<script type="text/javascript">

var settings = {"contextPath": "${pageContext.request.contextPath}"};
var ntManager = new org.apache.sling.jcr.js.nodetypes.NodeTypeManager(settings);

function getNTNames(){
	var ntNames = ntManager.getNodeTypeNames();
	var ntNamesString = "[ ";
	for (var ntNameIndex in ntNames) {
		ntNamesString+= ntNames[ntNameIndex]
		if (ntNameIndex != ntNames.length-1) {
			ntNamesString += ", ";
		}
	}
	ntNamesString+= " ]";
	document.getElementById("ntNames").innerHTML=ntNamesString;
};

function getNTJson(){
	var ntName = document.getElementById("ntName").value;
	return ntManager.getNodeType(ntName);
};

function loadNTJson(){
	document.getElementById("ntJson").innerHTML=JSON.stringify(getNTJson(), null, 4);
};
function loadChildNodeDefs(){
	var ntJson = getNTJson();
	if (ntJson != null){
		var allChildNodeDefs = ntJson.getAllChildNodeDefinitions();
		document.getElementById("ntMethodResult").innerHTML=JSON.stringify(allChildNodeDefs, null, 4);
	}
};
function loadPropertyDefs(){
	var ntJson = getNTJson();
	if (ntJson != null){
		var propertyDefs = ntJson.getAllPropertyDefinitions();
		document.getElementById("ntMethodResult").innerHTML=JSON.stringify(propertyDefs, null, 4);
	}
};
function loadApplicableChildNodeTypes(){
	var ntJson = getNTJson();
	if (ntJson != null){
		var includeMixins = document.getElementById("includeMixins").checked;
		var applicableChildNodeTypes = ntJson.getApplicableCnTypesPerCnDef(includeMixins);
		document.getElementById("ntMethodResult").innerHTML=JSON.stringify(applicableChildNodeTypes, null, 4);
	}
};
function canAddChildNode(){
	var ntJson = getNTJson();
	if (ntJson != null){
		var nodeName = document.getElementById("nodeName").value;
		var nodeTypeToAdd = document.getElementById("nodeTypeToAdd").value;
		var canAddChildNode = ntJson.canAddChildNode(nodeName, ntManager.getNodeType(nodeTypeToAdd));
		document.getElementById("ntMethodResult").innerHTML=canAddChildNode;
	}
}
function canAddProperty(){
	var ntJson = getNTJson();
	if (ntJson != null){
		var propertyName = document.getElementById("propertyName").value;
		var propertyType = document.getElementById("propertyType").value;
		var canAddProperty = ntJson.canAddProperty(propertyName, propertyType);
		document.getElementById("ntMethodResult").innerHTML=canAddProperty;
	}
}
</script>

</head>
<body>
	<div id="main">
		<h1><em>JSNodeTypes</em> - A JavaScript Node Types library for Apache Sling</h1>
		<p>It uses Apache Sling to generate JavaScript object literals for JCR node types and calculates applicable child nodes and properties. Default binary values are converted to paths where they can be downloaded.</p>
		<h2>Live Demo of the API</h2>
		<p>Have a look at the simple source code of this page to see how it's done.</p>
		<h3>Methods of the node type manager:</h3>
		<div>
			<input id="ntNamesButton" type="button" value="ntManager.getNodeTypeNames();" onclick="getNTNames();"/>
		</div>
		<div class="block-code code">
			<pre id="ntNames">[]</pre>
		</div>
		
		<div>
			<input id="ntButton" type="button" value="ntManager.getNodeType" onclick="loadNTJson();"/>
			(<input id="ntName" type="text" value="nt:version"/>);
		</div>
		<div>
			<div class="code">
				<pre id="ntJson">{}</pre>
			</div>
		</div>
		<h3>Methods of the node type object selected above:</h3>
		<ul class="nodeTypeMethods">
	    	<li>
	    		<input type="button" value="getAllChildNodeDefinitions();" onclick="loadChildNodeDefs();"/>
				<span class="doc">That method returns the <strong>child node definitions</strong> of the node type and those <strong>of all inherited node types</strong>.<br/>Definitions with the same name are not overwritten but aggregated. If they are equal they are only listed once.</span>
			</li>
			<li>
				<input type="button" value="getAllPropertyDefinitions();" onclick="loadPropertyDefs();"/>
				<span class="doc">That method returns the <strong>property definitions</strong> of the node type and those <strong>of all inherited node types</strong>.<br/>Definitions with the same name are not overwritten but aggregated. If they are equal they are only listed once.<br/></span>
			</li>
			<li><input type="button" value="canAddChildNode" onclick="canAddChildNode();"/>
					(<input type="text" value="nodeName" id="nodeName" class="parameter"/>,
					<span style="display:inline-block" class="parameter">ntManager.getNodeType(<input type="select" value="nodeTypeToAdd" id="nodeTypeToAdd" class="parameter"/>)</span>);
				<span class="doc"><br>That method returns `true` if a node with the specified node name and node type can be added as a child node of the current node type. The residual definitions and subtypes are considered.<br/>The <strong>first parameter is the string</strong> of the node name and the <strong>second parameter is a node type object</strong> (not a string).</span>
			</li>
			<li><input type="button" value="canAddProperty" onclick="canAddProperty();"/>
					(<input type="text" value="propertyName" id="propertyName" class="parameter"/>, <input type="text" value="propertyType" id="propertyType" class="parameter"/>);
				<span class="doc"><br>That method returns `true` if a property with the specified name and type can be to the current node type. The residual definitions and undefined types are considered.<br/>The <strong>first parameter is the string</strong> of the property name and the <strong>second parameter is the property type</strong> (case insensitive).</span>
			</li>
			<li>
				<input type="button" value="getApplicableCnTypesPerCnDef()" onclick="loadApplicableChildNodeTypes();"/>
					(<input type="checkbox" value="includeMixins" id="includeMixins" class="parameter"/> includeMixins);
				<span class="doc"><br>Returns all node types that can be used for child nodes of this node type and its super types.<br/>If a child node definition specifies multiple required primary types, only node types that are subtypes of all of them are applicable.</br>The keys on the first level are the names of the child node definitions. Its values / the keys on the second level contain the node type names and its values in turn contain the node type definition itself. The <strong>parameter is a boolean</strong> and specifies if mixin types should be included in the result.</span>
			</li>
		</ul>
		<div class="code">
			<pre id="ntMethodResult">{}</pre>
		</div>
		<h2>Use</h2>
		<ol>
			<li>
				<p>Include the JavaScript file to your page:</p>		
				<div class="code">
					<pre class="JavaScript">&lt;script type="text/javascript" src="/libs/jsnodetypes/js/jsnodetypes.js"&gt;&lt;/script&gt;</pre>
				</div>
			</li>
			<li>
				<p>Instantiate the NodeTypeManager:</p>
				<div class="code">
					<pre class="JavaScript">// this works if your WAR is deployed under the root context '/'
var ntManager = new org.apache.sling.jcr.js.nodetypes.NodeTypeManager();
					</pre>
				</div>
				<p>If your WAR is not deployed under the root context you have to specify it:</p>
				<div class="code">
					<pre class="JavaScript">var settings = {"contextPath": "/yourContextPath"};
var ntManager = new org.apache.sling.jcr.js.nodetypes.NodeTypeManager(settings);
					</pre>
				</div>
			</li>
			<li>
				<p>Use the NodeTypeManager instance like described above:</p>
				<div class="code">
					<pre class="JavaScript">var nodeTypesArray = ntManager.getNodeTypeNames();
var firstNodeType = ntManager.getNodeType(nodeTypesArray[0]);
var allChildNodeDefs = firstNodeType.getAllChildNodeDefinitions();
var allPropertyDefs = firstNodeType.getAllPropertyDefinitions();
var canAddChildNode = firstNodeType.canAddChildNode("myNodeName", nodeTypesArray[1]);
					</pre>
				</div>
			</li>
		</ol>
		<h2>Version History</h2>
		<ul>
			<li><strong>0.1.0 (org.apache.sling.jcr.js.nodetypes)</strong>
				<ul>
					<li><strong>Non backwards compatible changes from SLING-5541:</strong>
						<ul>
							<li>New artifact name 'org.apache.sling.jcr.js.nodetypes'</li>
							<li>Moved Java and JavaScript packages from org.apache.sling.commons and from de.sandroboehme to org.apache.sling.jcr.</li>
						</ul>
					</li>
					<li>Other fixes for SLING-5541.
						<ul>
							<li>Moved out of contrib/commons.</li>
							<li>Changed this documentation page to reflect begin hosted at Apache Sling.</li>
						</ul>
					</li>
				</ul>
			</li>
			<li><strong>4.0.1</strong>
				<ul>
					<li><strong>Non backwards compatible change:</strong>Renamed getApplicableChildNodeTypes() to getApplicableCnTypesPerCnDef() to allow other similar methods and still have no overlapping meanings.</li>
				</ul>
			</li>
			<li><strong>3.0.2</strong>
				<ul>
					<li>Fixed canAddChildNode() to check node name and node type combination and to return 'true' also for subtypes of a requiredPrimaryType.</li>
					<li>Fixed canAddChildNode() to handle the protected property correctly</li>
					<li>Added includeMixins parameter to getApplicableChildNodeTypes()</li>
					<li>Added canAddProperty()</li>
				</ul>
			</li>
			<li><strong>3.0.1</strong>
				<ul>
					<li>canAddChildNode(): checking if the provided node type is null</li>
				</ul>
			</li>
			<li><strong>3.0</strong>
				<ul>
					<li>Rework of almost all of the runtime source code to remove the Google Gson dependency and use the sling.commons.json dependency instead as the latter it is already part of the Sling launchpad and Gson is not.</li>
					<li>Added the new types of JCR 2.0: 'weakReference', 'uri', 'undefined' and 'decimal' as types for default values.</li>
					<li><strong>Non backwards compatible changes:</strong> Changed the format of the default values:
						<ul>
							<li>The 'name' value can now be found with the 'name' key. Not with the 'string' key anymore.</li>
							<li>The 'path' value can now be found with the 'path' key. Not with the 'string' key anymore.</li>
							<li>The 'reference' value can now be found with the 'reference' key. Not with the 'string' key anymore.</li>
							<li>The format of the 'date' value changed to provide more information from the Calendar object that is retrieved from the repository. Until this version the date value contained a JavaScript
							object like this: '{"year": 2012, "month": 1, "dayOfMonth": 1, "hourOfDay": 0, "minute": 0, "second": 0}' and now it is a String containing an ISO8601 date. E.g. "2012-02-01T00:00:00.000+01:00". 
							For the date ISO8601 conversion the dependency to 'jackrabbit-jcr-commons' has been added which is already part of the sling launchpad. 
							</li>
							<li>Empty 'declaredChildNodeDefinitions', 'declaredSupertypes' and 'declaredPropertyDefinitions' are not part of the JSON output anymore.</li>
						</ul>
					</li>
				</ul>
			</li>
			<li><strong>2.0</strong>
				<ul>
					<li>getApplicableChildNodeTypes() added</li>
					<li><strong>Non backwards compatible changes:</strong> getAllChildNodeDefinitions() and getAllPropertyDefinitions() changed to now return definitions with the same name as well. But only if they differ in any other way.</li>
				</ul>
			</li>
			<li><strong>1.0</strong>
				<ul>
					<li>Initial version</li>
				</ul>
			</li>
		</ul>
		<h2>Architecture</h2>
	 	<p>The JavaScript NodeTypeManager is developed in an object oriented way. It is instantiated in its own namespace and then loads <a href="${pageContext.request.contextPath}/libs/jsnodetypes/content/nodetypes.json">all available node types from the server in the JSON format</a>.</p> 
	 	<p>This is handled by the <code>org.apache.sling.jcr.js.nodetypes.NodeTypesJSONServlet</code> at the server side. It</p> 
	 	<ul>
	 		<li>reads the node types from the repository</li>
	 		<li>converts them to JSON</li>
	 		<li>replaces the default binary values with URL's where they can later be downloaded from</li>
	 		<li>removes the  <a href="${pageContext.request.contextPath}/libs/jsnodetypes/js/defaultNT/defaultNT.json">default values</a> to have smaller node type objects</li>
	 		<li>and returns the result back to the JavaScript client.</li>
	 	</ul>
	 	<p>When <code>ntManager.getNodeType(nodeTypeName)</code> is called at the client side, the defaults are added again and 
	 	the methods are added to the JSON object / JavaScript object literal and are finally returned.
	 	</p>
	 	<p>The internal 'processNodeTypeGraph()' method in jsnodetype.js is the basis for the other methods as it collects the needed data.</p>
		<h2>Tests</h2>
		<p>All JavaScript tests and Java tests are run in the Maven test phase as usual.
		<h3>JavaScript</h3>
		<p>The JavaScript tests are implemented in <code>src/test/javascript/NodeTypesSpec.js</code> using <a href="http://pivotal.github.com/jasmine/">Jasmine</a>. When you call <code>mvn jasmine:bdd</code> you can edit the tests and refresh the browser at <code>http://localhost:8234</code> to rerun the tests. 
		<h3>Java</h3>
		<p>The Java tests can be found in <code>src/test/java/org/apache/sling/jcr/js/nodetypes</code>. They query the <code>org.apache.sling.jcr.js.nodetypes.NodeTypesJSONServlet</code> while mocking the <code>javax.jcr.nodetype.NodeTypeManager</code> using <a href="http://docs.mockito.googlecode.com/hg/latest/org/mockito/Mockito.html">Mockito</a>.
		The result is then compared to the expected values in the <code>src/test/resources/expectedNTJSON/*.json</code> files using <code>org.apache.sling.jcr.js.nodetypes.testJSONAssert</code>. This class is actually copied from 
		<code>org.apache.sling.commons.json.test.JSONAssert</code>. If somebody knows a better way to reuse this class please open an bug and let me know.
		</p>
		<h2>Build</h2>
		<p>Please read the instructions in the README.txt file for instructions on how to build the project.</p>
		<h2>License</h2>
		<p>This library is licensed under the terms of the <a href="http://www.apache.org/licenses/LICENSE-2.0.html">Apache 2 license</a>.</p>
		<h2>Compatibility</h2>
		<p>This library has out of the box support for: IE 8.0 standards mode, Firefox 20.0, Chrome 26.0, Opera 12.1 and Safari 5.1</p>
		<h3>Support for older browser versions</h3>
		<p>If you would like to support older browser versions you can download the <a href="https://raw.github.com/douglascrockford/JSON-js/master/json2.js">json2.js script</a> and include it like that:</p>
		<div class="code">
			<pre class="JavaScript">&lt;script type="text/javascript" src="/path/to/json2.js"&gt;&lt;/script&gt;</pre>
		</div>
	</div>
</body>
</html>