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
 */
package org.apache.sling.httpauth.impl;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

/**
 * The <code>LoginServlet</code> TODO
 * 
 * @scr.component metatype="no"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description" value="HTTP Header Login Servlet"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="sling.servlet.paths" value="/system/sling/login"
 * @scr.property name="sling.servlet.methods" values.0="GET" values.1="POST"
 */
public class LoginServlet extends SlingAllMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        PrintWriter pw = response.getWriter();

        prolog(pw);

        final String contexPath = request.getContextPath();
        String authType = request.getAuthType();
        String user = request.getRemoteUser();

        if (authType == null) {
            login(pw, contexPath);
        } else {
            logout(pw, contexPath, user);
        }

        epilog(pw);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException {
        response.sendRedirect(request.getRequestURI());
    }

    private void login(PrintWriter pw, String contextPath) {

        pw.println("<script>");
        ajax(pw, contextPath);
        pw.println("function loginuser() {");
        pw.println("    var user = document.forms['login'].usr.value;");
        pw.println("    var pass = document.forms['login'].pwd.value;");
        pw.println("    sendRequest(user, pass);");
        pw.println("    document.location = document.location");
        pw.println("}");
        pw.println("</script>");

        pw.println("<form name='login'>");
        pw.println("<table align='center'>");
        pw.println("<tr><td colspan='2' align='center'>You are not currently logged in</td></tr>");
        pw.println("<tr><td>Name</td><td><input type='text' name='usr' /></td></tr>");
        pw.println("<tr><td>Password</td><td><input type='text' name='pwd' /></td></tr>");
        pw.println("<tr><td colspan='2' align='center'><input type='button' value='Login' onClick='loginuser();'/></td></tr>");
        pw.println("</table>");
        pw.println("</form>");
    }

    private void logout(PrintWriter pw, String contextPath, String user) {
        pw.println("<script>");
        ajax(pw, contextPath);
        pw.println("function logoutuser() {");

        pw.println("    try {");
        pw.println("        // 'ClearAuthenticationCache' is only available in some browsers");
        pw.println("        // including the IE; for eg. Firefox, who cannot handle this command,");
        pw.println("        // we have the try-catch statement");

        pw.println("        // works in IE");
        pw.println("        document.execCommand('ClearAuthenticationCache');");

        pw.println("    } catch (e) {");
        pw.println("        sendRequest('"
            + AuthorizationHeaderAuthenticationHandler.NOT_LOGGED_IN_USER
            + "', 'null');");
        pw.println("    }");

        pw.println("    document.location = document.location");
        pw.println("}");
        pw.println("</script>");

        pw.println("<table align='center'>");
        pw.println("<tr><td align='center'>You are logged in as " + user
            + "</td></tr>");
        pw.println("<tr><td align='center'><input type='button' value='Logout'  onClick='logoutuser();'/></td></tr>");
        pw.println("</table>");
    }

    private void prolog(PrintWriter pw) {
        pw.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
        pw.println("<html>");
        pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; utf-8\">");
        pw.println("<title>Login/Logout</title>");
        pw.println("<style type=\"text/css\">");
        pw.println("body {");
        pw.println("  font-family: Verdana, Arial, Helvetica, sans-serif;");
        pw.println("  font-size: 10px;");
        pw.println("  color: black;");
        pw.println("  background-color: white;");
        pw.println("}");
        pw.println("#main {");
        pw.println("  border: 1px solid black;");
        pw.println("  margin-top: 25%;");
        pw.println("  margin-left: 25%;");
        pw.println("  width: 20em;");
        pw.println("  padding: 10px;");
        pw.println("}");
        pw.println("#main table {");
        pw.println("  width: 100%;");
        pw.println("}");
        pw.println("#main form {");
        pw.println("  padding: 0px;");
        pw.println("  margin: 0px;");
        pw.println("}");
        pw.println("</style>");
        pw.println("</head>");
        pw.println("<body>");

        pw.println("<div id=\"main\">");
    }

    private void ajax(PrintWriter pw, final String contextPath) {
        pw.println("//-----------------------------------------------------------------------------");
        pw.println("// Ajax Support");

        pw.println("// request object, do not access directly, use getXmlHttp instead");
        pw.println("var xmlhttp = null;");
        pw.println("function getXmlHttp() {");
        pw.println("    if (xmlhttp) {");
        pw.println("        return xmlhttp;");
        pw.println("   }");

        pw.println("         if (window.XMLHttpRequest) {");
        pw.println("             xmlhttp = new XMLHttpRequest();");
        pw.println("         } else if (window.ActiveXObject) {");
        pw.println("             try {");
        pw.println("                 xmlhttp = new ActiveXObject('Msxml2.XMLHTTP');");
        pw.println("             } catch (ex) {");
        pw.println("                 try {");
        pw.println("                     xmlhttp = new ActiveXObject('Microsoft.XMLHTTP');");
        pw.println("                 } catch (ex) {");
        pw.println("                 }");
        pw.println("             }");
        pw.println("         }");
        pw.println("");
        pw.println("         return xmlhttp;");
        pw.println("     }");

        pw.println("     function sendRequest(/* String */ user, /* String */ pass) {");
        pw.println("         var xmlhttp = getXmlHttp();");
        pw.println("         if (!xmlhttp) {");
        pw.println("             return;");
        pw.println("         }");

        pw.println("         if (xmlhttp.readyState < 4) {");
        pw.println("             xmlhttp.abort();");
        pw.println("         }");

        pw.println("         xmlhttp.open('POST', '" + contextPath + "?"
            + AuthorizationHeaderAuthenticationHandler.REQUEST_LOGIN_PARAMETER
            + "=1', false, user, pass);");

        pw.println("         xmlhttp.send('');");
        pw.println("     }");

    }

    private void epilog(PrintWriter pw) {
        pw.println("</div>");
        pw.println("</body>");
        pw.println("</html>");
    }

}
