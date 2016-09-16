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
package org.apache.sling.contextaware.config.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.ConfigurationBuilder;
import org.apache.sling.contextaware.config.ConfigurationResolver;
import org.apache.sling.contextaware.config.management.impl.ConfigurationPersistenceStrategyMultiplexer;
import org.apache.sling.contextaware.config.resource.ConfigurationResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service=ConfigurationResolver.class, immediate=true)
public class ConfigurationResolverImpl implements ConfigurationResolver {

    @Reference
    private ConfigurationResourceResolver configurationResourceResolver;

    @Reference
    private ConfigurationPersistenceStrategyMultiplexer configurationResourcePersistenceStrategy;
    
    @Override
    public ConfigurationBuilder get(Resource resource) {
        return new ConfigurationBuilderImpl(resource, this,
                configurationResourceResolver, configurationResourcePersistenceStrategy);
    }

}
