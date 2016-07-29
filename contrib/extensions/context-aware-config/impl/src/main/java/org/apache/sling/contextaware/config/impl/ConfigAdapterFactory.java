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
package org.apache.sling.contextaware.config.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.contextaware.config.ConfigurationResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

@Component(service = AdapterFactory.class)
public class ConfigAdapterFactory implements AdapterFactory {

    private BundleTracker<ServiceRegistration<?>[]> bundleTracker;

    @Reference
    private ConfigurationResolver configurationResolver;

    private ConcurrentMap<String, ClassMapping> classMappings = new ConcurrentHashMap<>();

    @Activate
    private void activate(BundleContext bundleContext) {
        BundleTrackerCustomizer<ServiceRegistration<?>[]> bundlerTrackerCustomizer = new ConfigPackageBundleListener(bundleContext, this);
        bundleTracker = new BundleTracker<ServiceRegistration<?>[]>(bundleContext, Bundle.ACTIVE, bundlerTrackerCustomizer);
        bundleTracker.open();
    }

    @Deactivate
    private void deactivate() {
        bundleTracker.close();
        bundleTracker = null;
    }

    @Override
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        if (!(adaptable instanceof Resource)) {
            return null;
        }
        ClassMapping classMapping = classMappings.get(type.getName());
        if (classMapping == null) {
            return null;
        }

        // TODO: optimize class mapping to support default values and make it more efficient during runtime
        Resource resource = (Resource)adaptable;
        return configurationResolver.get(resource).as(type);
    }

    void addClassMapping(Class<?> configClass) {
        classMappings.put(configClass.getName(), new ClassMapping(configClass));
    }

    void removeClassMapping(String configClassName) {
        classMappings.remove(configClassName);
    }

    private static class ClassMapping {

        @SuppressWarnings("unused")
        private final Class<?> clazz;

        public ClassMapping(Class<?> clazz) {
            this.clazz = clazz;
        }


    }
}
