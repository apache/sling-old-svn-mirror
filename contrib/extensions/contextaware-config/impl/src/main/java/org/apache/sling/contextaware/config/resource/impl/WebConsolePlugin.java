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
package org.apache.sling.contextaware.config.resource.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.contextaware.config.resource.spi.ConfigurationResourcePersistence;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

// TODO: this web console plugin is currently quite broken and need to be refactored
@Component(service=Servlet.class,
           property={"org.osgi.framework.Constants.SERVICE_DESCRIPTION=Apache Sling Web Console Plugin for configurations",
                   WebConsoleConstants.PLUGIN_LABEL + "=" + WebConsolePlugin.LABEL,
                   WebConsoleConstants.PLUGIN_TITLE + "=" + WebConsolePlugin.TITLE})
@SuppressWarnings("serial")
public class WebConsolePlugin extends AbstractWebConsolePlugin {

    public static final String LABEL = "configresolver";
    public static final String TITLE = "Config Resolver";

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private ResourceResolverFactory resolverFactory;

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private ConfigurationResourcePersistence configResolver;

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private XSSAPI xssAPI;

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    protected void renderContent(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        final PrintWriter pw = response.getWriter();

        info(pw, "Configurations are managed in the resource tree. Use this tool to test configuration resolutions.");

        printConfiguration(pw);

        pw.println("<br/>");

        printResolutionTestTool(request, pw);
    }

    private void printConfiguration(final PrintWriter pw) {
        final DefaultConfigurationResourcePersistence configResolverImpl = (DefaultConfigurationResourcePersistence)configResolver;
        tableStart(pw, "Configuration", 2);
        pw.println("<tr>");
        pw.println("<td style='width:20%'>Allowed paths</td>");
        pw.print("<td>");
        pw.print(xssAPI.encodeForHTML(Arrays.toString(configResolverImpl.getConfiguration().allowedPaths())));
        pw.println("</td>");
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<td style='width:20%'>Fallback paths</td>");
        pw.print("<td>");
        pw.print(xssAPI.encodeForHTML(Arrays.toString(configResolverImpl.getConfiguration().fallbackPaths())));
        pw.println("</td>");
        pw.println("</tr>");
        pw.println("<tr>");
        pw.println("<td></td>");
        pw.println("<td>");
        pw.print("<form method='get' action='${appRoot}/confMgr/");
        pw.print(ConfigurationResourceResolverImpl.class.getName());
        pw.println("'>");
        pw.println("<input type='submit' value='Configure'/>");
        pw.println("</form>");
        pw.println("</td>");
        pw.println("</tr>");
        tableEnd(pw);
    }

    private String getParameter(final HttpServletRequest request, final String name, final String defaultValue) {
        String value = request.getParameter(name);
        if ( value != null && !value.trim().isEmpty() ) {
            return value.trim();
        }
        return defaultValue;
    }

    private void printResolutionTestTool(HttpServletRequest request, PrintWriter pw) {
        final String path = this.getParameter(request, "path", null);
        final String item = this.getParameter(request, "item", ".");
        final String user = this.getParameter(request, "user", null);

        ResourceResolver resolver = null;
        try {
            Resource content = null;
            if (path != null) {
                resolver = getResolver(user);
                if (resolver != null) {
                    content = resolver.getResource(path);
                }
            }

            pw.println("<form method='get'>");

            tableStart(pw, "Test Configuration Resolution", 2);
            pw.println("<td style='width:20%'>Content Path</td>");
            pw.print("<td><input name='path' value='");
            pw.print(xssAPI.encodeForHTMLAttr(StringUtils.defaultString(path)));
            pw.println("' style='width:100%'/>");
            if (resolver != null && content == null) {
                pw.println("<div>");
                pw.println("<span class='ui-icon ui-icon-alert' style='float:left'></span>");
                pw.println("<span style='float:left'>Path does not exist.</span>");
                pw.println("</div>");
            }
            pw.println("</td>");

            tableRows(pw);
            pw.println("<td>Item</td>");
            pw.print("<td><input name='item' value='");
            pw.print(xssAPI.encodeForHTMLAttr(item));
            pw.println("' style='width:100%'/></td>");
            tableRows(pw);

            pw.println("<td>User</td>");
            pw.println("<td><input name='user' value='");
            pw.print(xssAPI.encodeForHTMLAttr(StringUtils.defaultString(user)));
            pw.println("' style='width:50%'/>");
            if (path != null && resolver == null) {
                pw.println("<div>");
                pw.println("<span class='ui-icon ui-icon-alert' style='float:left'></span>");
                pw.println("<span style='float:left'>User does not exist.</span>");
                pw.println("</div>");
            }
            pw.println("</td>");
            tableRows(pw);

            pw.println("<td></td>");
            pw.println("<td><input type='submit' value='Resolve'/></td>");
            tableEnd(pw);

            pw.println("</form>");

            pw.println("<br/>");

            if (content != null) {

                // TODO: use sensible bucket name or make it configurable
                final Resource confRsrc = configResolver.getResource(content, "sling:configs", item);

                tableStart(pw, "Resolved", 2);
                pw.println("<td style='width:20%'>Code</td>");
                pw.println("<td>");
                pw.print("<code>resolve(\"");
                pw.print(xssAPI.encodeForHTML(content.getPath()));
                pw.print("\", \"");
                pw.print(xssAPI.encodeForHTML(item));
                pw.println("\")</code>");
                pw.println("<br/>&nbsp;");
                pw.println("</td>");
                tableRows(pw);

                pw.println("<td style='width:20%'>Item</td>");
                if (confRsrc != null) {
                    pw.print("<td>");
                    pw.print(xssAPI.encodeForHTML(confRsrc.getPath()));
                    pw.println("<br/>&nbsp;</td>");
                } else {
                    pw.println("<td>");
                    pw.println("<div>");
                    pw.println("<span class='ui-icon ui-icon-alert' style='float:left'></span>");
                    pw.println("<span style='float:left'>No matching item found.</span>");
                    pw.println("</div>");
                    pw.println("<br/>&nbsp;</td>");
                }
                tableRows(pw);

                pw.println("<td>Config paths</td>");

                pw.println("<td>");
                for (String p : ((DefaultConfigurationResourcePersistence)configResolver).getResolvePaths(content)) {
                    if (confRsrc != null && confRsrc.getPath().startsWith(p + "/")) {
                        pw.print("<b>");
                        pw.print(xssAPI.encodeForHTML(p));
                        pw.println("</b>");
                    } else {
                        pw.println(xssAPI.encodeForHTML(p));
                    }
                    pw.println("<br/>");
                }
                pw.println("</td>");

                tableEnd(pw);
            }

        } finally {
            if (resolver != null && user != null) {
                resolver.close();
            }
        }
    }

    private void info(PrintWriter pw, String text) {
        pw.print("<p class='statline ui-state-highlight'>");
        pw.print(xssAPI.encodeForHTML(text));
        pw.println("</p>");
    }

    private void tableStart(PrintWriter pw, String title, int colspan) {
        pw.println("<table class='nicetable ui-widget'>");
        pw.println("<thead class='ui-widget-header'>");
        pw.println("<tr>");
        pw.print("<th colspan=");
        pw.print(String.valueOf(colspan));
        pw.print(">");
        pw.print(xssAPI.encodeForHTML(title));
        pw.println("</th>");
        pw.println("</tr>");
        pw.println("</thead>");
        pw.println("<tbody class='ui-widget-content'>");
        pw.println("<tr>");
    }

    private void tableEnd(PrintWriter pw) {
        pw.println("</tr>");
        pw.println("</tbody>");
        pw.println("</table>");
    }

    private void tableRows(PrintWriter pw) {
        pw.println("</tr>");
        pw.println("<tr>");
    }

    private ResourceResolver getResolver(final String user) {
        ResourceResolver requestResolver = resolverFactory.getThreadResourceResolver();
        if ( user == null ) {
            return requestResolver;
        }
        try {
            return requestResolver.clone(Collections.singletonMap(ResourceResolverFactory.USER_IMPERSONATION, (Object)user));
        } catch (final LoginException e) {
            return null;
        }
    }
}
