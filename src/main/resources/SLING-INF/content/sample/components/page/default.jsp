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
<%@page session="false"%>
<%@page import="org.apache.sling.sample.*"%>
<%@page import="org.apache.sling.component.*"%>
<%@taglib prefix="sling"
	uri="http://jackrabbit.apache.org/taglibs/sling/1.0"%>

<%-- Ensure the presence of the ComponentAPI objects --%>
<sling:defineObjects contentClass="SamplePage" />

<%-- This is a top level component, so we have to draw the html and head tags --%>
<html>
<head>
<title><%= content.getTitle() %></title>
</head>
<body>
<h1><%= content.getTitle() %></h1>

<table style="border: none; height: 90%; ">
	<tr valign="top">
		<td style="padding-top: 20px; padding-right: 20px; background-color: cornsilk">
		<%
		final String naviRootPath = "/sample/content";
		Content naviRoot = new Content() {
		    public String getComponentId() {
		        return Navigation.class.getName();
		    }
		    public String getPath() {
		        return naviRootPath;
		    }
		};
		%> <sling:include content="<%= naviRoot %>" />
		</td>
		<td>
		<table>
			<tr>
				<td><sling:include path="content" /></td>
			</tr>
		</table>
		</td>
	</tr>
</table>

</body>
</html>