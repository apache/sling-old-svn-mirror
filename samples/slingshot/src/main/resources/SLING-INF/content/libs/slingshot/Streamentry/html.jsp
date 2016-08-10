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
                  org.apache.sling.sample.slingshot.ratings.RatingsService,
                  org.apache.sling.sample.slingshot.ratings.RatingsUtil"%><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%
    final StreamEntry entry = new StreamEntry(resource);

    final RatingsService ratingsService = sling.getService(RatingsService.class);

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
            <% if ( entry.getLocation().length() > 0 ) { %>
              <p>Location: <%=ResponseUtil.escapeXml(entry.getLocation())%></p>
            <% } %>
            <p><%=ResponseUtil.escapeXml(entry.getDescription())%></p>
            <% if ( slingRequest.getRemoteUser() != null && slingRequest.getRemoteUser().equals(SlingshotUtil.getUserId(resource)) )  { %>
              <button class="ui-button ui-form-button ui-slingshot-clickable" 
                data-link="<%= request.getContextPath() %><%=resource.getName() %>.edit.html" type="button">Edit</button>
            <% } %>
          </div>
        </div>
        <div class="col-xs-6 col-md-4">
          <h2>Rating</h2>
          <input id="rating" name="rating" type="number" class="rating" data-howerenabled="false" data-disabled="false" data-show-clear="false" data-show-caption="false" data-readonly="true" value="<%= ratingsService.getRating(resource) %>"/>
          <h2>Your Rating</h2>
          <input id="own_rating" name="own_rating" type="number" class="rating" data-show-caption="false" value="<%= ratingsService.getRating(resource, request.getRemoteUser()) %>"/>
        </div>
      </div>
      <sling:include resource="<%= resource %>" replaceSelectors="comments"/>
    </div>
    <sling:include resource="<%= resource %>" replaceSelectors="bottom"/>
          <script>
            $(function(){
              $("#own_rating").on('rating.clear', function(event) {
                  $.post( "<%= resource.getName() %>.ratings", { <%= RatingsUtil.PROPERTY_RATING %> : 0 }, function( data ) {
                      $("#rating").rating('update', data.rating);
                    }, "json");
              });
              $("#own_rating").on('rating.reset', function(event) {
                  $.post( "<%= resource.getName() %>.ratings", { <%= RatingsUtil.PROPERTY_RATING %> : 0 }, function( data ) {
                      $("#rating").rating('update', data.rating);
                    }, "json");
              });
              $("#own_rating").on('rating.change', function(event, value) {
                  $.post( "<%= resource.getName() %>.ratings", { <%= RatingsUtil.PROPERTY_RATING %> : value }, function( data ) {
                      $("#rating").rating('update', data.rating);
                    }, "json");
              });
            });
          </script>
  </body>
</html>