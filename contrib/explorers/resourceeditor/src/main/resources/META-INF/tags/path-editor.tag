<%@ tag body-content="empty" isELIgnored="false" display-name="Path-Editor" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%@attribute name="component_id" %>
<%@attribute name="value"  %>

<c:if test="${not empty value}" >
	<c:set var="escapedValue" value="${fn:escapeXml(value)}" />
</c:if>

<input id="${component_id}" class="propinput form-control property-value path-editor" name="new-property-value" value="${escapedValue}"/>
