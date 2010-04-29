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

import java.util.Iterator;
import java.util.Set;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.servlets.resolver.internal.ServletResolverConstants;
import org.apache.sling.servlets.resolver.internal.resource.ServletResourceProviderFactory;

/**
 * The <code>ResourceCollector</code> class provides a single public method -
 * {@link #getServlets(ResourceResolver)} - which is used to find an ordered collection
 * of <code>Resource</code> instances which may be used to find a servlet or
 * script to handle a request to the given resource.
 */
public class ResourceCollector extends AbstractResourceCollector {

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

    // the request selectors as a string converted to a realtive path or
    // null if the request has no selectors
    private final String[] requestSelectors;

    // the number of request selectors of the request or 0 if none
    private final int numRequestSelectors;

    // request is GET or HEAD
    private final boolean isGet;

    // request is GET or HEAD and extension is html
    private final boolean isHtml;

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
     * @param workspaceName
     * @return The <code>ResourceCollector</code> to find servlets and scripts
     *         suitable for handling the <code>request</code>.
     */
    public static ResourceCollector create(final SlingHttpServletRequest request,
            final String workspaceName,
            final String[] executionPaths) {
        return new ResourceCollector(request, workspaceName, executionPaths);
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
    public ResourceCollector(final String methodName,
            final String baseResourceType,
            final Resource resource,
            final String workspaceName,
            final String[] executionPaths) {
        super((baseResourceType != null ? baseResourceType : ServletResolverConstants.DEFAULT_SERVLET_NAME),
                resource.getResourceType(),
                resource.getResourceSuperType(),
                null,
                executionPaths);
        this.methodName = methodName;
        this.requestSelectors = new String[0];
        this.numRequestSelectors = 0;
        this.isGet = false;
        this.isHtml = false;

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
    private ResourceCollector(final SlingHttpServletRequest request,
            final String workspaceName,
            final String[] executionPaths) {
        super(ServletResolverConstants.DEFAULT_SERVLET_NAME,
                request.getResource().getResourceType(),
                request.getResource().getResourceSuperType(),
                request.getRequestPathInfo().getExtension(),
                executionPaths);
        this.methodName = request.getMethod();

        RequestPathInfo requestpaInfo = request.getRequestPathInfo();

        requestSelectors = requestpaInfo.getSelectors();
        numRequestSelectors = requestSelectors.length;

        isGet = "GET".equals(methodName) || "HEAD".equals(methodName);
        isHtml = isGet && "html".equals(extension);

        this.workspaceName = workspaceName;
        // create the hash code once
        final String key = methodName + ':' + baseResourceType + ':' + extension + ':' + requestpaInfo.getSelectorString() + ':' +
            (this.resourceType == null ? "" : this.resourceType)+ ':' + (this.resourceSuperType == null ? "" : this.resourceSuperType) +
            ':' + (this.workspaceName == null ? "" : this.workspaceName);
        this.hashCode = key.hashCode();
    }

    protected void getWeightedResources(final Set<Resource> resources,
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

                if ( !this.isPathAllowed(child.getPath()) ) {
                    continue;
                }
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
        if ( this.isPathAllowed(path) ) {
            location = location.getResourceResolver().getResource(path);
            if (location != null) {
                addWeightedResource(resources, location, 0,
                    WeightedResource.WEIGHT_LAST_RESSORT);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof ResourceCollector) ) {
            return false;
        }
        if ( obj == this ) {
            return true;
        }
        if ( super.equals(obj) ) {
            final ResourceCollector o = (ResourceCollector)obj;
            if ( isGet == o.isGet
                 && isHtml == o.isHtml
                 && numRequestSelectors == o.numRequestSelectors
                 && stringEquals(methodName, o.methodName)
                 && stringEquals(workspaceName, o.workspaceName)) {
                // now compare selectors
                for(int i=0;i<numRequestSelectors;i++) {
                    if ( !stringEquals(requestSelectors[i], o.requestSelectors[i]) ) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
