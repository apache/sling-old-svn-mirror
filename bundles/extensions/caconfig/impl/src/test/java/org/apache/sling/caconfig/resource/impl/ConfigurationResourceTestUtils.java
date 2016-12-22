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
package org.apache.sling.caconfig.resource.impl;

import org.apache.sling.caconfig.management.impl.ContextPathStrategyMultiplexerImpl;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.caconfig.resource.impl.def.DefaultConfigurationResourceResolvingStrategy;
import org.apache.sling.caconfig.resource.impl.def.DefaultContextPathStrategy;
import org.apache.sling.testing.mock.sling.junit.SlingContext;

public final class ConfigurationResourceTestUtils {
    
    private ConfigurationResourceTestUtils() {
        // static methods only
    }

    /**
     * Register all services for {@link ConfigurationResourceResolver}.
     * @param context Sling context
     */
    public static ConfigurationResourceResolver registerConfigurationResourceResolver(SlingContext context) {
        context.registerInjectActivateService(new DefaultContextPathStrategy());
        context.registerInjectActivateService(new ContextPathStrategyMultiplexerImpl());
        context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());
        context.registerInjectActivateService(new ConfigurationResourceResolvingStrategyMultiplexer());
        return context.registerInjectActivateService(new ConfigurationResourceResolverImpl());
    }
    
    /**
     * Register all services for {@link ConfigurationResourceResolver}
     * without the default implementations of the multiplexed services.
     * @param context Sling context
     */
    public static ConfigurationResourceResolver registerConfigurationResourceResolverWithoutDefaultImpl(SlingContext context) {
        context.registerInjectActivateService(new ContextPathStrategyMultiplexerImpl());
        context.registerInjectActivateService(new ConfigurationResourceResolvingStrategyMultiplexer());
        return context.registerInjectActivateService(new ConfigurationResourceResolverImpl());
    }
    
}
