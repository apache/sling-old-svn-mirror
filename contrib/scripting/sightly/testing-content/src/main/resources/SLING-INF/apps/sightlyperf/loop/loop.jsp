<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling"%>
<p>selector: ${param.selector} [<strong>jsp</strong>, jsp-el, sly-java, sly-js]</p>
<p>count: ${param.count} [1 - 20, <strong>20</strong>]</p>
<c:forEach begin="1" end="${empty param.count || param.count < 1 || param.count > 20 ? 20 : param.count}">
    <sling:include path="/sightlyperf/test" replaceSelectors="${param.selector == 'sly-java' || param.selector == 'sly-js' || param.selector == 'jsp-el'  ? param.selector : 'jsp'}" />
</c:forEach>