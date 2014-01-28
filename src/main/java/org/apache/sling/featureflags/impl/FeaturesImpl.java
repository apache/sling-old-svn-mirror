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

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.featureflags.ClientContext;
import org.apache.sling.featureflags.Feature;
import org.apache.sling.featureflags.Features;

/**
 * This is a wrapper around the internal feature manager.
 */
public class FeaturesImpl implements Features {

    private final FeatureManager manager;

    FeaturesImpl(final FeatureManager manager) {
        this.manager = manager;
    }

    @Override
    public String[] getAvailableFeatureNames() {
        return this.manager.getAvailableFeatureNames();
    }

    @Override
    public boolean isAvailable(final String featureName) {
        return this.manager.isAvailable(featureName);
    }

    @Override
    public Feature[] getAvailableFeatures() {
        return this.manager.getAvailableFeatures();
    }

    @Override
    public Feature getFeature(final String name) {
        return this.manager.getFeature(name);
    }

    @Override
    public ClientContext getCurrentClientContext() {
        return this.manager.getCurrentClientContext();
    }

    @Override
    public ClientContext createClientContext(final ResourceResolver resolver) {
        return this.manager.createClientContext(resolver);
    }

    @Override
    public ClientContext createClientContext(final HttpServletRequest request) {
        return this.manager.createClientContext(request);
    }
}
