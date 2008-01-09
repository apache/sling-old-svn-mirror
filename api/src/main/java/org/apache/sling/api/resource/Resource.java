/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.api.resource;

/** Resources are pieces of content on which Sling acts */
public interface Resource {

    /**
     * The special resource type for resource instances representing nonexisting
     * resources (value is "sling:nonexisting"). This resource type is used by
     * {@link ResourceResolver} instances to mark a resource which could not
     * actually be resolved.
     *
     * @see #getResourceType()
     * @see ResourceResolver#resolve(javax.servlet.http.HttpServletRequest)
     */
    static final String RESOURCE_TYPE_NON_EXISTING = "sling:nonexisting";

    /**
     * This resource's path - for now that could be a JCR path. It's also
     * possible to have an URI for other data sources.
     */
    String getPath();

    /**
     * The resource type is meant to point to rendering/processing scripts,
     * editing dialogs, etc. It is usually a path in the repository, where
     * scripts and other tools definitions are found, but the
     * {@link ResourceResolver} is free to set this to any suitable value such
     * as the primary node type of the JCR node from which the resource is
     * created.
     * <p>
     * If the resource instance represents a resource which is not actually
     * existing, this method returns the {@link #RESOURCE_TYPE_NON_EXISTING}.
     */
    String getResourceType();

    /**
     * Returns the metadata of this resource. The concrete data contained in the
     * {@link ResourceMetadata} object returned is implementation specific
     * except for the {@link ResourceMetadata#RESOLUTION_PATH} property which is
     * required to be set to the part of the request URI used to resolve the
     * resource.
     *
     * @see ResourceMetadata
     */
    ResourceMetadata getResourceMetadata();

    /**
     * Adapts this resource to another type. A JCR based resource might support
     * adapting to the JCR Node on which the resource is based.
     *
     * @param <AdapterType> The generic type to which this resource is adapted
     *            to
     * @param type The Class object of the target type, such as
     *            <code>Node.class</code>
     * @return The adapter target or <code>null</code> if the resource cannot
     *         adapt to the requested type
     */
    <AdapterType> AdapterType adaptTo(Class<AdapterType> type);

}
