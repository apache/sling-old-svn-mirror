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
package org.apache.sling.distribution.component.impl;

import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.component.DistributionComponentFactory;
import org.apache.sling.distribution.component.ManagedDistributionComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An content based service factory for {@link org.apache.sling.distribution.component.DistributionComponent}s using a compact configuration, already existing OSGi services
 * for the components to be wired can be used as well as directly instantiated components (called by type name).
 */
@Component(metatype = true,
        label = "Sling Distribution - Content Based Component Factory",
        description = "Content configuration Replication Component factory",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
public class ContentBasedDistributionComponentFactory {

    private static final int MAX_LEVEL = 2;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(label = "Name")
    public static final String NAME = "name";

    @Property(label = "Kind")
    public static final String KIND = "kind";

    @Property(label = "Path")
    public static final String PATH = "path";

    @Property(label = "Service Name")
    public static final String SERVICE_NAME = "servicename";


    @Reference
    DistributionComponentFactory componentFactory;

    @Reference
    ResourceResolverFactory resourceResolverFactory;


    private Map<String, ServiceRegistration> componentRegistrations = new ConcurrentHashMap<String, ServiceRegistration>();
    private ServiceRegistration resourceListenerRegistration;

    private BundleContext savedContext;
    private Map<String, Object> savedConfig;

    private String name;
    private String kind;
    private String path;
    private String servicename;

    @Activate
    public void activate(BundleContext context, Map<String, Object> config) throws Exception {
        log.debug("activating agent with config {}", config);

        savedContext = context;
        savedConfig = config;

        // inject configuration
        Dictionary<String, Object> componentListenerProperties = new Hashtable<String, Object>();

        name = PropertiesUtil.toString(config.get(NAME), null);
        kind = PropertiesUtil.toString(config.get(KIND), null);
        path = PropertiesUtil.toString(config.get(PATH), null);
        servicename = PropertiesUtil.toString(config.get(SERVICE_NAME), null);

        componentListenerProperties.put(NAME, name);


        Dictionary<String, Object> eventProperties = new Hashtable<String, Object>();
        eventProperties.put(EventConstants.EVENT_TOPIC, new String[]{
                SlingConstants.TOPIC_RESOURCE_ADDED,
                SlingConstants.TOPIC_RESOURCE_CHANGED,
                SlingConstants.TOPIC_RESOURCE_REMOVED
        });

        eventProperties.put(EventConstants.EVENT_FILTER, "(path=" + path + "/*)");
        resourceListenerRegistration = context.registerService(EventHandler.class.getName(),
                new ResourceChangeEventHandler() , eventProperties);

        registerAll();
    }

    @Deactivate
    private void deactivate(BundleContext context) {


        if (resourceListenerRegistration != null) {
            resourceListenerRegistration.unregister();
            resourceListenerRegistration = null;
        }

        unregisterAll();
    }

    private void refresh(String name) {
        unregister(name);
        register(name);
    }


    private void unregisterAll() {
        for(String name : componentRegistrations.keySet()) {
            unregister(name);
        }
    }

    private void registerAll() {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = getResolver();
            Resource resourceRoot = resourceResolver.getResource(path);

            if (resourceRoot != null && !ResourceUtil.isNonExistingResource(resourceRoot)) {
                for (Resource resource : resourceResolver.getChildren(resourceRoot)) {
                    String name = resource.getName();
                    Map<String, Object> config = extractMap(0, resource);
                    register(name, config);
                }
            }
        } catch (LoginException e) {
            if (resourceResolver != null) {
                resourceResolver.close();

            }
        }
    }

    ResourceResolver getResolver() throws LoginException {
        Map<String, Object> authenticationInfo = new HashMap<String, Object>();
        authenticationInfo.put(ResourceResolverFactory.SUBSERVICE, servicename);
        ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(authenticationInfo);

        return resourceResolver;
    }

    private void register(String name) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = getResolver();
            Resource resource = resourceResolver.getResource(path + "/" + name);

            Map<String, Object> config = extractMap(0, resource);
            register(name, config);
        } catch (LoginException e) {
            if (resourceResolver != null) {
                resourceResolver.close();

            }
        }

    }

    private void unregister(String name) {

        ServiceRegistration componentReg = componentRegistrations.get(name);

        if (componentReg != null) {
            ServiceReference reference = componentReg.getReference();

            if ("agent".equals(kind)) {
                Object replicationComponent =  savedContext.getService(reference);
                if (replicationComponent instanceof ManagedDistributionComponent) {
                    ((ManagedDistributionComponent) replicationComponent).disable();
                }
            }

            componentReg.unregister();

        }
    }

    private Map<String, Object> extractMap(int level, Resource resource) {
        if (level > MAX_LEVEL)
            return null;

        Map<String, Object> result = new HashMap<String, Object>();
        ValueMap resourceProperties = ResourceUtil.getValueMap(resource);
        result.putAll(resourceProperties);

        for (Resource childResource : resource.getChildren()) {
            Map<String, Object> childMap = extractMap(level+1, childResource);
            if (childMap != null) {
                result.put(childResource.getName(), childMap);
            }
        }

        return result;
    }

    private void register(String componentName, Map<String, Object> config) {
        String componentClass = null;
        Object componentObject = null;

        if ("agent".equals(kind)) {
            componentClass = DistributionAgent.class.getName();
            componentObject = componentFactory.createComponent(DistributionAgent.class, config, null);
        }


        if (componentObject != null && componentClass != null) {
            if (componentObject instanceof ManagedDistributionComponent) {
                ((ManagedDistributionComponent) componentObject).enable();
            }

            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(NAME, componentName);
            ServiceRegistration componentReg = savedContext.registerService(componentClass, componentObject, props);
            componentRegistrations.put(componentName, componentReg);
            log.debug("activated component kind {} name", kind, componentName);
        }
    }

    private class ResourceChangeEventHandler implements EventHandler {

        public void handleEvent(Event event) {
            String eventPath = (String) event.getProperty("path");
            String prefix = path + "/";
            if (eventPath != null && eventPath.startsWith(prefix)) {
                String name = eventPath.substring(prefix.length());
                int slashIndex = name.indexOf('/');
                if (slashIndex >= 0) {
                    name = name.substring(0, slashIndex);
                }

                refresh(name);
            }

        }
    }


}
