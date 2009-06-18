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
<%@page import="org.apache.sling.openidauth.OpenIDConstants.OpenIDFailure"%>
<%
	OpenIDConstants.OpenIDFailure failureReason = 
		(OpenIDConstants.OpenIDFailure)request.getAttribute(
			OpenIDConstants.OPENID_FAILURE_REASON_ATTRIBUTE);

	String failureMessage = null;
	if(failureReason == OpenIDConstants.OpenIDFailure.DISCOVERY) {
		failureMessage = "Unable to find OpenID provider";
	} else if (failureReason == OpenIDConstants.OpenIDFailure.ASSOCIATION) {
		failureMessage = "Unable to associate with OpenID provider";
	} else if (failureReason == OpenIDConstants.OpenIDFailure.REPOSITORY) {
		failureMessage = "No matching repository user found";
	} else if (failureReason != null) {
		failureMessage = "Unknown login error";
	}
	
	String currentLogin = null;
	if(request.getAttribute(OpenIDConstants.OPEN_ID_USER_ATTRIBUTE) != null) {
		currentLogin = ((OpenIdUser)request.getAttribute(OpenIDConstants.OPEN_ID_USER_ATTRIBUTE)).getIdentity();
	}
	
	String origRequestUrl = (String)request.getAttribute(OpenIDConstants.ORIGINAL_URL_ATTRIBUTE);
	if(origRequestUrl == null || "".equals(origRequestUrl.trim())) {
		origRequestUrl = "/";
	}
%>

<html>
	<head>
		<style>
			#openid_identifier {
				background-image:url('http://www.plaxo.com/images/openid/login-bg.gif');
				background-repeat: no-repeat;
				background-position: center left;
				padding-left: 18px;
			}
			
			body {
				font-family: verdana;
				font-size: 10pt;
			}
			
			.login-box {
				position: absolute;
				left: 30%;
				top: 10%;
				border: thin outset grey;
				padding: 30px;
			}
			
			.login-header {
				font-size: 14pt
			}
			
			.login-form {
				padding: 10px;
			}
			
			.login-form label {
				font-size: 10pt;
			}
			
			.login-status {
				font-style: italic;
			}
			
			.login-status .username {
				font-weight: bold;
				color: orange;
			}
			
			.login-status .error {
				font-weight: bold;
				color: red;
			}
		</style>
	</head>
	<body>
	    <div class="login-box">
			<div class="login-header">Sling OpenID Login</div>
			<form class="login-form" action="<%= origRequestUrl %>">
				<label for="openid_identifier">Identifier</label>
				<input id="openid_identifier" name="openid_identifier" size="40" />
				<input class="login-button" type="submit" value="Login" />
			</form>
			<div class="login-status">
				<% if (failureReason != null) { %>
					<div class="error"><%= failureMessage %></div>
				<% } %>
				<% if(currentLogin != null) { %>
	            		Currently logged in as: 
	            		<span class="username"><%= currentLogin %></span>
	            <% } %>
            </div>
	    </div>
	</body>
</html>
