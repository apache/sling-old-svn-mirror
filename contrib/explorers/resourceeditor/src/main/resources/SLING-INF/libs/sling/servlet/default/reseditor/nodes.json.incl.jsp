
[
	<c:forEach var="theResource" items="<%=resource.listChildren()%>" varStatus="status">
		<% Resource theResource = (Resource) pageContext.getAttribute("theResource");%>
	{
        "text": "<i class=\"jstree-icon node-icon open-icon\"></i><i class=\"jstree-icon node-icon remove-icon\"></i><i class=\"jstree-icon node-icon add-icon\"></i>${fn:escapeXml(theResource.name)} [<span class=\"node-type\">${theResource.resourceType}</span>]",
		"li_attr": { "nodename" : "${fn:escapeXml(theResource.name)}" },
		"a_attr": { "href" : "${fn:escapeXml(theResource.path)}.reseditor.html" },
        "children" : <%= theResource.listChildren().hasNext() %> <%--${theResource.listChildren().hasNext()} will work in Servlet 3.0 --%>
	}${!status.last ? ',': ''}
	</c:forEach>
]
