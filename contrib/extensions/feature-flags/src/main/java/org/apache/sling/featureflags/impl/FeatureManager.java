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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.featureflags.ClientContext;
import org.apache.sling.featureflags.Feature;
import org.apache.sling.featureflags.Features;
import org.apache.sling.featureflags.ExecutionContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service implements the feature handling.
 * It keeps track of all {@link Feature} services.
 */
@Component
@Reference(name="feature",
           cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
           policy=ReferencePolicy.DYNAMIC,
           referenceInterface=Feature.class)
public class FeatureManager implements Features {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, List<FeatureDescription>> allFeatures = new HashMap<String, List<FeatureDescription>>();

    private Map<String, FeatureDescription> activeFeatures = new TreeMap<String, FeatureDescription>();

    /**
     * Bind a new feature
     */
    protected void bindFeature(final Feature f, final Map<String, Object> props) {
        synchronized ( this.allFeatures ) {
            final String name = f.getName();
            final FeatureDescription info = new FeatureDescription(f, props);

            List<FeatureDescription> candidates = this.allFeatures.get(name);
            if ( candidates == null ) {
                candidates = new ArrayList<FeatureDescription>();
                this.allFeatures.put(name, candidates);
            }
            candidates.add(info);
            Collections.sort(candidates);

            this.calculateActiveProviders();
        }
    }

    /**
     * Unbind a feature
     */
    protected void unbindFeature(final Feature f, final Map<String, Object> props) {
        synchronized ( this.allFeatures ) {
            final String name = f.getName();
            final FeatureDescription info = new FeatureDescription(f, props);

            final List<FeatureDescription> candidates = this.allFeatures.get(name);
            if ( candidates != null ) { // sanity check
                candidates.remove(info);
                if ( candidates.size() == 0 ) {
                    this.allFeatures.remove(name);
                }
            }
            this.calculateActiveProviders();
        }
    }

    private void calculateActiveProviders() {
        final Map<String, FeatureDescription> activeMap = new TreeMap<String, FeatureDescription>();
        for(final Map.Entry<String, List<FeatureDescription>> entry : this.allFeatures.entrySet()) {
            final FeatureDescription desc = entry.getValue().get(0);

            activeMap.put(entry.getKey(), desc);
            if ( entry.getValue().size() > 1 ) {
                logger.warn("More than one feature service for feature {}", entry.getKey());
            }
        }
        this.activeFeatures = activeMap;
    }

    private final ThreadLocal<ClientContextImpl> perThreadClientContext = new ThreadLocal<ClientContextImpl>();

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

    @Override
    public ClientContext getCurrentClientContext() {
        ClientContext result = perThreadClientContext.get();
        if ( result == null ) {
            result = defaultClientContext;
        }
        return result;
    }

    public void setCurrentClientContext(final SlingHttpServletRequest request) {
        final ExecutionContext providerContext = new ExecutionContextImpl(request);
        final ClientContextImpl ctx = this.createClientContext(providerContext);
        perThreadClientContext.set(ctx);
    }

    public void unsetCurrentClientContext() {
        perThreadClientContext.remove();
    }

    @Override
    public ClientContext createClientContext(final ResourceResolver resolver) {
        if ( resolver == null ) {
            throw new IllegalArgumentException("Resolver must not be null.");
        }
        final ExecutionContext providerContext = new ExecutionContextImpl(resolver);
        final ClientContext ctx = this.createClientContext(providerContext);
        return ctx;
    }

    @Override
    public ClientContext createClientContext(final SlingHttpServletRequest request) {
        if ( request == null ) {
            throw new IllegalArgumentException("Request must not be null.");
        }
        final ExecutionContext providerContext = new ExecutionContextImpl(request);
        final ClientContext ctx = this.createClientContext(providerContext);
        return ctx;
    }

    private ClientContextImpl createClientContext(final ExecutionContext providerContext) {
        final List<Feature> enabledFeatures = new ArrayList<Feature>();

        for(final Map.Entry<String, FeatureDescription> entry : this.activeFeatures.entrySet()) {
            final Feature f = entry.getValue().feature;

            if ( entry.getValue().feature.isEnabled(providerContext) ) {
                enabledFeatures.add(f);
            }
        }

        final ClientContextImpl ctx = new ClientContextImpl(providerContext, enabledFeatures);
        return ctx;
    }

    @Override
    public Feature[] getAvailableFeatures() {
        final List<Feature> result = new ArrayList<Feature>();
        for(final Map.Entry<String, FeatureDescription> entry : this.activeFeatures.entrySet()) {
            final Feature f = entry.getValue().feature;
            result.add(f);
        }
        return result.toArray(new Feature[result.size()]);
    }

    @Override
    public Feature getFeature(final String name) {
        final FeatureDescription desc = this.activeFeatures.get(name);
        if ( desc != null ) {
            return desc.feature;
        }
        return null;
    }

    @Override
    public String[] getAvailableFeatureNames() {
        return this.activeFeatures.keySet().toArray(new String[this.activeFeatures.size()]);
    }

    @Override
    public boolean isAvailable(final String featureName) {
        return this.activeFeatures.containsKey(featureName);
    }

    /**
     * Internal class caching some feature meta data like service id and ranking.
     */
    private final static class FeatureDescription implements Comparable<FeatureDescription> {

        public final int ranking;
        public final long serviceId;
        public final Feature feature;

        public FeatureDescription(final Feature feature,
                final Map<String, Object> props) {
            this.feature = feature;
            final Object sr = props.get(Constants.SERVICE_RANKING);
            if ( sr == null || !(sr instanceof Integer)) {
                this.ranking = 0;
            } else {
                this.ranking = (Integer)sr;
            }
            this.serviceId = (Long)props.get(Constants.SERVICE_ID);
        }

        @Override
        public int compareTo(final FeatureDescription o) {
            if ( this.ranking < o.ranking ) {
                return 1;
            } else if (this.ranking > o.ranking ) {
                return -1;
            }
            // If ranks are equal, then sort by service id in descending order.
            return (this.serviceId < o.serviceId) ? -1 : 1;
        }

        @Override
        public boolean equals(final Object obj) {
            if ( obj instanceof FeatureDescription ) {
                return ((FeatureDescription)obj).serviceId == this.serviceId;
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
