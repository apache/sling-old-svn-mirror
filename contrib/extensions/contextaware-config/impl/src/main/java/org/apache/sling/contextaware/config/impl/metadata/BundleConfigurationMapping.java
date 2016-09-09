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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains metadata of all configuration classes that where declared via the bundle header for a bundle.
 * The configuration class metadata is not parsed on initialization, but lazily on first access.
 */
class BundleConfigurationMapping {
    
    private final Bundle bundle;
    private final String classNamesList;
    private final AtomicReference<Map<String,ConfigurationMapping>> configMappingsRef = new AtomicReference<>(null);
    
    private static final Logger log = LoggerFactory.getLogger(BundleConfigurationMapping.class);
    
    public BundleConfigurationMapping(Bundle bundle, String classNamesList) {
        this.bundle = bundle;
        this.classNamesList = classNamesList;
    }

    public Bundle getBundle() {
        return bundle;
    }
    
    /**
     * Thread-safe lazy initialization of configuration mappings.
     * @return Configuration mappings
     */
    private Map<String,ConfigurationMapping> getConfigMappings() {
        Map<String,ConfigurationMapping> configMappings = configMappingsRef.get();
        if (configMappings == null) {
            configMappings = initializeConfigMappings();
            if (configMappingsRef.compareAndSet(null, configMappings)) {
                return configMappings;
            }
            else { 
                return configMappingsRef.get();
            }
        }
        else {
            return configMappings;
        }
    }
    
    /**
     * Parse all annotation classes
     * @return
     */
    private Map<String,ConfigurationMapping> initializeConfigMappings() {
        Map<String,ConfigurationMapping> configMappings = new HashMap<>();
        
        String[] classNames = StringUtils.split(StringUtils.deleteWhitespace(classNamesList), ",");
        for (String className : classNames) {
            try {
                Class<?> configClass = bundle.loadClass(className);
                if (AnnotationClassParser.isContextAwareConfig(configClass)) {
                    log.debug("{}: Add configuration class {}", bundle.getSymbolicName(), className);
                    
                    ConfigurationMapping configMapping = new ConfigurationMapping(configClass);
                    if (!hasMappingConflict(configMapping, configMappings)) {
                        configMappings.put(configMapping.getConfigName(), configMapping);
                    }
                }
                else {
                    log.warn("Ignoring invalid configuration class: {}", className);
                }
            }
            catch (ClassNotFoundException ex) {
                log.warn("Unable to load class: " + className, ex);
            }
        }
        
        return configMappings;
    }
    
    private boolean hasMappingConflict(ConfigurationMapping newConfigMapping,
            Map<String,ConfigurationMapping> configMappings) {
        ConfigurationMapping conflictingConfigMapping = configMappings.get(newConfigMapping.getConfigName());
        if (conflictingConfigMapping != null) {
            log.warn("Configuration name conflict in bundle {}: Both configuration classes {} and {} define the configuration name '{}', ignoring the latter.",
                    bundle.getSymbolicName(),
                    conflictingConfigMapping.getConfigClass().getName(),
                    newConfigMapping.getConfigClass().getName(),
                    newConfigMapping.getConfigName());
            return true;
        }
        else {
            return false;
        }
    }
    
    public Set<String> getConfigurationNames() {
        return getConfigMappings().keySet();
    }
    
    public ConfigurationMapping getConfigurationMapping(String configName) {
        return getConfigMappings().get(configName);
    }
    
}
