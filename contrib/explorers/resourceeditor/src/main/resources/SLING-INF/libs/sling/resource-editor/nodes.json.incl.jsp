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
[
	<c:forEach var="theResource" items="${sling:listChildren(resource)}" varStatus="status">
		<c:set var="resourceIsNode" scope="request" value="${sling:adaptTo(resource,'javax.jcr.Node') != null}"/>
		<%--Hiding the resource provider root. --%>
		<c:if test="${theResource.path != '/reseditor'}">
			<% Resource theResource = (Resource) pageContext.getAttribute("theResource");
			   Node node = theResource.adaptTo(Node.class);
			   String nodeType = (node !=null) ? node.getPrimaryNodeType().getName() : "nt:unstructured";
			   pageContext.setAttribute("nodeType", nodeType);
			%>
			{
		        "text": "<i class=\"jstree-icon node-icon open-icon\"></i><i class=\"jstree-icon node-icon remove-icon\"></i><i class=\"jstree-icon node-icon add-icon\"></i>${fn:escapeXml(theResource.name)} [<span class=\"node-type\">${theResource.resourceType}</span>]",
				"li_attr": { "nodename" : "${fn:escapeXml(theResource.name)}", "nodetype" :"${nodeType}" },
				"a_attr": { "href" : "/reseditor${fn:escapeXml(theResource.path)}.html" },
		        "children" : <%= theResource.listChildren().hasNext() %> <%--${theResource.listChildren().hasNext()} will work in Servlet 3.0 --%>
			}${!status.last ? ',': ''}
		</c:if>
	</c:forEach>
]
