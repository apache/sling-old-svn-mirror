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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.sling.extensions.featureflags.ClientContext;
import org.apache.sling.extensions.featureflags.ProviderContext;

/**
 * Implementation of the client context
 */
public class ClientContextImpl implements ClientContext {

    private final ProviderContext featureContext;

    private final List<String> enabledFeatures = new ArrayList<String>();

    public ClientContextImpl(final ProviderContext featureContext) {
        this.featureContext = featureContext;
    }

    public void addFeature(final String name) {
        this.enabledFeatures.add(name);
        Collections.sort(this.enabledFeatures);
    }

    public ProviderContext getFeatureContext() {
        return this.featureContext;
    }

    @Override
    public boolean isEnabled(final String featureName) {
        return this.enabledFeatures.contains(featureName);
    }

    @Override
    public Collection<String> getEnabledFeatures() {
        return this.enabledFeatures;
    }
}
