<!--
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
--><%@page session="false" import="
    java.util.Map,
    java.util.List,
    java.util.Locale,
    org.apache.sling.api.resource.Resource,
    org.apache.sling.validation.ValidationFailure,
    org.apache.sling.validation.examples.models.UserModel
    "%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %>
<sling:defineObjects />
<%
    UserModel user = resource.adaptTo(UserModel.class);
%>
<table>
    <tr>
        <td><strong>Username:</strong></td>
        <td><%= user.getUsername() %></td>
    </tr>
    <tr>
        <td><strong>First Name:</strong></td>
        <td><%= user.getFirstName() %></td>
    </tr>
    <tr>
        <td><strong>Last Name:</strong></td>
        <td><%= user.getLastName() %></td>
    </tr>
    <tr>
        <td><strong>Admin:</strong></td>
        <td><%= user.isAdmin() %></td>
    </tr>
</table>
<h3>Validation Errors</h3>
<%
    for (ValidationFailure error : user.getErrors()) {
%>
<span><strong>Location:</strong> <%= error.getLocation() %></span><br>
<span><%= error.getMessage(slingRequest.getResourceBundle(Locale.US)) %></span><br>
<%
    }
%>