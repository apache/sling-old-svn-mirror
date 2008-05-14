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
package org.apache.sling.servlets.resolver.helper;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.servlets.resolver.ServletResolverConstants;

/**
 * The <code>LocationIterator</code> provides access to an ordered collection
 * of absolute paths containing potential request handling. The primary order of
 * the collection is the resource type hierarchy with the base resource type at
 * the top. The secondary order is the search path retrieved from the resource
 * resolver.
 * <p>
 * Example: For a node type hierarchy "sample" > "super" > "default" and a
 * search path of [ "/apps", "/libs" ], the iterator would provide access to the
 * following list of paths:
 * <ol>
 * <li><code>/apps/sample</code></li>
 * <li><code>/libs/sample</code></li>
 * <li><code>/apps/super</code></li>
 * <li><code>/libs/super</code></li>
 * <li><code>/apps/default</code></li>
 * <li><code>/libs/default</code></li>
 * </ol>
 */
public class LocationIterator implements Iterator<String> {

    // The resource for which this iterator is created. This resource
    // gives the initial resource type and the first resource super type
    private final Resource resource;

    // The resource resolver used to find resource super types of
    // resource types
    private final ResourceResolver resolver;

    // The base resource type to be used as a final entry if there is
    // no more resource super type. This is kind of the java.lang.Object
    // the resource type hierarchy.
    private final String baseResourceType;

    // The search path of the resource resolver
    private final String[] searchPath;

    // counter into the search path array
    private int pathCounter;

    // The resource type to use for the next iteration loop
    private String resourceType;

    // The current relative path generated from the resource type
    private String relPath;

    // the next absolute path to return from next(). This is null
    // if there is no more location to return
    private String nextLocation;

    /**
     * Creates an instance of this iterator starting with a location built from
     * the resource type of the <code>resource</code> and ending with the
     * given <code>baseResourceType</code>.
     * 
     * @param resource The <code>Resource</code> used to define the initial
     *            resource type and resource super type.
     * @param baseResourceType The base resource type. If this is
     *            <code>null</code> the
     *            {@link ServletResolverConstants#DEFAULT_SERVLET_NAME} is used.
     */
    public LocationIterator(Resource resource, String baseResourceType) {
        this.resource = resource;
        this.resolver = resource.getResourceResolver();
        this.baseResourceType = (baseResourceType != null)
                ? baseResourceType
                : ServletResolverConstants.DEFAULT_SERVLET_NAME;

        String[] tmpPath = resolver.getSearchPath();
        if (tmpPath == null || tmpPath.length == 0) {
            tmpPath = new String[] { "/" };
        }
        searchPath = tmpPath;

        resourceType = resource.getResourceType();

        nextLocation = seek();
    }

    /**
     * Returns <code>true</code> if there is another entry
     */
    public boolean hasNext() {
        return nextLocation != null;
    }

    /**
     * Returns the next entry of this iterator.
     * 
     * @throws NoSuchElementException if {@link #hasNext()} returns
     *             <code>false</code>.
     */
    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        String result = nextLocation;
        nextLocation = seek();
        return result;
    }

    /**
     * @throws UnsupportedOperationException
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private String seek() {

        if (relPath == null) {

            if (resourceType == null) {
                return null;
            }

            String typePath = JcrResourceUtil.resourceTypeToPath(resourceType);
            if (typePath.startsWith("/")) {
                resourceType = getResourceSuperType(resourceType);
                return typePath;
            }

            relPath = typePath;
        }

        String result = searchPath[pathCounter].concat(relPath);

        pathCounter++;
        if (pathCounter >= searchPath.length) {
            relPath = null;
            resourceType = getResourceSuperType(resourceType);
            pathCounter = 0;
        }

        return result;
    }

    /**
     * Returns the resource super type of the given resource type:
     * <ol>
     * <li>If the resource type is the base resource type <code>null</code>
     * is returned.</li>
     * <li>If the resource type is the resource type of the resource the
     * resource super type from the resource is returned.</li>
     * <li>Otherwise the resource super type is tried to be found in the
     * resource tree. If one is found, it is returned.</li>
     * <li>Otherwise the base resource type is returned.</li>
     * </ol>
     */
    private String getResourceSuperType(String resourceType) {

        // if the current resource type is the default value, there are no more
        if (resourceType.equals(baseResourceType)) {
            return null;
        }

        // get the super type of the current resource type
        String superType;
        if (resourceType.equals(resource.getResourceType())) {
            superType = resource.getResourceSuperType();
        } else {
            superType = JcrResourceUtil.getResourceSuperType(resolver,
                resourceType);
        }

        // use default resource type if there is no super type any more
        if (superType == null) {
            superType = baseResourceType;
        }

        return superType;
    }

}
