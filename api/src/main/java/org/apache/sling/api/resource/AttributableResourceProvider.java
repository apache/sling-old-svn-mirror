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

import java.util.Collection;

import aQute.bnd.annotation.ConsumerType;

/**
 * The attributes provider is an extensions of a {@link ResourceProvider}.
 * It allows to add attributes to the set of available attributes from a
 * resource resolver.
 *
 * This extension is supported for services directly implementing the
 * {@link ResourceProvider} interface and {@link ResourceProvider}s
 * returned through a {@link ResourceProviderFactory}.
 *
 * @see ResourceResolver#getAttribute(String)
 * @see ResourceResolver#getAttributeNames()
 *
 * @since 2.2
 */
@ConsumerType
public interface AttributableResourceProvider extends ResourceProvider {

    /**
     * Returns a collection of attribute names whose value can be retrieved
     * calling the {@link #getAttribute(ResourceResolver, String)} method.
     *
     * @return A collection of attribute names or <code>null</code>
     * @throws IllegalStateException if this resource provider has already been
     *                               closed.
     */
    Collection<String> getAttributeNames(ResourceResolver resolver);

    /**
     * Returns the value of the given resource provider attribute or <code>null</code>
     * if the attribute is not set or not visible (as e.g. security
     * sensitive attributes).
     *
     * @param name
     *            The name of the attribute to access
     * @return The value of the attribute or <code>null</code> if the attribute
     *         is not set or not accessible.
     * @throws NullPointerException
     *             if <code>name</code> is <code>null</code>.
     * @throws IllegalStateException
     *             if this resource provider has already been closed.
     */
    Object getAttribute(ResourceResolver resolver, String name);
}
