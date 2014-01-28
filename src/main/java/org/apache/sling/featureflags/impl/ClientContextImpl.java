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
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.featureflags.ClientContext;
import org.apache.sling.featureflags.Feature;
import org.apache.sling.featureflags.ExecutionContext;

/**
 * Implementation of the client context
 */
public class ClientContextImpl implements ClientContext {

    private final ExecutionContext featureContext;

    private final Map<String, Feature> enabledFeatures;

    private final List<ResourceDecorator> resourceDecorators;

    public ClientContextImpl(final ExecutionContext featureContext, final Map<String, Feature> features) {
        ArrayList<ResourceDecorator> resourceDecorators = new ArrayList<ResourceDecorator>(features.size());
        for (final Feature f : features.values()) {
            if (f instanceof ResourceDecorator) {
                resourceDecorators.add((ResourceDecorator) f);
            }
        }
        resourceDecorators.trimToSize();

        this.featureContext = featureContext;
        this.enabledFeatures = Collections.unmodifiableMap(features);
        this.resourceDecorators = Collections.unmodifiableList(resourceDecorators);
    }

    public ExecutionContext getFeatureContext() {
        return this.featureContext;
    }

    @Override
    public boolean isEnabled(final String featureName) {
        return this.enabledFeatures.get(featureName) != null;
    }

    @Override
    public Collection<Feature> getEnabledFeatures() {
        return this.enabledFeatures.values();
    }

    public List<ResourceDecorator> getResourceDecorators() {
        return this.resourceDecorators;
    }
}
