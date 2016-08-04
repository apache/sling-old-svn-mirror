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


import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager implementation which represents the distribution configurations as resources.
 */
public class ResourceConfigurationManager implements DistributionConfigurationManager {

    final String CONTENT_NODE = "jcr:content";
    final String configRootPath;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String[] configProperties;
    private final Map<String, String> configDefaults;

    public ResourceConfigurationManager(String configRootPath, String[] configProperties, Map<String, String> configDefaults) {
        this.configRootPath = configRootPath;
        this.configProperties = configProperties;
        this.configDefaults = configDefaults;
    }

    @Override
    public List<DistributionConfiguration> getConfigs(ResourceResolver resolver, DistributionComponentKind kind) {

        List<DistributionConfiguration> configurations = new ArrayList<DistributionConfiguration>();

        Resource configRoot = resolver.getResource(configRootPath);

        if (configRoot == null) {
            return new ArrayList<DistributionConfiguration>();
        }

        for (Resource configResource : configRoot.getChildren()) {
            Map<String, Object> configMap = getFilteredMap(configResource);

            configurations.add(new DistributionConfiguration(kind, configResource.getName(), configMap));

        }

        return configurations;
    }

    @Override
    public DistributionConfiguration getConfig(ResourceResolver resolver, DistributionComponentKind kind, String name) {

        Resource configRoot = resolver.getResource(configRootPath);

        if (configRoot == null) {
            return null;
        }

        Resource configResource = configRoot.getChild(name);

        if (configResource == null) {
            return null;
        }


        Map<String, Object> configMap = getFilteredMap(configResource);

        return new DistributionConfiguration(kind, configResource.getName(), configMap);
    }

    @Override
    public void saveConfig(ResourceResolver resolver, DistributionConfiguration config) {
        Resource configRoot = resolver.getResource(configRootPath);

        if (configRoot == null) {
            return;
        }

        Resource configResource = configRoot.getChild(config.getName());
        Resource contentResource = (configResource != null)
                ? configResource.getChild(CONTENT_NODE)
                : null ;

        try {
            if (configResource == null) {
                Map<String, Object> configMap = new HashMap<String, Object>();
                configMap.put("jcr:primaryType", "sling:Folder");
                configResource = resolver.create(configRoot, config.getName(), configMap);
            }

            if (contentResource == null) {
                Map<String, Object> contentMap = new HashMap<String, Object>();
                contentMap.put("jcr:primaryType", "nt:unstructured");
                contentResource = resolver.create(configResource, CONTENT_NODE, contentMap);
            }

            Map<String, Object> properties = config.getProperties();
            properties = filterMap(properties);
            properties.putAll(configDefaults);

            ModifiableValueMap valueMap = contentResource.adaptTo(ModifiableValueMap.class);
            valueMap.putAll(properties);
        } catch (PersistenceException e) {
            log.error("cannot save config {}", config.getName(),  e);
        }

    }

    @Override
    public void deleteConfig(ResourceResolver resolver, DistributionComponentKind kind, String name) {
        Resource configRoot = resolver.getResource(configRootPath);

        Resource configResource = configRoot.getChild(name);

        if (configResource == null) {
            return;
        }

        try {
            resolver.delete(configResource);
            resolver.commit();
        } catch (PersistenceException e) {
            log.error("cannot delete config {}", name,  e);
        }

    }

    Map<String, Object> getMap(Resource resource) {
        Map<String, Object> result = new HashMap<String, Object>();
        Resource contentResource = resource.getChild(CONTENT_NODE);

        if (contentResource != null) {
            ValueMap configMap = contentResource.adaptTo(ValueMap.class);

            result.putAll(configMap);
        }

        return result;
    }

    Map<String, Object> getFilteredMap(Resource resource) {
        Map<String, Object> result = getMap(resource);

        result = filterMap(result);

        return result;
    }

    Map<String, Object> filterMap(Map<String, Object> configMap) {
        Map<String, Object> result = new HashMap<String, Object>();

        for (String key: configMap.keySet()) {
            if (isAccepted(key)) {
                result.put(key, configMap.get(key));
            }
        }

        return result;
    }

    boolean isAccepted(String key) {
        for (String property : configProperties) {
           if (property.equals(key)) {
               return true;
           }
        }
        return false;
    }




}
