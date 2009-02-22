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
package org.apache.sling.api.resource;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingException;

/**
 * API for providers of resources. Used by the {@link ResourceResolver} to
 * transparently access resources from different locations such as a JCR
 * repository (the default) or OSGi bundles.
 * <p>
 * This interface is intended to be implemented by providers of Resource
 * instances on behalf of the {@link ResourceResolver}. It is not intended to
 * be used by client applications directly.
 */
public interface ResourceProvider {

    /**
     * The service name to use when registering implementations of this
     * interface as services (value is
     * "org.apache.sling.api.resource.ResourceProvider").
     */
    static final String SERVICE_NAME = ResourceProvider.class.getName();

    /**
     * The name of the service registration property containing the root paths
     * of the resources provided by this provider (value is "provider.roots").
     */
    static final String ROOTS = "provider.roots";

    /**
     * The resource type be set on resources returned by the
     * {@link #listChildren(Resource)} method to enable traversing the resource
     * tree down to a deeply nested provided resource which has no concrete
     * parent hierarchy (value is"sling:syntheticResourceProviderResource").
     * 
     * @see #listChildren(Resource)
     */
    static final String RESOURCE_TYPE_SYNTHETIC = "sling:syntheticResourceProviderResource";

    /**
     * Returns a resource from this resource provider or <code>null</code> if
     * the resource provider cannot find it. The path should have one of the
     * {@link #ROOTS} strings as its prefix.
     * <p>
     * This method is called to resolve a resource for the given request. The
     * properties of the request, such as request parameters, may be use to
     * parametrize the resource resolution. An example of such parametrization
     * is support for a JSR-311 style resource provider to support the
     * parametrized URL patterns.
     * 
     * @param resourceResolver The {@link ResourceResolver} to which the
     *            returned {@link Resource} is attached.
     * @return <code>null</code> If this provider does not have a resource for
     *         the path.
     * @throws SlingException may be thrown in case of any problem creating the
     *             <code>Resource</code> instance.
     */
    Resource getResource(ResourceResolver resourceResolver,
            HttpServletRequest request, String path);

    /**
     * Returns a resource from this resource provider or <code>null</code> if
     * the resource provider cannot find it. The path should have one of the
     * {@link #ROOTS} strings as its prefix.
     * 
     * @param resourceResolver The {@link ResourceResolver} to which the
     *            returned {@link Resource} is attached.
     * @return <code>null</code> If this provider does not have a resource for
     *         the path.
     * @throws SlingException may be thrown in case of any problem creating the
     *             <code>Resource</code> instance.
     */
    Resource getResource(ResourceResolver resourceResolver, String path);

    /**
     * Returns an <code>Iterator</code> of {@link Resource} objects loaded
     * from the children of the given <code>Resource</code>. The returned
     * {@link Resource} instances are attached to the same
     * {@link ResourceResolver} as the given <code>parent</code> resource.
     * <p>
     * This method may be called for resource providers whose root path list
     * contains a path such that the resource path is a prefix of the list
     * entry. This allows for the enumeration of deeply nested provided
     * resources for which no actual parent hierarchy exists.
     * <p>
     * The returned iterator may in turn contain resources which do not actually
     * exist but are required to traverse the resource tree. Such resources
     * SHOULD be {@link SyntheticResource} objects whose resource type MUST be
     * set to {@link #RESOURCE_TYPE_SYNTHETIC}.
     * 
     * @param parent The {@link Resource Resource} whose children are requested.
     * @return An <code>Iterator</code> of {@link Resource} objects or
     *         <code>null</code> if the resource provider has no children for
     *         the given resource.
     * @throws NullPointerException If <code>parent</code> is
     *             <code>null</code>.
     * @throws SlingException If any error occurs acquiring the child resource
     *             iterator.
     */
    Iterator<Resource> listChildren(Resource parent);

}
