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
%><%@page import="java.util.Iterator,
                  org.apache.sling.api.resource.Resource,
                  org.apache.sling.sample.slingshot.Constants" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%
%><div class="tree">
  <div class="albumlist">
    <h2>Contained Albums</h2>
    <%
    final Iterator<Resource> ri = resource.listChildren();
    while ( ri.hasNext()) {
        final Resource current = ri.next();
        if ( current.isResourceType(Constants.RESOURCETYPE_ALBUM) 
             && Constants.includeAsAlbum(current)) {
            %><sling:include resource="<%= current %>" resourceType="slingshot/Album"  replaceSelectors="treeentry"/><%
        }
    }
    %>
  </div>
</div>