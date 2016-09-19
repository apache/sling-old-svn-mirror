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

import org.apache.sling.contextaware.config.ConfigurationResolver;
import org.apache.sling.contextaware.config.impl.def.DefaultConfigurationPersistenceStrategy;
import org.apache.sling.contextaware.config.management.impl.ConfigurationPersistenceStrategyMultiplexer;
import org.apache.sling.contextaware.config.resource.impl.ConfigurationResourceTestUtils;
import org.apache.sling.testing.mock.sling.junit.SlingContext;

public final class ConfigurationTestUtils {
    
    private ConfigurationTestUtils() {
        // static methods only
    }

    /**
     * Register all services for {@link ConfigurationResolver}.
     * @param context Sling context
     */
    public static ConfigurationResolver registerConfigurationResolver(SlingContext context) {
        ConfigurationResourceTestUtils.registerConfigurationResourceResolver(context);
        context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());
        context.registerInjectActivateService(new ConfigurationPersistenceStrategyMultiplexer());
        return context.registerInjectActivateService(new ConfigurationResolverImpl());
    }
    
}
