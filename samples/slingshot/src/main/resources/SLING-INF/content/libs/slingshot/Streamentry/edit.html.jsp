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
%><%@page import="org.apache.sling.api.resource.Resource,
                  org.apache.sling.api.resource.ResourceUtil,
                  org.apache.sling.api.resource.ValueMap,
                  org.apache.sling.api.request.ResponseUtil,
                  org.apache.sling.sample.slingshot.SlingshotConstants"%><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%
    final ValueMap attributes = resource.getValueMap();
    final String title = ResponseUtil.escapeXml(attributes.get(SlingshotConstants.PROPERTY_TITLE, resource.getName()));

    String imagePath = null;
    final Resource imagesResource = resource.getResourceResolver().getResource(resource, "images");
    if ( imagesResource != null ) {
        for(final Resource imgResource : imagesResource.getChildren()) {
            imagePath = imgResource.getPath();
            break;
        }
    }
%><html>
  <head>
    <title><%= title %></title>
    <sling:include resource="<%= resource %>" replaceSelectors="head"/>
  </head>
  <body class="ui-slingshot-main">
    <sling:include resource="<%= resource %>" replaceSelectors="menu"/>
    <h1><%= title %></h1>
    <img src="<%= request.getContextPath() %><%= imagePath %>"/>
    <form method="POST" action="<%= request.getContextPath() %><%=resource.getName() %>">
      <input type="hidden" name=":redirect" value="<%= request.getContextPath() %><%=resource.getPath() %>.html"/>
      <p>Title: <input name="<%= SlingshotConstants.PROPERTY_TITLE %>" value="<%= title %>"/></p>
      <p>Description: <input name="<%= SlingshotConstants.PROPERTY_DESCRIPTION %>" value="<%=ResponseUtil.escapeXml(attributes.get(SlingshotConstants.PROPERTY_DESCRIPTION, ""))%>"/></p>
      <p>Location: <input name="<%= SlingshotConstants.PROPERTY_LOCATION %>" value="<%=ResponseUtil.escapeXml(attributes.get(SlingshotConstants.PROPERTY_LOCATION, ""))%>"/></p>
      <button class="ui-button ui-form-button" type="submit">Save</button>
      <p><a href="<%= request.getContextPath() %><%=resource.getPath() %>.html">Cancel</a></p>
    </form>
  </body>
</html>