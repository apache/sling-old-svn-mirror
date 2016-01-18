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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * a readable {@link ResourceProvider} for distribution.
 */
public abstract class AbstractReadableResourceProvider implements ResourceProvider {

    protected static final String INTERNAL_ADAPTABLE = "internal:adaptable";

    protected static final String INTERNAL_ITEMS_PROPERTIES = "internal:propertiesMap";

    protected static final String ITEMS = "items";


    protected static final String SLING_RESOURCE_TYPE = "sling:resourceType";

    final String resourceRoot;

    protected AbstractReadableResourceProvider(String resourceRoot) {

        this.resourceRoot = resourceRoot;
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

        Map<String, Object> properties = getResourceProperties(pathInfo);

        if (properties != null) {
            Object adaptable = properties.remove(INTERNAL_ADAPTABLE);
            properties.remove(INTERNAL_ITEMS_PROPERTIES);

            resource = buildMainResource(resourceResolver, pathInfo, properties, adaptable);
        }

        return resource;
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


    public Iterator<Resource> listChildren(Resource parent) {
        String path = parent.getPath();
        ResourceResolver resourceResolver = parent.getResourceResolver();

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

        List<Resource> resourceList = new ArrayList<Resource>();
        Iterable<String> childrenList = getResourceChildren(pathInfo);
        Map<String, Map<String, Object>> childrenProperties = new HashMap<String, Map<String, Object>>();

        if (childrenList == null) {
            Map<String, Object> properties = getResourceProperties(pathInfo);

            if (properties != null && properties.containsKey(ITEMS)
                    && properties.get(ITEMS) instanceof String[]) {
                String[] itemsArray = (String[]) properties.get(ITEMS);
                childrenList = Arrays.asList(itemsArray);
            }

            if (properties != null && properties.containsKey(INTERNAL_ITEMS_PROPERTIES)) {
                childrenProperties = (Map) properties.get(INTERNAL_ITEMS_PROPERTIES);
            }
        }

        if (childrenList != null) {
            for (String childResourceName : childrenList) {

                Resource childResource;
                if (childrenProperties.containsKey(childResourceName)) {
                    Map<String, Object> childProperties = childrenProperties.get(childResourceName);
                    SimplePathInfo childPathInfo = extractPathInfo(path + "/" + childResourceName);
                    childResource = buildMainResource(resourceResolver, childPathInfo, childProperties);

                } else {
                    childResource = getResource(resourceResolver, path + "/" + childResourceName);

                }
                resourceList.add(childResource);
            }
        }

        return resourceList.listIterator();
    }


    protected abstract Map<String, Object> getResourceProperties(SimplePathInfo pathInfo);

    protected abstract Iterable<String> getResourceChildren(SimplePathInfo pathInfo);

}
