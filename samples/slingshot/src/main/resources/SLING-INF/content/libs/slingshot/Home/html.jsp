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

  <body class="login">
    <%
    if ( request.getRemoteUser() != null && !request.getRemoteUser().equals("anonymous") ) {
        %>
    <div class="container">
        <h1 class="bg-primary">Welcome to SlingShot</h1>
        <p class="bg-primary">Welcome back, <%= request.getRemoteUser() %></p>
        <p class="bg-warning"><a href="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/users/<%= request.getRemoteUser() %>.html">Go to your SlingShot home page</a></p>
    </div>
        <%
    } else {
        %>
    <div class="container">

      <form class="form-signin" method="POST" action="<%= request.getContextPath() %><%= resource.getPath() %>.user.html" enctype="multipart/form-data" accept-charset="UTF-8">
        <input type="hidden" name="_charset_" value="UTF-8" />
        <input type="hidden" name="resource" value="<%= request.getContextPath() %><%= resource.getPath() %>.user.html" />
        <p class="form-signin-heading bg-primary">Welcome to SlingShot</p>
        <label for="j_username" class="sr-only" accesskey="u">Username</label>
        <input type="text" id="j_username" class="form-control" placeholder="Username" required autofocus>
        <label for="j_password" class="sr-only" accesskey="p">Password</label>
        <input type="password" id="j_password" class="form-control" placeholder="Password" required>
        <p>
        <button class="btn btn-lg btn-success btn-block" type="submit">Go</button>
        </p>
        <p>
        <button class="btn btn-primary btn-block form-button-new" type="button">New Account</button>
        <button class="btn btn-info btn-block form-button-help" type="button">Help Me</button>
        </p>
      </form>

    </div>
        <%
    }
    %>
</body>
    <sling:include resource="<%= resource %>" replaceSelectors="bottom"/>
</html>