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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects deployed bundles with configuration class header.
 */
class ConfigClassBundleTackerCustomizer implements BundleTrackerCustomizer<List<ConfigurationMapping>> {

    static final String CONFIGURATION_CLASSES_HEADER = "Sling-ContextAware-Configuration-Classes";
    
    private final AnnotationClassConfigurationMetadataProvider metadataProvider;
    
    private static final Logger log = LoggerFactory.getLogger(ConfigClassBundleTackerCustomizer.class);
    
    public ConfigClassBundleTackerCustomizer(AnnotationClassConfigurationMetadataProvider metadataProvider) {
        this.metadataProvider = metadataProvider;
    }

    @Override
    public List<ConfigurationMapping> addingBundle(Bundle bundle, BundleEvent event) {
        Dictionary<String, String> headers = bundle.getHeaders();
        String classeNamesList = headers.get(CONFIGURATION_CLASSES_HEADER);
        if (classeNamesList == null) {
            return null;
        }
        
        List<ConfigurationMapping> configMappings = new ArrayList<>();

        classeNamesList = StringUtils.deleteWhitespace(classeNamesList);
        String[] classNames = StringUtils.split(classeNamesList, ",");
        for (String className : classNames) {
            try {
                Class<?> configClass = bundle.loadClass(className);
                
                if (AnnotationClassParser.isContextAwareConfig(configClass)) {
                    log.debug("{}: Add configuration class {}", bundle.getSymbolicName(), className);
                    
                    ConfigurationMapping configMapping = new ConfigurationMapping(configClass);
                    if (metadataProvider.addConfigurationMetadata(configMapping)) {
                        configMappings.add(configMapping);
                    }
                }
            }
            catch (ClassNotFoundException ex) {
                log.warn("Unable to load class: " + className, ex);
            }
        }
        return configMappings;
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