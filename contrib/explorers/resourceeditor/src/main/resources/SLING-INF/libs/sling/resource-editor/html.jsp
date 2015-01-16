<!DOCTYPE html>
<%@ page session="false"%>
<%@ page isELIgnored="false"%>
<%@ page import="javax.jcr.*,org.apache.sling.api.resource.Resource, org.apache.sling.api.resource.ValueMap"%>
<%@ page import="java.security.Principal"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0"%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<sling:defineObjects />
<html lang="en">
<head>
<meta http-equiv="content-type" content="text/html; charset=utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">

<link href='<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/css/font.css' rel='stylesheet' type='text/css'>
 <!--[if lt IE 9]>
<link href='<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/css/font_ie.css' rel='stylesheet' type='text/css'>
  <![endif]-->
  
<!-- 
original 
<link href='http://fonts.googleapis.com/css?family=Michroma' rel='stylesheet' type='text/css'>
 -->

<script type="text/javascript" src="<%= request.getContextPath() %>/libs/jsnodetypes/js/jsnodetypes.js"></script>

<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/jquery.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/bootstrap.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/bootbox.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/jstree.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/select2.min.js"></script>

<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/reseditor/tree/JSTreeAdapter.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/reseditor/tree/TreeController.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/reseditor/tree/AddNodeController.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/reseditor/LoginController.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/reseditor/MainController.js"></script>

<!-- 
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/jquery.scrollTo-min.js"></script>
 -->
<script type="text/javascript" src="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/js/urlEncode.js"></script>

<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/css/style.css">
<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/css/bootstrap.css">
<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/css/bootbox.reseditor.css">
<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/css/shake.css">
<link rel="stylesheet" type="text/css" media="all" href="<%= request.getContextPath() %>/libs/sling/resource-editor-static-content/css/select2.css">
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
var ntManager = new de.sandroboehme.NodeTypeManager();

var mainControllerSettings = {
		contextPath: "<%= request.getContextPath() %>",
		nodeTypes: ntManager.getNodeTypeNames() 
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

</script>	

</head>
<body>
	<div id="container-fluid" class="container-fluid">
		<div id="login" class="row">
			<div class="col-sm-12">
			 	<div class="logo">
				Sling Resource Editor <span class="edition">node-edit version</span>
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
		<div class="row">
			<div class="col-sm-4">
				<div id="sidebar" class="plate">
					<div id="tree" class="root" ></div>
				</div>
			</div>
			<div class="col-sm-8">
				<div id="outer_content" class="plate">
					<div id="inner_content_margin">
						<form action="not_configured_yet.change.properties" method="post">
							<c:set var="resourceIsNode" scope="request" value="<%=resource.adaptTo(Node.class) !=null %>"/>
							<c:choose>
							     <c:when test="${resourceIsNode}" >
									<%--
									For some reason I get the following exception when using the JSTL expression '${currentNode.properties}'
									instead of the scriptlet code 'currentNode.getProperties()':
									org.apache.sling.scripting.jsp.jasper.JasperException: Unable to compile class for JSP: 
									org.apache.sling.scripting.jsp.jasper.el.JspValueExpression cannot be resolved to a type
									see https://issues.apache.org/jira/browse/SLING-2455
									 --%>
									<c:forEach var="property" items="<%=currentNode.getProperties()%>">
								<%  Property property = (Property) pageContext.getAttribute("property");%>
										<fieldset>
											<label class="proplabel" for='${property.name}'>${property.name} [<%=PropertyType.nameFromValue(property.getType())%>${property.multiple ? ' multiple' : ''}]</label>
											<c:choose>
											     <c:when test="${property.multiple}" >
											     	<fieldset class="propmultival_fieldset">
											     		<div>&nbsp;</div>
											     	<c:forEach var="value" items="<%=property.getValues()%>">
											     		<c:choose>
											     		<c:when test="${property.type == PropertyType.BINARY}" >
													     	<p>I'm a binary property</p>
													     </c:when>
													     <c:otherwise>
												     		<input class="propinputmultival form-control" value="${value.string}"/>
													     </c:otherwise>
													     </c:choose>
											     	</c:forEach>
					     							</fieldset>
											     </c:when>
											     <c:when test="${false}" >
											     </c:when>
											     <c:otherwise>
												     <c:choose>
												     <c:when test="<%=property.getType() == PropertyType.BINARY%>" >
												     	<c:choose>
													     	<c:when test='<%=currentNode.getParent().isNodeType("nt:file") %>'>
													     		<a class="propinput" href="<%= request.getContextPath() %>${resource.parent.path}">Download</a>
													     	</c:when>
													     	<c:otherwise>
													     		<a class="propinput" href="<%= request.getContextPath() %>${resource.path}.property.download?property=${property.name}">View (choose "Save as..." to download)</a>
													     	</c:otherwise>
												     	</c:choose>
												     </c:when>
												     <c:otherwise>
														<input class="propinput form-control" id="${property.name}" name="${property.name}" value="${property.string}"/>							
												     </c:otherwise>
												     </c:choose>
											     </c:otherwise>
											 </c:choose>
										</fieldset>
									</c:forEach>
							     </c:when>
							     <c:otherwise>
									<c:forEach var="property" items="<%=resource.adaptTo(ValueMap.class)%>">	
										<fieldset>										
											<label class="proplabel" for='${property.key}'>${property.key}</label>
											<input class="propinput form-control" id="${property.key}" name="${property.key}" value="${property.value}"/>
										</fieldset>							
									</c:forEach>
							     </c:otherwise>
							 </c:choose>
						</form>
					</div>
			    </div>
			</div>
	    </div>
		<div class="row" style="visibility:hidden; display:none;">
			<div class="col-sm-12">
				 <div id="footer" class="plate">
						<p>I'm looking forward to be filled with useful content</p>
				</div>
			</div>
		</div>
	</div>
	<!-- Add node dialog -->
	<div class="modal fade" id="addNodeDialog" tabindex="-1" role="dialog" aria-labelledby="addNodeDialogLabel" aria-hidden="true">
	  <div class="modal-dialog modal-sm">
	    <div class="modal-content">
		      <div class="modal-header">
		        <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
		        <h4 class="modal-title" id="addNodeDialogLabel">Add Node</h4>
		      </div>
		      <div class="modal-body">
					  <div class="form-group">
					    <label for="nodeName">Node Name</label>
					    <input name="nodeName" type="text" class="form-control" id="nodeName" placeholder="Node Name">
					  </div>
					  <div class="form-group">
					    <label for="nodeType">Node Type</label>
					    <input name="jcr:primaryType" type="hidden" id="nodeType">
					  </div>
					  <div class="form-group">
					    <label for="resourceType">Sling Resource Type</label>
					    <input name="sling:resourceType" type="hidden" id="resourceType">
					  </div>
		      </div>
		      <div class="modal-footer">
		        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
		        <button type="button" class="btn btn-primary submit">Save changes</button>
		      </div>
	    </div>
	  </div>
	</div>
</body>
</html>
