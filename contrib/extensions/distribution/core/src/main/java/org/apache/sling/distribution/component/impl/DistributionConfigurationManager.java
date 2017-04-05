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

import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

/**
 * A configuration manager allows basic access/editing operations on distribution configurations.
 */
public interface DistributionConfigurationManager {

    List<DistributionConfiguration> getConfigs(ResourceResolver resolver, DistributionComponentKind kind);

    DistributionConfiguration getConfig(ResourceResolver resolver, DistributionComponentKind kind, String name);

    void saveConfig(ResourceResolver resolver, DistributionConfiguration config);

    void deleteConfig(ResourceResolver resolver, DistributionComponentKind kind, String name);
}
