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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.featureflags.ClientContext;
import org.apache.sling.featureflags.ExecutionContext;
import org.apache.sling.featureflags.Feature;
import org.apache.sling.featureflags.Features;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service implements the feature handling. It keeps track of all
 * {@link Feature} services.
 */
@Component
@Reference(
        name = "feature",
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        referenceInterface = Feature.class)
public class FeatureManager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ThreadLocal<ClientContext> perThreadClientContext = new ThreadLocal<ClientContext>();

    private final ClientContext defaultClientContext = new ClientContext() {
        @Override
        public boolean isEnabled(final String featureName) {
            return false;
        }

        @Override
        public Collection<Feature> getEnabledFeatures() {
            return Collections.emptyList();
        }
    };

    private final Map<String, List<FeatureDescription>> allFeatures = new HashMap<String, List<FeatureDescription>>();

    private Map<String, Feature> activeFeatures = Collections.emptyMap();

    private List<ServiceRegistration> services;

    @SuppressWarnings("serial")
    @Activate
    private void activate(BundleContext bundleContext) {
        ArrayList<ServiceRegistration> services = new ArrayList<ServiceRegistration>();
        services.add(bundleContext.registerService(Features.class.getName(), new FeaturesImpl(this), null));
        services.add(bundleContext.registerService(ResourceDecorator.class.getName(),
            new FeatureResourceDecorator(this), null));
        services.add(bundleContext.registerService(Servlet.class.getName(), new FeatureWebConsolePlugin(this),
            new Hashtable<String, Object>() {
                {
                    put("felix.webconsole.label", "features");
                    put("felix.webconsole.title", "Features");
                    put("felix.webconsole.category", "Sling");
                }
            }));
        services.add(bundleContext.registerService(Filter.class.getName(), new CurrentClientContextFilter(this),
            new Hashtable<String, Object>() {
                {
                    put("pattern", "/.*");
                    put("service.ranking", Integer.MIN_VALUE);
                }
            }));
        services.add(bundleContext.registerService(Filter.class.getName(), new CurrentClientContextFilter(this),
            new Hashtable<String, Object>() {
            {
                put("sling.filter.scope", "REQUEST");
                put("service.ranking", Integer.MIN_VALUE);
            }
        }));
        this.services = services;
    }

    @Deactivate
    private void deactivate() {
        if (this.services != null) {
            for (ServiceRegistration service : this.services) {
                if (service != null) {
                    service.unregister();
                }
            }
            this.services.clear();
            this.services = null;
        }
    }

    //--- Feature binding

    // bind method for Feature services
    @SuppressWarnings("unused")
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

    ClientContext getCurrentClientContext() {
        ClientContext result = perThreadClientContext.get();
        if (result == null) {
            result = defaultClientContext;
        }
        return result;
    }

    ClientContext setCurrentClientContext(final ServletRequest request) {
        final ClientContext current = perThreadClientContext.get();
        if (request instanceof HttpServletRequest) {
            final ExecutionContext providerContext = new ExecutionContextImpl((HttpServletRequest) request);
            final ClientContextImpl ctx = this.createClientContext(providerContext);
            perThreadClientContext.set(ctx);
        }
        return current;
    }

    void unsetCurrentClientContext(final ClientContext previous) {
        if (previous != null) {
            perThreadClientContext.set(previous);
        } else {
            perThreadClientContext.remove();
        }
    }

    ClientContext createClientContext(final ResourceResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("Resolver must not be null.");
        }
        final ExecutionContext providerContext = new ExecutionContextImpl(resolver);
        final ClientContext ctx = this.createClientContext(providerContext);
        return ctx;
    }

    ClientContext createClientContext(final HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null.");
        }
        final ExecutionContext providerContext = new ExecutionContextImpl(request);
        final ClientContext ctx = this.createClientContext(providerContext);
        return ctx;
    }

    private ClientContextImpl createClientContext(final ExecutionContext providerContext) {
        final Map<String, Feature> enabledFeatures = new HashMap<String, Feature>();
        for (final Map.Entry<String, Feature> entry : this.activeFeatures.entrySet()) {
            if (entry.getValue().isEnabled(providerContext)) {
                enabledFeatures.put(entry.getKey(), entry.getValue());
            }
        }

        return new ClientContextImpl(providerContext, enabledFeatures);
    }

    //--- Feature access

    Feature[] getAvailableFeatures() {
        final Map<String, Feature> activeFeatures = this.activeFeatures;
        return activeFeatures.values().toArray(new Feature[activeFeatures.size()]);
    }

    Feature getFeature(final String name) {
        return this.activeFeatures.get(name);
    }

    String[] getAvailableFeatureNames() {
        final Map<String, Feature> activeFeatures = this.activeFeatures;
        return activeFeatures.keySet().toArray(new String[activeFeatures.size()]);
    }

    boolean isAvailable(final String featureName) {
        return this.activeFeatures.containsKey(featureName);
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
