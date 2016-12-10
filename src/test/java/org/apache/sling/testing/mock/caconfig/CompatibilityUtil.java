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
package org.apache.sling.testing.mock.caconfig;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.testing.mock.sling.junit.SlingContext;

/**
 * Helper methods interacting with Context-Aware context supporting both version 1.1 and 2.0 of
 * the Management API via reflection.
 */
public class CompatibilityUtil {

    /**
     * Write configuration for impl 1.2
     */
    public static void writeConfig(SlingContext context, Resource contextResource, String configName, Map<String,Object> props) {
        try {
            Class<?> configurationPersistDataClass;
            try {
                configurationPersistDataClass = Class.forName("org.apache.sling.caconfig.spi.ConfigurationPersistData");
            }
            catch (ClassNotFoundException e) {
                // fallback to caconfig impl 1.1
                writeConfigImpl11(context, contextResource, configName, props);
                return;
            }

            Object persistData = configurationPersistDataClass.getConstructor(Map.class).newInstance(props);
            ConfigurationManager configManager = context.getService(ConfigurationManager.class);
            Method persistMethod = ConfigurationManager.class.getMethod("persistConfiguration", Resource.class, String.class, configurationPersistDataClass);
            persistMethod.invoke(configManager, contextResource, configName, persistData);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Fallback: Write configuration for impl 1.1
     */
    public static void writeConfigImpl11(SlingContext context, Resource contextResource, String configName, Map<String,Object> props) throws Exception {
        ConfigurationManager configManager = context.getService(ConfigurationManager.class);
        Method persistMethod = ConfigurationManager.class.getMethod("persist", Resource.class, String.class, Map.class);
        persistMethod.invoke(configManager, contextResource, configName, props);
    }

}
