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

package org.apache.sling.distribution.resources.impl.common;


import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;

public abstract class AbstractReadableResourceProvider implements ResourceProvider {

    protected static final String ADAPTABLE_PROPERTY_NAME = "adaptable";

    private static final String MAIN_RESOURCE_PREFIX = ".";

    private final String resourceRoot;
    private final Map<String, Map<String, String>> additionalResourcePropertiesMap = new HashMap<String, Map<String, String>>();

    protected AbstractReadableResourceProvider(String resourceRoot,
                                               Map<String, String> additionalResourceProperties) {

        this.resourceRoot = resourceRoot;

        additionalResourcePropertiesMap.put(MAIN_RESOURCE_PREFIX, new HashMap<String, String>());
        for (Map.Entry<String, String> entry : additionalResourceProperties.entrySet()) {
            String resourceName = MAIN_RESOURCE_PREFIX;
            String propertyName = entry.getKey();
            int idx = propertyName.indexOf("/");
            if (idx >= 0) {
                resourceName = propertyName.substring(0, idx);
                propertyName = propertyName.substring(idx + 1);
            }

            if (!additionalResourcePropertiesMap.containsKey(resourceName)) {
                additionalResourcePropertiesMap.put(resourceName, new HashMap<String, String>());
            }

            additionalResourcePropertiesMap.get(resourceName).put(propertyName, entry.getValue());
        }
    }

    public Resource getResource(ResourceResolver resourceResolver, HttpServletRequest request, String path) {
        return getResource(resourceResolver, path);
    }

    public Resource getResource(ResourceResolver resourceResolver, String path) {
        SimplePathInfo pathInfo = extractPathInfo(path);

        if (pathInfo == null) {
            return null;
        }

        if (pathInfo.getResourcePathInfo() != null && pathInfo.getResourcePathInfo().length() > 0) {
            return null;
        }

        if (!hasPermission(resourceResolver, pathInfo.getResourcePath(), Session.ACTION_READ)) {
            return null;
        }

        Resource resource = null;

        if (pathInfo.isRoot()) {

            Map<String, Object> properties = getResourceRootProperties();

            if (properties != null) {
                Object adaptable = properties.remove(ADAPTABLE_PROPERTY_NAME);

                Map<String, String> additionalProperties = additionalResourcePropertiesMap.get(MAIN_RESOURCE_PREFIX);
                if (!properties.containsKey("sling:resourceType") && additionalProperties.containsKey("sling:resourceType")) {
                    properties.put("sling:resourceType", additionalProperties.get("sling:resourceType") + "/list");
                }
                if (!properties.containsKey("sling:resourceSuperType") && additionalProperties.containsKey("sling:resourceSuperType")) {
                    properties.put("sling:resourceSuperType", additionalProperties.get("sling:resourceSuperType") + "/list");
                }

                resource = new SimpleReadableResource(resourceResolver, pathInfo.getResourcePath(), properties, adaptable);
            }
        } else if (pathInfo.isMain()) {
            Map<String, Object> properties = getMainResourceProperties(pathInfo.getMainResourceName());

            Map<String, String> additionalProperties = additionalResourcePropertiesMap.get(MAIN_RESOURCE_PREFIX);
            if (!properties.containsKey("sling:resourceType") && additionalProperties.containsKey("sling:resourceType")) {
                properties.put("sling:resourceType", additionalProperties.get("sling:resourceType"));
            }

            if (properties != null) {
                Object adaptable = properties.remove(ADAPTABLE_PROPERTY_NAME);

                resource = buildMainResource(resourceResolver, pathInfo, properties, adaptable);
            }
        } else if (pathInfo.isChild()) {
            Map<String, Object> mainProperties = getMainResourceProperties(pathInfo.getMainResourceName());
            Map<String, String> childProperties = additionalResourcePropertiesMap.get(pathInfo.getChildResourceName());

            if (mainProperties != null && childProperties != null) {
                Object adaptable = mainProperties.remove(ADAPTABLE_PROPERTY_NAME);

                Map<String, Object> properties = new HashMap<String, Object>();
                properties.putAll(childProperties);
                resource = new SimpleReadableResource(resourceResolver, pathInfo.getResourcePath(), properties, adaptable);
            }
        }

        return resource;
    }

    Map<String, Object> getMainResourceProperties(String resourceName) {
        return getResourceProperties(resourceName);
    }

    Resource buildMainResource(ResourceResolver resourceResolver,
                               SimplePathInfo pathInfo,
                               Map<String, Object> properties,
                               Object... adapters) {
        return new SimpleReadableResource(resourceResolver, pathInfo.getResourcePath(), properties, adapters);
    }

    SimplePathInfo extractPathInfo(String path) {
        return SimplePathInfo.parsePathInfo(resourceRoot, path);
    }

    boolean hasPermission(ResourceResolver resourceResolver, String resourcePath, String permission) {

        boolean hasPermission = false;
        Session session = resourceResolver.adaptTo(Session.class);

        if (session != null) {
            try {
                hasPermission = session.hasPermission(resourcePath, permission);
            } catch (RepositoryException e) {
                hasPermission = false;
            }
        }

        return hasPermission;
    }


    protected abstract Map<String, Object> getResourceProperties(String resourceName);

    protected abstract Map<String, Object> getResourceRootProperties();
}
