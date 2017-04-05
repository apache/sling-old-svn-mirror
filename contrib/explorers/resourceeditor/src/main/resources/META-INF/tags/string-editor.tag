<%@ tag body-content="empty" isELIgnored="false" display-name="String-Editor" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%@attribute name="property_name"  %>
<%@attribute name="value"  %>
<%@attribute name="rows"  %>

<c:choose>
  <c:when test="${empty rows}">
	<textarea data-property-name="${fn:escapeXml(property_name)}" name="new-property-value" class="propinput form-control property-value">${value}</textarea>
  </c:when>
  <c:otherwise>
	<textarea data-property-name="${fn:escapeXml(property_name)}" name="new-property-value" class="propinput form-control property-value" rows="${rows}">${value}</textarea>
  </c:otherwise>
</c:choose>
