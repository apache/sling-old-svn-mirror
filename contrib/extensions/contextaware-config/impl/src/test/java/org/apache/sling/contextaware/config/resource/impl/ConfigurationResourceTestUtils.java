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
package org.apache.sling.contextaware.config.resource.impl;

import static org.junit.Assert.assertArrayEquals;

import java.util.Collection;
import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.resource.ConfigurationResourceResolver;
import org.apache.sling.contextaware.config.resource.impl.def.DefaultConfigurationResourcePersistenceStrategy;
import org.apache.sling.contextaware.config.resource.impl.def.DefaultConfigurationResourceResolvingStrategy;
import org.apache.sling.contextaware.config.resource.impl.def.DefaultContextPathStrategy;
import org.apache.sling.testing.mock.sling.junit.SlingContext;

public final class ConfigurationResourceTestUtils {
    
    private ConfigurationResourceTestUtils() {
        // static methods only
    }

    /**
     * Assert that resources with the given path exist in the given order in the given collection.
     * @param expectedPaths Expected path
     * @param actualResources Actual resources
     */
    public static void assetResourcePaths(String[] expectedPaths, Collection<Resource> actualResources) {
        String[] actualPaths = new String[actualResources.size()];
        int i = 0;
        for (Iterator<Resource> it=actualResources.iterator(); it.hasNext(); i++) {
            actualPaths[i] = it.next().getPath();
        }
        assertArrayEquals(expectedPaths, actualPaths);
    }
    
    /**
     * Register all services for {@link org.apache.sling.contextaware.config.resource.ConfigurationResourceResolver}.
     * @param context Sling context
     */
    public static ConfigurationResourceResolver registerConfigurationResourceResolver(SlingContext context) {
        context.registerInjectActivateService(new DefaultContextPathStrategy());
        context.registerInjectActivateService(new ContextPathStrategyMultiplexer());
        context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());
        context.registerInjectActivateService(new ConfigurationResourceResolvingStrategyMultiplexer());
        context.registerInjectActivateService(new DefaultConfigurationResourcePersistenceStrategy());
        context.registerInjectActivateService(new ConfigurationResourcePersistenceStrategyMultiplexer());
        return context.registerInjectActivateService(new ConfigurationResourceResolverImpl());
    }
    
}
