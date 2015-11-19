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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

@Component
public class MergedResourcePickerWhiteboard implements ServiceTrackerCustomizer {

    private ServiceTracker tracker;

    private BundleContext bundleContext;

    private final Map<Long, ServiceRegistration> serviceRegistrations = new ConcurrentHashMap<Long, ServiceRegistration>();

    @Activate
    protected void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        tracker = new ServiceTracker(bundleContext, MergedResourcePicker.class.getName(), this);
        tracker.open();
    }

    @Deactivate
    protected void deactivate() {
        tracker.close();
    }

    public Object addingService(final ServiceReference reference) {
        final MergedResourcePicker picker = (MergedResourcePicker) bundleContext.getService(reference);
        if ( picker != null ) {
            final String mergeRoot = PropertiesUtil.toString(reference.getProperty(MergedResourcePicker.MERGE_ROOT), null);
            if (mergeRoot != null) {
                boolean readOnly = PropertiesUtil.toBoolean(reference.getProperty(MergedResourcePicker.READ_ONLY), true);
                boolean traverseParent = PropertiesUtil.toBoolean(reference.getProperty(MergedResourcePicker.TRAVERSE_PARENT), false);

                MergingResourceProvider provider = readOnly ?
                        new MergingResourceProvider(mergeRoot, picker, true, traverseParent) :
                        new CRUDMergingResourceProvider(mergeRoot, picker, traverseParent);

                final Dictionary<Object, Object> props = new Hashtable<Object, Object>();
                props.put(ResourceProvider.PROPERTY_NAME, readOnly ? "Merging" : "CRUDMerging");
                props.put(ResourceProvider.PROPERTY_ROOT, mergeRoot);
                props.put(ResourceProvider.PROPERTY_MODIFIABLE, !readOnly);
                props.put(ResourceProvider.PROPERTY_AUTHENTICATE, ResourceProvider.AUTHENTICATE_NO);

                final Long key = (Long) reference.getProperty(Constants.SERVICE_ID);
                final ServiceRegistration reg = bundleContext.registerService(ResourceProvider.class.getName(), provider, props);

                serviceRegistrations.put(key, reg);
            }
            return picker;
        }
        return null;
    }

    public void modifiedService(final ServiceReference reference, final Object service) {
        removedService(reference, service);
        addingService(reference);
    }

    public void removedService(final ServiceReference reference, final Object service) {
        final Long key = (Long) reference.getProperty(Constants.SERVICE_ID);
        final ServiceRegistration reg = serviceRegistrations.get(key);
        if ( reg != null ) {
            reg.unregister();
            this.bundleContext.ungetService(reference);
        }
    }

}
