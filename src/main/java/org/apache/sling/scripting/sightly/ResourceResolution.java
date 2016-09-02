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
 * Utility class which used by the HTL engine &amp; extensions to resolve resources.
 */
public final class ResourceResolution {

    /**
     * Maximum number of iterations that can be performed when searching for a resource in
     * the resource superType chain
     */
    private static final int RECURSION_LIMIT = 100;

    /**
     * <p>
     * Resolves a resource from the search path relative to the {@code base} resource by traversing the {@code sling:resourceSuperType}
     * chain.
     * </p>
     * <p>
     * Since this method will traverse the {@code sling:resourceSuperType} chain, the {@code ResourceResolver} used for resolving the
     * {@code base} resource should be able to read the super type resources.
     * </p>
     *
     * @param base the base resource from which to start the lookup
     * @param path the relative path to the resource; if the path is absolute the {@code Resource} identified by this path will be
     *             returned
     * @return the resource identified by the relative {@code path} or {@code null} if no resource was found
     * @throws java.lang.UnsupportedOperationException if the resource is not in the resource resolver's search path
     * @throws java.lang.IllegalStateException         if the number of steps necessary to search for the resource on the resource
     *                                                 superType chain has reached the maximum limit
     * @see ResourceResolver#getSearchPath()
     */
    public static Resource getResourceFromSearchPath(Resource base, String path) {
        if (path.startsWith("/")) {
            Resource resource = base.getResourceResolver().getResource(path);
            if (resource != null) {
                return searchPathChecked(resource);
            }
            return null;
        }
        Resource internalBase = null;
        if (base != null) {
            if ("nt:file".equals(base.getResourceType())) {
                internalBase = retrieveParent(base);
            } else {
                internalBase = base;
            }
        }
        return resolveComponentInternal(internalBase, path);
    }

    /**
     * <p>
     * Resolves the resource accessed by a {@code request}. Since the {@code request} can use an anonymous {@code ResourceResolver}, the
     * passed {@code resolver} parameter should have read access rights to resources from the search path.
     * </p>
     *
     * @param resolver a {@link ResourceResolver} that has read access rights to resources from the search path
     * @param request  the request
     * @return the resource identified by the {@code request} or {@code null} if no resource was found
     */
    public static Resource getResourceForRequest(ResourceResolver resolver, SlingHttpServletRequest request) {
        String resourceType = request.getResource().getResourceType();
        if (StringUtils.isNotEmpty(resourceType)) {
            if (resourceType.startsWith("/")) {
                return resolver.getResource(resourceType);
            }
            for (String searchPath : resolver.getSearchPath()) {
                String componentPath = ResourceUtil.normalize(searchPath + "/" + resourceType);
                if (componentPath != null) {
                    Resource componentResource = resolver.getResource(componentPath);
                    if (componentResource != null) {
                        return componentResource;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Resolve the resource in the specified component
     *
     * @param base The resource for the component. It can be null if path is absolute
     * @param path the path to the resource
     * @return the retrieved resource or null if no resource was found
     * @throws java.lang.UnsupportedOperationException if the resource is not in the resource resolver's search path
     * @throws java.lang.IllegalStateException         if more than {@link ResourceResolution#RECURSION_LIMIT} steps were
     *                                                 necessary to search for the resource on the resource superType chain
     */
    private static Resource resolveComponentInternal(Resource base, String path) {
        if (base == null || path == null) {
            throw new NullPointerException("Arguments cannot be null");
        }
        Resource resource = recursiveResolution(base, path);
        if (resource == null) {
            resource = locateInSearchPath(base.getResourceResolver(), path);
        }
        return resource != null ? searchPathChecked(resource) : null;
    }

    private static Resource recursiveResolution(Resource base, String path) {
        ResourceResolver resolver = base.getResourceResolver();
        for (int iteration = 0; iteration < RECURSION_LIMIT; iteration++) {
            Resource resource = resolver.getResource(base, path);
            if (resource != null) {
                return resource;
            }
            base = findSuperComponent(base);
            if (base == null) {
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

    private static boolean isInSearchPath(Resource resource) {
        String resourcePath = resource.getPath();
        ResourceResolver resolver = resource.getResourceResolver();
        for (String path : resolver.getSearchPath()) {
            if (resourcePath.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    private static Resource findSuperComponent(Resource base) {
        ResourceResolver resolver = base.getResourceResolver();
        String superType = resolver.getParentResourceType(base);
        if (superType == null) {
            return null;
        }
        return resolver.getResource(superType);
    }

    private static Resource searchPathChecked(Resource resource) {
        if (!isInSearchPath(resource)) {
            throw new UnsupportedOperationException("Access to resource " + resource.getPath() + " is denied, since the resource does not" +
                    " reside on the search path");
        }
        return resource;
    }

    private static Resource retrieveParent(Resource resource) {
        String parentPath = ResourceUtil.getParent(resource.getPath());
        if (parentPath == null) {
            return null;
        }
        return resource.getResourceResolver().getResource(parentPath);
    }
}
