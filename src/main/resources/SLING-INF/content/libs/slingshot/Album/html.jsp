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
%><%@page import="org.apache.sling.api.resource.ResourceUtil,
                org.apache.sling.api.resource.ValueMap,
                org.apache.sling.api.request.ResponseUtil" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%

    final ValueMap attributes = ResourceUtil.getValueMap(resource);
    final String albumName = ResponseUtil.escapeXml(attributes.get("jcr:title", ResourceUtil.getName(resource)));
%><html>
  <head>
    <title>Album <%= albumName %></title>
  </head>
  <style>
div.photolist {
    width: 100%;
    text-align: left;
}
div.photo {
    display: inline-block;
    margin-left: 10px;
}
div.tags {
    width: 100%;
    background-color: #BBBBBB;
    height: 30px;
    text-align: left;
    vertical-align: middle;
    padding-right: 10px;
    padding-top: 8px;
    color: #FFFFFF;
    font-size: 15px;
}
div.tag {
    display: inline-block;
    margin-left: 10px;
}
div.trail {
    width: 100%;
    background-color: #BBBBBB;
    height: 30px;
    text-align: left;
    vertical-align: middle;
    padding-right: 10px;
    padding-top: 8px;
    color: #FFFFFF;
    font-size: 15px;
}
div.trailpart {
    display: inline-block;
    margin-left: 10px;
}  </style>
  <body>
  <h1><%= albumName %></h1>
  <sling:include resource="<%= resource %>" replaceSelectors="trail"/>
  <sling:include resource="<%= resource %>" replaceSelectors="tree"/>
  <hr/>
  <sling:include resource="<%= resource %>" replaceSelectors="photolist"/>
  <sling:include resource="<%= resource %>" replaceSelectors="tags"/>
</body>
</html>