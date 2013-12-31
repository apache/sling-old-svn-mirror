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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.featureflags.ClientContext;
import org.apache.sling.featureflags.Feature;
import org.apache.sling.featureflags.FeatureProvider;
import org.apache.sling.featureflags.Features;
import org.apache.sling.featureflags.ProviderContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service implements the feature handling.
 * It keeps track of all {@link FeatureProvider} services.
 */
@Component
@Reference(name="featureProvider",
           cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
           policy=ReferencePolicy.DYNAMIC,
           referenceInterface=FeatureProvider.class)
public class FeatureManager implements Features {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, List<FeatureProviderDescription>> providers = new HashMap<String, List<FeatureProviderDescription>>();

    private Map<String, FeatureDescription> activeProviders = new HashMap<String, FeatureDescription>();

    /**
     * Bind a new feature provider
     */
    protected void bindFeatureProvider(final FeatureProvider provider, final Map<String, Object> props) {
        final Feature[] features = provider.getFeatures();
        if ( features != null && features.length > 0 ) {
            synchronized ( this.providers ) {
                boolean changed = false;
                for(final Feature f : features) {
                    final String name = f.getName();
                    final FeatureProviderDescription info = new FeatureProviderDescription(provider, props, f);

                    List<FeatureProviderDescription> candidates = this.providers.get(name);
                    if ( candidates == null ) {
                        candidates = new ArrayList<FeatureProviderDescription>();
                        this.providers.put(name, candidates);
                    }
                    candidates.add(info);
                    Collections.sort(candidates);
                    changed = true;
                }
                if ( changed ) {
                    this.calculateActiveProviders();
                }
            }
        }
    }

    /**
     * Unbind a feature provider
     */
    protected void unbindFeatureProvider(final FeatureProvider provider, final Map<String, Object> props) {
        final Feature[] features = provider.getFeatures();
        if ( features != null && features.length > 0 ) {
            synchronized ( this.providers ) {
                boolean changed = false;
                for(final Feature f : features) {
                    final String name = f.getName();
                    final FeatureProviderDescription info = new FeatureProviderDescription(provider, props, f);

                    final List<FeatureProviderDescription> candidates = this.providers.get(name);
                    if ( candidates != null ) { // sanity check
                        candidates.remove(info);
                        if ( candidates.size() == 0 ) {
                            this.providers.remove(name);
                            changed = true;
                        }
                    }
                }
                if ( changed ) {
                    this.calculateActiveProviders();
                }
            }
        }
    }

    private void calculateActiveProviders() {
        final Map<String, FeatureDescription> activeMap = new HashMap<String, FeatureDescription>();
        for(final Map.Entry<String, List<FeatureProviderDescription>> entry : this.providers.entrySet()) {
            final FeatureProviderDescription desc = entry.getValue().get(0);
            final FeatureDescription info = new FeatureDescription();
            info.feature = desc.feature;
            info.provider = desc.provider;
            activeMap.put(entry.getKey(), info);
            if ( entry.getValue().size() > 1 ) {
                logger.warn("More than one feature provider for feature {}", entry.getKey());
            }
        }
        this.activeProviders = activeMap;
    }

    private final ThreadLocal<ClientContextImpl> perThreadClientContext = new ThreadLocal<ClientContextImpl>();

    @Override
    public ClientContext getCurrentClientContext() {
        return perThreadClientContext.get();
    }

    public void setCurrentClientContext(final SlingHttpServletRequest request) {
        final ProviderContext providerContext = new ProviderContextImpl(request);
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
        final ProviderContext providerContext = new ProviderContextImpl(resolver);
        final ClientContext ctx = this.createClientContext(providerContext);
        return ctx;
    }

    @Override
    public ClientContext createClientContext(final SlingHttpServletRequest request) {
        if ( request == null ) {
            throw new IllegalArgumentException("Request must not be null.");
        }
        final ProviderContext providerContext = new ProviderContextImpl(request);
        final ClientContext ctx = this.createClientContext(providerContext);
        return ctx;
    }

    private ClientContextImpl createClientContext(final ProviderContext providerContext) {
        final List<Feature> enabledFeatures = new ArrayList<Feature>();

        for(final Map.Entry<String, FeatureDescription> entry : this.activeProviders.entrySet()) {
            final Feature f = entry.getValue().feature;

            if ( entry.getValue().provider.isEnabled(f, providerContext) ) {
                enabledFeatures.add(f);
            }
        }

        final ClientContextImpl ctx = new ClientContextImpl(providerContext, enabledFeatures);
        return ctx;
    }

    @Override
    public Feature[] getAvailableFeatures() {
        final List<Feature> result = new ArrayList<Feature>();
        for(final Map.Entry<String, FeatureDescription> entry : this.activeProviders.entrySet()) {
            final Feature f = entry.getValue().feature;
            result.add(f);
        }
        return result.toArray(new Feature[result.size()]);
    }

    @Override
    public Feature getFeature(final String name) {
        final FeatureDescription desc = this.activeProviders.get(name);
        if ( desc != null ) {
            return desc.feature;
        }
        return null;
    }

    @Override
    public String[] getAvailableFeatureNames() {
        return this.activeProviders.keySet().toArray(new String[this.activeProviders.size()]);
    }

    @Override
    public boolean isAvailable(final String featureName) {
        return this.activeProviders.containsKey(featureName);
    }

    /**
     * Internal class caching some provider infos like service id and ranking.
     */
    private final static class FeatureProviderDescription implements Comparable<FeatureProviderDescription> {

        public final FeatureProvider provider;
        public final int ranking;
        public final long serviceId;
        public final Feature feature;

        public FeatureProviderDescription(final FeatureProvider provider,
                final Map<String, Object> props,
                final Feature feature) {
            this.provider = provider;
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
        public int compareTo(final FeatureProviderDescription o) {
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
            if ( obj instanceof FeatureProviderDescription ) {
                return ((FeatureProviderDescription)obj).serviceId == this.serviceId;
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

    private final static class FeatureDescription {
        public Feature feature;
        public FeatureProvider provider;

    }
}
