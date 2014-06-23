<%@page session="false" contentType="text/html; charset=utf-8"%>
<%@page import="java.util.Date" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/><c:set var="success" value="true" />
AdaptTo Tag
    Test 1: AdaptTo Tag
    result: <sling:adaptTo adaptable="${resource}" adaptTo="org.apache.sling.api.resource.ValueMap" var="props" /><c:choose><c:when test="${not empty props}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
    Test 2: Test Null Adaptable Handling
    result: <sling:adaptTo adaptable="${res}" adaptTo="org.apache.sling.api.resource.ValueMap" var="props3" /><c:choose><c:when test="${empty props3}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
    Test 3: Test Non-Adaptable Handling
    result: <c:catch var="adaptionException"><sling:adaptTo adaptable="res" adaptTo="org.apache.sling.api.resource.ValueMap" var="props3" /></c:catch><c:choose><c:when test="${not empty adaptionException}">SUCCESS: ${adaptionException}</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

Encode Tag
    Test 1: HTML Encode
    Result: HTML_ENCODE:<sling:encode value="&amp;Hello World!<script></script>" mode="HTML" />

    Test 2: Default
    Result: DEFAULT:<sling:encode default="&amp;Hello World!<script></script>" mode="HTML" />

    Test 3: EL Value
    <c:set var="encode_test">I'm Awesome!!</c:set>
    Result: EL_VALUE:<sling:encode value="${encode_test}" mode="HTML" />
    
    Test 4: Body Content
    Result: BODY_CONTENT:<sling:encode mode="HTML">&copy;Body Content</sling:encode>
    
    Test 5: Body Content Fallback
    Result: BODY_CONTENT_FALLBACK:<sling:encode value="1" mode="HTML">2</sling:encode>
    
Find Resources Tag
    Test 1: Find Resources
    Result: <sling:findResources query="/jcr:root//element(*, nt:file) order by @jcr:score" language="xpath" var="foundResources" /><c:choose><c:when test="${not empty foundResources}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 2: Invalid Query
    Result: <c:catch var="queryException"><sling:findResources query="/jcr:rootelement(*, nt:file) order by @jcr:score" language="xpath" var="foundResources2" /></c:catch><c:choose><c:when test="${not empty queryException}">SUCCESS: ${queryException}</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 3: Invalid Language
    Result: <c:catch var="queryException2"><sling:findResources query="/jcr:root//element(*, nt:file) order by @jcr:score" language="not-a-real-lang" var="foundResources3" /></c:catch><c:choose><c:when test="${not empty queryException2}">SUCCESS: ${queryException}</c:when><c:otherwise>SUCCESS: ${foundResources3}</c:otherwise></c:choose>
    
Get Property Tag
<sling:getResource path="/apps/integration-test/taglib-test" var="component" /><sling:adaptTo var="componentProps" adaptable="${component}" adaptTo="org.apache.sling.api.resource.ValueMap" />
    Test 1: Get Property
    Result: <sling:getProperty properties="${componentProps}" key="jcr:created" var="property1" /><c:choose><c:when test="${not empty property1}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
    Test 2: Not Found Property
    Result: <sling:getProperty properties="${componentProps}" key="sling:resourceType2" var="property2" /><c:choose><c:when test="${empty property2}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 3: Coercing Value
    Result: <sling:getProperty properties="${componentProps}" key="jcr:created" var="property3" returnClass="java.util.Calendar" /><c:choose><c:when test="${not empty property3.time.month}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 4: Default Value
    Result: <sling:getProperty properties="${componentProps}" key="emptyKey" var="property4" defaultValue="bob" /><c:choose><c:when test="${not empty property4}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
Get Resource Tag
    Test 1: Get Resource
    Result: <sling:getResource path="/apps/integration-test/" var="contentResource" /><c:choose><c:when test="${not empty contentResource.path}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 2: Get Relative Resource
    Result: <sling:getResource path="taglib-test" var="contentResource2" base="${contentResource}" /><c:choose><c:when test="${not empty contentResource2.path}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 3: Not Found Handling
    Result: <sling:getResource path="/does/not/exist" var="nfResource" /><c:choose><c:when test="${empty nfResource.path}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
    Test 4: Bad Base Handling
    Result: <sling:getResource path="does/not/exist" var="nfResource2" base="${nfResource}" /><c:choose><c:when test="${empty nfResource2.path}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
List Children Tag
    Test 1: Get Children
    Result: <sling:listChildren resource="${contentResource}" var="childResources" /><c:choose><c:when test="${not empty childResources}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 2: Non-Existing Parent Handing
    Result: <sling:listChildren resource="${nfResource}" var="childResources2" /><c:choose><c:when test="${empty childResources2}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 3: Bad Parent Handling
    Result: <c:catch var="listException"><sling:listChildren resource="nfResource" var="childResources3" /></c:catch><c:choose><c:when test="${not empty listException}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
    
AdaptTo Function
    Test 1: AdaptTo
    result: <c:set var="props4" value="${sling:adaptTo(resource,'org.apache.sling.api.resource.ValueMap')}" /><c:choose><c:when test="${not empty props4}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
    Test 2: Test Null Adaptable Handling
    result: <c:set var="props6" value="${sling:adaptTo(res,'org.apache.sling.api.resource.ValueMap')}" /><c:choose><c:when test="${empty props6}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
    Test 3: Test Non-Adaptable Handling
    result: <c:catch var="adaptionException2"><c:set var="props7" value="${sling:adaptTo('res','org.apache.sling.api.resource.ValueMap')}" /></c:catch><c:choose><c:when test="${not empty adaptionException2}">SUCCESS: ${adaptionException}</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

Find Resources Function
    Test 1: Find Resources
    Result: <c:set var="foundResources4" value="${sling:findResources(resourceResolver, '/jcr:root//element(*, nt:file) order by @jcr:score', 'xpath')}" /><c:choose><c:when test="${not empty foundResources4}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 2: Invalid Query
    Result: <c:catch var="queryException3"><c:set var="foundResources5" value="${sling:findResources(resourceResolver, '/jcr:rootelement(*, nt:file) order by @jcr:score', 'xpath')}" /></c:catch><c:choose><c:when test="${not empty queryException3}">SUCCESS: ${queryException}</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 3: Invalid Language
    Result: <c:catch var="queryException4"><c:set var="foundResources6" value="${sling:findResources(resourceResolver, '/jcr:rootelement(*, nt:file) order by @jcr:score', 'xpath')}" /></c:catch><c:choose><c:when test="${not empty queryException4}">SUCCESS: ${queryException}</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
Get Relative Resource Function
    Test 1: Get Relative Resource
    Result: <c:set var="contentResource4" value="${sling:getRelativeResource(contentResource, 'taglib-test')}" /><c:choose><c:when test="${not empty contentResource4.path}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 2: Bad Base Handling
    Result: <c:set var="nfresource4" value="${sling:getRelativeResource(nfResource, 'test123123')}" /><c:choose><c:when test="${empty nfresource4.path}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
    Test 3: Not Found Handling
    Result: <c:set var="nfresource5" value="${sling:getRelativeResource(contentResource, 'test123123')}" /><c:choose><c:when test="${empty nfresource5.path}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
Get Resource Function
    Test 1: Get Resource
    Result: <c:set var="contentResource5" value="${sling:getResource(resourceResolver, '/apps')}" /><c:choose><c:when test="${not empty contentResource5.path}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 2: Bad Resolver
    Result: <c:catch var="resourceException"><c:set var="nfresource6" value="${sling:getResource(badResolver, '/content')}" /></c:catch><c:choose><c:when test="${not empty resourceException}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
    Test 3: Not Found Handling
    Result: <c:set var="nfresource7" value="${sling:getResource(resourceResolver, 'test123123')}" /><c:choose><c:when test="${empty nfresource7.path}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
Get Value Function
    Test 1: Get Property
    <%
    pageContext.setAttribute("dateClass", Date.class);
    %>
    Result: <c:set var="property5" value="${sling:getValue(componentProps,'jcr:created',dateClass)}" /><c:choose><c:when test="${not empty property5}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
    Test 2: Not Found Property
    Result: <c:set var="property6" value="${sling:getValue(componentProps,'jcr:created2',dateClass)}" /><c:choose><c:when test="${empty property6}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 3: Default Value
    Result: <c:set var="property7" value="${sling:getValue(componentProps,'jcr:created2','fakeval')}" /><c:choose><c:when test="${not empty property7}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
    
List Children Function
    Test 1: Get Children
    Result: <c:set var="childResources4" value="${sling:listChildren(contentResource)}" /><c:choose><c:when test="${not empty childResources4}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 2: Non-Existing Parent Handing
    Result: <c:set var="childResources5" value="${sling:listChildren(nfResource)}" /><c:choose><c:when test="${empty childResources5}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>

    Test 3: Bad Parent Handling
    Result: <c:catch var="listException2"><c:set var="childResources6" value="${sling:listChildren('noVar')}" /></c:catch><c:choose><c:when test="${not empty listException2}">SUCCESS</c:when><c:otherwise>ERROR<c:set var="success" value="false" /></c:otherwise></c:choose>
    
<c:choose>
    <c:when test="${success}">
        All Tests Succeeded
    </c:when>
    <c:otherwise>
        Test Failures
    </c:otherwise>
</c:choose>