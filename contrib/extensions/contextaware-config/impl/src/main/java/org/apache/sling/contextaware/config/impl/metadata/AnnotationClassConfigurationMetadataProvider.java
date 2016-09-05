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
package org.apache.sling.contextaware.config.impl.metadata;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.contextaware.config.spi.ConfigurationMetadataProvider;
import org.apache.sling.contextaware.config.spi.metadata.ConfigurationMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects configuration annotation classes deployed by any bundle via OSGi extender pattern.
 */
@Component(immediate = true, service = ConfigurationMetadataProvider.class)
public class AnnotationClassConfigurationMetadataProvider implements ConfigurationMetadataProvider {
    
    static final String HEADER = "Sling-ContextAware-Config-Packages";
        
    private BundleTracker<List<ConfigurationMapping>> bundleTracker;
    
    private ConcurrentMap<String,ConfigurationMapping> configurationMetadataMap = new ConcurrentHashMap<>();
    
    private static final Logger log = LoggerFactory.getLogger(AnnotationClassConfigurationMetadataProvider.class);
        
    @Activate
    private void activate(BundleContext bundleContext) {
        ConfigPackageBundleTackerCustomizer bundlerTrackerCustomizer = new ConfigPackageBundleTackerCustomizer(this);
        bundleTracker = new BundleTracker<List<ConfigurationMapping>>(bundleContext, Bundle.ACTIVE, bundlerTrackerCustomizer);
        bundleTracker.open();
    }
    
    @Deactivate
    private void deactivate() {
        bundleTracker.close();
        bundleTracker = null;
    }

    @Override
    public Set<String> getConfigurationNames() {
        return configurationMetadataMap.keySet();
    }

    @Override
    public ConfigurationMetadata getConfigurationMetadata(String configName) {
        ConfigurationMapping mapping = configurationMetadataMap.get(configName);
        if (mapping != null) {
            return mapping.getConfigMetadata();
        }
        else {
            return null;
        }
    }

    ConfigurationMapping addConfigurationMetadata(Class<?> configClass) {
        ConfigurationMetadata configMetadata = AnnotationClassParser.buildConfigurationMetadata(configClass);
        ConfigurationMapping configMapping = new ConfigurationMapping(configMetadata, configClass);
        ConfigurationMapping conflictingConfigMapping = configurationMetadataMap.putIfAbsent(configMetadata.getName(), configMapping);
        
        if (conflictingConfigMapping != null) {
            log.warn("Configuration name conflict: Both configuration classes {} and {} define the configuration name '{}', ignoring the latter.",
                    conflictingConfigMapping.getConfigClass().getName(),
                    configMapping.getConfigClass().getName(),
                    configMetadata.getName());
            return null;
        }
        else {
            return configMapping;
        }
    }

    void removeConfigurationMetadata(ConfigurationMapping configMapping) {
        configurationMetadataMap.remove(configMapping.getConfigMetadata().getName());
    }
    

    private static class ConfigurationMapping {
        private final ConfigurationMetadata configMetadata;
        private final Class<?> configClass;
        
        public ConfigurationMapping(ConfigurationMetadata configMetadata, Class<?> configClass) {
            this.configMetadata = configMetadata;
            this.configClass = configClass;
        }
        
        public ConfigurationMetadata getConfigMetadata() {
            return configMetadata;
        }
        
        public Class<?> getConfigClass() {
            return configClass;
        }
    }

    private static class ConfigPackageBundleTackerCustomizer implements BundleTrackerCustomizer<List<ConfigurationMapping>> {

        private final AnnotationClassConfigurationMetadataProvider metadataProvider;
        
        public ConfigPackageBundleTackerCustomizer(AnnotationClassConfigurationMetadataProvider metadataProvider) {
            this.metadataProvider = metadataProvider;
        }

        @Override
        public List<ConfigurationMapping> addingBundle(Bundle bundle, BundleEvent event) {
            Dictionary<String, String> headers = bundle.getHeaders();
            String packageList = headers.get(HEADER);
            if (packageList == null) {
                return null;
            }
            
            List<ConfigurationMapping> configMappings = new ArrayList<>();

            packageList = StringUtils.deleteWhitespace(packageList);
            String[] packages = StringUtils.split(packageList, ",");
            for (String singlePackage : packages) {
                Enumeration<URL> classUrls = bundle.findEntries("/" + singlePackage.replace('.', '/'), "*.class", true);

                if (classUrls == null) {
                    log.warn("No configuration classes found in package {}, ignoring.", singlePackage);
                    continue;
                }

                while (classUrls.hasMoreElements()) {
                    String className = toClassName(classUrls.nextElement());
                    try {
                        Class<?> configType = bundle.loadClass(className);
                        
                        if (AnnotationClassParser.isContextAwareConfig(configType)) {
                            log.debug("{}: Add configuration class {}", bundle.getSymbolicName(), className);
                            
                            ConfigurationMapping configMapping = metadataProvider.addConfigurationMetadata(configType);
                            if (configMapping != null) {
                                configMappings.add(configMapping);
                            }
                        }
                    }
                    catch (ClassNotFoundException ex) {
                        log.warn("Unable to load class: " + className, ex);
                    }

                }
            }
            return configMappings;
       }

        /**
         * Convert class URL to class name.
         */
        private String toClassName(URL url) {
            final String f = url.getFile();
            final String cn = f.substring(1, f.length() - ".class".length());
            return cn.replace('/', '.');
        }
        
        @Override
        public void modifiedBundle(Bundle bundle, BundleEvent event, List<ConfigurationMapping> configurationMappings) {
            // nothing to do   
        }

        @Override
        public void removedBundle(Bundle bundle, BundleEvent event, List<ConfigurationMapping> configurationMappings) {
            for (ConfigurationMapping configMapping : configurationMappings) {
                metadataProvider.removeConfigurationMetadata(configMapping);
            }
        }

    }
    
}
