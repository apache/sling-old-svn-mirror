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
%><%@page import="org.apache.sling.sample.slingshot.SlingshotConstants" %><%
%><link href="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/css/jquery-ui.min.css" rel="stylesheet"/>
  <link href="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/css/jquery-ui.structure.min.css" rel="stylesheet"/>
  <link href="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/css/jquery-ui.theme.min.css" rel="stylesheet"/>
  <link href="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/css/metro-bootstrap.min.css" rel="stylesheet"/>
  <link href="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/css/jquery-breadcrumbs.css" rel="stylesheet"/>
  <link href="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/css/slingshot.css" rel="stylesheet"/>
  <script src="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/js/jquery.js" type="text/javascript" ></script>
  <script src="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/js/jquery-ui.min.js" type="text/javascript" ></script>
  <script src="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/js/metro.min.js" type="text/javascript" ></script>
  <script src="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/js/jquery-breadcrumbs.js" type="text/javascript" ></script>
  <script src="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/js/slingshot.js"> type="text/javascript" </script>  