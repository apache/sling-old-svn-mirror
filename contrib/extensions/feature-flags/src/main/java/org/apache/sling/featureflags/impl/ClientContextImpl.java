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

import org.apache.sling.featureflags.ClientContext;
import org.apache.sling.featureflags.Feature;
import org.apache.sling.featureflags.ProviderContext;
import org.apache.sling.featureflags.ResourceHiding;
import org.apache.sling.featureflags.ResourceTypeMapping;

/**
 * Implementation of the client context
 */
public class ClientContextImpl implements ClientContext {

    private final ProviderContext featureContext;

    private final List<Feature> enabledFeatures;

    private final List<ResourceHiding> hidingFeatures;

    private final Map<String, String> mapperFeatures = new HashMap<String, String>();

    public ClientContextImpl(final ProviderContext featureContext, final List<Feature> features) {
        this.enabledFeatures = Collections.unmodifiableList(features);
        final List<ResourceHiding> hiding = new ArrayList<ResourceHiding>();
        for(final Feature f : this.enabledFeatures) {
            final ResourceHiding rh = f.adaptTo(ResourceHiding.class);
            if ( rh != null ) {
                hiding.add(rh);
            }
            final ResourceTypeMapping rm = f.adaptTo(ResourceTypeMapping.class);
            if ( rm != null ) {
                final Map<String, String> mapping = rm.getResourceTypeMapping();
                mapperFeatures.putAll(mapping);
            }
        }
        this.hidingFeatures = hiding;
        this.featureContext = featureContext;
    }

    public ProviderContext getFeatureContext() {
        return this.featureContext;
    }

    @Override
    public boolean isEnabled(final String featureName) {
        for(final Feature f : this.enabledFeatures) {
            if ( featureName.equals(f.getName()) ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<Feature> getEnabledFeatures() {
        return this.enabledFeatures;
    }

    public Collection<ResourceHiding> getHidingFeatures() {
        return this.hidingFeatures;
    }

    public Map<String, String> getResourceTypeMapping() {
        return this.mapperFeatures;
    }
}
