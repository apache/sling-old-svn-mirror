<%--
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
--%><%@page session="false"%><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%><%
%><sling:defineObjects/><%
 
// used by IncludeTest
%><%!

private String getProperty(javax.jcr.Node node, String propertyName) {
    try {
        if (node.hasProperty(propertyName)) {
            return node.getProperty(propertyName).getString();
        }
    } catch (Throwable t) {
        // don't care
    }
    return null;
}

%><%

String text = getProperty(currentNode, "text");
String pathToInclude = getProperty(currentNode, "pathToInclude");
String forceResourceType = getProperty(currentNode, "forceResourceType");
String testInfiniteLoop = getProperty(currentNode, "testInfiniteLoop");
String testMaxCalls = getProperty(currentNode, "testMaxCalls");
String testCallScript = getProperty(currentNode, "testCallScript");

%><html>
	<body>
		<h1>JSP template</h1>
		<p class="main"><%= text %></p>
		
		<h2>Test 1</h2>
		<%
			if(pathToInclude != null) {
			  %>
			  <p>pathToInclude = <%= pathToInclude %></p>
  		  	  <p>Including <%= pathToInclude %></p>
  		  	  <sling:include path="<%= pathToInclude %>"/>
			  <%
			}
		%>
		
		<h2>Test 2</h2>
		<%
			if(testInfiniteLoop != null) {
			  %>
			  <p>testInfiniteLoop = <%= testInfiniteLoop %></p>
			  <%
			  // try to include the item itself, to cause an infinite loop
			  %>
  		  	  <sling:include path="<%= resource.getPath() %>"/>
  		  	  <%
			}
		%>
		
		<h2>Test 3</h2>
		<%
			if(pathToInclude != null && forceResourceType != null) {
			  %>
			  <p>pathToInclude = <%= pathToInclude %></p>
  		  	  <p>Including <%= pathToInclude %></p>
  		  	  <sling:include path="<%= pathToInclude %>" resourceType="<%= forceResourceType %>"/>
			  <%
			}
		%>
		
		<h2>Test 4</h2>
		<%
			if(pathToInclude != null && testMaxCalls != null) {
				%>
				<p>pathToInclude = <%= pathToInclude %></p>
				<p>Including <%= pathToInclude %></p>
				<%
			    for (int i=0; i < 1200; i++) {
			        %>
			        <%= i %><br />
			        <hr />
					<sling:include path="<%= pathToInclude %>" />
			        <hr />
					<%
			    }
			}
		%>

		<h2>Test 5</h2>
		<%
			if (testCallScript != null) {
		%>
				<p>Calling <%= testCallScript %></p>
				<sling:call script="<%= testCallScript %>" />
		<%
			}
		%>
	</body>
</html>
