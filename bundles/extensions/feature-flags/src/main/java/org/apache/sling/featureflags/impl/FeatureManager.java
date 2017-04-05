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
package org.apache.sling.featureflags.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.request.ResponseUtil;
import org.apache.sling.featureflags.Feature;
import org.apache.sling.featureflags.Features;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service implements the feature handling. It keeps track of all
 * {@link Feature} services.
 */
@Component(service = {Features.class, Filter.class, Servlet.class},
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = {
                   HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=org.apache.sling)",
                   HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN + "=/",
                   "felix.webconsole.label=features",
                   "felix.webconsole.title=Features",
                   "felix.webconsole.category=Sling",
                   Constants.SERVICE_RANKING + ":Integer=16384",
                   Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
           })
public class FeatureManager implements Features, Filter, Servlet {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ThreadLocal<ExecutionContextImpl> perThreadClientContext = new ThreadLocal<ExecutionContextImpl>();

    private final Map<String, List<FeatureDescription>> allFeatures = new HashMap<String, List<FeatureDescription>>();

    private Map<String, Feature> activeFeatures = Collections.emptyMap();

    private ServletConfig servletConfig;

    //--- Features

    @Override
    public Feature[] getFeatures() {
        final Map<String, Feature> activeFeatures = this.activeFeatures;
        return activeFeatures.values().toArray(new Feature[activeFeatures.size()]);
    }

    @Override
    public Feature getFeature(final String name) {
        return this.activeFeatures.get(name);
    }

    @Override
    public boolean isEnabled(final String featureName) {
        final Feature feature = this.getFeature(featureName);
        if (feature != null) {
            return getCurrentExecutionContext().isEnabled(feature);
        }
        return false;
    }

    //--- Filter

    @Override
    public void init(final FilterConfig filterConfig) {
        // nothing to do
    }

    @Override
    public void doFilter(final ServletRequest request,
            final ServletResponse response,
            final FilterChain chain)
    throws IOException, ServletException {
        this.pushContext((HttpServletRequest) request);
        try {
            chain.doFilter(request, response);
        } finally {
            this.popContext();
        }
    }

    @Override
    public void destroy() {
        // method shared by Servlet and Filter interface
        this.servletConfig = null;
    }

    //--- Servlet

    @Override
    public void init(final ServletConfig config) {
        this.servletConfig = config;
    }

    @Override
    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }

    @Override
    public String getServletInfo() {
        return "Features";
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws IOException {
        if ("GET".equals(((HttpServletRequest) req).getMethod())) {
            final PrintWriter pw = res.getWriter();
            final Feature[] features = getFeatures();
            if (features == null || features.length == 0) {
                pw.println("<p class='statline ui-state-highlight'>No Features currently defined</p>");
            } else {
                pw.printf("<p class='statline ui-state-highlight'>%d Feature(s) currently defined</p>%n",
                    features.length);
                pw.println("<table class='nicetable'>");
                pw.println("<tr><th>Name</th><th>Description</th><th>Enabled</th></tr>");
                final ExecutionContextImpl ctx = getCurrentExecutionContext();
                for (final Feature feature : features) {
                    pw.printf("<tr><td>%s</td><td>%s</td><td>%s</td></tr>%n", ResponseUtil.escapeXml(feature.getName()),
                            ResponseUtil.escapeXml(feature.getDescription()), ctx.isEnabled(feature));
                }
                pw.println("</table>");
            }
        } else {
            ((HttpServletResponse) res).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            res.flushBuffer();
        }
    }

    //--- Feature binding

    // bind method for Feature services
    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    private void bindFeature(final Feature f, final Map<String, Object> props) {
        synchronized (this.allFeatures) {
            final String name = f.getName();
            final FeatureDescription info = new FeatureDescription(f, props);

            List<FeatureDescription> candidates = this.allFeatures.get(name);
            if (candidates == null) {
                candidates = new ArrayList<FeatureDescription>();
                this.allFeatures.put(name, candidates);
            }
            candidates.add(info);
            Collections.sort(candidates);

            this.calculateActiveProviders();
        }
    }

    // unbind method for Feature services
    @SuppressWarnings("unused")
    private void unbindFeature(final Feature f, final Map<String, Object> props) {
        synchronized (this.allFeatures) {
            final String name = f.getName();
            final FeatureDescription info = new FeatureDescription(f, props);

            final List<FeatureDescription> candidates = this.allFeatures.get(name);
            if (candidates != null) { // sanity check
                candidates.remove(info);
                if (candidates.size() == 0) {
                    this.allFeatures.remove(name);
                }
            }
            this.calculateActiveProviders();
        }
    }

    // calculates map of active features (eliminating Feature name
    // collisions). Must be called while synchronized on this.allFeatures
    private void calculateActiveProviders() {
        final Map<String, Feature> activeMap = new HashMap<String, Feature>();
        for (final Map.Entry<String, List<FeatureDescription>> entry : this.allFeatures.entrySet()) {
            final FeatureDescription desc = entry.getValue().get(0);
            activeMap.put(entry.getKey(), desc.feature);
            if (entry.getValue().size() > 1) {
                logger.warn("More than one feature service for feature {}", entry.getKey());
            }
        }
        this.activeFeatures = activeMap;
    }

    //--- Client Context management and access

    void pushContext(final HttpServletRequest request) {
        this.perThreadClientContext.set(new ExecutionContextImpl(this, request));
    }

    void popContext() {
        this.perThreadClientContext.set(null);
    }

    ExecutionContextImpl getCurrentExecutionContext() {
        ExecutionContextImpl ctx = this.perThreadClientContext.get();
        return (ctx != null) ? ctx : new ExecutionContextImpl(this, null);
    }

    /**
     * Internal class caching some feature meta data like service id and
     * ranking.
     */
    private final static class FeatureDescription implements Comparable<FeatureDescription> {

        public final int ranking;

        public final long serviceId;

        public final Feature feature;

        public FeatureDescription(final Feature feature, final Map<String, Object> props) {
            this.feature = feature;
            final Object sr = props.get(Constants.SERVICE_RANKING);
            if (sr instanceof Integer) {
                this.ranking = (Integer) sr;
            } else {
                this.ranking = 0;
            }
            this.serviceId = (Long) props.get(Constants.SERVICE_ID);
        }

        @Override
        public int compareTo(final FeatureDescription o) {
            if (this.ranking < o.ranking) {
                return 1;
            } else if (this.ranking > o.ranking) {
                return -1;
            }
            // If ranks are equal, then sort by service id in descending order.
            return (this.serviceId < o.serviceId) ? -1 : 1;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof FeatureDescription) {
                return ((FeatureDescription) obj).serviceId == this.serviceId;
            }
            return false;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (serviceId ^ (serviceId >>> 32));
            return result;
        }
    }
}
