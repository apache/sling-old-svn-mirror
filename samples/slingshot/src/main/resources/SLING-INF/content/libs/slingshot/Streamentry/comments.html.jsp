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
                org.apache.sling.sample.slingshot.comments.CommentsService,
                org.apache.sling.sample.slingshot.comments.CommentsUtil,
                org.apache.sling.api.request.ResponseUtil" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%
%><div class="ui-slingshot-comments">
<h3>Comments</h3>
<%
    final CommentsService commentsService = sling.getService(CommentsService.class);
    final Resource parent = resource.getResourceResolver().getResource(commentsService.getCommentsResourcePath(resource));
    if ( parent == null ) {
        %><p>No comments...</p><%
    } else {
        int count = 0;
        for(final Resource c : parent.getChildren()) {
            %><hr/><sling:include resource="<%= c %>" replaceSelectors="item"/><%
            count++;
            if( count >= 10 ) {
                break;
            }
        }
    }
    if ( slingRequest.getAuthType() != null ) {
        %>
        <hr/><p>Leave a comment...</p>
<form class="navbar-form navbar-left" role="comment" method="POST" action="<%= request.getContextPath() %><%=resource.getName() %>.comments">
  <input type="hidden" name=":redirect" value="<%= request.getContextPath() %><%=resource.getPath() %>.html"/>
  <div class="form-group">
    <input type="text" class="form-control" placeholder="Title" name="<%= CommentsUtil.PROPERTY_TITLE %>"/>
    <input type="text" class="form-control" placeholder="Text" name="<%= CommentsUtil.PROPERTY_TEXT %>"/>
  </div>
  <button type="submit" class="ui-button ui-form-button">Add</button>
</form>
      <%
    }
%>
</div>
