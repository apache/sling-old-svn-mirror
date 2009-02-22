/*
 * $Url: $
 * $Id: $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.jcr.resource.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.internal.helper.MapEntries;
import org.apache.sling.jcr.resource.internal.helper.MapEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class JcrResourceResolverPlugin extends AbstractWebConsolePlugin {

    private static final String ATTR_TEST = "plugin.test";

    private static final String ATTR_SUBMIT = "plugin.submit";

    private static final String ATTR_RESULT = "plugin.result";

    private final JcrResourceResolverFactoryImpl resolverFactory;

    private ServiceRegistration service;

    JcrResourceResolverPlugin(BundleContext context,
            JcrResourceResolverFactoryImpl resolverFactory) {
        this.resolverFactory = resolverFactory;

        activate(context);

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION,
            "JCRResourceResolver2 Web Console Plugin");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(Constants.SERVICE_PID, getClass().getName());
        props.put(WebConsoleConstants.PLUGIN_LABEL, getLabel());

        service = context.registerService(Servlet.class.getName(), this, props);
    }

    void dispose() {
        if (service != null) {
            service.unregister();
            deactivate();
            service = null;
        }
    }

    @Override
    public String getLabel() {
        return "jcrresolver";
    }

    @Override
    public String getTitle() {
        return "JCR ResourceResolver";
    }

    @Override
    protected void renderContent(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String test = (String) request.getAttribute(ATTR_TEST);
        if (test == null) test = "";
        String result = (String) request.getAttribute(ATTR_RESULT);

        PrintWriter pw = response.getWriter();

        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");

        MapEntries mapEntries = resolverFactory.getMapEntries();

        title(
            pw,
            "Configuration Test",
            "To test the configuration, enter an URL or a resource path into the field and click 'Resolve' to resolve the URL or click 'Map' to map the resource path");

        pw.println("<tr class='content'>");
        pw.println("<td class='content'>Test</td>");
        pw.print("<td class='content' colspan='2'>");
        pw.print("<form method='post'>");
        pw.println("<input type='text' name='" + ATTR_TEST + "' value='" + test
            + "' class='input' size='50'>");
        pw.println("&nbsp;&nbsp;<input type='submit' name='" + ATTR_SUBMIT
            + "' value='Resolve' class='submit'>");
        pw.println("&nbsp;&nbsp;<input type='submit' name='" + ATTR_SUBMIT
            + "' value='Map' class='submit'>");
        pw.print("</form>");
        pw.print("</td>");
        pw.println("</tr>");

        if (result != null) {
            pw.println("<tr class='content'>");
            pw.println("<td class='content'>&nbsp;</td>");
            pw.println("<td class='content' colspan='2'>" + result + "</td>");
            pw.println("</tr>");
        }

        separator(pw);

        dumpMap(
            pw,
            "Resolver Map Entries",
            "Lists the entries used by the ResourceResolver.resolve methods to map URLs to Resources",
            mapEntries.getResolveMaps());

        separator(pw);

        dumpMap(
            pw,
            "Mapping Map Entries",
            "Lists the entries used by the ResourceResolver.map methods to map Resource Paths to URLs",
            mapEntries.getMapMaps());

        pw.println("</table>");

    }

    private void dumpMap(PrintWriter pw, String title, String description,
            Collection<MapEntry> list) {

        title(pw, title, description);

        pw.println("<tr class='content'>");
        pw.println("<th class='content'>Pattern</th>");
        pw.println("<th class='content'>Replacement</th>");
        pw.println("<th class='content'>Redirect</th>");
        pw.println("</tr>");

        for (MapEntry entry : list) {
            pw.println("<tr class='content'>");
            pw.println("<td class='content' style='vertical-align: top'>"
                + entry.getPattern() + "</td>");

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

    private void title(PrintWriter pw, String title, String description) {
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

    private void separator(PrintWriter pw) {
        pw.println("<tr class='content'>");
        pw.println("<td class='content' colspan='3'>&nbsp;</td>");
        pw.println("</tr>");
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        String test = request.getParameter(ATTR_TEST);
        if (test != null && test.length() > 0) {

            // set test value for the re-rendering of the form
            request.setAttribute(ATTR_TEST, test);

            Session session = null;
            try {
                // prepare the request for the resource resolver
                HttpServletRequest helper = new ResolverRequest(request, test);

                // get the resource resolver with an administrative session
                session = resolverFactory.getRepository().loginAdministrative(
                    null);
                ResourceResolver resolver = resolverFactory.getResourceResolver(session);

                // map or resolve as instructed
                Object result;
                if ("Map".equals(request.getParameter(ATTR_SUBMIT))) {
                    result = resolver.map(helper, helper.getPathInfo());
                } else {
                    result = resolver.resolve(helper, helper.getPathInfo());
                }

                // set the result to render the result
                request.setAttribute(ATTR_RESULT, result.toString());

            } catch (Throwable t) {
                // TOOD: log
            } finally {
                if (session != null) {
                    session.logout();
                }
            }

        }

        // finally render the result
        doGet(request, response);
    }

    private static class ResolverRequest extends HttpServletRequestWrapper {

        private final URI uri;

        public ResolverRequest(HttpServletRequest request, String uriString)
                throws URISyntaxException {
            super(request);
            uri = new URI(uriString);
        }

        @Override
        public String getScheme() {
            return uri.getScheme();
        }

        @Override
        public String getServerName() {
            return uri.getHost();
        }

        @Override
        public int getServerPort() {
            return uri.getPort();
        }

        @Override
        public String getPathInfo() {
            return uri.getPath();
        }
    }

}
