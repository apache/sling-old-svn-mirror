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
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
@Component(service = Servlet.class,
    property = {
            Constants.SERVICE_DESCRIPTION + "=Adapter Web Console Plugin",
            Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
            "felix.webconsole.label=adapters",
            "felix.webconsole.title=Sling Adapters",
            "felix.webconsole.css=/adapters/res/ui/adapters.css",
            "felix.webconsole.configprinter.modes=always",
            "felix.webconsole.category=Sling"
    })
public class AdapterWebConsolePlugin extends HttpServlet implements ServiceTrackerCustomizer, BundleListener {

    private static final int INDENT = 4;

    private static final String ADAPTER_CONDITION = "adapter.condition";

    private static final String ADAPTER_DEPRECATED = "adapter.deprecated";

    private final Logger logger = LoggerFactory.getLogger(AdapterWebConsolePlugin.class);

    @Reference
    private PackageAdmin packageAdmin;

    private List<AdaptableDescription> allAdaptables;
    private Map<ServiceReference, List<AdaptableDescription>> adapterServiceReferences;
    private Map<Bundle, List<AdaptableDescription>> adapterBundles;

    private ServiceTracker adapterTracker;

    private BundleContext bundleContext;

    @Override
    public Object addingService(final ServiceReference reference) {
        final Object service = this.bundleContext.getService(reference);
        addServiceMetadata(reference, service);
        return service;
    }

    private void addServiceMetadata(final ServiceReference reference, final Object service) {
        final String[] adapters = PropertiesUtil.toStringArray(reference.getProperty(ADAPTER_CLASSES));
        final String condition = PropertiesUtil.toString(reference.getProperty(ADAPTER_CONDITION), null);
        final boolean deprecated = PropertiesUtil.toBoolean(reference.getProperty(ADAPTER_DEPRECATED), false);
        final String[] adaptables = PropertiesUtil.toStringArray(reference.getProperty(ADAPTABLE_CLASSES));
        final List<AdaptableDescription> descriptions = new ArrayList<>(adaptables.length);
        final boolean allowedInPrivatePackage = PropertiesUtil.toBoolean(reference.getProperty(AdapterManagerImpl.ALLOWED_IN_PRIVATE), false);
        for (final String adaptable : adaptables) {
            descriptions.add(new AdaptableDescription(reference.getBundle(), adaptable, adapters, condition, deprecated));
        }
        synchronized (this) {
            adapterServiceReferences.put(reference, descriptions);
            update();
        }
    }

    @Override
    public void bundleChanged(final BundleEvent event) {
        if (event.getType() == BundleEvent.STOPPED) {
            removeBundle(event.getBundle());
        } else if (event.getType() == BundleEvent.STARTED) {
            addBundle(event.getBundle());
        }
    }

    @Override
    public void modifiedService(final ServiceReference reference, final Object service) {
        addServiceMetadata(reference, service);
    }

    @Override
    public void removedService(final ServiceReference reference, final Object service) {
        synchronized (this) {
            adapterServiceReferences.remove(reference);
            update();
        }
    }

    @SuppressWarnings("unchecked")
    private void addBundle(final Bundle bundle) {
        final List<AdaptableDescription> descs = new ArrayList<>();
        try {
            final Enumeration<URL> files = bundle.getResources("SLING-INF/adapters.json");
            if (files != null) {
                while (files.hasMoreElements()) {
                    final InputStream stream = files.nextElement().openStream();
                    final String contents = IOUtils.toString(stream);
                    IOUtils.closeQuietly(stream);
                    Map<String, Object> config = new HashMap<>();
                    config.put("org.apache.johnzon.supports-comments", true);
                    final JsonObject obj = Json.createReaderFactory(config).createReader(new StringReader(contents)).readObject();
                    for (final Iterator<String> adaptableNames = obj.keySet().iterator(); adaptableNames.hasNext();) {
                        final String adaptableName = adaptableNames.next();
                        final JsonObject adaptable = obj.getJsonObject(adaptableName);
                        for (final Iterator<String> conditions = adaptable.keySet().iterator(); conditions.hasNext();) {
                            final String condition = conditions.next();
                            String[] adapters;
                            final Object value = adaptable.get(condition);
                            if (value instanceof JsonArray) {
                                adapters = toStringArray((JsonArray) value);
                            } else {
                                adapters = new String[] { unbox(value).toString() };
                            }
                            descs.add(new AdaptableDescription(bundle, adaptableName, adapters, condition, false));
                        }
                    }
                }
            }
            if (!descs.isEmpty()) {
                synchronized ( this ) {
                    adapterBundles.put(bundle, descs);
                    update();
                }
            }
        } catch (final IOException e) {
            logger.error("Unable to load adapter descriptors for bundle " + bundle, e);
        } catch (final JsonException e) {
            logger.error("Unable to load adapter descriptors for bundle " + bundle, e);
        } catch (IllegalStateException e) {
            logger.debug("Unable to load adapter descriptors for bundle " + bundle);
        }

    }

    private Object unbox(Object o) {
        if (o instanceof JsonValue) {
            switch (((JsonValue)o).getValueType()) {
                case FALSE:
                    return false;
                case TRUE:
                    return true;
                case NULL:
                    return null;
                case NUMBER:
                    return ((JsonNumber) o).isIntegral() ? ((JsonNumber)o).longValue() : ((JsonNumber) o).doubleValue();
                case STRING:
                    return ((JsonString) o).getString();
                default:
                    return o;
            }
        }
        return o;
    }

    private String[] toStringArray(final JsonArray value) {
        final List<String> result = new ArrayList<>(value.size());
        for (int i = 0; i < value.size(); i++) {
            result.add(value.getString(i));
        }
        return result.toArray(new String[value.size()]);
    }

    private void removeBundle(final Bundle bundle) {
        synchronized ( this ) {
            adapterBundles.remove(bundle);
            update();
        }
    }

    private void update() {
        final List<AdaptableDescription> newList = new ArrayList<>();
        for (final List<AdaptableDescription> descriptions : adapterServiceReferences.values()) {
            newList.addAll(descriptions);
        }
        for (final List<AdaptableDescription> list : adapterBundles.values()) {
            newList.addAll(list);
        }
        Collections.sort(newList);
        allAdaptables = newList;
    }

    protected void activate(final ComponentContext ctx) throws InvalidSyntaxException {
        this.bundleContext = ctx.getBundleContext();
        this.adapterServiceReferences = new HashMap<>();
        this.adapterBundles = new HashMap<>();
        for (final Bundle bundle : this.bundleContext.getBundles()) {
            if (bundle.getState() == Bundle.ACTIVE) {
                addBundle(bundle);
            }
        }

        this.bundleContext.addBundleListener(this);
        final Filter filter = this.bundleContext.createFilter("(&(adaptables=*)(adapters=*)(" + Constants.OBJECTCLASS + "=" + AdapterFactory.SERVICE_NAME + "))");
        this.adapterTracker = new ServiceTracker(this.bundleContext, filter, this);
        this.adapterTracker.open();
    }

    protected void deactivate(final ComponentContext ctx) {
        this.bundleContext.removeBundleListener(this);
        this.adapterTracker.close();
        this.adapterServiceReferences = null;
        this.adapterBundles = null;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo().endsWith("/data.json")) {
            getJson(resp);
        } else {
            getHtml(resp);
        }

    }

    private void getJson(final HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        try {
            Map<String, Map<String, List<String>>> values = new HashMap<>();
            for (final AdaptableDescription desc : allAdaptables) {
                final Map<String, List<String>> adaptableObj;
                if (values.containsKey(desc.adaptable))
                {
                    adaptableObj = values.get(desc.adaptable);
                }
                else {
                    adaptableObj = new HashMap<>();
                    values.put(desc.adaptable, adaptableObj);
                }
                for (final String adapter : desc.adapters) {
                    List<String> conditions = adaptableObj.get(desc.condition == null ? "" : desc.condition);
                    if (conditions == null)
                    {
                        conditions = new ArrayList<>();
                        adaptableObj.put(desc.condition == null ? "" : desc.condition, conditions);
                    }
                    conditions.add(adapter);
                }

            }
            final JsonObjectBuilder obj = Json.createObjectBuilder();

            for (Map.Entry<String, Map<String, List<String>>> entry : values.entrySet())
            {
                JsonObjectBuilder adaptable = Json.createObjectBuilder();

                for (Map.Entry<String, List<String>> subEnty : entry.getValue().entrySet())
                {
                    if (subEnty.getValue().size() > 1)
                    {
                        JsonArrayBuilder array = Json.createArrayBuilder();
                        for (String condition : subEnty.getValue())
                        {
                            array.add(condition);
                        }
                        adaptable.add(subEnty.getKey(),  array);
                    }
                    else
                    {
                        adaptable.add(subEnty.getKey(), subEnty.getValue().get(0));
                    }
                }

                obj.add(entry.getKey(), adaptable);
            }

            Json.createGenerator(resp.getWriter()).write(obj.build()).flush();
        } catch (final JsonException e) {
            throw new ServletException("Unable to produce JSON", e);
        }
    }

    private void getHtml(final HttpServletResponse resp) throws IOException {
        final PrintWriter writer = resp.getWriter();
        writer.println("<p class=\"statline ui-state-highlight\">${Introduction}</p>");
        writer.println("<p>${intro}</p>");
        writer.println("<p class=\"statline ui-state-highlight\">${How to Use This Information}</p>");
        writer.println("<p>${usage}</p>");
        writer.println("<table class=\"adapters nicetable\">");
        writer.println("<thead><tr><th class=\"header\">${Adaptable Class}</th><th class=\"header\">${Adapter Class}</th><th class=\"header\">${Condition}</th><th class=\"header\">${Deprecated}</th><th class=\"header\">${Providing Bundle}</th></tr></thead>");
        String rowClass = "odd";
        for (final AdaptableDescription desc : allAdaptables) {
            writer.printf("<tr class=\"%s ui-state-default\"><td>", rowClass);
            boolean packageExported = AdapterManagerImpl.checkPackage(packageAdmin, desc.adaptable);
            if (!packageExported) {
                writer.print("<span class='error'>");
            }
            writer.print(desc.adaptable);
            if (!packageExported) {
                writer.print("</span>");
            }
            writer.print("</td>");
            writer.print("<td>");
            for (final String adapter : desc.adapters) {
                packageExported = AdapterManagerImpl.checkPackage(packageAdmin, adapter);
                if (!packageExported) {
                    writer.print("<span class='error'>");
                }
                writer.print(adapter);
                if (!packageExported) {
                    writer.print("</span>");
                }
                writer.print("<br/>");
            }
            writer.print("</td>");
            if (desc.condition == null) {
                writer.print("<td>&nbsp;</td>");
            } else {
                writer.printf("<td>%s</td>", desc.condition);
            }
            if (desc.deprecated) {
                writer.print("<td>${Deprecated}</td>");
            } else {
                writer.print("<td></td>");
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
    private URL getResource(final String path) {
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
        private final boolean deprecated;

        public AdaptableDescription(final Bundle bundle, final String adaptable, final String[] adapters,
                        final String condition, boolean deprecated) {
            this.adaptable = adaptable;
            this.adapters = adapters;
            this.condition = condition;
            this.bundle = bundle;
            this.deprecated = deprecated;
        }

        @Override
        public String toString() {
            return "AdapterDescription [adaptable=" + this.adaptable + ", adapters=" + Arrays.toString(this.adapters)
                            + ", condition=" + this.condition + ", bundle=" + this.bundle + ", deprecated= " + this.deprecated + "]";
        }

        @Override
        public int compareTo(final AdaptableDescription o) {
            return new CompareToBuilder().append(this.adaptable, o.adaptable).append(this.condition, o.condition)
                            .append(this.adapters.length, o.adapters.length)
                            .append(this.bundle.getBundleId(), o.bundle.getBundleId()).toComparison();
        }

    }

}
