<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling"%>
<sling:defineObjects />
<c:set var="properties" value="${sling:adaptTo(resource,'org.apache.sling.api.resource.ValueMap')}" />
<c:set var="tag" value="${sling:getValue(properties, 'tag', '')}" />
<c:if test="${tag != ''}"><${tag}></c:if>
${sling:encode(sling:getValue(properties, 'text', resource.path), 'HTML')}
<c:if test="${tag != ''}"></${tag}></c:if>
<sling:call script="mode.jsp" />
<c:if test="${sling:getValue(properties, 'includeChildren', false)}">
    <ul>
        <c:forEach items="${sling:listChildren(resource)}" var="child">
            <li><sling:include path="${child.path}" /></li>
        </c:forEach>
    </ul>
</c:if>
