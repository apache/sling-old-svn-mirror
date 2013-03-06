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
	import="java.util.Arrays, org.apache.sling.api.SlingHttpServletRequest" 
%>

<%	
final String SELECTOR_401 ="401";
final String SELECTOR_500 ="500";
final String SELECTOR_THROWABLE ="throwable";
final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest)request;

if(Arrays.asList(slingRequest.getRequestPathInfo().getSelectors()).contains(SELECTOR_401)) {
	 response.setStatus(401);
	 response.sendError(401);
}else if(Arrays.asList(slingRequest.getRequestPathInfo().getSelectors()).contains(SELECTOR_500)) {
	response.setStatus(500);
 	response.sendError(500);
}else if(Arrays.asList(slingRequest.getRequestPathInfo().getSelectors()).contains(SELECTOR_THROWABLE)) {
	throw new Exception("throwable selector was specified");
}
%>
