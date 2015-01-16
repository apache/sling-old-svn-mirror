<%@page session="false" import="
    java.util.Map,
    java.util.List,
    org.apache.sling.api.resource.Resource,
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
    for (Map.Entry<String, List<String>> entry : user.getErrors().entrySet()) {
%>
<span><strong>Key:</strong> <%= entry.getKey() %></span><br>
<%
        for (String message : entry.getValue()) {
%>
<span><%= message %></span><br>
<%
        }
    }
%>