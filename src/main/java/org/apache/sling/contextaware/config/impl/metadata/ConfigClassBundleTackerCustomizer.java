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

import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Detects deployed bundles with configuration class header.
 */
class ConfigClassBundleTackerCustomizer implements BundleTrackerCustomizer<BundleConfigurationMapping> {

    static final String CONFIGURATION_CLASSES_HEADER = "Sling-ContextAware-Configuration-Classes";
    
    private final AnnotationClassConfigurationMetadataProvider metadataProvider;
    
    public ConfigClassBundleTackerCustomizer(AnnotationClassConfigurationMetadataProvider metadataProvider) {
        this.metadataProvider = metadataProvider;
    }

    @Override
    public BundleConfigurationMapping addingBundle(Bundle bundle, BundleEvent event) {
        Dictionary<String, String> headers = bundle.getHeaders();
        String classeNamesList = headers.get(CONFIGURATION_CLASSES_HEADER);
        if (classeNamesList == null) {
            return null;
        }
        BundleConfigurationMapping bundleMapping = new BundleConfigurationMapping(bundle, classeNamesList);
        metadataProvider.addBundeMapping(bundleMapping);
        return bundleMapping;
   }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, BundleConfigurationMapping bundleMapping) {
        // nothing to do   
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, BundleConfigurationMapping bundleMapping) {
        metadataProvider.removeBundleMapping(bundleMapping);
    }

}
