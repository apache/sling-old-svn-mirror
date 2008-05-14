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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.servlets.resolver.resource.ServletResourceProvider;

/**
 * The <code>ResourceCollector</code> class provides a single public method -
 * {@link #getServlets(Resource)} - which is used to find an ordered collection
 * of <code>Resource</code> instances which may be used to find a servlet or
 * script to handle a request to the given resource.
 */
public class ResourceCollector {

    // the request method name used to indicate the script name
    private final String methodName;

    // the most generic resource type to use. This may be null in which
    // case the default servlet name will be used as the base name
    private final String baseResourceType;

    /**
     * Creates a <code>ResourceCollector</code> for the given
     * <code>request</code>. If the request is a GET or HEAD request, a
     * specialized instance is returned which also takes the request selectors
     * and request extension into account for finding servlet resources.
     * Otherwise an instance of this class itself is returned which just takes
     * the resource type and request method name into account.
     * 
     * @param request The <code>SlingHttpServletRequest</code> for which to
     *            return a <code>ResourceCollector</code>.
     * @return The <code>ResourceCollector</code> to find servlets and scripts
     *         suitable for handling the <code>request</code>.
     */
    public static ResourceCollector create(SlingHttpServletRequest request) {
        if (HttpConstants.METHOD_GET.equals(request.getMethod())
            || HttpConstants.METHOD_HEAD.equals(request.getMethod())) {
            return new ResourceCollectorGet(request);
        }

        return new ResourceCollector(request.getMethod(), null);
    }

    /**
     * Creates a <code>ResourceCollector</code> finding servlets and scripts
     * for the given <code>methodName</code>.
     * 
     * @param methodName The <code>methodName</code> used to find scripts for.
     *            This must not be <code>null</code>.
     * @param baseResourceType The basic resource type to use as a final
     *            resource super type. If this is <code>null</code> the
     *            default value
     *            {@link org.apache.sling.servlets.resolver.ServletResolverConstants#DEFAULT_SERVLET_NAME}
     *            is assumed.
     */
    public ResourceCollector(String methodName, String baseResourceType) {
        this.methodName = methodName;
        this.baseResourceType = baseResourceType;
    }

    public final Collection<Resource> getServlets(Resource resource) {

        SortedSet<Resource> resources = new TreeSet<Resource>();

        ResourceResolver resolver = resource.getResourceResolver();
        Iterator<String> locations = new LocationIterator(resource,
            getBaseResourceType());
        while (locations.hasNext()) {
            String location = locations.next();

            // get the location resource, use a synthetic resource if there
            // is no real location. There may still be children at this
            // location
            Resource locationRes = getResource(resolver, null, location);
            getWeightedResources(resources, locationRes);
        }

        return resources;
    }

    /**
     * Returns all resources inside the <code>location</code> whose base name
     * equals the {@link #getMethodName()} and which just have a single
     * extension after the base name. In addition, the special resource at
     * <code>location.getPath + ".servlet"</code> is checked to find servlets
     * which are registered with no specific method name.
     * 
     * @param resources The collection into which any resources found are added.
     * @param location The location in the resource tree where the servlets and
     *            scripts are to be found.
     */
    protected void getWeightedResources(Set<Resource> resources,
            Resource location) {

        // now list the children and check them
        Iterator<Resource> children = location.getResourceResolver().listChildren(
            location);
        while (children.hasNext()) {
            Resource child = children.next();

            String name = ResourceUtil.getName(child.getPath());
            String[] parts = name.split("\\.");

            // require method name plus script extension
            if (parts.length == 2 && getMethodName().equals(parts[0])) {
                addWeightedResource(resources, child, 0,
                    WeightedResource.WEIGHT_NONE);
            }
        }

        // special treatment for servlets registered with neither a method
        // name nor extensions and selectors
        String path = location.getPath()
            + ServletResourceProvider.SERVLET_PATH_EXTENSION;
        location = location.getResourceResolver().getResource(path);
        if (location != null) {
            addWeightedResource(resources, location, 0,
                WeightedResource.WEIGHT_LAST_RESSORT);
        }
    }

    /**
     * Returns the basic resource type assigned to this instance
     */
    public final String getBaseResourceType() {
        return baseResourceType;
    }

    /**
     * Returns the method name assigned to this instance
     */
    public final String getMethodName() {
        return methodName;
    }

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
    protected final void addWeightedResource(Set<Resource> resources,
            Resource resource, int numSelectors, int methodPrefixWeight) {
        WeightedResource lr = new WeightedResource(resources.size(), resource,
            numSelectors, methodPrefixWeight);
        resources.add(lr);
    }

    /**
     * Returns a resource for the given <code>path</code>. If the
     * <code>path</code> is relative, the resource is accessed relative to the
     * <code>base</code> resource. If no resource exists at the given path -
     * absolute or relative to the base resource - a
     * <code>SyntheticResource</code> is returned.
     * 
     * @param resolver The <code>ResourceResolver</code> used to access the
     *            resource.
     * @param base The (optional) base resource. This may be <code>null</code>
     *            if the <code>path</code> is absolute.
     * @param path The absolute or relative (to the base resource) path of the
     *            resource to return.
     * @return The actual resource at the given <code>path</code> or a
     *         synthetic resource representing the path location.
     */
    protected final Resource getResource(ResourceResolver resolver,
            Resource base, String path) {
        Resource res;
        if (base == null) {
            res = resolver.getResource(path);
        } else {
            res = resolver.getResource(base, path);
        }

        if (res == null) {
            if (!path.startsWith("/")) {
                if (base == null) {
                    path = "/".concat(path);
                } else {
                    path = base.getPath() + "/" + path;
                }
            }

            res = new SyntheticResource(resolver, path, "$synthetic$");
        }

        return res;
    }
}
