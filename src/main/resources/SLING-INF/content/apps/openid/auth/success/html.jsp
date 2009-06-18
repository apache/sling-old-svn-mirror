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
<%@page import="com.dyuproject.openid.OpenIdUser"%>
<%@page import="org.apache.sling.openidauth.OpenIDConstants"%>
<%
	// user could access success page directly when not authenticated
	// this should be prevented by setting an ACL on success page nodes
	// in this case, however, we simply redirect them to login
	if(request.getSession().getAttribute(OpenIDConstants.OPEN_ID_USER_ATTRIBUTE) == null) {
		response.sendRedirect("loginform.html");
	} else {
		String origUrl = (String)
			request.getAttribute(OpenIDConstants.ORIGINAL_URL_ATTRIBUTE);
		
		// this shows anonymous even if logged in when 'Allow Anonymous Access' is true
		// String userName = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "";
		// also note, Display ID is usually <> Principal Name
		// however OpenIDUserUtil.getPrincipalName(Display ID) should = Principal Name
		String userName = ((OpenIdUser) request.getSession().getAttribute(OpenIDConstants.OPEN_ID_USER_ATTRIBUTE)).getIdentity();
%>
You were successfully logged in via OpenID as: <%= userName %>

<br/>
<% if(origUrl != null) { %>
You were trying to go <a href="<%= origUrl %>">here</a>
<% 	}
	}
%> 