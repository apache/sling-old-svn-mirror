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
package org.apache.sling.tenant.internal.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.tenant.Tenant;
import org.apache.sling.tenant.internal.TenantProviderImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * This is a webconsole plugin displaying the active queues, some statistics and
 * the configurations.
 */
public class WebConsolePlugin extends HttpServlet {

    private static final long serialVersionUID = -6983227434841706385L;

    private static final String LABEL = "tenants";

    private static final String TITLE = "Tenant Administration";

    private static final String CATEGORY = "Sling";

    /** tenant name parameter */
    private static final String REQ_PRM_TENANT_NAME = "tenantName";

    /** tenant id parameter */
    private static final String REQ_PRM_TENANT_ID = "tenantId";

    /** tenant description parameter */
    private static final String REQ_PRM_TENANT_DESC = "tenantDesc";

    /** command parameter for tenant changing actions */
    private static final String REQ_PRM_CMD = "action";

    /** status message for the commands */
    private static final String REQ_PRM_MESSAGE = "message";

    private TenantProviderImpl tenantProvider;

    private final ServiceRegistration<?> service;

    /** Escape the output for html. */
    private String escape(final String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public WebConsolePlugin(final BundleContext bundleContext, final TenantProviderImpl tenantProvider) {
        this.tenantProvider = tenantProvider;

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Tenant Management Console");
        props.put("felix.webconsole.label", LABEL);
        props.put("felix.webconsole.title", TITLE);
        props.put("felix.webconsole.category", CATEGORY);
        // props.put("felix.webconsole.configprinter.modes", new String[]{"zip",
        // "txt"});

        this.service = bundleContext.registerService(Servlet.class.getCanonicalName(), this, props);
    }

    public void dispose() {
        if (this.service != null) {
            this.service.unregister();
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        String msg = null;

        final String cmd = req.getParameter(REQ_PRM_CMD);
        final String tenantId = req.getParameter(REQ_PRM_TENANT_ID);

        // Check required parameters
        if (tenantId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    String.format("Missing parameter %s", REQ_PRM_TENANT_ID));
            return;
        }

        if ("create".equals(cmd)) {

            final Map<String, Object> properties = new HashMap<String, Object>(2);
            properties.put(Tenant.PROP_NAME, req.getParameter(REQ_PRM_TENANT_NAME));
            properties.put(Tenant.PROP_DESCRIPTION, req.getParameter(REQ_PRM_TENANT_DESC));
            final Tenant t = tenantProvider.create(tenantId, properties);

            // null checks matches the current implementation of TenantManagerImpl.
            // It may require to be changed according to the outcome of SLING-4234.
            if (t != null) {
                msg = String.format("Created tenant %s", tenantId);
            } else {
                msg = String.format("Cannot create tenant %s", tenantId);
            }
        } else if ("remove".equals(cmd)) {
            final Tenant tenant = this.tenantProvider.getTenant(tenantId);
            if (tenant != null) {
                this.tenantProvider.remove(tenant);
                msg = String.format("Removed tenant %s", tenantId);
            } else {
                msg = String.format("Tenant %s already removed", tenantId);
            }
        } else {
            msg = "Unknown command";
        }

        final String path = LABEL;
        final String redirectTo;
        if (msg == null) {
            redirectTo = path;
        } else {
            redirectTo = String.format("%s?%s=%s", path, REQ_PRM_MESSAGE, msg);
        }

        resp.sendRedirect(redirectTo);
    }

    private void printForm(final PrintWriter pw, final Tenant t, final String buttonLabel, final String cmd) {
        pw.printf("<button class='ui-state-default ui-corner-all' onclick='javascript:cmdsubmit(\"%s\", \"%s\");'>"
            + "%s</button>", cmd, (t != null ? t.getId() : ""), buttonLabel);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res) throws IOException {
        final PrintWriter pw = res.getWriter();

        final String msg = req.getParameter(REQ_PRM_MESSAGE);

        pw.println("<form method='POST' name='cmd'>" + "<input type='hidden' name='action' value=''/>"
            + "<input type='hidden' name='tenantId' value=''/>" + "</form>");
        pw.println("<script type='text/javascript'>");
        pw.println("function cmdsubmit(action, tenantId) {" + " document.forms['cmd'].action.value = action;"
            + " document.forms['cmd'].tenantId.value = tenantId;" + " document.forms['cmd'].submit();" + "} "
            + "function createsubmit() {" + " document.forms['editorForm'].submit();" + "} " + "</script>");
        pw.printf("<p class='statline ui-state-highlight'>Apache Sling Tenant Support</p>");

        if (msg != null) {
            pw.printf("<p class='statline ui-state-highlight'><span class='ui-icon ui-icon-info' " +
                    "style='float: left; margin-right: .3em;'></span> " + msg + "</p>");
        }

        pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>");
        pw.println("<span style='float: left; margin-left: 1em'>Add New Tenant </span>");
        pw.println("<button class='ui-state-default ui-corner-all' onclick='javascript:createsubmit();'> Create </button></div></td></tr>");
        pw.println("</div>");
        pw.println("<table id='editortable' class='nicetable'><tbody>");

        pw.println("<tr width='100%'><td colspan='2'><form id='editorForm' method='POST'>");
        pw.println("<input name='action' type='hidden' value='create' class='ui-state-default ui-corner-all'>");
        pw.println("<table border='0' width='100%'><tbody>");
        pw.println("<tr><td style='width: 30%;'>Identifier</td><td>");
        pw.println("<div><input name='tenantId' type='text' value=''></div>");
        pw.println("</td></tr>");
        pw.println("<tr><td style='width: 30%;'>Name</td><td>");
        pw.println("<div><input name='tenantName' type='text' value=''></div>");
        pw.println("</td></tr>");
        pw.println("<tr><td style='width: 30%;'>Description</td><td>");
        pw.println("<div><input name='tenantDesc' type='text' value=''></div>");
        pw.println("</td></tr>");
        pw.println("</tbody></table></form>");
        pw.println("</tbody></table>");

        Iterator<Tenant> tenants = this.tenantProvider.getTenants();
        int count = 0;
        while (tenants.hasNext()) {
            count++;
            Tenant tenant = tenants.next();
            if (count == 1) {
                pw.printf("<p class='statline ui-state-highlight'>Registered Tenants</p>");
            }
            pw.println("<div class='ui-widget-header ui-corner-top buttonGroup'>");
            pw.printf("<span style='float: left; margin-left: 1em'>Tenant : %s </span>", escape(tenant.getName()));
            this.printForm(pw, tenant, "Remove", "remove");
            pw.println("</div>");
            pw.println("<table class='nicetable'><tbody>");

            pw.printf("<tr><td style='width: 30%%;'>Identifier</td><td>%s</td></tr>", escape(tenant.getId()));
            pw.printf("<tr><td style='width: 30%%;'>Name</td><td>%s</td></tr>", escape(tenant.getName()));
            pw.printf("<tr><td style='width: 30%%;'>Description</td><td>%s</td></tr>", escape(tenant.getDescription()));
            pw.println("</tbody></table>");
        }
        // no existing tenants
        if (count == 0) {
            pw.printf("<p class='statline ui-state-highlight'>There are not registered tenants</p>");
        }
    }
}
