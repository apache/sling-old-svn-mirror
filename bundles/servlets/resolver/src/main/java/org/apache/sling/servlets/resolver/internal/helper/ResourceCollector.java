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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.servlets.resolver.internal.ServletResolverConstants;
import org.apache.sling.servlets.resolver.internal.WorkspaceResourceResolver;
import org.apache.sling.servlets.resolver.internal.resource.ServletResourceProviderFactory;

/**
 * The <code>ResourceCollector</code> class provides a single public method -
 * {@link #getServlets(ResourceResolver)} - which is used to find an ordered collection
 * of <code>Resource</code> instances which may be used to find a servlet or
 * script to handle a request to the given resource.
 */
public class ResourceCollector {

    /**
     * The special value returned by
     * {@link #calculatePrefixMethodWeight(Resource, String, boolean)} if the
     * resource is not suitable to handle the request according to the location
     * prefix, request selectors and request extension (value is
     * <code>Integer.MIN_VALUE</code>).
     */
    protected static final int WEIGHT_NO_MATCH = Integer.MIN_VALUE;

    // the request method name used to indicate the script name
    private final String methodName;

    // the most generic resource type to use. This may be null in which
    // case the default servlet name will be used as the base name
    private final String baseResourceType;

    // the request selectors as a string converted to a realtive path or
    // null if the request has no selectors
    private final String[] requestSelectors;

    // the number of request selectors of the request or 0 if none
    private final int numRequestSelectors;

    // the request extension or null if the request has no extension
    private final String extension;

    // request is GET or HEAD
    private final boolean isGet;

    // request is GET or HEAD and extension is html
    private final boolean isHtml;

    private final int hashCode;

    private final String resourceType;

    private final String resourceSuperType;

    private final String workspaceName;

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
     * @param scriptResolver
     * @return The <code>ResourceCollector</code> to find servlets and scripts
     *         suitable for handling the <code>request</code>.
     */
    public static ResourceCollector create(SlingHttpServletRequest request, String workspaceName) {
        return new ResourceCollector(request, workspaceName);
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
     *            {@link org.apache.sling.servlets.resolver.internal.ServletResolverConstants#DEFAULT_SERVLET_NAME}
     *            is assumed.
     * @param resource the resource to invoke, the resource type and resource super type are taken from this resource.
     */
    public ResourceCollector(String methodName, String baseResourceType, Resource resource, String workspaceName) {
        this.methodName = methodName;
        this.baseResourceType = (baseResourceType != null ? baseResourceType : ServletResolverConstants.DEFAULT_SERVLET_NAME);
        this.requestSelectors = new String[0];
        this.numRequestSelectors = 0;
        this.extension = null;
        this.isGet = false;
        this.isHtml = false;
        this.resourceType = resource.getResourceType();
        this.resourceSuperType = resource.getResourceSuperType();

        this.workspaceName = workspaceName;
        // create the hash code once
        final String key = methodName + ':' + baseResourceType + ':' + extension + "::" +
            (this.resourceType == null ? "" : this.resourceType)+ ':' + (this.resourceSuperType == null ? "" : this.resourceSuperType) +
            ':' + (this.workspaceName == null ? "" : this.workspaceName);
        this.hashCode = key.hashCode();
    }

    /**
     * Creates a <code>ResourceCollector</code> finding servlets and scripts
     * for the given <code>methodName</code>.
     *
     * @param methodName The <code>methodName</code> used to find scripts for.
     *            This must not be <code>null</code>.
     * @param workspaceName The <code>workspaceName</code>.
     * @param baseResourceType The basic resource type to use as a final
     *            resource super type. If this is <code>null</code> the
     *            default value
     *            {@link org.apache.sling.servlets.resolver.internal.ServletResolverConstants#DEFAULT_SERVLET_NAME}
     *            is assumed.
     */
    private ResourceCollector(SlingHttpServletRequest request, String workspaceName) {
        this.methodName = request.getMethod();
        this.baseResourceType = ServletResolverConstants.DEFAULT_SERVLET_NAME;
        this.resourceType = request.getResource().getResourceType();
        this.resourceSuperType = request.getResource().getResourceSuperType();

        RequestPathInfo requestpaInfo = request.getRequestPathInfo();

        requestSelectors = requestpaInfo.getSelectors();
        numRequestSelectors = requestSelectors.length;
        extension = request.getRequestPathInfo().getExtension();

        isGet = "GET".equals(methodName) || "HEAD".equals(methodName);
        isHtml = isGet && "html".equals(extension);

        this.workspaceName = workspaceName;
        // create the hash code once
        final String key = methodName + ':' + baseResourceType + ':' + extension + ':' + requestpaInfo.getSelectorString() + ':' +
            (this.resourceType == null ? "" : this.resourceType)+ ':' + (this.resourceSuperType == null ? "" : this.resourceSuperType) +
            ':' + (this.workspaceName == null ? "" : this.workspaceName);
        this.hashCode = key.hashCode();
    }

    public final Collection<Resource> getServlets(ResourceResolver resolver) {

        SortedSet<Resource> resources = new TreeSet<Resource>();

        Iterator<String> locations = new LocationIterator(resourceType, resourceSuperType,
            baseResourceType, resolver);
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

    protected void getWeightedResources(Set<Resource> resources,
            Resource location) {

        ResourceResolver resolver = location.getResourceResolver();
        Resource current = location;
        String parentName = ResourceUtil.getName(current);

        int selIdx = 0;
        String selector;
        do {
            selector = (selIdx < numRequestSelectors)
                    ? requestSelectors[selIdx]
                    : null;

            Iterator<Resource> children = resolver.listChildren(current);
            while (children.hasNext()) {
                Resource child = children.next();

                String scriptName = ResourceUtil.getName(child);
                int lastDot = scriptName.lastIndexOf('.');
                if (lastDot < 0) {
                    // no extension in the name, this is not a script
                    continue;
                }

                scriptName = scriptName.substring(0, lastDot);

                if (isGet) {

                    if (selector != null
                        && scriptName.equals(selector + "." + extension)) {
                        addWeightedResource(resources, child, selIdx + 1,
                            WeightedResource.WEIGHT_EXTENSION);
                        continue;
                    }

                    if (scriptName.equals(parentName + "." + extension)) {
                        addWeightedResource(resources, child, selIdx,
                            WeightedResource.WEIGHT_EXTENSION
                                + WeightedResource.WEIGHT_PREFIX);
                        continue;
                    }

                    if (scriptName.equals(extension)) {
                        addWeightedResource(resources, child, selIdx,
                            WeightedResource.WEIGHT_EXTENSION);
                        continue;
                    }

                    if (isHtml) {
                        if (selector != null && scriptName.equals(selector)) {
                            addWeightedResource(resources, child, selIdx + 1,
                                WeightedResource.WEIGHT_NONE);
                            continue;
                        }
                        if (scriptName.equals(parentName)) {
                            addWeightedResource(resources, child, selIdx,
                                WeightedResource.WEIGHT_PREFIX);
                            continue;
                        }
                    }
                }

                if (selector != null
                    && scriptName.equals(selector + "." + methodName)) {
                    addWeightedResource(resources, child, selIdx + 1,
                        WeightedResource.WEIGHT_NONE);
                    continue;
                }

                if (scriptName.equals(methodName)) {
                    addWeightedResource(resources, child, selIdx,
                        WeightedResource.WEIGHT_NONE);
                    continue;
                }
            }

            if (selector != null) {
                current = resolver.getResource(current, selector);
                parentName = selector;
                selIdx++;
            }
        } while (selector != null && current != null);

        // special treatment for servlets registered with neither a method
        // name nor extensions and selectors
        String path = location.getPath()
            + ServletResourceProviderFactory.SERVLET_PATH_EXTENSION;
        location = location.getResourceResolver().getResource(path);
        if (location != null) {
            addWeightedResource(resources, location, 0,
                WeightedResource.WEIGHT_LAST_RESSORT);
        }
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

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof ResourceCollector) ) {
            return false;
        }
        if ( obj == this ) {
            return true;
        }
        final ResourceCollector o = (ResourceCollector)obj;
        if ( isGet == o.isGet
             && isHtml == o.isHtml
             && numRequestSelectors == o.numRequestSelectors
             && stringEquals(resourceType, o.resourceType)
             && stringEquals(resourceSuperType, o.resourceSuperType)
             && stringEquals(extension, o.extension)
             && stringEquals(baseResourceType, o.baseResourceType)
             && stringEquals(methodName, o.methodName) ) {
            // now compare selectors
            for(int i=0;i<numRequestSelectors;i++) {
                if ( !stringEquals(requestSelectors[i], o.requestSelectors[i]) ) {
                    return false;
                }
            }
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
    private boolean stringEquals(final String s1, final String s2) {
        if ( s1 == null && s2 == null ) {
            return true;
        }
        if ( s1 == null || s2 == null ) {
            return false;
        }
        return s1.equals(s2);
    }
}
