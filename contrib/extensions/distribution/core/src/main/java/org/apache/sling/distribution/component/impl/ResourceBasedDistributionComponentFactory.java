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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An resource based service factory for distribution components using resource based configuration.
 * A root path must be configured for listening on content changes.
 * The changes to the resource settings will be checked periodically (30s) to avoid event bursts and concurrency issues.
 */
@Component(label = "Sling Distribution - Resource Based Component Factory",
        description = "Resource configuration for Distribution Components Factory",
        specVersion = "1.1",
        policy = ConfigurationPolicy.REQUIRE
)
@Service(value = Runnable.class)
@Properties({
        @Property( name = "scheduler.period", longValue = 30),
        @Property( name="scheduler.concurrent", boolValue=false)

})
public class ResourceBasedDistributionComponentFactory implements Runnable {

    /**
     * Max depth level to explore the content structure for settings
     * (e.g. .../settings/agents/publish/level1/level2)
     */
    private static final int MAX_DEPTH_LEVEL = DistributionComponentUtils.MAX_DEPTH_LEVEL;

    /**
     * The name of the sub folder that contains defaults
     * (e.g. .../settings/defaults/agents)
     */
    private static final String DEFAULTS_FOLDER = "defaults";


    private final Logger log = LoggerFactory.getLogger(getClass());


    @Property(label = "Kind", cardinality = 10)
    public static final String KIND_FOLDERS = "kind.folder.name";

    @Property(label = "Path")
    public static final String ROOT_PATH = "path";


    @Reference
    private DistributionComponentManager componentManager;


    @Reference
    ResourceResolverFactory resourceResolverFactory;

    Map<String, Boolean> scheduledComponentsMap = new ConcurrentHashMap<String, Boolean>();


    private ServiceRegistration resourceListenerRegistration;


    private BundleContext savedContext;
    private Map<String, Object> savedConfig;

    private Map<String, String> kindFolders;
    private String rootPath;

    @Activate
    public void activate(BundleContext context, Map<String, Object> config) throws Exception {
        log.debug("activating agent with config {}", config);

        savedContext = context;
        savedConfig = config;


        kindFolders = PropertiesUtil.toMap(config.get(KIND_FOLDERS), new String[]{"agent=agents",
                "importer=importers",
                "exporter=exporters"

        });
        rootPath = PropertiesUtil.toString(config.get(ROOT_PATH), null);

        {
            Dictionary<String, Object> eventProperties = new Hashtable<String, Object>();
            eventProperties.put(EventConstants.EVENT_TOPIC, new String[]{
                    SlingConstants.TOPIC_RESOURCE_ADDED,
                    SlingConstants.TOPIC_RESOURCE_CHANGED,
                    SlingConstants.TOPIC_RESOURCE_REMOVED
            });

            eventProperties.put(EventConstants.EVENT_FILTER, "(path=" + rootPath + "/*)");
            resourceListenerRegistration = context.registerService(EventHandler.class.getName(),
                    new ResourceChangeEventHandler() , eventProperties);

        }

        scheduleRefreshAll();

    }


    @Deactivate
    private void deactivate(BundleContext context) {


        if (resourceListenerRegistration != null) {
            resourceListenerRegistration.unregister();
            resourceListenerRegistration = null;
        }



        unregisterAll();
    }


    private void scheduleRefreshAll() {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = getResolver();
            Resource resourceRoot = resourceResolver.getResource(rootPath);


            if (resourceRoot != null && !ResourceUtil.isNonExistingResource(resourceRoot)) {
                for (String folderName : kindFolders.values()) {
                    Resource folderResource = resourceRoot.getChild(folderName);
                    if (folderResource != null && !ResourceUtil.isNonExistingResource(folderResource)) {
                        for (Resource resource : resourceResolver.getChildren(folderResource)) {
                            scheduleRefresh(resource.getPath());
                        }
                    }
                }
            }
        } catch (LoginException e) {
            log.error("unable to register", e);
        }
        finally {
            if (resourceResolver != null) {
                resourceResolver.close();

            }
        }
    }

    private void scheduleRefresh(String componentPath) {
        scheduledComponentsMap.put(componentPath, true);
    }



    private void unregisterAll() {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = getResolver();
            Resource resourceRoot = resourceResolver.getResource(rootPath);


            if (resourceRoot != null && !ResourceUtil.isNonExistingResource(resourceRoot)) {
                for (String folderName : kindFolders.values()) {
                    Resource folderResource = resourceRoot.getChild(folderName);
                    if (folderResource != null && !ResourceUtil.isNonExistingResource(folderResource)) {
                        for (Resource resource : resourceResolver.getChildren(folderResource)) {
                            unregister(resource.getPath());
                        }
                    }
                }
            }
        } catch (LoginException e) {
            log.error("unable to register", e);
        }
        finally {
            if (resourceResolver != null) {
                resourceResolver.close();

            }
        }
    }

    private void refreshAll() {
        List<String> toBeRefreshed = getComponentsToRefresh();
        if (toBeRefreshed != null && toBeRefreshed.size() > 0) {
            refreshList(toBeRefreshed);
        }
    }

    private List<String> getComponentsToRefresh() {
        List<String> result = new ArrayList<String>();
        result.addAll(scheduledComponentsMap.keySet());

        scheduledComponentsMap.clear();
        return result;
    }

    private void refreshList(List<String> resourcePaths) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = getResolver();

            for (String resourcePath : resourcePaths) {
                log.info("Refresh started for {}", resourcePath);
                unregister(resourcePath);
                boolean registered = register(resourcePath);
                log.info("Refresh finished {} for {}",  registered, resourcePath);

                if (!registered) {
                    scheduleRefresh(resourcePath);
                }
            }
        } catch (LoginException e) {
            log.error("Cannot obtain resource resolver", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    ResourceResolver getResolver() throws LoginException {
        ResourceResolver resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

        return resourceResolver;
    }

    private boolean register(String componentPath) {
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = getResolver();

            Resource resource = resourceResolver.getResource(componentPath);

            if (resource == null) {
                return true;
            }


            String kind = getComponentKind(componentPath);
            String name = getComponentName(componentPath);
            Map<String, Object> componentConfig = extractMap(0, resource);
            String type = PropertiesUtil.toString(componentConfig.get(DistributionComponentUtils.PN_TYPE), null);

            Map<String, Object> config = new HashMap<String, Object>();

            String defaultsPath = getGlobalDefaultsPath(kind, type);

            if (defaultsPath != null) {
                Resource defaultsResource = resourceResolver.getResource(defaultsPath);
                if (defaultsResource != null) {
                    config = extractMap(0, defaultsResource);
                }
            }

            mergeMap(0, componentConfig, config);


            config.put(DistributionComponentUtils.PN_KIND, kind);
            config.put(DistributionComponentUtils.PN_TYPE, type);
            config.put(DistributionComponentUtils.PN_NAME, name);

            componentManager.createComponent(kind, name,  config);

        } catch (Throwable e) {
            log.error("Cannot register {}", componentPath, e);
            return false;
        }
        finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }

        return true;
    }



    private void unregister(String componentPath) {

        try {

            String kind = getComponentKind(componentPath);
            String componentName = getComponentName(componentPath);
            componentManager.deleteComponent(kind, componentName);

        }
        catch (Throwable e) {
            log.error("Cannot unregister {}", componentPath, e);
        }
    }

    private Map<String, Object> extractMap(int level, Resource resource) {
        if (level > MAX_DEPTH_LEVEL)
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

    private void mergeMap(int level, Map<String, Object> source, Map<String, Object> target) {
        if (level > MAX_DEPTH_LEVEL)
            return;

        for (Map.Entry<String, Object> entry : source.entrySet()) {

            if (target.containsKey(entry.getKey())
                    && entry.getValue() instanceof Map
                    && target.get(entry.getKey()) instanceof Map) {
                mergeMap(level, (Map) entry.getValue(), (Map) target.get(entry.getKey()));

            }
            else  {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void run() {
        try {
            refreshAll();
        }
        catch (Throwable e) {
            log.error("Cannot refresh components", e);
        }

    }

    private class ResourceChangeEventHandler implements EventHandler {

        public void handleEvent(Event event) {

            String eventPath = (String) event.getProperty("path");

            String componentPath = getComponentPath(eventPath);

            if (componentPath != null) {
                scheduleRefresh(componentPath);
            }
        }
    }




    Map<String, String> parsePath(String resourcePath)  {
        Map<String, String> result = new HashMap<String, String>();
        if (resourcePath.startsWith(rootPath +"/")) {
            String relativePath = resourcePath.substring(rootPath.length() + 1);
            for (Map.Entry<String, String> entry : kindFolders.entrySet()) {
                String kind = entry.getKey();
                String folderName = entry.getValue();

                if (relativePath.startsWith(folderName +"/")) {
                    String componentName = relativePath.substring(folderName.length() + 1);
                    int idx = componentName.indexOf("/");
                    if (idx >=0) {
                        componentName = componentName.substring(0, idx);
                    }

                    result.put("componentPath", rootPath + "/" + folderName + "/" + componentName);
                    result.put("componentKind", kind);
                    result.put("componentName", componentName);
                }
            }
        }
        return result;
    }


    String getComponentPath(String path) {
        Map<String, String> result = parsePath(path);
        return result.get("componentPath");
    }

    String getGlobalDefaultsPath(String kind, String type) {
        if (kind != null && type != null) {
            String kindFolder = kindFolders.get(kind);
            return rootPath + "/" + DEFAULTS_FOLDER + "/" + kindFolder + "/" + type;
        }
        return null;

    }

    String getComponentKind(String path) {
        Map<String, String> result = parsePath(path);
        return result.get("componentKind");
    }

    String getComponentName(String path) {
        Map<String, String> result = parsePath(path);
        return result.get("componentName");
    }


}
