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
package org.apache.sling.caconfig.management.impl;

import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.spi.ConfigurationCollectionPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceException;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy;

/**
 * This is a variant of {@link org.apache.sling.caconfig.impl.def.DefaultConfigurationPersistenceStrategy}
 * which reads and stores data from a sub-resources named "jcr:content".
 */
public class CustomConfigurationPersistenceStrategy implements ConfigurationPersistenceStrategy {
    
    private static final String DEFAULT_RESOURCE_TYPE = JcrConstants.NT_UNSTRUCTURED;
    
    private static final String CHILD_NODE_NAME = "jcr:content";
    
    @Override
    public Resource getResource(Resource resource) {
        return resource.getChild(CHILD_NODE_NAME);
    }

    @Override
    public String getResourcePath(String resourcePath) {
        return resourcePath + "/" + CHILD_NODE_NAME;
    }

    @Override
    public boolean persistConfiguration(ResourceResolver resourceResolver, String configResourcePath,
            ConfigurationPersistData data) {
        getOrCreateResource(resourceResolver, configResourcePath + "/" + CHILD_NODE_NAME, data.getProperties());
        commit(resourceResolver);
        return true;
    }

    @Override
    public boolean persistConfigurationCollection(ResourceResolver resourceResolver, String configResourceCollectionParentPath,
            ConfigurationCollectionPersistData data) {
        Resource configResourceParent = getOrCreateResource(resourceResolver, configResourceCollectionParentPath, ValueMap.EMPTY);
        
        // delete existing children and create new ones
        deleteChildren(configResourceParent);
        for (ConfigurationPersistData item : data.getItems()) {
            String path = configResourceParent.getPath() + "/" + item.getCollectionItemName() + "/" + CHILD_NODE_NAME;
            getOrCreateResource(resourceResolver, path, item.getProperties());
        }
        
        // if resource collection parent properties are given replace them as well
        if (data.getProperties() != null) {
            replaceProperties(configResourceParent, data.getProperties());
        }
        
        commit(resourceResolver);
        return true;
    }
    
    @Override
    public boolean deleteConfiguration(ResourceResolver resourceResolver, String configResourcePath) {
        Resource resource = resourceResolver.getResource(configResourcePath);
        if (resource != null) {
            try {
                resourceResolver.delete(resource);
            }
            catch (PersistenceException ex) {
                throw new ConfigurationPersistenceException("Unable to delete configuration at " + configResourcePath, ex);
            }
        }
        commit(resourceResolver);
        return true;
    }

    private Resource getOrCreateResource(ResourceResolver resourceResolver, String path, Map<String,Object> properties) {
        try {
            Resource resource = ResourceUtil.getOrCreateResource(resourceResolver, path, DEFAULT_RESOURCE_TYPE, DEFAULT_RESOURCE_TYPE, false);
            replaceProperties(resource, properties);
            return resource;
        }
        catch (PersistenceException ex) {
            throw new ConfigurationPersistenceException("Unable to persist configuration to " + path, ex);
        }
    }

    private void deleteChildren(Resource resource) {
        ResourceResolver resourceResolver = resource.getResourceResolver();
        try {
            for (Resource child : resource.getChildren()) {
                resourceResolver.delete(child);
            }
        }
        catch (PersistenceException ex) {
            throw new ConfigurationPersistenceException("Unable to remove children from " + resource.getPath(), ex);
        }
    }
    
    private void replaceProperties(Resource resource, Map<String,Object> properties) {
        ModifiableValueMap modValueMap = resource.adaptTo(ModifiableValueMap.class);
        // remove all existing properties that do not have jcr: namespace
        for (String propertyName : new HashSet<>(modValueMap.keySet())) {
            if (StringUtils.startsWith(propertyName, "jcr:")) {
                continue;
            }
            modValueMap.remove(propertyName);
        }
        modValueMap.putAll(properties);
    }

    private void commit(ResourceResolver resourceResolver) {
        try {
            resourceResolver.commit();
        }
        catch (PersistenceException ex) {
            throw new ConfigurationPersistenceException("Unable to save configuration: " + ex.getMessage(), ex);
        }
    }

}
