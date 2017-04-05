<%@ tag body-content="empty" isELIgnored="false" display-name="Path-Editor" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%@attribute name="component_id" %>
<%@attribute name="value" %>
<%@attribute name="editor_component_id" %>

<c:if test="${not empty value}" >
	<c:set var="escapedValue" value="${fn:escapeXml(value)}" />
</c:if>

<script type="text/javascript">
$(document).ready(function() {
	
	$('\#${editor_component_id}').on("keyup", function(){
		$('\#${component_id}').attr('href', '/reseditor'+$(this).val()+'.html');
		$('\#${component_id}').text($(this).val()+'.html');
	});
});
</script>

<a id="${component_id}" class="path-viewer" href="/reseditor${value}.html">${escapedValue}.html</a>