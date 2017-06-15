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

import org.apache.sling.caconfig.impl.ConfigurationBuilderAdapterFactory;
import org.apache.sling.caconfig.impl.ConfigurationResolverImpl;
import org.apache.sling.caconfig.impl.def.DefaultConfigurationPersistenceStrategy;
import org.apache.sling.caconfig.impl.metadata.AnnotationClassConfigurationMetadataProvider;
import org.apache.sling.caconfig.management.impl.ConfigurationManagerImpl;
import org.apache.sling.caconfig.management.impl.ContextPathStrategyMultiplexerImpl;
import org.apache.sling.caconfig.resource.impl.ConfigurationResourceResolverImpl;
import org.apache.sling.caconfig.resource.impl.def.DefaultConfigurationResourceResolvingStrategy;
import org.apache.sling.caconfig.resource.impl.def.DefaultContextPathStrategy;
import org.apache.sling.testing.mock.osgi.context.AbstractContextPlugin;
import org.apache.sling.testing.mock.osgi.context.ContextPlugin;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Mock context plugins.
 * The plugin supports all versions from 1.2 to the most recent versions of the Impl/SPI.
 */
@ProviderType
public final class ContextPlugins {

    private ContextPlugins() {
        // constants only
    }

    /**
     * Context plugin for Sling Context-Aware Configuration.
     */
    public static final ContextPlugin<? extends SlingContextImpl> CACONFIG = new AbstractContextPlugin<SlingContextImpl>() {
        @Override
        public void afterSetUp(SlingContextImpl context) throws Exception {
            registerConfigurationResourceResolver(context);
            registerConfigurationResolver(context);
            registerConfigurationManagement(context);
            registerConfigurationResourceResolverDefaultImpl(context);
            registerConfigurationResolverDefaultImpl(context);

            // Scan MANIFEST.MF in the classpath and automatically register all Configuration annotation classes found.
            ConfigurationMetadataUtil.addAnnotationClassesForManifestEntries(context.bundleContext());
        }
    };

    /**
     * Context plugin for Sling Context-Aware Configuration (without the default implementations).
     */
    public static final ContextPlugin<? extends SlingContextImpl> CACONFIG_NODEF = new AbstractContextPlugin<SlingContextImpl>() {
        @Override
        public void afterSetUp(SlingContextImpl context) throws Exception {
            registerConfigurationResourceResolver(context);
            registerConfigurationResolver(context);
            registerConfigurationManagement(context);
        }
    };

    /**
     * Register all services for ConfigurationResourceResolver (without the default implementations).
     * @param context Sling context
     */
    private static void registerConfigurationResourceResolver(SlingContextImpl context) {
        
        context.registerInjectActivateService(new ContextPathStrategyMultiplexerImpl());
        
        if (!registerByClassName(context, "org.apache.sling.caconfig.resource.impl.ConfigurationResourceResolvingStrategyMultiplexerImpl")) {
            // fallback to impl 1.2
            registerByClassName(context, "org.apache.sling.caconfig.resource.impl.ConfigurationResourceResolvingStrategyMultiplexer");
        }
        
        context.registerInjectActivateService(new ConfigurationResourceResolverImpl());
    }

    /**
     * Register default implementations for for ConfigurationResourceResolver.
     * @param context Sling context
     */
    private static void registerConfigurationResourceResolverDefaultImpl(SlingContextImpl context) {
        context.registerInjectActivateService(new DefaultContextPathStrategy());
        context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());
    }

    /**
     * Register all services for ConfigurationResolver (without the default implementations).
     * @param context Sling context
     */
    private static void registerConfigurationResolver(SlingContextImpl context) {
        registerByClassName(context, "org.apache.sling.caconfig.management.impl.ConfigurationManagementSettingsImpl");
        
        if (!registerByClassName(context, "org.apache.sling.caconfig.management.impl.ConfigurationPersistenceStrategyMultiplexerImpl")) {
            // fallback to impl 1.2
            registerByClassName(context, "org.apache.sling.caconfig.management.impl.ConfigurationPersistenceStrategyMultiplexer");
        }
        
        // only required for impl 1.3+
        registerByClassName(context, "org.apache.sling.caconfig.impl.ConfigurationPersistenceStrategyBridge");

        if (!registerByClassName(context, "org.apache.sling.caconfig.impl.metadata.ConfigurationMetadataProviderMultiplexerImpl")) {
            // fallback to impl 1.2
            registerByClassName(context, "org.apache.sling.caconfig.impl.metadata.ConfigurationMetadataProviderMultiplexer");
        }
        
        if (!registerByClassName(context, "org.apache.sling.caconfig.impl.ConfigurationInheritanceStrategyMultiplexerImpl")) {
            // fallback to impl 1.2 (not existing in 1.1 or below)
            registerByClassName(context, "org.apache.sling.caconfig.impl.ConfigurationInheritanceStrategyMultiplexer");
        }
        
        if (!registerByClassName(context, "org.apache.sling.caconfig.impl.override.ConfigurationOverrideMultiplexerImpl")) {
            // fallback to impl 1.2 (not existing in 1.1 or below)
            registerByClassName(context, "org.apache.sling.caconfig.impl.override.ConfigurationOverrideManager");
        }

        context.registerInjectActivateService(new ConfigurationResolverImpl());
        context.registerInjectActivateService(new ConfigurationBuilderAdapterFactory());
    }

    /**
     * Register default implementations for for ConfigurationResolver.
     * @param context Sling context
     */
    private static void registerConfigurationResolverDefaultImpl(SlingContextImpl context) {
        context.registerInjectActivateService(new DefaultConfigurationPersistenceStrategy());

        // only required for impl 1.2+
        registerByClassName(context,"org.apache.sling.caconfig.impl.def.DefaultConfigurationInheritanceStrategy");
    }

    private static void registerConfigurationManagement(SlingContextImpl context) {
        context.registerInjectActivateService(new ConfigurationManagerImpl());
        context.registerInjectActivateService(new AnnotationClassConfigurationMetadataProvider());
    }

    private static boolean registerByClassName(SlingContextImpl context, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            context.registerInjectActivateService(clazz.newInstance());
            return true;
        }
        catch (ClassNotFoundException ex) {
            return false;
        }
        catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        }
        catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }

    }

}
