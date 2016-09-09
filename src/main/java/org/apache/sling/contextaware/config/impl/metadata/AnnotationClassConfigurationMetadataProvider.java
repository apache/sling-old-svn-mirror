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

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;

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
    
    private BundleTracker<BundleConfigurationMapping> bundleTracker;
    private ConcurrentSkipListMap<Bundle,BundleConfigurationMapping> bundleMappings = new ConcurrentSkipListMap<>();
    
    private static final Logger log = LoggerFactory.getLogger(AnnotationClassConfigurationMetadataProvider.class);
    
    @Activate
    private void activate(BundleContext bundleContext) {
        ConfigClassBundleTackerCustomizer bundlerTrackerCustomizer = new ConfigClassBundleTackerCustomizer(this);
        bundleTracker = new BundleTracker<BundleConfigurationMapping>(bundleContext, Bundle.ACTIVE, bundlerTrackerCustomizer);
        bundleTracker.open();
    }
    
    @Deactivate
    private void deactivate() {
        bundleTracker.close();
        bundleTracker = null;
    }

    @Override
    public SortedSet<String> getConfigurationNames() {
        SortedSet<String> allConfigNames = new TreeSet<String>();
        for (BundleConfigurationMapping bundleMapping : bundleMappings.values()) {
            allConfigNames.addAll(bundleMapping.getConfigurationNames());
        }
        return Collections.unmodifiableSortedSet(allConfigNames);
    }

    @Override
    public ConfigurationMetadata getConfigurationMetadata(String configName) {
        ConfigurationMapping mapping = getConfigurationMapping(configName);
        if (mapping != null) {
            return mapping.getConfigMetadata();
        }
        else {
            return null;
        }
    }
    
    /**
     * Get configuration mapping for given config name.
     * On the way check for config name mapping conflicts accross bundles and log a warning if found.
     * Is difficult to do this beforehand due to the lazy initialization of the bundle config mappings and the
     * dynamic behavior of coming and going bundles with configuration classes.
     * @param configName Configuration name
     * @return Configuration mapping or null if none found
     */
    ConfigurationMapping getConfigurationMapping(String configName) {
        ConfigurationMapping matchingConfigMapping = null;
        BundleConfigurationMapping matchingBundleMapping = null;
        for (BundleConfigurationMapping bundleMapping : bundleMappings.values()) {
            ConfigurationMapping configMapping = bundleMapping.getConfigurationMapping(configName);
            if (configMapping != null) {
                if (matchingConfigMapping == null) {
                    matchingConfigMapping = configMapping;
                    matchingBundleMapping = bundleMapping;
                }
                else {
                    // conflict in name mapping across bundles found
                    log.warn("Configuration name conflict: Both configuration classes {} (Bundle {}) "
                            + "and {} (Bundle {}) define the configuration name '{}', ignoring the latter.",
                            matchingConfigMapping.getConfigClass().getName(),
                            matchingBundleMapping.getBundle().getSymbolicName(),
                            configMapping.getConfigClass().getName(),
                            bundleMapping.getBundle().getSymbolicName(),
                            configName);
                }
            }
        }
        return matchingConfigMapping;
    }

    void addBundeMapping(BundleConfigurationMapping bundleMapping) {
        bundleMappings.put(bundleMapping.getBundle(), bundleMapping);
    }

    void removeBundleMapping(BundleConfigurationMapping bundleMapping) {
        bundleMappings.remove(bundleMapping.getBundle());
    }
    
}
