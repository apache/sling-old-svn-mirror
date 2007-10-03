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
<%@page import="org.apache.sling.component.*" %>
<%@page import="java.util.*" %>
<%@taglib prefix="sling" uri="http://jackrabbit.apache.org/taglibs/sling/1.0" %>

<%-- Ensure the presence of the ComponentAPI objects --%>
<sling:defineObjects contentClass="SampleContent"/>

<h1><%= content.getTitle() %></h1>
<p><%= content.getText() %></p>
<table border="1" cellpadding="3" cellspacing="0">
<%
	Enumeration ci = renderRequest.getChildren(content);
	while (ci.hasMoreElements()) {
		Content child = (Content) ci.nextElement();
		%><tr><td><sling:include content="<%= child %>" /></td></tr><%
	}
%>
</table>