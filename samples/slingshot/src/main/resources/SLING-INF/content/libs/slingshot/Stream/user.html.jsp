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
                org.apache.sling.sample.slingshot.model.Stream,
                org.apache.sling.api.request.ResponseUtil" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%
%><%
    final Stream stream = new Stream(resource);
%>
<div class="col-md-4">
   <h2><%= ResponseUtil.escapeXml(stream.getInfo().getTitle()) %></h2>
   <!--  <img class="img-responsive" style="padding-top: 5px" src="<%= request.getContextPath() %>"/>  -->
   <p><a class="btn btn-default" href="<%= request.getContextPath() %><%=resource.getPath()%>.html" role="button">View details &raquo;</a> <span class="badge"><%= stream.getInfo().getEntryCount() %></span></p>
</div>
