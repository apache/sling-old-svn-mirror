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
package org.apache.sling.distribution.resources.impl;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.distribution.component.impl.DistributionComponentKind;
import org.apache.sling.distribution.component.impl.DistributionConfiguration;
import org.apache.sling.distribution.component.impl.DistributionConfigurationManager;
import org.apache.sling.distribution.resources.DistributionResourceTypes;
import org.apache.sling.distribution.resources.impl.common.AbstractModifyingResourceProvider;
import org.apache.sling.distribution.resources.impl.common.SimplePathInfo;

/**
 * {@link ResourceProvider} for Osgi configurations for a specific configuration factory.
 * All CRUD operations are supported for the underlying resources.
 * The accepted path is resourceRoot/{friendlyNameProperty}/childResourceName.
 */
public class DistributionConfigurationResourceProvider extends AbstractModifyingResourceProvider implements ResourceProvider, ModifyingResourceProvider {

    private static final String SETTINGS_RESOURCE_TYPE = DistributionResourceTypes.DEFAULT_SETTING_RESOURCE_TYPE;

    private final DistributionConfigurationManager configurationManager;

    private final DistributionComponentKind kind;

    public DistributionConfigurationResourceProvider(DistributionConfigurationManager configurationManager, String kind, String resourceRoot) {
        super(resourceRoot);
        this.configurationManager = configurationManager;
        this.kind = DistributionComponentKind.fromName(kind);
    }

    @Override
    protected void saveInternalResources(ResourceResolver resourceResolver, Map<String, Map<String, Object>> changedResources,
                                         Set<String> deletedResources) throws PersistenceException {
        for (Map.Entry<String, Map<String, Object>> entry : changedResources.entrySet()) {
            String resourceName = entry.getKey();
            Map<String, Object> properties = entry.getValue();

            String componentName = getConfigName(resourceName);

            DistributionConfiguration config = new DistributionConfiguration(kind, componentName, properties);
            configurationManager.saveConfig(resourceResolver, config);
        }

        for (String resourceName : deletedResources) {
            String componentName = getConfigName(resourceName);

            configurationManager.deleteConfig(resourceResolver, kind, componentName);
        }
    }

    @Override
    protected Map<String, Object> getInternalResourceProperties(ResourceResolver resolver, SimplePathInfo pathInfo) {
        if (pathInfo.isRoot()) {
            return getResourceRootProperties(resolver);
        } else if (pathInfo.isMain()) {
            return getResourceProperties(resolver, pathInfo.getMainResourceName());
        } else if (pathInfo.isChild()) {
            String configName = getConfigName(pathInfo.getMainResourceName());

            DistributionConfiguration config = configurationManager.getConfig(resolver, kind, configName);

            if (config != null) {
                return getChildResourceProperties(config, pathInfo.getChildResourceName());
            }
        }

        return null;
    }

    @Override
    protected Iterable<String> getInternalResourceChildren(ResourceResolver resolver, SimplePathInfo pathInfo) {
        if (pathInfo.isMain()) {
            String configName = getConfigName(pathInfo.getMainResourceName());

            DistributionConfiguration config = configurationManager.getConfig(resolver, kind, configName);

            if (config != null) {
                return getChildResourceChildren(config, pathInfo.getChildResourceName());
            }
        }

        return null;
    }

    protected Map<String, Object> getChildResourceProperties(DistributionConfiguration config, String childResourceName) {
        return null;
    }

    protected Iterable<String> getChildResourceChildren(DistributionConfiguration config, String childResourceName) {
        return null;
    }

    private Map<String, Object> getResourceRootProperties(ResourceResolver resolver) {
        List<DistributionConfiguration> configsList = configurationManager.getConfigs(resolver, kind);

        List<String> nameList = new ArrayList<String>();
        for (DistributionConfiguration config : configsList) {
            nameList.add(config.getName());
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(ITEMS, nameList.toArray(new String[nameList.size()]));

        String resourceType = getRootResourceType(kind);
        result.put(SLING_RESOURCE_TYPE, resourceType);

        return result;
    }


    private Map<String, Object> getResourceProperties(ResourceResolver resolver, String resourceName) {

        String componentName = getConfigName(resourceName);

        DistributionConfiguration config = configurationManager.getConfig(resolver, kind, componentName);

        if (config != null) {

            Map<String, Object> result = new HashMap<String, Object>();

            result.putAll(config.getProperties());
            String resourceType = getResourceType(kind);
            result.put(SLING_RESOURCE_TYPE, resourceType);

            return result;
        }

        return null;
    }


    private String getConfigName(String configName) {
        return configName;
    }

    private String getResourceType(DistributionComponentKind kind) {
        return SETTINGS_RESOURCE_TYPE;
    }

    private String getRootResourceType(DistributionComponentKind kind) {
        return SETTINGS_RESOURCE_TYPE;
    }


}
