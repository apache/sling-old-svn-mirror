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
package org.apache.sling.caconfig.impl;

import static org.apache.sling.caconfig.impl.ConfigurationNameConstants.CONFIGS_BUCKET_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.caconfig.ConfigurationResolver;
import org.apache.sling.caconfig.impl.metadata.ConfigurationMetadataProviderMultiplexer;
import org.apache.sling.caconfig.impl.override.ConfigurationOverrideManager;
import org.apache.sling.caconfig.management.impl.ConfigurationPersistenceStrategyMultiplexer;
import org.apache.sling.caconfig.resource.impl.ConfigurationResourceResolvingStrategyMultiplexer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service={ ConfigurationResolver.class, ConfigurationResourceResolverConfig.class }, immediate=true)
@Designate(ocd=ConfigurationResolverImpl.Config.class)
public class ConfigurationResolverImpl implements ConfigurationResolver, ConfigurationResourceResolverConfig {

    @Reference
    private ConfigurationResourceResolvingStrategyMultiplexer configurationResourceResolvingStrategy;
    @Reference
    private ConfigurationPersistenceStrategyMultiplexer configurationPersistenceStrategy;
    @Reference
    private ConfigurationInheritanceStrategyMultiplexer configurationInheritanceStrategy;
    @Reference
    private ConfigurationOverrideManager configurationOverrideManager;
    @Reference
    private ConfigurationMetadataProviderMultiplexer configurationMetadataProvider;
    
    @ObjectClassDefinition(name="Apache Sling Context-Aware Configuration Resolver",
            description="Getting context-aware configurations for a given resource context.")
    static @interface Config {
    
        @AttributeDefinition(name = "Config bucket names",
                description = "Additional bucket resource names to '" + CONFIGS_BUCKET_NAME + "' to store configuration resources. "
                + "The names are used in the order defined, always starting with " + CONFIGS_BUCKET_NAME + ". "
                + "Once a bucket resource with a matching name is found, that bucket is used and the following names are skipped. "
                + "For writeback via ConfigurationManager always " + CONFIGS_BUCKET_NAME + " is used.")
        String[] configBucketNames();
    
    }
    
    private Collection<String> configBucketNames;
    
    @Activate
    private void activate(Config config) {
        configBucketNames = new ArrayList<>();
        configBucketNames.add(ConfigurationNameConstants.CONFIGS_BUCKET_NAME);
        if (!ArrayUtils.isEmpty(config.configBucketNames())) {
            configBucketNames.addAll(Arrays.asList(config.configBucketNames()));
        }
    }
    
    @Override
    public ConfigurationBuilder get(Resource resource) {
        return new ConfigurationBuilderImpl(resource, this,
                configurationResourceResolvingStrategy, configurationPersistenceStrategy,
                configurationInheritanceStrategy, configurationOverrideManager, configurationMetadataProvider,
                configBucketNames);
    }

    @Override
    public Collection<String> configBucketNames() {
        return configBucketNames;
    }

}
