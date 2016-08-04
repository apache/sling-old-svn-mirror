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
<%@ page import="javax.jcr.*,org.apache.sling.api.resource.Resource, org.apache.sling.api.resource.ValueMap"%>
<%@ page import="java.security.Principal"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling"%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<sling:defineObjects />
<html lang="en">
<head>
<title>Apache Sling Resource Editor</title>
<meta http-equiv="content-type" content="text/html; charset=utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">

<link href='<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/css/font.css' rel='stylesheet' type='text/css'>
 <!--[if lt IE 9]>
<link href='<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/css/font_ie.css' rel='stylesheet' type='text/css'>
  <![endif]--> 
 
 <!--[if IE]>
<link href='<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/css/ie.css' rel='stylesheet' type='text/css'>
  <![endif]-->
  
<!-- 
original 
<link href='http://fonts.googleapis.com/css?family=Michroma' rel='stylesheet' type='text/css'>
 -->

<script type="text/javascript" src="<%= request.getContextPath() %>/libs/jsnodetypes/js/jsnodetypes.js"></script>

<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/generated/3rd_party/js/jquery.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/generated/3rd_party/js/bootstrap.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/generated/3rd_party/js/bootbox.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/generated/3rd_party/js/jstree.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/generated/3rd_party/js/select2.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/generated/3rd_party/js/bootstrap-notify.min.js"></script>

<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/tree/JSTreeAdapter.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/tree/TreeController.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/tree/AddNodeController.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/properties/PropertyController.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/LoginController.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/MainController.js"></script>

<!-- 
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/generated/3rd_party/js/jquery.scrollTo-min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/generated/3rd_party/js/urlEncode.js"></script>
 -->

<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/generated/3rd_party/css/style.min.css">
<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/css/jstree.reseditor.css">
<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/generated/css/bootstrap.css">
<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/css/bootbox.reseditor.css">
<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/generated/3rd_party/css/animate.min.css">
<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/generated/3rd_party/css/select2.css">
<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/css/select2.reseditor.css">

<!--[if IE]>
	<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/css/browser_ie.css"/>
<![endif]-->

<%
Principal userPrincipal = ((HttpServletRequest)pageContext.getRequest()).getUserPrincipal();
%>
<c:set var="authorized" value='<%=!"anonymous".equals(userPrincipal.getName()) %>'/>
<c:set var="userPrincipal" value='<%=userPrincipal %>'/>

<script type="text/javascript">
var ntManager = new org.apache.sling.jcr.js.nodetypes.NodeTypeManager();

var mainControllerSettings = {
		contextPath: "<%= request.getContextPath() %>",
		nodeTypes: ntManager.getNodeTypeNames(),
		errorStatus: '${requestScope["javax.servlet.error.status_code"]}',
		errorMessage: '<%=request.getAttribute("javax.servlet.error.message") == null ? "" : request.getAttribute("javax.servlet.error.message") %>'
};
var mainController = new org.apache.sling.reseditor.MainController(mainControllerSettings, ntManager);

var treeControllerSettings = {
		contextPath: "<%= request.getContextPath() %>"
};
var treeController = new org.apache.sling.reseditor.TreeController(treeControllerSettings, mainController);

var loginControllerSettings = {
		authorized : ${authorized},
		authorizedUser : '${userPrincipal.name}',
		contextPath: "<%= request.getContextPath() %>"
};
var loginController = new org.apache.sling.reseditor.LoginController(loginControllerSettings, mainController);

var jsTreeAdapterSettings = {
		resourcePath : "${resource.path}",
		requestURI: "${pageContext.request.requestURI}",
		contextPath: "<%= request.getContextPath() %>",
		resolutionPathInfo: "${resource.resourceMetadata['sling.resolutionPathInfo']}"
};
new org.apache.sling.reseditor.JSTreeAdapter(jsTreeAdapterSettings, treeController, mainController);

new org.apache.sling.reseditor.PropertyController({}, mainController);

</script>	

</head>
<body>
	<div id="container-fluid" class="container-fluid">
		<div id="login" class="row">
			<div class="col-sm-12">
			 	<div class="logo">
				The Sling Resource Editor <span class="edition">build with passion</span>
				</div>			 	
				<div class="tabbable tabs-below"> 
				  <div id="login_tab_content" class="tab-content plate-background plate-box-shadow" style="display:none;">
				    <div class="tab-pane active">
						<div>
			                <form id="login_form" class="form-horizontal" action="/j_security_check" method="post">
			                        <div class="form-group">
										<div class="controls">
						                    <input class="form-control" type="hidden" value="${pageContext.request.requestURI}" name="resource" />
					                        <input class="form-control" type="hidden" value="form" name="selectedAuthType" />
											<input class="form-control" type="hidden" value="UTF-8" name="_charset_">
										</div>
									</div>
			                        <div class="form-group">
										<label class="control-label" for="j_username">Username:</label>
										<div class="controls">
											<input class="form-control" type="text" name="j_username" />
										</div>
									</div>
			                        <div class="form-group">
										<label class="control-label" for="j_password">Password:</label>
										<div class="controls">
											<input class="form-control" type="password" name="j_password" />
										</div>
									</div>
			                        <div class="form-group error">
										<div class="controls">
			                        		<span id="login_error" class="help-block alert-warning"></span>
										</div>
									</div>
			                        <div class="form-group" id="login_submit_control_group">
										<div class="controls">
			                        		<input id="login_submit" type="button" class="btn btn-default form-control" value="Login" >
										</div>
									</div>
			                </form>
						</div>
				    </div>
				  </div>
				  
				  <ul class="nav nav-tabs">
				    <li class="active">
				    	<a id="login_tab" href="#login_tab_content" data-toggle="tab">Login</a>
				    </li>
				  </ul>
				</div>
			</div>
		</div>
		<div id="header" class="row">
			<div class="col-sm-12" style="display:none;">
				 <div class="plate">
				</div> 
			</div>
		</div>
		<div id="alerts" class="row">
			<div id="alert" style="display:none;" class="col-sm-12">
			  	<div id="alertMsg" class="alert alert-error alert-warning alert-dismissable">
			  		<button id="alertClose" type="button" class="close">&times;</button>
			  		<h4>Error</h4>
		  		</div>
		  	</div>		
		</div>
		<div id="main-row" class="row">
			<div id="sidebar-col" class="col-sm-4">
				<div id="sidebar" class="plate">
					<div class="ie9filter-plate-div">
						<div style="display:none;" class="info-content-container" >
							<div class="well well-sm info-content">
								<button type="button" class="close"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
							  	<h4>Cheat Sheet</h4>
						  		<p>You can use</p>
						  		<ul>
						  			<li>the arrow keys <kbd>&#x25C0;</kbd>,<kbd>&#x25B6;</kbd>,<kbd>&#x25B2;</kbd>,<kbd>&#x25BC;</kbd> for navigation. You can keep them pressed for faster navigation.</li>
						  			<li>the <kbd>space</kbd> key for selecting nodes.</li>
						  			<li>the <kbd>cmd</kbd> key for Mac or <kbd>ctrl</kbd> key for non Mac systems for multi selecting single nodes.</li>
						  			<li>the <kbd>shift</kbd> key for multi selecting a list of nodes.</li>
						  			<li>the <kbd>del</kbd> key for deleting the selected nodes.</li>
						  			<li>the <kbd>c</kbd> key on a node when the tree has the focus for opening the dialog to add a child node.</li>
						  			<li><kbd><kbd>ctrl</kbd>+<kbd>c</kbd></kbd> and <kbd><kbd>ctrl</kbd>+<kbd>v</kbd></kbd> can be used to copy and paste a node.</li>
						  			<li>a double click to rename a node. Special JCR characters like ':' are not allowed as node names.</li>
						  		</ul>
							</div>
					  	</div>	
						<div id="tree" class="root" ></div>
					</div>
				</div>
			</div>
			<%@ include file="node-content.jsp" %>
	    </div> 
		<div class="row" style="visibility:hidden; display:none;">
			<div class="col-sm-12">
				 <div id="footer" class="plate">
						<p>I'm looking forward to be filled with useful content</p>
				</div>
			</div>
		</div>
	</div>
	<span id="tree-info-icon" class="info-icon info-icon-lightgray pull-right clearfix" style="display:none;"></span>
	<!-- Add node dialog -->
	<div class="modal fade" id="addNodeDialog" tabindex="-1" role="dialog" aria-labelledby="addNodeDialogLabel" aria-hidden="true">
	  <div class="modal-dialog modal-sm">
	    <div class="modal-content">
		      <div class="modal-header">
		        <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
		        <h4 class="modal-title" id="addNodeDialogLabel">Add Child Node
			  		<span class="info-icon info-icon-dark pull-right"></span>
		        </h4>
		      </div>
		      <div class="modal-body vertical-form">
					  <div id="cheatsheet">
							<div id="cheatsheet-content" style="display:none;" class="well well-sm info-content">
								<button type="button" class="close"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
							  	<h3>Cheat Sheet</h3>
							  	<h4>Shortcuts</h4>
						  		<p>Submitting the dialog is only allowed if no search dialog is open and the fields are set.</p>
						  		<p>You can use the</p>
						  		<ul>
					  				<li><kbd>c</kbd> key on a node when the tree has the focus for opening the dialog to add a child node.</li>
					  				<li><kbd>esc</kbd> key to close the dialog.</li>
						  			<li><kbd><kbd>ctrl</kbd>+<kbd>a</kbd></kbd> keys to submit the form.</li>
						  			<li><kbd>tab</kbd> key to navigate from one form element to the next one and <kbd><kbd>shift</kbd>+<kbd>tab</kbd></kbd> to navigate back.</li>
						  		</ul>
						  		<h4>Defaults</h4>
						  		<p>Defaults are used for empty fields if you submit the dialog.</p>
						  		<h4>New dropdown entries</h4>
						  		<ul>
						  			<li>New node names are allowed for most of the node types.</li>
						  			<li>It's not allowed to enter new node type names.</li>
					  				<li>You can enter new values for resource types if the node type allows for the 'sling:resourceType' String property.</li>
						  		</ul>
						  		<h4>Searching dropdown entries</h4>
						  		<p>Entering text in the dropdown boxes searches for entries that contain that text.</p>
						  	</div>		
					  </div>
					  <div class="form-group">
					    <label for="nodeName">Node Name</label>
					    <input name="nodeName" type="hidden" id="nodeName">
					    <span class="only-listed-node-names-allowed help-block" style="display:none;">Only the node names listed in the drop down box are allowed.</span>
					  </div>
					  <div class="form-group node-type">
					    <label for="nodeType">Node Type - <span class="nt-dependency-description">applicable together with node name</span> (<a class="nt-toggle" href="javascript:void(0)">show generally applicable</a>)</label>
					    <input name="jcr:primaryType" type="hidden" id="nodeType">
					  </div>
					  <div class="form-group resource-type">
					    <label for="resourceType">Sling Resource Type</label>
					    <input name="sling:resourceType" type="hidden" id="resourceType">
					    <span class="resource-type-not-allowed help-block" style="display:none;">The selected node type does not allow the resulting node to have a Sling resource type property.</span>
					  </div>
		      </div>
		      <div class="modal-footer">
		        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
		        <button type="button" class="btn btn-primary submit">Save changes</button>
		      </div>
	    </div>
	  </div>
	</div>
	<div id="last-element"></div>
</body>
</html>
