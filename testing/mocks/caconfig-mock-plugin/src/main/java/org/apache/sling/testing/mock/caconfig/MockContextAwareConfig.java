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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.management.ConfigurationManager;
import org.apache.sling.caconfig.spi.ConfigurationCollectionPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistData;
import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Helps setting up a mock environment for Context-Aware Configuration.
 */
@ProviderType
public final class MockContextAwareConfig {
    
    private MockContextAwareConfig() {
        // static methods only
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Configuration annotation.
     * @param context Sling context
     * @param classNames Java class names
     */
    public static void registerAnnotationClasses(SlingContextImpl context, String... classNames) {
        ConfigurationMetadataUtil.registerAnnotationClasses(context.bundleContext(), classNames);
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Configuration annotation.
     * @param context Sling context
     * @param classes Java classes
     */
    public static void registerAnnotationClasses(SlingContextImpl context, Class... classes) {
        ConfigurationMetadataUtil.registerAnnotationClasses(context.bundleContext(), classes);
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Configuration annotation.
     * @param context Sling context
     * @param packageNames Java package names
     */
    public static void registerAnnotationPackages(SlingContextImpl context, String... packageNames) {
        Collection<Class> classes = ConfigurationMetadataUtil.getConfigurationClassesForPackages(StringUtils.join(packageNames, ","));
        registerAnnotationClasses(context, classes.toArray(new Class[classes.size()]));
    }

    /**
     * Writes configuration parameters using the primary configured persistence
     * provider.
     * @param context Sling context
     * @param contextPath Configuration id
     * @param configClass Configuration class
     * @param values Configuration values
     */
    public static void writeConfiguration(SlingContextImpl context, String contextPath, Class<?> configClass,
            Map<String, Object> values) {
        writeConfiguration(context, contextPath, getConfigurationName(configClass), values);
    }
    
    /**
     * Writes configuration parameters using the primary configured persistence
     * provider.
     * @param context Sling context
     * @param contextPath Configuration id
     * @param configName Config name
     * @param values Configuration values
     */
    public static void writeConfiguration(SlingContextImpl context, String contextPath, String configName,
            Map<String, Object> values) {
        ConfigurationManager configManager = context.getService(ConfigurationManager.class);
        Resource contextResource = context.resourceResolver().getResource(contextPath);
        configManager.persistConfiguration(contextResource, configName, new ConfigurationPersistData(values));
    }

    /**
     * Writes configuration parameters using the primary configured persistence
     * provider.
     * @param context Sling context
     * @param contextPath Configuration id
     * @param configClass Configuration class
     * @param values Configuration values
     */
    public static void writeConfiguration(SlingContextImpl context, String contextPath, Class<?> configClass, Object... values) {
        writeConfiguration(context, contextPath, getConfigurationName(configClass), values);
    }

    /**
     * Writes configuration parameters using the primary configured persistence
     * provider.
     * @param context Sling context
     * @param contextPath Configuration id
     * @param configName Config name
     * @param values Configuration values
     */
    public static void writeConfiguration(SlingContextImpl context, String contextPath, String configName, Object... values) {
        writeConfiguration(context, contextPath, configName, MapUtil.toMap(values));
    }

    /**
     * Writes a collection of configuration parameters using the primary
     * configured persistence provider.
     * @param context Sling context
     * @param contextPath Configuration id
     * @param configClass Configuration class
     * @param values Configuration values
     */
    public static void writeConfigurationCollection(SlingContextImpl context, String contextPath,  Class<?> configClass,
            Collection<Map<String, Object>> values) {
        writeConfigurationCollection(context, contextPath, getConfigurationName(configClass), values);
    }
    
    /**
     * Writes a collection of configuration parameters using the primary
     * configured persistence provider.
     * @param context Sling context
     * @param contextPath Configuration id
     * @param configName Config name
     * @param values Configuration values
     */
    public static void writeConfigurationCollection(SlingContextImpl context, String contextPath, String configName,
            Collection<Map<String, Object>> values) {
        ConfigurationManager configManager = context.getService(ConfigurationManager.class);
        Resource contextResource = context.resourceResolver().getResource(contextPath);
        List<ConfigurationPersistData> items = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> map : values) {
            items.add(new ConfigurationPersistData(map).collectionItemName("item" + (index++)));
        }
        configManager.persistConfigurationCollection(contextResource, configName,
                new ConfigurationCollectionPersistData(items));
    }
    
    private static String getConfigurationName(Class<?> configClass) {
        Configuration annotation = configClass.getAnnotation(Configuration.class);
        if (annotation != null && StringUtils.isNotBlank(annotation.name())) {
            return annotation.name();
        }
        return configClass.getName();
    }

}
