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

import aQute.bnd.annotation.ProviderType;

/**
 * The {@code Features} service is the applications access point to the Feature
 * Flag functionality. It can be used to query the available features and to
 * create client contexts to be used for enabled feature checking.
 */
@ProviderType
public interface Features {

    /**
     * Get the list of all (known) features.
     * <p>
     * Features are known if they are registered as {@link Feature} services or
     * are configured with OSGi configuration whose factory PID is
     * {@code org.apache.sling.featureflags.Feature}.
     *
     * @return The known features
     */
    Feature[] getFeatures();

    /**
     * Returns the feature with the given name.
     * <p>
     * Features are known if they are registered as {@link Feature} services or
     * are configured with OSGi configuration whose factory PID is
     * {@code org.apache.sling.featureflags.Feature}.
     *
     * @param name The name of the feature.
     * @return The feature or <code>null</code> if not known or the name is an
     *         empty string or {@code null}.
     */
    Feature getFeature(String name);

    /**
     * Returns {@code true} if a feature with the given name is known and
     * enabled under the current {@link ExecutionContext}.
     * <p>
     * Features are known if they are registered as {@link Feature} services or
     * are configured with OSGi configuration whose factory PID is
     * {@code org.apache.sling.featureflags.Feature}.
     *
     * @param name The name of the feature to check for enablement.
     * @return {@code true} if the named feature is known and enabled.
     *         Specifically {@code false} is also returned if the named feature
     *         is not known.
     */
    boolean isEnabled(String name);
}
