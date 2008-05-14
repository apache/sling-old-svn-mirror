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
import java.util.Set;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ResourceCollectorGet</code> is a special
 * {@link ResourceCollector} which takes into account the request selectors and
 * request extension to find servlets and scripts. This class is instantiated by
 * the {@link ResourceCollector#create(SlingHttpServletRequest)} method on
 * behalf of GET and HEAD requests.
 */
class ResourceCollectorGet extends ResourceCollector {

    /**
     * The special value returned by
     * {@link #calculatePrefixMethodWeight(Resource, String, boolean)} if the
     * resource is not suitable to handle the request according to the location
     * prefix, request selectors and request extension (value is
     * <code>Integer.MIN_VALUE</code>).
     */
    protected static final int WEIGHT_NO_MATCH = Integer.MIN_VALUE;

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    // the request selectors as a string converted to a realtive path or
    // null if the request has no selectors
    private final String requestSelectors;

    // the number of request selectors of the request or 0 if none
    private final int numRequestSelectors;

    // the request extension or null if the request has no extension
    private final String extension;

    // whether the request extension is "html"
    private final boolean isHtmlRequest;

    ResourceCollectorGet(SlingHttpServletRequest request) {
        super(request.getMethod(), null);

        RequestPathInfo requestpaInfo = request.getRequestPathInfo();
        String rs = requestpaInfo.getSelectorString();
        if (rs == null || rs.length() == 0) {
            requestSelectors = null;
            numRequestSelectors = 0;
        } else {
            requestSelectors = rs.replace('.', '/');
            numRequestSelectors = requestpaInfo.getSelectors().length;
        }

        extension = requestpaInfo.getExtension();
        isHtmlRequest = "html".equals(extension);
    }

    /**
     * In addition to the base class implementation this method also considers
     * resources for the request selectors, request extension and location name.
     */
    @Override
    protected void getWeightedResources(Set<Resource> resources,
            Resource location) {

        ResourceResolver resolver = location.getResourceResolver();

        // print/a4.html.esp
        // print.html.esp
        if (requestSelectors != null) {
            String relativePath = requestSelectors;
            int numSelectors = numRequestSelectors;
            while (relativePath.length() > 0) {

                String parent;
                String name;
                Resource loc;

                int slash = relativePath.lastIndexOf('/');
                if (slash > 0) {
                    parent = relativePath.substring(0, slash);
                    name = relativePath.substring(slash + 1);
                    loc = getResource(resolver, location, parent);
                } else {
                    parent = "";
                    name = relativePath;
                    loc = location;
                }

                getWeightedResources(resources, loc, name, false, numSelectors);
                relativePath = parent;

                numSelectors--;
            }
        }

        // sample.html.esp
        // sample.esp
        // html.esp
        getWeightedResources(resources, location,
            ResourceUtil.getName(location.getPath()), true, 0);

        // base class implementation supporting the method name
        super.getWeightedResources(resources, location);
    }

    /**
     * Scan the given <code>location</code> for any resources whose name
     * starts with the <code>locationPrefix</code> (optionally if
     * <code>optionalLocationPrefix</code> is <code>true</code>) and
     * contains the request method and/or request extension.
     * 
     * @param location The location in which to find resources.
     * @param locationPrefix The prefix to be matched by resources.
     * @param optionalLocationPrefix Whether the resources must start with the
     *            <code>locationPrefix</code> or not. If this is
     *            <code>true</code>, the resources name need not start with
     *            the prefix.
     * @param numSelectors The number of selectors matched by the resources in
     *            this location. This value is only used to created
     *            {@link WeightedResource} instances to be added to the resource
     *            set.
     * @param resources The set of resources to which any more
     *            <code>WeightedResource</code> instances are added.
     */
    protected void getWeightedResources(Set<Resource> resources,
            Resource location, String locationPrefix,
            boolean optionalLocationPrefix, int numSelectors) {

        // now list the children and check them
        Iterator<Resource> children = location.getResourceResolver().listChildren(
            location);
        while (children.hasNext()) {
            Resource child = children.next();
            int methodPrefixWeight = calculatePrefixMethodWeight(child,
                locationPrefix, optionalLocationPrefix);
            if (methodPrefixWeight != WEIGHT_NO_MATCH) {
                addWeightedResource(resources, child, numSelectors,
                    methodPrefixWeight);
            }
        }
    }

    /**
     * Calculates the method/prefix weight to be assigned to a
     * {@link WeightedResource} created for the given <code>resource</code>.
     * If the name of the resource does not meet the requirements for matching
     * resources the special value {@link #WEIGHT_NO_MATCH} is returned.
     * Otherwise the actual method/prefix weight to use is returned.
     * <p>
     * The requirements for resource names to be accepted are:
     * <ul>
     * <li>If <code>optionalLocationPrefix</code> is <code>false</code>,
     * the resource name must be prefixed with the <code>locationPrefix</code>.</li>
     * <li>If the request has an extension the resource name must contain this
     * request extension unless the resource name is prefixed with the
     * <code>locationPrefix</code> and the request extension is
     * <code>html</code>.</li>
     * <li>Besides the location prefix and method name, just a single extension
     * is allowed.</li>
     * </ul>
     * 
     * @param resource The <code>resource</code> whose name is to be checked
     * @param locationPrefix The prefix to be matched by resources.
     * @param optionalLocationPrefix Whether the resources must start with the
     *            <code>locationPrefix</code> or not. If this is
     *            <code>true</code>, the resources name need not start with
     *            the prefix.
     * @return The method/prefix weight to assign a {@link WeightedResource}
     *         based on the given <code>resource</code> or
     *         {@link #WEIGHT_NO_MATCH} if the resource name does not meet above
     *         listed requirements.
     */
    protected int calculatePrefixMethodWeight(final Resource resource,
            final String locationPrefix, final boolean optionalLocationPrefix) {

        // split the remaing name to ease further checks
        String resPath = resource.getPath();
        String name = ResourceUtil.getName(resPath);
        String[] parts = name.split("\\.");
        int i = 0;

        // flag whether we require the request extension in the script name
        // to begin with, this is only required for non-html requests
        boolean requireExtension = !isHtmlRequest;

        int methodExtensionWeight = WeightedResource.WEIGHT_NONE;

        // expect locationPrefix being the last part of the selector
        if (i >= parts.length || !locationPrefix.equals(parts[i])) {
            if (!optionalLocationPrefix) {
                log.debug(
                    "createLocationResource: Ignoring Resource {}: Name does not start with {}",
                    resource, locationPrefix);
                return WEIGHT_NO_MATCH;
            }

            log.debug(
                "createLocationResource: Resource {} does not start with {}",
                resource, locationPrefix);

            // flag that the request extension is required in the name
            requireExtension = true;
        } else {
            // increment counter, we have the locationPrefix
            i++;
            methodExtensionWeight += WeightedResource.WEIGHT_PREFIX;
        }

        // next may be extension name
        if (extension != null) {
            if (i < parts.length && extension.equals(parts[i])) {
                i++;
                methodExtensionWeight += WeightedResource.WEIGHT_EXTENSION;
            } else if (requireExtension) {
                log.debug(
                    "createLocationResource: Ignoring Resource {} because request extension {} is missing in the name",
                    resource, extension);
                return WEIGHT_NO_MATCH;
            }
        }

        // next would be script extension
        if ((i + 1) != parts.length) {
            // more than one more part, we expect just a single one or none
            return WEIGHT_NO_MATCH;
        }

        return methodExtensionWeight;
    }

}
