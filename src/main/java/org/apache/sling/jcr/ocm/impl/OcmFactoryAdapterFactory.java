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
package org.apache.sling.jcr.ocm.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Session;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

class OcmFactoryAdapterFactory implements AdapterFactory {

    private final ObjectContentManagerFactoryImpl ocmFactory;

    private ServiceRegistration registration;

    OcmFactoryAdapterFactory(ObjectContentManagerFactoryImpl factory, BundleContext bundleContext) {
        this.ocmFactory = factory;

        final Dictionary<String, Object> registrationProperties = new Hashtable<String, Object>();
        registrationProperties.put(ADAPTABLE_CLASSES, ResourceResolver.class.getName());
        registrationProperties.put(ADAPTER_CLASSES, ObjectContentManager.class.getName());
        registrationProperties.put(Constants.SERVICE_DESCRIPTION, "Sling OCM Adapter Factory");
        registrationProperties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

        registration = bundleContext.registerService(SERVICE_NAME, this, registrationProperties);
    }

    void dispose() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Object adaptable,
            Class<AdapterType> type) {

        // must work, we only support ResourceResolver
        final ResourceResolver factory = (ResourceResolver) adaptable;

        // the resource resolver must be adaptable to a session
        final Session session = factory.adaptTo(Session.class);
        if (session == null) {
            return null;
        }

        return (AdapterType)this.ocmFactory.getObjectContentManager(session);
    }
}
