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
import java.util.Comparator;
import java.util.List;

import org.apache.sling.featureflags.ClientContext;
import org.apache.sling.featureflags.Feature;
import org.apache.sling.featureflags.ProviderContext;
import org.apache.sling.featureflags.ResourceHiding;
import org.apache.sling.featureflags.ResourceTypeMapper;

/**
 * Implementation of the client context
 */
public class ClientContextImpl implements ClientContext {

    private final ProviderContext featureContext;

    private final List<Feature> enabledFeatures;

    private final List<ResourceHiding> hidingFeatures;

    private final List<ResourceTypeMapper> mapperFeatures;

    public ClientContextImpl(final ProviderContext featureContext, final List<Feature> features) {
        Collections.sort(features, new Comparator<Feature>() {

            @Override
            public int compare(final Feature arg0, final Feature arg1) {
                return arg0.getName().compareTo(arg1.getName());
            }

        });
        this.enabledFeatures = Collections.unmodifiableList(features);
        final List<ResourceHiding> hiding = new ArrayList<ResourceHiding>();
        final List<ResourceTypeMapper> mapping = new ArrayList<ResourceTypeMapper>();
        for(final Feature f : this.enabledFeatures) {
            final ResourceHiding rh = f.adaptTo(ResourceHiding.class);
            if ( rh != null ) {
                hiding.add(rh);
            }
            final ResourceTypeMapper rm = f.adaptTo(ResourceTypeMapper.class);
            if ( rm != null ) {
                mapping.add(rm);
            }
        }
        this.hidingFeatures = hiding;
        this.mapperFeatures = mapping;
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

    public Collection<ResourceTypeMapper> getMappingFeatures() {
        return this.mapperFeatures;
    }
}
