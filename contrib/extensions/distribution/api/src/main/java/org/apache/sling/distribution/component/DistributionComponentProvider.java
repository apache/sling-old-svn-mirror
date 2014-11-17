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
package org.apache.sling.distribution.component;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import aQute.bnd.annotation.ProviderType;

/**
 * provider for already existing {@link DistributionComponent}s
 */
@ProviderType
public interface DistributionComponentProvider {

    /**
     * Retrieves an already existing component by name.
     * If <code>null</code> is passed as componentName then a default component is returned.
     *
     * @param type            the {@link java.lang.Class} of the component to be retrieved
     * @param componentName   the component name
     * @param <ComponentType> the actual type of the {@link DistributionComponent}
     *                        to be retrieved
     * @return the {@link DistributionComponent} of the specified type,
     * with the specified name, or <code>null</code> if such a {@link DistributionComponent}
     * doesn't exist
     */
    @CheckForNull
    <ComponentType extends DistributionComponent> ComponentType getComponent(@Nonnull java.lang.Class<ComponentType> type,
                                                                            @Nullable String componentName);
}
