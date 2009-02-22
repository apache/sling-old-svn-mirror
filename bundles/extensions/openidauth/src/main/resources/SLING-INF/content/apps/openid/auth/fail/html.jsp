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
--%>
<%@page import="org.apache.sling.openidauth.OpenIDConstants"%>
<% 
	OpenIDConstants.OpenIDFailure failureReason = 
		(OpenIDConstants.OpenIDFailure)request.getAttribute(
			OpenIDConstants.OPENID_FAILURE_REASON_ATTRIBUTE);
	
	// user could access auth fail page directly while authenticated
	// in this case, we could redirect them to success page
	// this is optional behaviour and is best left out of the
	// authentication handler
	if(failureReason == null) {
		response.sendRedirect("authsuccess.html");
	} else {
		String origUrl = (String)
			request.getAttribute(OpenIDConstants.ORIGINAL_URL_ATTRIBUTE);
%>
The system was unable to authenticate you via OpenID<br/>
Reason: <%= failureReason.toString() %><br/>
Accessed URL: <%= (origUrl != null ? origUrl : "") %>
<% } %>