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

/**
 * Detects configuration annotation classes deployed by any bundle via OSGi extender pattern.
 */
@Component(immediate = true, service = ConfigurationMetadataProvider.class)
public class AnnotationClassConfigurationMetadataProvider implements ConfigurationMetadataProvider {
    
    private BundleTracker<BundleConfigurationMapping> bundleTracker;
    
    private ConcurrentMap<Bundle,BundleConfigurationMapping> bundleMappings = new ConcurrentHashMap<>();
    
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
    
    ConfigurationMapping getConfigurationMapping(String configName) {
        for (BundleConfigurationMapping bundleMapping : bundleMappings.values()) {
            ConfigurationMapping configMapping = bundleMapping.getConfigurationMapping(configName);
            if (configMapping != null) {
                return configMapping;
            }
        }
        return null;
    }

    void addBundeMapping(BundleConfigurationMapping bundleMapping) {
        bundleMappings.put(bundleMapping.getBundle(), bundleMapping);
    }

    void removeBundleMapping(BundleConfigurationMapping bundleMapping) {
        bundleMappings.remove(bundleMapping.getBundle());
    }
    
}
