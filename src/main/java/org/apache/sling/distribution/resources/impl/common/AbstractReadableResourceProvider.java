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

    public static final String INTERNAL_NAME = "internal:adaptable";

    protected static final String INTERNAL_ITEMS_ITERATOR = "internal:itemsIterator";

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

        Map<String, Object> properties = getResourceProperties(resourceResolver,  pathInfo);


        if (properties != null) {
            Object adaptable = properties.remove(INTERNAL_ADAPTABLE);
            properties.remove(INTERNAL_ITEMS_ITERATOR);

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
        Iterable<String> childrenList = getResourceChildren(resourceResolver, pathInfo);
        Iterator<Map<String,Object>> childrenProperties = null;

        if (childrenList == null) {
            Map<String, Object> properties = getResourceProperties(resourceResolver, pathInfo);

            if (properties != null && properties.containsKey(ITEMS)
                    && properties.get(ITEMS) instanceof String[]) {
                String[] itemsArray = (String[]) properties.get(ITEMS);
                childrenList = Arrays.asList(itemsArray);
            }

            if (properties != null && properties.containsKey(INTERNAL_ITEMS_ITERATOR)) {
                childrenProperties = (Iterator<Map<String,Object>>) properties.get(INTERNAL_ITEMS_ITERATOR);
            }
        }

        if (childrenProperties != null) {
            return new SimpleReadableResourceIterator(childrenProperties, resourceResolver, path);
        } else if (childrenList != null) {
            for (String childResourceName : childrenList) {
                Resource childResource = getResource(resourceResolver, path + "/" + childResourceName);
                resourceList.add(childResource);
            }
        }



        return resourceList.listIterator();
    }


    protected Map<String, Object> getResourceProperties(ResourceResolver resolver, SimplePathInfo pathInfo) {
        return getInternalResourceProperties(resolver, pathInfo);
    }

    protected Iterable<String> getResourceChildren(ResourceResolver resolver, SimplePathInfo pathInfo) {
        return getInternalResourceChildren(resolver, pathInfo);
    }




    protected abstract Map<String, Object> getInternalResourceProperties(ResourceResolver resolver, SimplePathInfo pathInfo);

    protected abstract Iterable<String> getInternalResourceChildren(ResourceResolver resolver, SimplePathInfo pathInfo);

}
