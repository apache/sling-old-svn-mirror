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
                  org.apache.sling.sample.slingshot.model.StreamEntry,                  
                  org.apache.sling.sample.slingshot.SlingshotUtil,                  
                  org.apache.sling.sample.slingshot.SlingshotConstants"%><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%
    final StreamEntry entry = new StreamEntry(resource);

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
    <title><%= ResponseUtil.escapeXml(entry.getTitle()) %></title>
    <sling:include resource="<%= resource %>" replaceSelectors="head"/>
  </head>
  <body>
    <sling:include resource="<%= resource %>" replaceSelectors="menu"/>
    <div class="jumbotron">
      <div class="container">
        <h1><%= ResponseUtil.escapeXml(entry.getStream().getInfo().getTitle()) %></h1>
        <p><%= ResponseUtil.escapeXml(entry.getStream().getInfo().getDescription()) %></p>
      </div>
    </div>
    <div class="container">
      <h1><%= ResponseUtil.escapeXml(entry.getTitle()) %></h1>
      <div class="row">
        <div class="col-xs-12 col-md-8">
          <img class="img-responsive center-block" src="<%= request.getContextPath() %><%= imagePath %>"/>
          <div class="well">
            <div class="row">
            <form class="navbar-form navbar-left" role="comment" method="POST" action="<%= request.getContextPath() %><%=resource.getName() %>">
              <input type="hidden" name=":redirect" value="<%= request.getContextPath() %><%=resource.getPath() %>.html"/>
              <div class="form-group">
                <label for="<%= SlingshotConstants.PROPERTY_TITLE %>">Title</label>
                <input type="text" class="form-control" placeholder="Title" name="<%= SlingshotConstants.PROPERTY_TITLE %>" value="<%=ResponseUtil.escapeXml(entry.getTitle())%>"/>
              </div>
              <div class="form-group">
                <label for="<%= SlingshotConstants.PROPERTY_DESCRIPTION %>">Description</label>
                <input type="text" class="form-control" placeholder="Description" name="<%= SlingshotConstants.PROPERTY_DESCRIPTION %>" value="<%=ResponseUtil.escapeXml(entry.getDescription())%>"/>
              </div>
              <div class="form-group">
                <label for="<%= SlingshotConstants.PROPERTY_LOCATION %>">Location</label>
                <input type="text" class="form-control" placeholder="Location" name="<%= SlingshotConstants.PROPERTY_LOCATION %>" value="<%=ResponseUtil.escapeXml(entry.getLocation())%>"/>
              </div>
              <button type="submit" class="ui-button ui-form-button">Update</button>
            </form>
            </div>
          </div>
        </div>
      </div>
    </div>
    <sling:include resource="<%= resource %>" replaceSelectors="bottom"/>
  </body>
</html>