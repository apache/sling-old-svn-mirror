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

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.sling.contextaware.config.spi.ConfigurationMetadataProvider;
import org.apache.sling.contextaware.config.spi.metadata.ConfigurationMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects configuration annotation classes deployed by any bundle via OSGi extender pattern.
 */
@Component(immediate = true, service = ConfigurationMetadataProvider.class)
public class AnnotationClassConfigurationMetadataProvider implements ConfigurationMetadataProvider {
    
    private BundleTracker<List<ConfigurationMapping>> bundleTracker;
    
    private ConcurrentMap<String,ConfigurationMapping> configurationMetadataMap = new ConcurrentHashMap<>();
    
    private static final Logger log = LoggerFactory.getLogger(AnnotationClassConfigurationMetadataProvider.class);
        
    @Activate
    private void activate(BundleContext bundleContext) {
        ConfigClassBundleTackerCustomizer bundlerTrackerCustomizer = new ConfigClassBundleTackerCustomizer(this);
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

    boolean addConfigurationMetadata(ConfigurationMapping configMapping) {
        ConfigurationMapping conflictingConfigMapping = configurationMetadataMap.putIfAbsent(configMapping.getConfigName(), configMapping);
        
        if (conflictingConfigMapping != null) {
            log.warn("Configuration name conflict: Both configuration classes {} and {} define the configuration name '{}', ignoring the latter.",
                    conflictingConfigMapping.getConfigClass().getName(),
                    configMapping.getConfigClass().getName(),
                    configMapping.getConfigName());
            return false;
        }
        else {
            return true;
        }
    }

    void removeConfigurationMetadata(ConfigurationMapping configMapping) {
        configurationMetadataMap.remove(configMapping.getConfigMetadata().getName());
    }
    
}
