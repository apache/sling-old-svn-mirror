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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An resource based service factory for distribution components using a compact configuration, already existing OSGi services
 * for the components to be wired can be used as well as directly instantiated components (called by type name).
 */
@Component(metatype = true,
        label = "Sling Distribution - Resource Based Component Factory",
        description = "Resource configuration for Distribution Components Factory",
        configurationFactory = true,
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
public class ResourceBasedDistributionComponentFactory {

    private static final int MAX_LEVEL = 2;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Property(label = "Name")
    public static final String NAME = "name";

    @Property(label = "Kind")
    public static final String KIND = "kind";

    @Property(label = "Path")
    public static final String PATH = "path";

    @Property(label = "Defaults Path")
    public static final String DEFAULTS_PATH = "defaults.path";


    @Reference
    private DistributionComponentManager componentManager;


    @Reference
    ResourceResolverFactory resourceResolverFactory;


    private Map<String, ServiceRegistration> componentRegistrations = new ConcurrentHashMap<String, ServiceRegistration>();
    private ServiceRegistration resourceListenerRegistration;

    private BundleContext savedContext;
    private Map<String, Object> savedConfig;

    private String name;
    private String kind;
    private String path;
    private String defaultsPath;

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
        defaultsPath = PropertiesUtil.toString(config.get(DEFAULTS_PATH), null);

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
                    register(name);
                }
            }
        } catch (LoginException e) {
            if (resourceResolver != null) {
                resourceResolver.close();

            }
        }
    }

    ResourceResolver getResolver() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

        return resourceResolver;
    }

    private void register(String name) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = getResolver();
            Resource resource = resourceResolver.getResource(path + "/" + name);
            Map<String, Object> config = new HashMap<String, Object>();

            if (defaultsPath != null) {
                Resource defaultsResource = resourceResolver.getResource(defaultsPath);
                if (defaultsResource != null) {
                    config = extractMap(0, defaultsResource);
                }
            }
            Map<String, Object> componentConfig = extractMap(0, resource);

            putMap(0, componentConfig, config);
            config.put(DistributionComponentUtils.NAME, name);

            register(name, config);
        } catch (LoginException e) {
            if (resourceResolver != null) {
                resourceResolver.close();

            }
        }

    }

    private void register(String componentName, Map<String, Object> config) {

        if ("agent".equals(kind)) {
            componentManager.createComponent(DistributionAgent.class, componentName,  config);
        }
    }


    private void unregister(String componentName) {

        if ("agent".equals(kind)) {
            componentManager.deleteComponent(DistributionAgent.class, componentName);
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

    private void putMap(int level, Map<String, Object> source, Map<String, Object> target) {
        if (level > MAX_LEVEL)
            return;

        for (Map.Entry<String, Object> entry : source.entrySet()) {

            if (target.containsKey(entry.getKey())
                    && entry.getValue() instanceof Map
                    && target.get(entry.getKey()) instanceof Map) {
                putMap(level, (Map) entry.getValue(), (Map) target.get(entry.getKey()));

            }
            else  {
                target.put(entry.getKey(), entry.getValue());
            }
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
