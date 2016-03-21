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
%><%@page import="java.util.ArrayList,
                  java.util.List,
                  org.apache.sling.api.resource.Resource,
                  org.apache.sling.api.resource.ResourceUtil,
                  org.apache.sling.api.resource.ValueMap,
                  org.apache.sling.sample.slingshot.SlingshotConstants,
                  org.apache.sling.sample.slingshot.SlingshotUtil,
                  org.apache.sling.api.request.ResponseUtil" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%
    final boolean isUser = request.getRemoteUser() != null && !request.getRemoteUser().equals("anonymous");

    final ValueMap attributes = resource.getValueMap();
    // let's create the trail
    final List<Object[]> parents = new ArrayList<Object[]>();
    if ( resource.getPath().startsWith(SlingshotConstants.APP_ROOT_PATH) && !resource.isResourceType(SlingshotConstants.RESOURCETYPE_USER)) {
        Resource parent = resource.getParent();
        String prefix = "../";
        boolean continueProcessing = true;
        do {
            if ( !parent.isResourceType(SlingshotConstants.RESOURCETYPE_CONTENT) ) {
                final ValueMap parentAttr = parent.getValueMap();
                final String parentName = parent.getName();
                parents.add(new Object[] {prefix + parentName, ResponseUtil.escapeXml(parentAttr.get(SlingshotConstants.PROPERTY_TITLE, parentName))});
            }
            if ( parent.isResourceType(SlingshotConstants.RESOURCETYPE_USER) ) {
                continueProcessing = false;
            } else {
                parent = parent.getParent();
                prefix = prefix + "../";
            }
        } while ( continueProcessing);
    }  
%><nav class="navbar navbar-default navbar-fixed-top">
      <div class="container">
        <div class="navbar-header">
          <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false" aria-controls="navbar">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
          <a class="navbar-brand" href="#">Slingshot</a>
        </div>
        <div id="navbar" class="navbar-collapse collapse">
          <ul class="nav navbar-nav"><%
    for(int k=parents.size()-1;k>=0;k--) {
      %>
        <li><a href="<%= parents.get(k)[0] %>.html"><%= parents.get(k)[1] %></a></li>
      <%
    }
            %>
          </ul>
          <ul class="nav navbar-nav navbar-right">
          <% if ( isUser ) {
          %>
            <li><a href="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/users/<%= request.getRemoteUser() %>.html"><span class="glyphicon glyphicon-home" aria-hidden="true"></span></a></li>
            <li><a href="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/users/<%= request.getRemoteUser() %>.html"><span class="glyphicon glyphicon-cog" aria-hidden="true"></span></a></li>
            <li><a href="<%= request.getContextPath() %>/system/sling/logout.html?resource=<%= SlingshotConstants.APP_ROOT_PATH %>.html"><span class="glyphicon glyphicon-off" aria-hidden="true"></span></a></li>
          <%
          } else { %>
          <li><a href="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>.html"><span class="glyphicon glyphicon-log-in" aria-hidden="true"></span></a></li>
          <%
          } %>
          </ul>
        </div>
      </div>
    </nav>
