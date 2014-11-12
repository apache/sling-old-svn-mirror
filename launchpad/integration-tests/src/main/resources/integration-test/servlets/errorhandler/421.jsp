<!--
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
-->
<!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN">

<!-- 
    Test setting various status codes and throwing 
    exceptions in the error handler script 
-->
<%@page 
    session="false"
    import="java.util.List, java.util.Arrays, org.apache.sling.api.SlingHttpServletRequest" 
%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0"%>

<html>
    <head>
        <title>421 test</title>
    </head>
    
<%
String customMessage= "421 error page";
final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest)request;
final List selectors = Arrays.asList(slingRequest.getRequestPathInfo().getSelectors());
if(selectors.contains("312")) {
    response.setStatus(312);
}
if(selectors.contains("errorScriptException")) {
    throw new Exception("Exception in error handler");
}
if(selectors.contains("errorScriptError")) {
    throw new Error("Error in error handler");
}

%>

	<body>
		<h1>421 test - <%=customMessage%></h1>
	</body>
</html>