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

package org.apache.sling.distribution.component.impl;

import org.apache.sling.distribution.component.DistributionComponent;
import org.apache.sling.distribution.component.DistributionComponentProvider;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * The componentManager is used to create components based on all {@link org.apache.sling.distribution.component.DistributionComponentFactory}s
 * registered in the system.
 */
public interface DistributionComponentManager {

    final static String TOPIC_DISTRIBUTION_COMPONENT_REFRESH = "org/apache/sling/distribution/component/REFRESH";

    <ComponentType extends DistributionComponent> ComponentType createComponent(@Nonnull Class<ComponentType> type,
                                                                                       @Nonnull Map<String, Object> properties,
                                                                                       @Nonnull final DistributionComponentProvider componentProvider);

    <ComponentType extends DistributionComponent> ComponentType createComponent(@Nonnull Class<ComponentType> type,
                                                                                       @Nonnull Map<String, Object> properties);
}
