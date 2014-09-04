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
package org.apache.sling.resourcemerger.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

@Component
public class MergedResourcePickerWhiteboard implements ServiceTrackerCustomizer {

    private ServiceTracker tracker;

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        tracker = new ServiceTracker(bundleContext, MergedResourcePicker.class.getName(), this);
        tracker.open();
    }

    @Deactivate
    protected void deactivate() {
        tracker.close();
    }

    public Object addingService(ServiceReference reference) {
        MergedResourcePicker picker = (MergedResourcePicker) bundleContext.getService(reference);
        String mergeRoot = PropertiesUtil.toString(reference.getProperty(MergedResourcePicker.MERGE_ROOT), null);
        if (mergeRoot != null) {
            ResourceProviderFactory providerFactory = new MergingResourceProviderFactory(mergeRoot, picker);
            Dictionary<Object, Object> props = new Hashtable<Object, Object>();
            props.put(ResourceProvider.ROOTS, mergeRoot);
            props.put(ResourceProvider.OWNS_ROOTS, true);
            return bundleContext.registerService(ResourceProviderFactory.class.getName(), providerFactory, props);
        } else {
            return null;
        }
    }

    public void modifiedService(ServiceReference reference, Object service) {
        // TODO Auto-generated method stub

    }

    public void removedService(ServiceReference reference, Object service) {
        if (service instanceof ServiceRegistration) {
            ((ServiceRegistration) service).unregister();
        }
    }

}
