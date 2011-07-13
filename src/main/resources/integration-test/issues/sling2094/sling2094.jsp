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
<%@page 
	session="false"
	errorPage="/apps/sling2094/custom-error-page.jsp"
	import="java.util.Arrays, org.apache.sling.api.SlingHttpServletRequest" 
%>

<%
final String SELECTOR = "witherror";
final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest)request;
if(Arrays.asList(slingRequest.getRequestPathInfo().getSelectors()).contains(SELECTOR)) {
	throw new Exception(SELECTOR + " selector was specified");
} else {
	%>All good, no exception<%
}
%>