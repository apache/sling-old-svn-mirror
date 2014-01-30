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

import aQute.bnd.annotation.ConsumerType;

/**
 * A feature is defined by its name. Features are registered as OSGi services.
 * <p>
 * Feature {@link #getName() names} should be globally unique. If multiple
 * features have the same name, the feature with the highest service ranking is
 * accessible through the {@link Features} service while those with lower
 * service rankings are ignored.
 * <p>
 * This interface is expected to be implemented by feature providers.
 */
@ConsumerType
public interface Feature {

    /**
     * The name of the feature.
     *
     * @return The name of this feature which must not be {@code null} or an
     *         empty string.
     */
    String getName();

    /**
     * The description of the feature.
     *
     * @return The optional description of this feature, which may be
     *         {@code null} or an empty string.
     */
    String getDescription();

    /**
     * Checks whether the feature is enabled for the given execution context.
     * <p>
     * Multiple calls to this method may but are not required to return the same
     * value. For example the return value may depend on the time of day, some
     * random number or some information provided by the given
     * {@link ExecutionContext}.
     * <p>
     * This method is called by the {@link Feature} manager and is not intended
     * to be called by application code directly.
     *
     * @param context The {@link ExecutionContext} providing a context to
     *            evaluate whether the feature is enabled or not.
     *            Implementations must not hold on to this context instance or
     *            the values provided for longer than executing this method.
     * @return {@code true} if this {@code Feature} is enabled in the given
     *         {@link ExecutionContext}.
     */
    boolean isEnabled(ExecutionContext context);
}
