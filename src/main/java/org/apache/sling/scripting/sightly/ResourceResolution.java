/*******************************************************************************
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
 ******************************************************************************/

package org.apache.sling.scripting.sightly;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;

/**
 * Utility class which used by the Sightly engine & extensions
 * to resolve resources
 */
public final class ResourceResolution {

    /**
     * Maximum number of iterations that can be performed when searching for a resource in
     * the resource superType chain
     */
    private static final int RECURSION_LIMIT = 100;

    /**
     * Resolve the resource with the given path relative to the base resource, using the
     * resource super type logic
     * @param resourceResolver The resource resolver to be used
     * @param base the base resource. It can be null if path is absolute
     * @param path the path to the resource
     * @return the retrieved resource or null if no resource was found
     * @throws java.lang.UnsupportedOperationException if the resource is not in the resource resolver's search path
     * @throws java.lang.IllegalStateException if the number of steps necessary to search for the resource on the resource
     * superType chain has reached the maximum limit
     * @throws java.lang.IllegalArgumentException if a null componentResource is provided but the path is not absolute
     */
    public static Resource resolveComponentRelative(ResourceResolver resourceResolver, Resource base, String path) {
        Resource componentResource = null;
        if (base != null) {
            if ("nt:file".equals(base.getResourceType())) {
                componentResource = retrieveParent(resourceResolver, base);
            } else {
                componentResource = base;
            }
        }
        return resolveComponent(resourceResolver, componentResource, path);
    }

    public static Resource resolveComponentForRequest(ResourceResolver resolver, SlingHttpServletRequest request) {
        String resourceType = request.getResource().getResourceType();
        if (StringUtils.isNotEmpty(resourceType)) {
            if (resourceType.startsWith("/")) {
                return resolver.getResource(resourceType);
            }
            for (String searchPath : resolver.getSearchPath()) {
                String componentPath = ResourceUtil.normalize(searchPath + "/" + resourceType);
                Resource componentResource = resolver.getResource(componentPath);
                if (componentResource != null) {
                    return componentResource;
                }
            }
        }
        return null;
    }

    /**
     * Resolve the resource in the specified component
     * @param resourceResolver The resource resolver to be used
     * @param componentResource The resource for the component. It can be null if path is absolute
     * @param path the path to the resource
     * @return the retrieved resource or null if no resource was found
     * @throws java.lang.UnsupportedOperationException if the resource is not in the resource resolver's search path
     * @throws java.lang.IllegalStateException if more than {@link ResourceResolution#RECURSION_LIMIT} steps were
     * necessary to search for the resource on the resource superType chain
     * @throws java.lang.IllegalArgumentException if a null componentResource is provided but the path is not absolute
     */
    private static Resource resolveComponent(ResourceResolver resourceResolver, Resource componentResource, String path) {
        if (resourceResolver == null || path == null) {
            throw new NullPointerException("Arguments cannot be null");
        }
        Resource resource = null;
        if (isAbsolutePath(path)) {
            resource = resourceResolver.getResource(path);
        }
        if (resource == null) {
            if (componentResource == null) {
                throw new IllegalArgumentException("Argument cannot be null if path is not absolute: " + path);
            }
            resource = recursiveResolution(resourceResolver, componentResource, path);
        }
        if (resource == null) {
            resource = locateInSearchPath(resourceResolver, path);
        }
        return resource != null ? searchPathChecked(resourceResolver, resource) : null;
    }

    private static Resource recursiveResolution(ResourceResolver resourceResolver, Resource componentResource, String path) {
        for (int iteration = 0; iteration < RECURSION_LIMIT; iteration++) {
            Resource resource = resourceResolver.getResource(componentResource, path);
            if (resource != null) {
                return resource;
            }
            componentResource = findSuperComponent(resourceResolver, componentResource);
            if (componentResource == null) {
                return null;
            }
        }
        //at this point we have reached recursion limit
        throw new IllegalStateException("Searching for resource in component chain took more than " +
                RECURSION_LIMIT + " steps");
    }

    private static Resource locateInSearchPath(ResourceResolver resourceResolver, String path) {
        for (String searchPath : resourceResolver.getSearchPath()) {
            String fullPath = searchPath + path;
            Resource resource = resourceResolver.getResource(fullPath);
            if (resource != null && resource.getPath().startsWith(searchPath)) { //prevent path traversal attack
                return resource;
            }
        }
        return null;
    }

    private static boolean isInSearchPath(ResourceResolver resourceResolver, Resource resource) {
        String resourcePath = resource.getPath();
        for (String path : resourceResolver.getSearchPath()) {
            if (resourcePath.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    private static Resource findSuperComponent(ResourceResolver resourceResolver, Resource base) {
        String superType = resourceResolver.getParentResourceType(base);
        if (superType == null) {
            return null;
        }
        return resourceResolver.getResource(superType);
    }

    private static Resource searchPathChecked(ResourceResolver resourceResolver, Resource resource) {
        if (!isInSearchPath(resourceResolver, resource)) {
            throw new UnsupportedOperationException("Access to resource " + resource.getPath() + " is denied, since the resource does not reside on the search path");
        }
        return resource;
    }

    private static Resource retrieveParent(ResourceResolver resourceResolver, Resource resource) {
        String parentPath = ResourceUtil.getParent(resource.getPath());
        if (parentPath == null) {
            return null;
        }
        return resourceResolver.getResource(parentPath);
    }

    private static boolean isAbsolutePath(String path) {
        return path.startsWith("/");
    }

}
