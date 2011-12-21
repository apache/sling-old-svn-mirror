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
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0"%><%
%><sling:defineObjects/><%

// used by JspForwardTest
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

String pathToInclude = getProperty(currentNode, "pathToInclude");
String forceResourceType = getProperty(currentNode, "forceResourceType");
String testInfiniteLoop = getProperty(currentNode, "testInfiniteLoop");
String forwardStyle = getProperty(currentNode, "forwardStyle");

// Test 3: Forced Resource Type
if(pathToInclude != null && forceResourceType != null) {
    %><sling:forward path="<%= pathToInclude %>" resourceType="<%= forceResourceType %>"/><%
}

else

// Test 1: Simple Forward 
if(pathToInclude != null) {
    if ("jsp".equals(forwardStyle)) {
        %><jsp:forward page="<%= pathToInclude %>"/><%
    } else {
        %><sling:forward path="<%= pathToInclude %>"/><%
    }
}

else

// Test 2: Infinite Loop
if(testInfiniteLoop != null) {
  // try to include the item itself, to cause an infinite loop
    %><sling:forward path="<%= resource.getPath() %>"/><%
}

else

{

// Test 0: No Forward
%><html>
	<body>
		<h1>JSP template</h1>
		<p class="main"><%= currentNode.getProperty("text").getString() %></p>
	</body>
</html><%

}
%>