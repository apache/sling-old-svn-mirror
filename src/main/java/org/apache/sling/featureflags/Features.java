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

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.ResourceResolver;

import aQute.bnd.annotation.ProviderType;

/**
 * The features service is the central gateway for feature handling. It can be
 * used to query the available features and to create client contexts to be used
 * for enabled feature checking.
 */
@ProviderType
public interface Features {

    /**
     * Get the list of all available (known) feature names.
     * <p>
     * Features are known if they are registered as {@link Feature} services or
     * are configured with OSGi configuration whose factory PID is
     * {@code org.apache.sling.featureflags.Feature}.
     *
     * @return The names of the known features
     */
    String[] getAvailableFeatureNames();

    /**
     * Get the list of all available (known) features.
     * <p>
     * Features are known if they are registered as {@link Feature} services or
     * are configured with OSGi configuration whose factory PID is
     * {@code org.apache.sling.featureflags.Feature}.
     *
     * @return The known features
     */
    Feature[] getAvailableFeatures();

    /**
     * Returns the feature with the given name.
     *
     * @param name The name of the feature.
     * @return The feature or <code>null</code> if not known or the name is an
     *         empty string or {@code null}.
     */
    Feature getFeature(String name);

    /**
     * Checks whether a feature with the given name is available (known).
     * <p>
     * Features are known if they are registered as {@link Feature} services or
     * are configured with OSGi configuration whose factory PID is
     * {@code org.apache.sling.featureflags.Feature}.
     *
     * @param featureName The name of the feature to check for availability.
     * @return {@code true} if the named feature is available.
     */
    boolean isAvailable(String featureName);

    /**
     * Returns the current client context. This method always returns a client
     * context object.
     *
     * @return A client context.
     */
    ClientContext getCurrentClientContext();

    /**
     * Create a client context for the resource resolver.
     * <p>
     * The {@link ClientContext} is a snapshot of the enablement state of the
     * features at the time of creation. A change in the feature enablement
     * state is not reflected in {@link ClientContext} objects created prior to
     * changing the state.
     * <p>
     * The {@link ClientContext} returned is not available through the
     * {@link #getCurrentClientContext()} method.
     *
     * @param resolver The {@code ResourceResolver} to base the
     *            {@link ClientContext} on.
     * @return A newly created client context based on the given
     *         {@code ResourceResolver}.
     * @throws IllegalArgumentException If {@code resolver} is {@code null}
     */
    ClientContext createClientContext(ResourceResolver resolver);

    /**
     * Create a client context for the request.
     * <p>
     * The {@link ClientContext} is a snapshot of the enablement state of the
     * features at the time of creation. A change in the feature enablement
     * state is not reflected in {@link ClientContext} objects created prior to
     * changing the state.
     * <p>
     * The {@link ClientContext} returned is not available through the
     * {@link #getCurrentClientContext()} method.
     *
     * @param request The {@code HttpServletRequest} to base the
     *            {@link ClientContext} on. If this is a
     *            {@code SlingHttpServletContext} the {@link ClientContext}'s
     *            resource resolver is set to the request's resource resolver.
     * @return A newly created client context based on the given
     *         {@code HttpServletRequest}.
     * @throws IllegalArgumentException If {@code request} is {@code null}
     */
    ClientContext createClientContext(HttpServletRequest request);
}
