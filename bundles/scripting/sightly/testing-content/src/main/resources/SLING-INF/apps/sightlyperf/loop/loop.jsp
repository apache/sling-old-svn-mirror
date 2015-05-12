<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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
<p>selector: ${param.selector} [<strong>jsp</strong>, jsp-el, sly-java, sly-js]</p>
<p>count: ${param.count} [1 - 20, <strong>20</strong>]</p>
<c:forEach begin="1" end="${empty param.count || param.count < 1 || param.count > 20 ? 20 : param.count}">
    <sling:include path="/sightlyperf/test" replaceSelectors="${param.selector == 'sly-java' || param.selector == 'sly-js' || param.selector == 'jsp-el'  ? param.selector : 'jsp'}" />
</c:forEach>