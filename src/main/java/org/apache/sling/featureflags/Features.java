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
package org.apache.sling.featureflags;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

/**
 * The features service is the central gateway for feature handling.
 * It can be used to query the available features and to create
 * client contexts to be used for enabled feature checking.
 */
@ProviderType
public interface Features {

    /**
     * Get the list of all available feature names. A feature is available
     * if there is a registered {@link Feature} service.
     */
    String[] getAvailableFeatureNames();

    /**
     * Get the list of all available features. A feature is available
     * if there is a registered {@link Feature} service.
     */
    Feature[] getAvailableFeatures();

    /**
     * Returns the feature with the given name.
     * @return The feature or <code>null</code>
     */
    Feature getFeature(String name);

    /**
     * Checks whether a feature with the given name is available.
     * A feature is available if there is a registered {@link Feature} service.
     */
    boolean isAvailable(String featureName);

    /**
     * Returns the current client context.
     * This method always returns a client context object
     * @return A client context.
     */
    ClientContext getCurrentClientContext();

    /**
     * Create a client context for the resource resolver.
     * @throws IllegalArgumentException If resolver is null
     */
    ClientContext createClientContext(ResourceResolver resolver);

    /**
     * Create a client context for the request.
     * @throws IllegalArgumentException If request is null
     */
    ClientContext createClientContext(SlingHttpServletRequest request);
}
