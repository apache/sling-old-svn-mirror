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
<%@page session="false" %>
<%@page import="org.apache.sling.sample.*" %>
<%@page import="org.apache.sling.api.*" %>
<%@page import="org.apache.sling.api.resource.Resource"%>
<%@page import="java.util.Iterator"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %>

<%-- Ensure the presence of the Sling objects --%>
<sling:defineObjects/>
<%
    final SampleContent sampleContent = resource.adaptTo(SampleContent.class);
%>
<h1><%= sampleContent.getTitle() %></h1>
<p><%= sampleContent.getText() %></p>
<table border="1" cellpadding="3" cellspacing="0">
<%
	Iterator<Resource> ci = resourceResolver.listChildren(resource);
	while (ci.hasNext()) {
		Resource child = ci.next();
		%><tr><td><sling:include resource="<%= child %>" /></td></tr><%
	}
%>
</table>