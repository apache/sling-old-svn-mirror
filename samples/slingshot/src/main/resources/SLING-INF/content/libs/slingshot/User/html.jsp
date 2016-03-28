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
                org.apache.sling.sample.slingshot.model.User" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%

    final User user = new User(resource);
%><html>
  <head>
    <title><%= ResponseUtil.escapeXml(user.getInfo().getName()) %></title>
    <sling:include resource="<%= resource %>" replaceSelectors="head"/>
  </head>
  <body>
    <sling:include resource="<%= resource %>" replaceSelectors="menu"/>
    <div class="jumbotron">
      <div class="container">
        <h1><%= ResponseUtil.escapeXml(user.getInfo().getName()) %></h1>
        <p><%= ResponseUtil.escapeXml(user.getInfo().getAbout()) %></p>
      </div>
    </div>
    <sling:include resource="<%= resource %>" replaceSelectors="itemlist"/>
    <sling:include resource="<%= resource %>" replaceSelectors="bottom"/>
</body>
</html>
