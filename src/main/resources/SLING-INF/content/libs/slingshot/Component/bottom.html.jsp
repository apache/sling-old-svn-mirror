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
%><script src="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/js/jquery-3.1.0.min.js"></script>
<script src="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/js/bootstrap.min.js"></script>
<script src="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/js/ie10-viewport-bug-workaround.js"></script>
<script src="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/js/star-rating.min.js"></script>
<script src="<%= request.getContextPath() %><%= SlingshotConstants.APP_ROOT_PATH %>/resources/js/slingshot.js"></script>