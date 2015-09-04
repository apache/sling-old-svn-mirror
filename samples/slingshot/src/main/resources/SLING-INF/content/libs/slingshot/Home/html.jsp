<%--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
--%><%@page session="false" %><%
%><%@page import="org.apache.sling.api.resource.ResourceUtil,
                org.apache.sling.api.resource.ValueMap,
                org.apache.sling.api.request.ResponseUtil,
                org.apache.sling.sample.slingshot.SlingshotConstants" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%

    final ValueMap attributes = resource.getValueMap();
    final String title = ResponseUtil.escapeXml(attributes.get(SlingshotConstants.PROPERTY_TITLE, resource.getName()));
%><html>
  <head>
    <title>Welcome to SlingShot!</title>
    <sling:include resource="<%= resource %>" replaceSelectors="head"/>
  </head>

  <body class="ui-body-app">
    <%
    if ( request.getRemoteUser() != null && !request.getRemoteUser().equals("anonymous") ) {
        %>
<div class="ui-widget ui-form">
    <div class="ui-widget-header">
        <h1>Welcome to SlingShot</h1>
    </div>
    <div class="ui-widget-content">
       <p>Welcome back, <%= request.getRemoteUser() %></p>
        <p><a href="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/users/<%= request.getRemoteUser() %>.html">Go to your SlingShot home page</a></p>
    </div>
 </div>
        <%
    } else {
        %>
<div class="ui-widget ui-form">
    <div class="ui-widget-header">
        <h1>Welcome to SlingShot</h1>
    </div>
    <div class="ui-widget-content">
        <form class="ui-form-form" method="POST" action="<%= request.getContextPath() %><%= resource.getPath() %>.user.html" enctype="multipart/form-data" accept-charset="UTF-8">
          <input type="hidden" name="_charset_" value="UTF-8" />
          <input type="hidden" name="resource" value="<%= request.getContextPath() %><%= resource.getPath() %>.user.html" />
          <span class="ui-form-label">
              <label for="j_username" accesskey="u">Username</label>
          </span>
          <input id="j_username" name="j_username" type="text" class="ui-form-field">
    
          <span class="ui-form-label">
              <label for="j_password" accesskey="p">Password</label>
          </span>
          <input id="j_password" name="j_password" type="password" class="ui-form-field">
          <button accesskey="l" class="ui-button ui-form-button ui-form-button-login" type="submit">Go</button>
          <button accesskey="n" class="ui-button ui-form-button ui-form-button-new" type="button">New Account</button>
          <button accesskey="n" class="ui-button ui-form-button ui-form-button-help" type="button">Help Me</button>
        </form>
    </div>
 </div>
        <%
    }
    %>
</body>
</html>