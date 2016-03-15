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
                  org.apache.sling.sample.slingshot.SlingshotConstants,
                  org.apache.sling.sample.slingshot.SlingshotUtil,
                  org.apache.sling.sample.slingshot.ratings.RatingsService,
                  org.apache.sling.sample.slingshot.ratings.RatingsUtil"%><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%
    final RatingsService ratingsService = sling.getService(RatingsService.class);
    final ValueMap attributes = resource.getValueMap();
    final String title = ResponseUtil.escapeXml(attributes.get(SlingshotConstants.PROPERTY_TITLE, resource.getName()));
    final String categoryName = ResponseUtil.escapeXml(resource.getParent().getValueMap().get(SlingshotConstants.PROPERTY_TITLE, resource.getParent().getName()));

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
  <body>
    <sling:include resource="<%= resource %>" replaceSelectors="trail"/>
    <div class="jumbotron">
      <div class="container">
        <h1><%= title %></h1>
        <p>Explore the world of bla....</p>
      </div>
    </div>
    <div class="container">
    <ul class="nav nav-tabs">
      <li role="presentation" class="active"><a href="#">Home</a></li>
      <li role="presentation"><a href="#">Comments</a></li>
      <li role="presentation"><a href="#">Edit</a></li>
    </ul>
        <img class="img-responsive center-block" src="<%= request.getContextPath() %><%= imagePath %>"/>
      <div style="width:30%; float:left; padding:15px; display:block;">
        <p><%=ResponseUtil.escapeXml(attributes.get(SlingshotConstants.PROPERTY_DESCRIPTION, ""))%></p>
        <% if ( attributes.get(SlingshotConstants.PROPERTY_LOCATION) != null ) { %>
            <p>Location</p>
            <p><%=ResponseUtil.escapeXml(attributes.get(SlingshotConstants.PROPERTY_LOCATION, ""))%></p>
        <% } %>
        <% if ( slingRequest.getRemoteUser() != null && slingRequest.getRemoteUser().equals(SlingshotUtil.getUserId(resource)) )  { %>
        <button class="ui-button ui-form-button ui-slingshot-clickable" 
                data-link="<%= request.getContextPath() %><%=resource.getName() %>.edit.html" type="button">Edit</button>
        <% } %>
        <div class="metro">
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
          <ul><li title="bad" class="rated"></li><li title="poor"></li><li title="regular"></li><li title="good"></li><li title="gorgeous"></li></ul><span class="score-hint">Current score: <%= ratingsService.getRating(resource, request.getRemoteUser()) %></span></div>
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
    <hr/>
    <sling:include resource="<%= resource %>" replaceSelectors="comments"/>
    </div>
    <sling:include resource="<%= resource %>" replaceSelectors="bottom"/>
  </body>
</html>