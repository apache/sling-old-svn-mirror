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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.extensions.featureflags.ExecutionContext;
import org.apache.sling.extensions.featureflags.Feature;
import org.apache.sling.extensions.featureflags.FeatureProvider;

/**
 * This service implements the feature handling.
 * It keeps track of all {@link FeatureProvider} services.
 */
@Component
@Service(value=Feature.class)
public class FeatureImpl implements Feature {

    @Reference
    private FeatureManager manager;

    @Override
    public boolean isEnabled(final String featureName, final ExecutionContext context) {
        return this.manager.isEnabled(featureName, context);
    }

    @Override
    public String[] getFeatureNames() {
        return this.manager.getFeatureNames();
    }

    @Override
    public boolean isAvailable(final String featureName) {
        // TODO Auto-generated method stub
        return this.manager.isAvailable(featureName);
    }
}
