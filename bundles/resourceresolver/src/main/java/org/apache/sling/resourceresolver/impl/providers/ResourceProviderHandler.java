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
package org.apache.sling.resourceresolver.impl.providers;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.resourceresolver.impl.legacy.LegacyResourceProviderWhiteboard;
import org.apache.sling.resourceresolver.impl.providers.tree.Pathable;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class ResourceProviderHandler implements Comparable<ResourceProviderHandler>, Pathable {

    private final ResourceProviderInfo info;

    private final BundleContext bundleContext;

    private final EventAdmin eventAdmin;

    private volatile ResourceProvider<?> provider;

    public ResourceProviderHandler(final BundleContext bc, final ResourceProviderInfo info, final EventAdmin eventAdmin) {
        this.info = info;
        this.bundleContext = bc;
        this.eventAdmin = eventAdmin;
    }

    public ResourceProviderInfo getInfo() {
        return this.info;
    }

    public ResourceProvider<?> getResourceProvider() {
        ResourceProvider<?> rp = this.provider;
        if ( rp == null ) {
            synchronized ( this ) {
                if ( this.provider == null ) {
                    this.provider = (ResourceProvider<?>) this.bundleContext.getService(this.info.getServiceReference());
                }
                rp = this.provider;
                postEvent(SlingConstants.TOPIC_RESOURCE_PROVIDER_ADDED);
            }
        }
        return rp;
    }

    private void postEvent(String topic) {
        final Dictionary<String, Object> eventProps = new Hashtable<String, Object>();
        eventProps.put(SlingConstants.PROPERTY_PATH, info.getPath());
        String pid = (String) info.getServiceReference().getProperty(Constants.SERVICE_PID);
        if (pid == null) {
            pid = (String) info.getServiceReference().getProperty(LegacyResourceProviderWhiteboard.ORIGINAL_SERVICE_PID);
        }
        if (pid != null) {
            eventProps.put(Constants.SERVICE_PID, pid);
        }
        eventAdmin.postEvent(new Event(topic, eventProps));
    }

    public void deactivate() {
        if ( this.provider != null ) {
            this.provider = null;
            this.bundleContext.ungetService(this.info.getServiceReference());
            postEvent(SlingConstants.TOPIC_RESOURCE_PROVIDER_REMOVED);
        }
    }

    @Override
    public int compareTo(final ResourceProviderHandler o) {
        return this.getInfo().compareTo(o.getInfo());
    }

    @Override
    public String getPath() {
        return this.getInfo().getPath();
    }
}
