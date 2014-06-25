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
package org.apache.sling.resourceresolver.impl.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.request.ResponseUtil;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.resourceresolver.impl.CommonResourceResolverFactoryImpl;
import org.apache.sling.resourceresolver.impl.helper.URI;
import org.apache.sling.resourceresolver.impl.helper.URIException;
import org.apache.sling.resourceresolver.impl.mapping.MapEntries;
import org.apache.sling.resourceresolver.impl.mapping.MapEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class ResourceResolverWebConsolePlugin extends HttpServlet {

    private static final long serialVersionUID = 0;

    private static final String ATTR_TEST = "plugin.test";

    private static final String ATTR_SUBMIT = "plugin.submit";

    private static final String PAR_MSG = "msg";
    private static final String PAR_TEST = "test";

    private final transient CommonResourceResolverFactoryImpl resolverFactory;

    private transient ServiceRegistration service;

    public ResourceResolverWebConsolePlugin(BundleContext context,
            CommonResourceResolverFactoryImpl resolverFactory) {
        this.resolverFactory = resolverFactory;

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION,
                "Resource Resolver Web Console Plugin");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(Constants.SERVICE_PID, getClass().getName());
        props.put("felix.webconsole.label", "jcrresolver");
        props.put("felix.webconsole.title", "Resource Resolver");
        props.put("felix.webconsole.css", "/jcrresolver/res/ui/resourceresolver.css");
        props.put("felix.webconsole.category", "Sling");
        props.put("felix.webconsole.configprinter.modes", "always");

        service = context.registerService(
                new String[] { "javax.servlet.Servlet" }, this, props);
    }

    public void dispose() {
        if (service != null) {
            service.unregister();
            service = null;
        }
    }

    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException,
            IOException {
        final String msg = request.getParameter(PAR_MSG);
        final String test;
        if (msg != null) {
            test = request.getParameter(PAR_TEST);
        } else {
            test = null;
        }

        final PrintWriter pw = response.getWriter();

        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");

        final MapEntries mapEntries = resolverFactory.getMapEntries();

        titleHtml(pw, "Configuration", null);
        pw.println("<tr class='content'>");
        pw.println("<td class='content'>Resource Search Path</td>");
        pw.print("<td class='content' colspan='2'>");
        pw.print(Arrays.asList(resolverFactory.getSearchPath()).toString());
        pw.print("</td>");
        pw.println("</tr>");
        pw.println("<tr class='content'>");
        pw.println("<td class='content'>Namespace Mangling</td>");
        pw.print("<td class='content' colspan='2'>");
        pw.print(resolverFactory.isMangleNamespacePrefixes() ? "Enabled"
                : "Disabled");
        pw.print("</td>");
        pw.println("</tr>");
        pw.println("<tr class='content'>");
        pw.println("<td class='content'>Mapping Location</td>");
        pw.print("<td class='content' colspan='2'>");
        pw.print(resolverFactory.getMapRoot());
        pw.print("</td>");
        pw.println("</tr>");

        separatorHtml(pw);

        titleHtml(
                pw,
                "Configuration Test",
                "To test the configuration, enter an URL or a resource path into "
                        + "the field and click 'Resolve' to resolve the URL or click 'Map' "
                        + "to map the resource path. To simulate a map call that takes the "
                        + "current request into account, provide a full URL whose "
                        + "scheme/host/port prefix will then be used as the request "
                        + "information. The path passed to map will always be the path part "
                        + "of the URL.");

        pw.println("<tr class='content'>");
        pw.println("<td class='content'>Test</td>");
        pw.print("<td class='content' colspan='2'>");
        pw.print("<form method='post'>");
        pw.print("<input type='text' name='" + ATTR_TEST + "' value='");
        if (test != null) {
            pw.print(ResponseUtil.escapeXml(test));
        }
        pw.println("' class='input' size='50'>");
        pw.println("&nbsp;&nbsp;<input type='submit' name='" + ATTR_SUBMIT
                + "' value='Resolve' class='submit'>");
        pw.println("&nbsp;&nbsp;<input type='submit' name='" + ATTR_SUBMIT
                + "' value='Map' class='submit'>");
        pw.print("</form>");
        pw.print("</td>");
        pw.println("</tr>");

        if (msg != null) {
            pw.println("<tr class='content'>");
            pw.println("<td class='content'>&nbsp;</td>");
            pw.print("<td class='content' colspan='2'>");
            pw.print(ResponseUtil.escapeXml(msg));
            pw.println("</td>");
            pw.println("</tr>");
        }

        separatorHtml(pw);

        dumpMapHtml(
                pw,
                "Resolver Map Entries",
                "Lists the entries used by the ResourceResolver.resolve methods to map URLs to Resources",
                mapEntries.getResolveMaps());

        separatorHtml(pw);

        dumpMapHtml(
                pw,
                "Mapping Map Entries",
                "Lists the entries used by the ResourceResolver.map methods to map Resource Paths to URLs",
                mapEntries.getMapMaps());

        pw.println("</table>");

    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        final String test = request.getParameter(ATTR_TEST);
        String msg = null;
        if (test != null && test.length() > 0) {

            ResourceResolver resolver = null;
            try {
                // prepare the request for the resource resolver
                HttpServletRequest helper = new ResolverRequest(request, test);

                // get an administrative resource resolver
                resolver = resolverFactory
                        .getAdministrativeResourceResolver(null);

                // map or resolve as instructed
                Object result;
                if ("Map".equals(request.getParameter(ATTR_SUBMIT))) {
                    if (helper.getServerName() == null) {
                        result = resolver.map(helper.getPathInfo());
                    } else {
                        result = resolver.map(helper, helper.getPathInfo());
                    }
                } else {
                    result = resolver.resolve(helper, helper.getPathInfo());
                }

                // set the result to render the result
                msg = result.toString();

            } catch (final Throwable t) {

                // some error occurred, report it as a result
                msg = "Test Failure: " + t;

            } finally {
                if (resolver != null) {
                    resolver.close();
                }
            }

        }

        // finally redirect
        final String path = request.getContextPath() + request.getServletPath()
                + request.getPathInfo();
        final String redirectTo;
        if (msg == null) {
            redirectTo = path;
        } else {
            redirectTo = path + '?' + PAR_MSG + '=' + encodeParam(msg) + '&'
                    + PAR_TEST + '=' + encodeParam(test);
        }
        response.sendRedirect(redirectTo);
    }

    private String encodeParam(final String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // should never happen
            return value;
        }
    }

    // ---------- ConfigurationPrinter

    public void printConfiguration(PrintWriter pw) {
        final MapEntries mapEntries = resolverFactory.getMapEntries();

        dumpMapText(pw, "Resolver Map Entries", mapEntries.getResolveMaps());

        separatorText(pw);

        dumpMapText(pw, "Mapping Map Entries", mapEntries.getMapMaps());
    }

    // ---------- internal

    private void dumpMapHtml(PrintWriter pw, String title, String description,
            Collection<MapEntry> list) {

        titleHtml(pw, title, description);

        pw.println("<tr class='content'>");
        pw.println("<th class='content'>Pattern</th>");
        pw.println("<th class='content'>Replacement</th>");
        pw.println("<th class='content'>Redirect</th>");
        pw.println("</tr>");

        Set<String> usedPatterns = new HashSet<String>();
        
        for (MapEntry entry : list) {
            String pattern = entry.getPattern();
            pw.print("<tr class='content");
            if (!usedPatterns.add(pattern)) {
                pw.print(" duplicate");
            }
            pw.println("'>");
            pw.println("<td class='content' style='vertical-align: top'>"
                    + pattern + "</td>");

            pw.print("<td class='content' style='vertical-align: top'>");
            String[] repls = entry.getRedirect();
            for (String repl : repls) {
                pw.print(repl + "<br/>");
            }
            pw.println("</td>");

            pw.print("<td class='content' style='vertical-align: top'>");
            if (entry.isInternal()) {
                pw.print("internal");
            } else {
                pw.print("external: " + entry.getStatus());
            }
            pw.println("</td>");

        }
    }

    private void titleHtml(PrintWriter pw, String title, String description) {
        pw.println("<tr class='content'>");
        pw.println("<th colspan='3'class='content container'>" + title
                + "</th>");
        pw.println("</tr>");

        if (description != null) {
            pw.println("<tr class='content'>");
            pw.println("<td colspan='3'class='content'>" + description
                    + "</th>");
            pw.println("</tr>");
        }
    }

    private void separatorHtml(PrintWriter pw) {
        pw.println("<tr class='content'>");
        pw.println("<td class='content' colspan='3'>&nbsp;</td>");
        pw.println("</tr>");
    }

    private void dumpMapText(PrintWriter pw, String title,
            Collection<MapEntry> list) {

        pw.println(title);

        final String format = "%25s%25s%15s\r\n";
        pw.printf(format, "Pattern", "Replacement", "Redirect");

        for (MapEntry entry : list) {
            final List<String> redir = Arrays.asList(entry.getRedirect());
            final String status = entry.isInternal() ? "internal"
                    : "external: " + entry.getStatus();
            pw.printf(format, entry.getPattern(), redir, status);
        }
    }

    private void separatorText(PrintWriter pw) {
        pw.println();
    }

    /**
     * Method to retrieve static resources from this bundle.
     */
    @SuppressWarnings("unused")
    private URL getResource(final String path) {
        if (path.startsWith("/jcrresolver/res/ui/")) {
            return this.getClass().getResource(path.substring(12));
        }
        return null;
    }

    private static class ResolverRequest extends HttpServletRequestWrapper {

        private final URI uri;

        public ResolverRequest(HttpServletRequest request, String uriString)
                throws URIException {
            super(request);
            uri = new URI(uriString, false);
        }

        @Override
        public String getScheme() {
            return uri.getScheme();
        }

        @Override
        public String getServerName() {
            try {
                return uri.getHost();
            } catch (URIException ue) {
                return null;
            }
        }

        @Override
        public int getServerPort() {
            return uri.getPort();
        }

        @Override
        public String getPathInfo() {
            try {
                return uri.getPath();
            } catch (URIException ue) {
                return "";
            }
        }
    }

}
