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

package org.apache.sling.replication.resources.impl;


import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.resources.impl.common.AbstractReadableResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ResourceProvider} for Osgi services for a specific service interface.
 * The main resource contains a list of service properties and can be adapted to the underlying service.
 * The accepted path is resourceRoot/{friendlyNameProperty}/childResourceName.
 */
public class OsgiServicePropertiesResourceProvider extends AbstractReadableResourceProvider implements ServiceTrackerCustomizer {

    private final BundleContext context;
    private final String friendlyNameProperty;

    private final Map<String, Object> services = new ConcurrentHashMap<String, Object>();
    private final Map<String, Map<String, Object>> serviceProperties = new ConcurrentHashMap<String, Map<String, Object>>();
    private final Map<String, String> resources = new ConcurrentHashMap<String, String>();

    public OsgiServicePropertiesResourceProvider(BundleContext context,
                                                 String serviceInterface,
                                                 String friendlyNameProperty,
                                                 String resourceRoot,
                                                 Map<String, String> additionalResourceProperties) {
        super(resourceRoot, additionalResourceProperties);
        this.context = context;
        this.friendlyNameProperty = friendlyNameProperty;
    }

    @Override
    protected Map<String, Object> getResourceProperties(String resourceName) {
        String serviceName = resources.get(resourceName);
        Map<String, Object> properties = serviceProperties.get(serviceName);
        Object service = services.get(serviceName);

        if (service != null && properties != null) {
            properties.put(ADAPTABLE_PROPERTY_NAME, service);
            return properties;
        }

        return null;
    }

    @Override
    protected Map<String, Object> getResourceRootProperties() {
        Set<String> serviceNames = services.keySet();

        Collection<Object> serviceObjects = services.values();

        Map<String, Object> result = new HashMap<String, Object>();

        result.put("items", serviceNames.toArray(new String[serviceNames.size()]));
        result.put(ADAPTABLE_PROPERTY_NAME, serviceObjects.toArray(new Object[serviceObjects.size()]));

        return result;
    }

    public Iterator<Resource> listChildren(Resource parent) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object addingService(ServiceReference serviceReference) {
        String serviceName = PropertiesUtil.toString(serviceReference.getProperty(friendlyNameProperty), null);

        if (serviceName == null) return null;

        String resourceName = getResourceName(serviceName);

        Map<String, Object> properties = new HashMap<String, Object>();

        for (String key : serviceReference.getPropertyKeys()) {
            properties.put(key, serviceReference.getProperty(key));
        }

        Object service = context.getService(serviceReference);

        services.put(serviceName, service);
        serviceProperties.put(serviceName, properties);
        resources.put(resourceName, serviceName);


        return service;
    }

    public void modifiedService(ServiceReference serviceReference, Object o) {
        // do nothing
    }

    public void removedService(ServiceReference serviceReference, Object o) {
        String serviceName = PropertiesUtil.toString(serviceReference.getProperty(friendlyNameProperty), null);

        if (serviceName == null) return;

        String resourceName = getResourceName(serviceName);

        services.remove(serviceName);
        serviceProperties.remove(serviceName);
        resources.remove(resourceName);

        context.ungetService(serviceReference);
    }

    private String  getResourceName(String serviceName) {
        String resourceName = serviceName;
        int index = serviceName.lastIndexOf("/");
        if (index >= 0) {
            resourceName = serviceName.substring(index+1);
        }
        return resourceName;

    }
}
