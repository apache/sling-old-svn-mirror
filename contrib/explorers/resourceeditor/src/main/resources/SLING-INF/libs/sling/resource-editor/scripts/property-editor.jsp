<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="re" uri="http://sling.apache.org/resource-editor"%>

<sling:defineObjects />

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
	     	<re:string-editor property_name="${fn:escapeXml(property.name)}-editor" value="${property.multiple ? value.string : property.string}"></re:string-editor>
	     </c:when>
	     <c:when test="<%=property.getType() == PropertyType.PATH%>" >
	     	<re:path-editor value="${property.multiple ? value.string : property.string}" component_id="property-${propertyLoopStatus.index}-path-editor"></re:path-editor>
	     	<re:path-viewer value="${property.multiple ? value.string : property.string}" component_id="property-${propertyLoopStatus.index}-path-viewer" editor_component_id="property-${propertyLoopStatus.index}-path-editor"></re:path-viewer>
	     </c:when>
	     <c:otherwise>
			<input class="propinput form-control" id="${property.name}" name="${property.name}" value="${property.multiple ? value.string : property.string}"/>							
	     </c:otherwise>
     </c:choose>