<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="re" uri="http://sling.apache.org/resource-editor"%>

<sling:defineObjects />

			<div id="properties" class="col-sm-8">
				<div id="outer_content" class="plate">
					<div class="ie9filter-plate-div">
						<div id="inner_content_margin">
							<div class="row">
								<div class="col-sm-12">
									<div style="display: none;" class="info-content-container" >
										<div class="well well-sm info-content">
											<button type="button" class="close"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
										  	<h4>Cheat Sheet</h4>
									  		<p>You can use</p>
									  		<ul>
									  			<li><kbd><kbd>ctrl</kbd> + <kbd>s</kbd></kbd> or <kbd><kbd>cmd</kbd> + <kbd>s</kbd></kbd> (for Mac) for saving a property.</li>
									  			<li><kbd><kbd>ctrl</kbd> + <kbd>del</kbd></kbd> or <kbd><kbd>cmd</kbd> + <kbd>del</kbd></kbd> (for Mac) for removing a property.</li>
									  			<li><kbd><kbd>ctrl</kbd> + <kbd>n</kbd></kbd> or <kbd><kbd>cmd</kbd> + <kbd>n</kbd></kbd> (for Mac) for opening the add property menu.</li>
									  		</ul>
										</div>
								  	</div>	
								  	<span id="properties-info-icon" class="info-icon info-icon-lightgray pull-right clearfix" ></span>
									<ul class="nav nav-pills">
									  <li class="dropdown" role="presentation">
									  	<a class="add-property-menu-item dropdown-toggle" data-toggle="dropdown"  href="#"><span class=""><span class="glyphicon glyphicon-plus"></span><span class="caret"></span></span>	
									  	</a>
										    <ul class="dropdown-menu add-property-menu" role="menu" aria-labelledby="propertyTypeMenu">
										        <li><a tabindex="-1" href="#" data-property-type="String">String</a></li>
										        <li><a tabindex="-1" href="#" data-property-type="Date">Date</a></li>
										        <li><a tabindex="-1" href="#" data-property-type="Boolean">Boolean</a></li>
										        <li><a tabindex="-1" href="#" data-property-type="Long">Long</a></li>
										        <li><a tabindex="-1" href="#" data-property-type="Double">Double</a></li>
										        <li><a tabindex="-1" href="#" data-property-type="Decimal">Decimal</a></li>
										        <li><a tabindex="-1" href="#" data-property-type="Binary">Binary</a></li>
										        <li><a tabindex="-1" href="#" data-property-type="Name">Name</a></li>
										        <li><a tabindex="-1" href="#" data-property-type="Path">Path</a></li>
										        <li><a tabindex="-1" href="#" data-property-type="Reference">Reference</a></li>
										        <li><a tabindex="-1" href="#" data-property-type="Uri">URI</a></li>
										    </ul>
									  </li>
									</ul>
								</div>
							</div>
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
										<c:forEach var="property" items="<%=currentNode.getProperties()%>" varStatus="propertyLoopStatus">
									<%  Property property = (Property) pageContext.getAttribute("property");%>
											<div id="property-${propertyLoopStatus.index}" class="row property-row" data-property-name="${fn:escapeXml(property.name)}" >
												<fieldset>
													<div class="col-sm-3">
														<label class="proplabel" for='${property.name}'>${property.name} [<%=PropertyType.nameFromValue(property.getType())%>${property.multiple ? ' multiple' : ''}]</label>
													</div>
													<div class="col-sm-7 property-col">
														<c:choose>
														     <c:when test="${property.multiple}" >
														     	<fieldset class="propmultival_fieldset">
														     		<div>&nbsp;</div>
														     	<c:forEach var="value" items="<%=property.getValues()%>">
														     		<c:choose>
															     		<c:when test="${property.type == PropertyType.BINARY}" >
																	     	<p>I'm a binary property</p>
																	     </c:when>
															     		<c:when test="${property.type == PropertyType.STRING}" >
																	     	<re:string-editor></re:string-editor>
																	     </c:when>
																	     <c:otherwise>
																     		<input class="propinputmultival form-control" value="${value.string}"/>
																	     </c:otherwise>
																     </c:choose>
														     	</c:forEach>
								     							</fieldset>
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
																     <c:when test="<%=property.getType() == PropertyType.STRING%>" >
																     	<re:string-editor property_name="${fn:escapeXml(property.name)}-editor" value="${property.string}"></re:string-editor>
																     </c:when>
																     <c:when test="<%=property.getType() == PropertyType.PATH%>" >
																     	<re:path-editor value="${property.string}" component_id="property-${propertyLoopStatus.index}-path-editor"></re:path-editor>
																     	<re:path-viewer value="${property.string}" component_id="property-${propertyLoopStatus.index}-path-viewer" editor_component_id="property-${propertyLoopStatus.index}-path-editor"></re:path-viewer>
																     </c:when>
																     <c:otherwise>
																		<input class="propinput form-control" id="${property.name}" name="${property.name}" value="${property.string}"/>							
																     </c:otherwise>
															     </c:choose>
														     </c:otherwise>
														 </c:choose>
													</div>
												 	<div class="col-sm-2">
												 		<span class="icon property-icon glyphicon glyphicon-save" aria-hidden="true"></span>
												 		<span class="icon property-icon glyphicon glyphicon-remove" aria-hidden="true"></span>
												 	</div>
												</fieldset>
											</div>
										</c:forEach>
								     </c:when>
								     <c:otherwise>
										<c:forEach var="property" items="<%=resource.adaptTo(ValueMap.class)%>">	
											<div class="row">
												<fieldset>			
													<div class="col-sm-3">							
														<label class="proplabel" for='${property.key}'>${property.key}</label>
													</div>
													<div class="col-sm-7">
														<input class="propinput form-control" id="${property.key}" name="${property.key}" value="${property.value}"/>
													</div>
												</fieldset>
											</div>							
										</c:forEach>
								     </c:otherwise>
								 </c:choose>
							</form>
						</div>
					</div>
			    </div>
			</div>
	<div class="modal fade" id="addPropertyDialog" tabindex="-1" role="dialog" aria-labelledby="addPropertyDialogLabel" aria-hidden="true">
	  <div class="modal-dialog modal-lg">
	    <div class="modal-content">
		      <div class="modal-header">
		        <button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>
		        <h4 class="modal-title" id="addPropertyDialogLabel">Add Property</h4>
		      </div>
		      <div class="modal-body">
			      	<div class="container-fluid">
									 <div class="row new-property">
										<fieldset>								
									 		<div class="col-sm-1">
									 			<label class="proplabel" for='new-property-key'>Key</label>
											</div>
										 	<div class="col-sm-3">
												<input id="new-property-key" class="propinput form-control" name="newPropertyKey" />
											</div>						
									 		<div class="col-sm-1">
									 			<label class="proplabel" for='new-property-value'>Value</label>
											</div>
										 	<div class="col-sm-7">
												<fieldset>								
													<label class="sr-only" for='new-property-value'>Value</label>	
													<div class="property-editor" data-property-type="Path">
<%-- 														<sling:include resource="${resource}" resourceType="sling/resource-editor/property-editor" addSelectors="path-editor"/> --%>
														<re:path-editor value="" component_id="property-path-editor-new"></re:path-editor>
													</div>		
													<div class="property-editor" data-property-type="String">
														<re:string-editor rows="4"></re:string-editor>
													</div>
												</fieldset>		
											</div>	
											</fieldset>		
									</div>	
					</div>
		      </div>
		      <div class="modal-footer">
		        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
		        <button type="button" class="btn btn-primary submit">Save changes</button>
		      </div>
	    </div>
	  </div>
	</div>