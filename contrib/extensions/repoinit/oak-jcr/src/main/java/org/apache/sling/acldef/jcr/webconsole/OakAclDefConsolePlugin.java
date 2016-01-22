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
package org.apache.sling.acldef.jcr.webconsole;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.acldef.jcr.AclOperationVisitor;
import org.apache.sling.acldef.parser.AclDefinitionsParser;
import org.apache.sling.acldef.parser.AclParsingException;
import org.apache.sling.acldef.parser.operations.Operation;
import org.apache.sling.acldef.parser.operations.OperationVisitor;
import org.apache.sling.api.request.ResponseUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Constants;

@Component
@Service(value=Servlet.class)
@Properties({
    @Property(name=Constants.SERVICE_VENDOR, value="The Apache Software Foundation"),
    @Property(name=Constants.SERVICE_DESCRIPTION, value="Apache Sling ACL Definitions Console Plugin"),
    @Property(name="felix.webconsole.label", value=OakAclDefConsolePlugin.LABEL),
    @Property(name="felix.webconsole.title", value="Set ACLs"),
    @Property(name="felix.webconsole.css", value="/" + OakAclDefConsolePlugin.LABEL + "/res/ui/acldef.css"),
    @Property(name="felix.webconsole.category", value="Sling"),
})
public class OakAclDefConsolePlugin extends HttpServlet {

    private static final long serialVersionUID = 1234;
    private static final String PAR_ACLDEF = "acldef";
    private static final String PAR_MSG = "msg";
    public static final String LABEL = "setACL";
    private static final String ATTR_SUBMIT = "plugin.submit";
    private AtomicInteger counter = new AtomicInteger();

    private static final String EXAMPLE =
            "# Example ACL definition\n"
            + "# (with service user creation commented out)\n"
            + "# create service user test_42\n"
            + "set ACL for test_42\n"
            + "  allow jcr:read,jcr:modifyProperties on /tmp\n"
            + "end\n"
            ;
    
    @Reference
    private SlingRepository repository;

    @Reference
    private AclDefinitionsParser parser;

    private String thisPath(HttpServletRequest request) {
        return request.getContextPath() + request.getServletPath() + request.getPathInfo();
    }
    
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) 
            throws ServletException, IOException {
        String aclDef = request.getParameter(PAR_ACLDEF);
        if(aclDef == null || aclDef.trim().length() == 0) {
            aclDef = EXAMPLE;
        }
        
        String msg = request.getParameter(PAR_MSG);
        if(msg == null) {
            msg = "";
        }

        final PrintWriter pw = response.getWriter();
        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");
        titleHtml(
                pw,
                "ACL definitions",
                "To create service users or set ACLs, enter a valid statement below."
                );

        pw.println("<tr class='content'>");
        pw.print("<td class='content' colspan='3'>");
        pw.print("<form method='post' action='" + thisPath(request) + "'>");
        pw.print("<input type='submit' name='" + ATTR_SUBMIT + "' value='Execute' class='submit'>");
        pw.print("<div class='msg'>");
        pw.print(msg);
        pw.println("</div>");
        pw.print("<textarea type='text' name='" + PAR_ACLDEF + "' class='input' cols='80' rows='25'>");
        pw.print(ResponseUtil.escapeXml(aclDef));
        pw.println("</textarea>");
        pw.print("</form>");
        pw.print("</td>");
        pw.println("</tr>");
        pw.println("</table>");
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        String aclDef = request.getParameter(PAR_ACLDEF);
        String msg = "No ACL definitions executed";
        if(aclDef == null || aclDef.trim().length() == 0) {
            aclDef = "";
        } else {
            try {
                setAcl(aclDef);
                msg = "ACL definitions successfully executed";
            } catch(Exception e) {
                throw new ServletException("Error setting ACLs:\n" + e.getMessage(), e);
            }
        }
        msg += " (" + counter.incrementAndGet() + ")";
        
        // Redirect to GET on the same page
        final StringBuilder target = new StringBuilder();
        target
            .append(thisPath(request))
            .append("?").append(PAR_ACLDEF).append("=").append(encodeParam(aclDef))
            .append("&").append(PAR_MSG).append("=").append(encodeParam(msg))
        ;
        response.sendRedirect(target.toString());
    }
    
    private static String encodeParam(final String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected UnsupportedEncodingException", e);
        }
    }
    
    private void setAcl(String aclDef) throws RepositoryException, IOException, AclParsingException {
        final Reader r = new StringReader(aclDef);
        Session s = null;
        try {
            s = repository.loginAdministrative(null);
            final OperationVisitor v = new AclOperationVisitor(s);
            for(Operation op : parser.parse(r)) {
                op.accept(v);
            }
            s.save();
        } finally {
            r.close();
            if(s != null) {
                s.logout();
            }
        }
    }

    private void titleHtml(PrintWriter pw, String title, String description) {
        pw.print("<tr class='content'>");
        pw.print("<th colspan='3'class='content container'>");
        pw.print(ResponseUtil.escapeXml(title));
        pw.println("</th></tr>");

        if (description != null) {
            pw.print("<tr class='content'>");
            pw.print("<td colspan='3'class='content'>");
            pw.print(ResponseUtil.escapeXml(description));
            pw.println("</th></tr>");
        }
    }

    /**
     * Method to retrieve static resources from this bundle.
     */
    @SuppressWarnings("unused")
    private URL getResource(final String path) {
        final String prefix = "/" + LABEL + "/res";
        if(path.startsWith(prefix + "/ui")) {
            return this.getClass().getResource(path.substring(prefix.length()));
        }
        return null;
    }
}
