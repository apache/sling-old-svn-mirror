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
package org.apache.sling.servlets.resolver.internal.helper;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.servlets.resolver.internal.SlingServletResolver;

/**
 * The <code>ResourceCollector</code> class provides a single public method -
 * {@link #getServlets(ResourceResolver)} - which is used to find an ordered collection
 * of <code>Resource</code> instances which may be used to find a servlet or
 * script to handle a request to the given resource.
 */
public abstract class AbstractResourceCollector {

    // the most generic resource type to use. This may be null in which
    // case the default servlet name will be used as the base name
    protected final String baseResourceType;

    // the request extension or null if the request has no extension
    protected final String extension;

    protected int hashCode;

    protected final String resourceType;

    protected final String resourceSuperType;

    protected final String[] executionPaths;

    protected final String workspaceName;


    public AbstractResourceCollector(final String baseResourceType,
            final String resourceType,
            final String resourceSuperType,
            final String workspaceName,
            final String extension,
            final String[] executionPaths) {
        this.baseResourceType = baseResourceType;
        this.resourceType = resourceType;
        this.resourceSuperType = resourceSuperType;
        this.extension = extension;
        this.executionPaths = executionPaths;
        this.workspaceName = workspaceName;
    }

    public final Collection<Resource> getServlets(ResourceResolver resolver) {

        final SortedSet<Resource> resources = new TreeSet<Resource>();
        final Iterator<String> locations = new LocationIterator(resourceType, resourceSuperType,
                                                                baseResourceType, workspaceName, resolver);
        while (locations.hasNext()) {
            final String location = locations.next();

            // get the location resource, use a synthetic resource if there
            // is no real location. There may still be children at this
            // location
            final String path;
            if ( location.endsWith("/") ) {
                path = location.substring(0, location.length() - 1);
            } else {
                path = location;
            }
            final Resource locationRes = getResource(resolver, path);
            getWeightedResources(resources, locationRes);
        }

        return resources;
    }

    abstract protected void getWeightedResources(final Set<Resource> resources,
                                                 final Resource location);

    /**
     * Creates a {@link WeightedResource} and adds it to the set of resources.
     * The number of resources already present in the set is used as the ordinal
     * number for the newly created resource.
     *
     * @param resources The set of resource to which the
     *            {@link WeightedResource} is added.
     * @param resource The <code>Resource</code> on which the
     *            {@link WeightedResource} is based.
     * @param numSelectors The number of request selectors which are matched by
     *            the name of the resource.
     * @param methodPrefixWeight The method/prefix weight assigned to the
     *            resource according to the resource name.
     */
    protected final void addWeightedResource(final Set<Resource> resources,
            final Resource resource,
            final int numSelectors,
            final int methodPrefixWeight) {
        final WeightedResource lr = new WeightedResource(resources.size(), resource,
            numSelectors, methodPrefixWeight);
        resources.add(lr);
    }

    /**
     * Returns a resource for the given <code>path</code>.
     * If no resource exists at the given path a
     * <code>SyntheticResource</code> is returned.
     *
     * @param resolver The <code>ResourceResolver</code> used to access the
     *            resource.
     * @param path The absolute path of the resource to return.
     * @return The actual resource at the given <code>path</code> or a
     *         synthetic resource representing the path location.
     */
    protected final Resource getResource(final ResourceResolver resolver,
                                         String path) {
        if ( this.workspaceName != null ) {
            path = workspaceName + ':' + path;
        }
        Resource res = resolver.getResource(path);

        if (res == null) {
            if (!path.startsWith("/")) {
                path = "/".concat(path);
            }

            res = new SyntheticResource(resolver, path, "$synthetic$");
        }

        return res;
    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof AbstractResourceCollector) ) {
            return false;
        }
        if ( obj == this ) {
            return true;
        }
        final AbstractResourceCollector o = (AbstractResourceCollector)obj;
        if ( stringEquals(resourceType, o.resourceType)
             && stringEquals(resourceSuperType, o.resourceSuperType)
             && stringEquals(extension, o.extension)
             && stringEquals(baseResourceType, o.baseResourceType)
             && stringEquals(workspaceName, o.workspaceName)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    /**
     * Helper method to compare two strings which can possibly be <code>null</code>
     */
    protected boolean stringEquals(final String s1, final String s2) {
        if ( s1 == null && s2 == null ) {
            return true;
        }
        if ( s1 == null || s2 == null ) {
            return false;
        }
        return s1.equals(s2);
    }

    protected boolean isPathAllowed(final String path) {
        return isPathAllowed(path, this.executionPaths);
    }

    /**
     * This method checks whether a path is allowed to be executed.
     *
     * @param path The path to check (must not be {@code null} or empty)
     * @param executionPaths The path to check against
     * @return {@code true} if the executionPaths is {@code null} or empty or if
     *         the path equals one entry or one of the executionPaths entries is
     *         a prefix to the path. Otherwise or if path is {@code null}
     *         {@code false} is returned.
     */
    public static boolean isPathAllowed(final String path, final String[] executionPaths) {
        if (executionPaths == null || executionPaths.length == 0) {
            SlingServletResolver.LOGGER.debug("Accepting servlet at '{}' as there are no configured execution paths.",
                path);
            return true;
        }

        if (path == null || path.length() == 0) {
            SlingServletResolver.LOGGER.debug("Ignoring servlet with empty path.");
            return false;
        }

        for (final String config : executionPaths) {
            if (config.endsWith("/")) {
                if (path.startsWith(config)) {
                    SlingServletResolver.LOGGER.debug(
                        "Accepting servlet at '{}' as the path is prefixed with configured execution path '{}'.", path,
                        config);
                    return true;
                }
            } else if (path.equals(config)) {
                SlingServletResolver.LOGGER.debug(
                    "Accepting servlet at '{}' as the path equals configured execution path '{}'.", path, config);
                return true;
            }
        }

        if (SlingServletResolver.LOGGER.isDebugEnabled()) {
            SlingServletResolver.LOGGER.debug(
                "Ignoring servlet at '{}' as the path is not in the configured execution paths.", path);
        }

        return false;
    }

}
