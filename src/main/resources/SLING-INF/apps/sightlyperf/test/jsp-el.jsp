<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling"%>
<%--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements.  See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership.  The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License.  You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~--%>
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
