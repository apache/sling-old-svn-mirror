<%@ page import="org.apache.sling.api.resource.Resource" %>
<%@ page import="org.apache.sling.api.resource.ValueMap" %>
<%@ page import="org.apache.sling.xss.XSSAPI" %>
<%@ page import="java.util.Iterator" %>
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
<%
    ValueMap properties = resource.adaptTo(ValueMap.class);

    String tag = properties.get("tag", null);
    if (tag != null) {
        out.println("<" + tag + ">");
    }
    XSSAPI xssAPI = sling.getService(XSSAPI.class);
    out.println(xssAPI.encodeForHTML(properties.get("text", resource.getPath()).toString()));
    if (tag != null) {
        out.println("</" + tag + ">");
    }

%>
    <sling:call script="mode.jsp" />
<%

    if (properties.get("includeChildren", false)) {
        Iterator<Resource> iter = resource.listChildren();
%>
<ul>
<%
        while(iter.hasNext()) {
            Resource child = iter.next();
%>
            <li><sling:include path="<%=child.getPath()%>" /></li>
<%
        }
%>
</ul>
<%
    }
%>
