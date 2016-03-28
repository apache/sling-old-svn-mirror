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
        </div>
        <div class="col-xs-6 col-md-4">
          <div id="rating" 
               data-score-hint="Rating: " 
               data-show-score="true" 
               data-role="rating" 
               data-stars="5" 
               data-score="<%= ratingsService.getRating(resource) %>" 
               data-static="false" 
               class="rating large" style="height: auto;">
               <ul><li></li><li></li><li></li><li></li><li></li></ul>
               <span class="score-hint">Rating: <%= ratingsService.getRating(resource) %></span>
          </div>
          <div class="fg-green rating active" id="own_rating" style="height: auto;">
            <ul><li title="bad" class="rated"></li><li title="poor"></li><li title="regular"></li><li title="good"></li><li title="gorgeous"></li></ul><span class="score-hint">Current score: <%= ratingsService.getRating(resource, request.getRemoteUser()) %></span>
          </div>
          <script>
                        $(function(){
                            $("#own_rating").rating({
                                static: false,
                                score: <%= ratingsService.getRating(resource, request.getRemoteUser()) %>,
                                stars: 5,
                                showHint: true,
                                showScore: true,
                                click: function(value, rating) {
                                    rating.rate(value);
                                    $.post( "<%= resource.getName() %>.ratings", { <%= RatingsUtil.PROPERTY_RATING %> : value }, function( data ) {
                                          $("#rating").rating("rate", data.rating);
                                        }, "json");
                                }
                            });
                        });
          </script>
        </div>
      </div>
      <div class="row">
          <p><%=ResponseUtil.escapeXml(entry.getDescription())%></p>
          <% if ( entry.getLocation() != null ) { %>
            <p>Location</p>
            <p><%=ResponseUtil.escapeXml(entry.getLocation())%></p>
          <% } %>
          <% if ( slingRequest.getRemoteUser() != null && slingRequest.getRemoteUser().equals(SlingshotUtil.getUserId(resource)) )  { %>
          <button class="ui-button ui-form-button ui-slingshot-clickable" 
                data-link="<%= request.getContextPath() %><%=resource.getName() %>.edit.html" type="button">Edit</button>
          <% } %>
      </div>
      <sling:include resource="<%= resource %>" replaceSelectors="comments"/>
    </div>
    <sling:include resource="<%= resource %>" replaceSelectors="bottom"/>
  </body>
</html>