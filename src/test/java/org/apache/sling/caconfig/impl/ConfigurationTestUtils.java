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

import org.apache.sling.caconfig.ConfigurationResolver;
import org.apache.sling.caconfig.impl.def.DefaultConfigurationInheritanceStrategy;
import org.apache.sling.caconfig.impl.def.DefaultConfigurationPersistenceStrategy;
import org.apache.sling.caconfig.impl.metadata.ConfigurationMetadataProviderMultiplexerImpl;
import org.apache.sling.caconfig.impl.override.ConfigurationOverrideMultiplexerImpl;
import org.apache.sling.caconfig.management.impl.ConfigurationManagementSettingsImpl;
import org.apache.sling.caconfig.management.impl.ConfigurationPersistenceStrategyMultiplexerImpl;
import org.apache.sling.caconfig.resource.impl.ConfigurationResourceTestUtils;
import org.apache.sling.testing.mock.sling.junit.SlingContext;

public final class ConfigurationTestUtils {
    
    private ConfigurationTestUtils() {
        // static methods only
    }

    /**
     * Register all services for {@link ConfigurationResolver}.
     * @param context Sling context
     */
    public static ConfigurationResolver registerConfigurationResolver(SlingContext context, Object... properties) {
        ConfigurationResourceTestUtils.registerConfigurationResourceResolver(context);
        context.registerInjectActivateService(new ConfigurationManagementSettingsImpl());
        context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());
        context.registerInjectActivateService(new ConfigurationPersistenceStrategyMultiplexerImpl());
        context.registerInjectActivateService(new DefaultConfigurationInheritanceStrategy());
        context.registerInjectActivateService(new ConfigurationInheritanceStrategyMultiplexerImpl());
        context.registerInjectActivateService(new ConfigurationOverrideMultiplexerImpl());
        context.registerInjectActivateService(new ConfigurationMetadataProviderMultiplexerImpl());
        return context.registerInjectActivateService(new ConfigurationResolverImpl(), properties);
    }
    
    /**
     * Register all services for {@link ConfigurationResolver}
     * without the default implementations of the multiplexed services.
     * @param context Sling context
     */
    public static ConfigurationResolver registerConfigurationResolverWithoutDefaultImpl(SlingContext context, Object... properties) {
        ConfigurationResourceTestUtils.registerConfigurationResourceResolverWithoutDefaultImpl(context);
        context.registerInjectActivateService(new ConfigurationManagementSettingsImpl());
        context.registerInjectActivateService(new ConfigurationPersistenceStrategyMultiplexerImpl());
        context.registerInjectActivateService(new ConfigurationInheritanceStrategyMultiplexerImpl());
        context.registerInjectActivateService(new ConfigurationOverrideMultiplexerImpl());
        context.registerInjectActivateService(new ConfigurationMetadataProviderMultiplexerImpl());
        return context.registerInjectActivateService(new ConfigurationResolverImpl(), properties);
    }
    
}
