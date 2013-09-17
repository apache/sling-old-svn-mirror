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
package org.apache.sling.bundleresource.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.ResourceProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

class BundleResourceWebConsolePlugin extends HttpServlet {

    private static final long serialVersionUID = 566337139719695235L;

    private static final String LABEL = "bundleresources";

    private ServiceRegistration serviceRegistration;

    private ServiceTracker providerTracker;

    private List<BundleResourceProvider> provider = new ArrayList<BundleResourceProvider>();

    //--------- setup and shutdown

    private static BundleResourceWebConsolePlugin INSTANCE;

    static void initPlugin(BundleContext context) {
        if (INSTANCE == null) {
            BundleResourceWebConsolePlugin tmp = new BundleResourceWebConsolePlugin();
            tmp.activate(context);
            INSTANCE = tmp;
        }
    }

    static void destroyPlugin() {
        if (INSTANCE != null) {
            try {
                INSTANCE.deactivate();
            } finally {
                INSTANCE = null;
            }
        }
    }

    // private constructor to force using static setup and shutdown
    private BundleResourceWebConsolePlugin() {
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res)
    throws ServletException, IOException {
        PrintWriter pw = res.getWriter();

        pw.println("<table class='content' cellpadding='0' cellspacing='0' width='100%'>");

        pw.println("<tr class='content'>");
        pw.println("<th colspan='2' class='content container'>Bundle Resource Provider</th>");
        pw.println("</tr>");

        BundleResourceProvider[] brp = provider.toArray(new BundleResourceProvider[provider.size()]);
        for (BundleResourceProvider bundleResourceProvider : brp) {

            BundleResourceCache cache = bundleResourceProvider.getBundleResourceCache();
            MappedPath[] paths = bundleResourceProvider.getMappedPaths();

            pw.println("<tr class='content'>");

            pw.println("<td class='content'>");
            pw.println(cache.getBundle().getBundleId());
            pw.println("</td>");

            pw.println("<td class='content'>");
            pw.println(getName(cache.getBundle()));
            pw.println("</td>");

            pw.println("</tr>");

            pw.println("<tr class='content'>");
            pw.println("<td class='content'>&nbsp;</td>");

            pw.println("<td class='content'>");

            pw.println("<table>");

            pw.println("<tr>");
            pw.println("<td>Mappings</td>");
            pw.println("<td>");
            for (MappedPath mappedPath : paths) {
                pw.print(mappedPath.getResourceRoot());
                if (mappedPath.getEntryRoot() != null) {
                    pw.print(" ==> ");
                    pw.print(mappedPath.getEntryRoot());
                }
                pw.print("<br>");
            }
            pw.println("</td>");
            pw.println("</tr>");

            pw.println("<tr>");
            pw.println("<td>Entry Cache</td>");
            pw.printf("<td>Size: %d, Limit: %d</td>%n",
                cache.getEntryCacheSize(), cache.getEntryCacheMaxSize());
            pw.println("</tr>");

            pw.println("<tr>");
            pw.println("<td>List Cache</td>");
            pw.printf("<td>Size: %d, Limit: %d</td>%n",
                cache.getListCacheSize(), cache.getListCacheMaxSize());
            pw.println("</tr>");

            pw.println("</table>");

            pw.println("</td>");
            pw.println("</tr>");
        }

        pw.println("</table>");

    }

    public void activate(BundleContext context) {
        providerTracker = new ServiceTracker(context,
            ResourceProvider.SERVICE_NAME, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                Object service = super.addingService(reference);
                if (service instanceof BundleResourceProvider) {
                    provider.add((BundleResourceProvider) service);
                }
                return service;
            }

            @Override
            public void removedService(ServiceReference reference,
                    Object service) {
                if (service instanceof BundleResourceProvider) {
                    provider.remove(service);
                }
                super.removedService(reference, service);
            }
        };
        providerTracker.open();

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION,
            "Web Console Plugin for Bundle Resource Providers");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(Constants.SERVICE_PID, getClass().getName());
        props.put("felix.webconsole.label", LABEL);
        props.put("felix.webconsole.title", "Bundle Resource Provider");
        props.put("felix.webconsole.category", "Sling");

        serviceRegistration = context.registerService(
            Servlet.class.getName(), this, props);
    }

    public void deactivate() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }

        if (providerTracker != null) {
            providerTracker.close();
            providerTracker = null;
        }
    }

    private String getName(Bundle bundle) {
        String name = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
        if (name == null) {
            name = bundle.getSymbolicName();
            if (name == null) {
                name = bundle.getLocation();
            }
        }
        return name;
    }
}
