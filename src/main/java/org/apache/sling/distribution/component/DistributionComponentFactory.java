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
import java.util.Map;

import aQute.bnd.annotation.ConsumerType;
import aQute.bnd.annotation.ProviderType;

/**
 * factory for {@link DistributionComponent}s.
 * A client should register a component factory if it has custom implementations of distribution components.
 * As components are hierarchical a factory can delegate the creation of its sub components to another factory.
 */
@ConsumerType
public interface DistributionComponentFactory {


    /**
     * create a {@link DistributionComponent}
     *
     * @param type              the {@link java.lang.Class} of the component to be created
     * @param properties        the properties to be supplied for the initialization of the component
     * @param <ComponentType>   the actual type of the {@link DistributionComponent}
     *                          to be created
     * @param subComponentFactory   the factory to be called for creating sub components
     * @return a {@link DistributionComponent} of the specified type initialized with given properties or <code>null</code>
     * if that could not be created
     */
    @CheckForNull
    <ComponentType extends DistributionComponent> ComponentType createComponent(@Nonnull java.lang.Class<ComponentType> type,
                                                                               @Nonnull Map<String, Object> properties,
                                                                               @Nullable DistributionComponentFactory subComponentFactory);
}
