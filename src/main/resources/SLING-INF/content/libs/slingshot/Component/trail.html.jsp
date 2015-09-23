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
%><ul id="breadcrumbs" data-home="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/public/<%= request.getRemoteUser() %>.html">
  <li class="ui-slingshot-clickable" data-link="<%= request.getContextPath() %>/system/sling/logout.html?resource=<%= SlingshotConstants.APP_ROOT_PATH %>.html">Logout</li>
  <%
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
    for(int k=parents.size()-1;k>=0;k--) {
      %>
        <li class="ui-slingshot-clickable" data-link="<%= parents.get(k)[0] %>.html"><%= parents.get(k)[1] %></li>
      <%
    }
  %>
</ul>

