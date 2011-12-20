/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.adapter.internal;

import static org.apache.sling.api.adapter.AdapterFactory.ADAPTABLE_CLASSES;
import static org.apache.sling.api.adapter.AdapterFactory.ADAPTER_CLASSES;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(Servlet.class)
@Properties({ @Property(name = Constants.SERVICE_DESCRIPTION, value = "Adapter Web Console Plugin"),
        @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation"),
        @Property(name = "felix.webconsole.label", value = "adapters"),
        @Property(name = "felix.webconsole.title", value = "Sling Adapters"),
        @Property(name = "felix.webconsole.css", value = "/adapters/res/ui/adapters.css"),
        @Property(name = "felix.webconsole.configprinter.modes", value = "always") })
@SuppressWarnings("serial")
public class AdapterWebConsolePlugin extends HttpServlet implements ServiceTrackerCustomizer, BundleListener {

    private static final int INDENT = 4;

    private static final String ADAPTER_CONDITION = "adapter.condition";

    private Logger logger = LoggerFactory.getLogger(AdapterWebConsolePlugin.class);

    private List<AdaptableDescription> allAdaptables;
    private Map<Object, List<AdaptableDescription>> adapterServices;
    private Map<Bundle, List<AdaptableDescription>> adapterBundles;

    private ServiceTracker adapterTracker;

    private BundleContext bundleContext;

    public Object addingService(ServiceReference reference) {
        Object service = this.bundleContext.getService(reference);
        addServiceMetadata(reference, service);
        return service;
    }

    private void addServiceMetadata(ServiceReference reference, Object service) {
        final String[] adapters = OsgiUtil.toStringArray(reference.getProperty(ADAPTER_CLASSES));
        final String condition = OsgiUtil.toString(reference.getProperty(ADAPTER_CONDITION), null);
        final String[] adaptables = OsgiUtil.toStringArray(reference.getProperty(ADAPTABLE_CLASSES));
        final List<AdaptableDescription> descriptions = new ArrayList<AdaptableDescription>(adaptables.length);
        for (final String adaptable : adaptables) {
            descriptions.add(new AdaptableDescription(reference.getBundle(), adaptable, adapters, condition));
        }
        adapterServices.put(service, descriptions);
        update();
    }

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.STOPPED) {
            removeBundle(event.getBundle());
        } else if (event.getType() == BundleEvent.STARTED) {
            addBundle(event.getBundle());
        }
    }

    public void modifiedService(ServiceReference reference, Object service) {
        addServiceMetadata(reference, service);
    }

    public void removedService(ServiceReference reference, Object service) {
        adapterServices.remove(service);
    }

    @SuppressWarnings("unchecked")
    private void addBundle(Bundle bundle) {
        List<AdaptableDescription> descs = new ArrayList<AdaptableDescription>();
        try {
            Enumeration<URL> files = bundle.getResources("SLING-INF/adapters.json");
            if (files != null) {
                while (files.hasMoreElements()) {
                    InputStream stream = files.nextElement().openStream();
                    String contents = IOUtils.toString(stream);
                    IOUtils.closeQuietly(stream);
                    JSONObject obj = new JSONObject(contents);
                    for (Iterator<String> adaptableNames = obj.keys(); adaptableNames.hasNext();) {
                        String adaptableName = adaptableNames.next();
                        JSONObject adaptable = obj.getJSONObject(adaptableName);
                        for (Iterator<String> conditions = adaptable.keys(); conditions.hasNext();) {
                            String condition = conditions.next();
                            String[] adapters;
                            Object value = adaptable.get(condition);
                            if (value instanceof JSONArray) {
                                adapters = toStringArray((JSONArray) value);
                            } else {
                                adapters = new String[] { value.toString() };
                            }
                            descs.add(new AdaptableDescription(bundle, adaptableName, adapters, condition));
                        }
                    }
                }
            }
            if (!descs.isEmpty()) {
                adapterBundles.put(bundle, descs);
                update();
            }
        } catch (IOException e) {
            logger.error("Unable to load adapter descriptors for bundle " + bundle, e);
        } catch (JSONException e) {
            logger.error("Unable to load adapter descriptors for bundle " + bundle, e);
        }

    }

    private String[] toStringArray(JSONArray value) throws JSONException {
        List<String> result = new ArrayList<String>(value.length());
        for (int i = 0; i < value.length(); i++) {
            result.add(value.getString(i));
        }
        return result.toArray(new String[value.length()]);
    }

    private void removeBundle(Bundle bundle) {
        adapterBundles.remove(bundle);
        update();
    }

    private void update() {
        final List<AdaptableDescription> newList = new ArrayList<AdaptableDescription>();
        for (final List<AdaptableDescription> descriptions : adapterServices.values()) {
            newList.addAll(descriptions);
        }
        for (final List<AdaptableDescription> list : adapterBundles.values()) {
            newList.addAll(list);
        }
        Collections.sort(newList);
        allAdaptables = newList;
    }

    protected void activate(ComponentContext ctx) throws InvalidSyntaxException {
        this.bundleContext = ctx.getBundleContext();
        this.adapterServices = new HashMap<Object, List<AdaptableDescription>>();
        this.adapterBundles = new HashMap<Bundle, List<AdaptableDescription>>();
        for (Bundle bundle : this.bundleContext.getBundles()) {
            if (bundle.getState() == Bundle.ACTIVE) {
                addBundle(bundle);
            }
        }

        this.bundleContext.addBundleListener(this);
        Filter filter = this.bundleContext.createFilter("(&(adaptables=*)(adapters=*))");
        this.adapterTracker = new ServiceTracker(this.bundleContext, filter, this);
        this.adapterTracker.open();
    }

    protected void deactivate(ComponentContext ctx) {
        this.bundleContext.removeBundleListener(this);
        this.adapterTracker.close();
        this.adapterServices = null;
        this.adapterBundles = null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo().endsWith("/data.json")) {
            getJson(resp);
        } else {
            getHtml(resp);
        }

    }

    private void getJson(final HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        try {
            final JSONObject obj = new JSONObject();
            for (final AdaptableDescription desc : allAdaptables) {
                final JSONObject adaptableObj;
                if (obj.has(desc.adaptable)) {
                    adaptableObj = obj.getJSONObject(desc.adaptable);
                } else {
                    adaptableObj = new JSONObject();
                    obj.put(desc.adaptable, adaptableObj);
                }
                for (final String adapter : desc.adapters) {
                    adaptableObj.accumulate(desc.condition == null ? "" : desc.condition, adapter);
                }

            }
            resp.getWriter().println(obj.toString(INDENT));
        } catch (JSONException e) {
            throw new ServletException("Unable to produce JSON", e);
        }
    }

    private void getHtml(HttpServletResponse resp) throws IOException {
        final PrintWriter writer = resp.getWriter();
        writer.println("<p class=\"statline ui-state-highlight\">${Introduction}</p>");
        writer.println("<p>${intro}</p>");
        writer.println("<p class=\"statline ui-state-highlight\">${How to Use This Information}</p>");
        writer.println("<p>${usage}</p>");
        writer.println("<table class=\"adapters nicetable\">");
        writer.println("<thead><tr><th class=\"header\">${Adaptable Class}</th><th class=\"header\">${Adapter Class}</th><th class=\"header\">${Condition}</th><th class=\"header\">${Providing Bundle}</th></tr></thead>");
        String rowClass = "odd";
        for (final AdaptableDescription desc : allAdaptables) {
            writer.printf("<tr class=\"%s ui-state-default\"><td>%s</td>", rowClass, desc.adaptable);
            writer.print("<td>");
            for (final String adapter : desc.adapters) {
                writer.print(adapter);
                writer.print("<br/>");
            }
            writer.print("</td>");
            if (desc.condition == null) {
                writer.print("<td>&nbsp;</td>");
            } else {
                writer.printf("<td>%s</td>", desc.condition);
            }
            writer.printf("<td><a href=\"${pluginRoot}/../bundles/%s\">%s (%s)</a></td>", desc.bundle.getBundleId(),
                    desc.bundle.getSymbolicName(), desc.bundle.getBundleId());
            writer.println("</tr>");

            if (rowClass.equals("odd")) {
                rowClass = "even";
            } else {
                rowClass = "odd";
            }
        }
        writer.println("</table>");
    }

    public void printConfiguration(final PrintWriter pw) {
        pw.println("Current Apache Sling Adaptables:");
        for (final AdaptableDescription desc : allAdaptables) {
            pw.printf("Adaptable: %s\n", desc.adaptable);
            if (desc.condition != null) {
                pw.printf("Condition: %s\n", desc.condition);
            }
            pw.printf("Providing Bundle: %s\n", desc.bundle.getSymbolicName());
            pw.printf("Available Adapters:\n");
            for (final String adapter : desc.adapters) {
                pw.print(" * ");
                pw.println(adapter);
            }
            pw.println();
        }
    }

    /**
     * Method to retreive static resources from this bundle.
     */
    @SuppressWarnings("unused")
    private URL getResource(String path) {
        if (path.startsWith("/adapters/res/ui/")) {
            return this.getClass().getResource(path.substring(9));
        }
        return null;
    }

    class AdaptableDescription implements Comparable<AdaptableDescription> {
        private final String adaptable;
        private final String[] adapters;
        private final String condition;
        private final Bundle bundle;

        public AdaptableDescription(final Bundle bundle, final String adaptable, final String[] adapters,
                final String condition) {
            this.adaptable = adaptable;
            this.adapters = adapters;
            this.condition = condition;
            this.bundle = bundle;
        }

        @Override
        public String toString() {
            return "AdapterDescription [adaptable=" + this.adaptable + ", adapters=" + Arrays.toString(this.adapters)
                    + ", condition=" + this.condition + ", bundle=" + this.bundle + "]";
        }

        public int compareTo(AdaptableDescription o) {
            return new CompareToBuilder().append(this.adaptable, o.adaptable).append(this.condition, o.condition)
                    .append(this.adapters.length, o.adapters.length)
                    .append(this.bundle.getBundleId(), o.bundle.getBundleId()).toComparison();
        }

    }

}
