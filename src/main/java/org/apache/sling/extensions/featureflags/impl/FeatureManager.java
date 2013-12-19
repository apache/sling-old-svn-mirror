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
package org.apache.sling.extensions.featureflags.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.extensions.featureflags.ExecutionContext;
import org.apache.sling.extensions.featureflags.Feature;
import org.apache.sling.extensions.featureflags.FeatureProvider;
import org.osgi.framework.Constants;

/**
 * This service implements the feature handling.
 * It keeps track of all {@link FeatureProvider} services.
 */
@Component
@Reference(name="featureProvider",
           cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
           policy=ReferencePolicy.DYNAMIC,
           referenceInterface=FeatureProvider.class)
public class FeatureManager implements Feature {

    private final Map<String, List<FeatureProviderDescription>> providers = new HashMap<String, List<FeatureProviderDescription>>();

    private Map<String, FeatureProviderDescription> activeProviders = new HashMap<String, FeatureProviderDescription>();

    /**
     * Bind a new feature provider
     */
    protected void bindFeatureProvider(final FeatureProvider provider, final Map<String, Object> props) {
        final String[] features = provider.getFeatureNames();
        if ( features != null && features.length > 0 ) {
            final FeatureProviderDescription info = new FeatureProviderDescription(provider, props);
            synchronized ( this.providers ) {
                boolean changed = false;
                for(final String n : features) {
                    if ( n != null ) {
                        final String name = n.trim();
                        if ( name.length() > 0 ) {
                            List<FeatureProviderDescription> candidates = this.providers.get(name);
                            if ( candidates == null ) {
                                candidates = new ArrayList<FeatureProviderDescription>();
                                this.providers.put(name, candidates);
                            }
                            candidates.add(info);
                            Collections.sort(candidates);
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

    /**
     * Unbind a feature provider
     */
    protected void unbindFeatureProvider(final FeatureProvider provider, final Map<String, Object> props) {
        final String[] features = provider.getFeatureNames();
        if ( features != null && features.length > 0 ) {
            final FeatureProviderDescription info = new FeatureProviderDescription(provider, props);
            synchronized ( this.providers ) {
                boolean changed = false;
                for(final String n : features) {
                    if ( n != null ) {
                        final String name = n.trim();
                        if ( name.length() > 0 ) {
                            final List<FeatureProviderDescription> candidates = this.providers.get(name);
                            if ( candidates != null ) { // sanity check
                                candidates.remove(info);
                                if ( candidates.size() == 0 ) {
                                    this.providers.remove(name);
                                    changed = true;
                                }
                            }
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
        final Map<String, FeatureProviderDescription> activeMap = new HashMap<String, FeatureManager.FeatureProviderDescription>();
        for(final Map.Entry<String, List<FeatureProviderDescription>> entry : this.providers.entrySet()) {
            activeMap.put(entry.getKey(), entry.getValue().get(0));
        }
        this.activeProviders = activeMap;
    }

    @Override
    public boolean isEnabled(final String featureName, final ExecutionContext context) {
        boolean result = false;
        final FeatureProviderDescription desc = this.activeProviders.get(featureName);
        if ( desc != null ) {
            final FeatureProvider prod = desc.getProvider();
            result = prod.isEnabled(featureName, context);
        }
        return result;
    }

    @Override
    public String[] getFeatureNames() {
        return this.activeProviders.keySet().toArray(new String[this.activeProviders.size()]);
    }

    @Override
    public boolean isAvailable(final String featureName) {
        return this.activeProviders.containsKey(featureName);
    }

    /**
     * Checks whether a resource should be hidden for a feature.
     * This check is only executed if {@link #isEnabled(String, ExecutionContext)}
     * return true for the given feature/context.
     */
    public boolean hideResource(final String featureName, final Resource resource) {
        final FeatureProviderDescription desc = this.activeProviders.get(featureName);
        if ( desc != null ) {
            final FeatureProvider prod = desc.getProvider();
            return prod.hideResource(featureName, resource);
        }
        return false;
    }

    /**
     * Internal class caching some provider infos like service id and ranking.
     */
    private final static class FeatureProviderDescription implements Comparable<FeatureProviderDescription> {

        public FeatureProvider provider;
        public final int ranking;
        public final long serviceId;

        public FeatureProviderDescription(final FeatureProvider provider, final Map<String, Object> props) {
            this.provider = provider;
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

        public FeatureProvider getProvider() {
            return provider;
        }
    }

    public String getResourceType(final String featureName, final String resourceType) {
        final FeatureProviderDescription desc = this.activeProviders.get(featureName);
        if ( desc != null ) {
            final FeatureProvider prod = desc.getProvider();
            final Map<String, String> mapping = prod.getResourceTypeMapping(featureName);
            if ( mapping != null ) {
                return mapping.get(resourceType);
            }
        }
        return null;
    }
}
