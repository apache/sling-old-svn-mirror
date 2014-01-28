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

import java.util.Collection;

import aQute.bnd.annotation.ProviderType;

/**
 * The client context can be used by client code to check whether a specific
 * feature is enable.
 * <p>
 * Prepared {@code ClientContext} instances are available through the
 * {@link Features} service. Consumers of this interface are not expected to
 * implement it.
 */
@ProviderType
public interface ClientContext {

    /**
     * Returns {@code true} if the named feature is enabled.
     *
     * @param featureName The name of the feature.
     * @return {@code true} if the named feature is enabled. {@code false} is
     *         returned if the named feature is not enabled, is not known or the
     *         {@code featureName} parameter is {@code null} or an empty String.
     */
    boolean isEnabled(String featureName);

    /**
     * Returns a possibly empty collection of enabled {@link Feature} instances.
     *
     * @return The collection of enabled {@link Feature} instances. This
     *         collection may be empty and is not modifiable.
     */
    Collection<Feature> getEnabledFeatures();
}
