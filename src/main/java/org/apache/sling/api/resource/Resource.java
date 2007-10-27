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

import javax.jcr.Item;

/** Resources are pieces of content on which Sling acts */
public interface Resource {

    /**
     * This resource's URI - for now that could be a JCR path, but having an URI
     * makes it possible to use other data sources (not sure if we really need
     * that though).
     */
    String getURI();

    /**
     * The resource type is meant to point to rendering/processing scripts,
     * editing dialogs, etc. It is usually a path in the repository, where
     * scripts and other tools definitions are found.
     */
    String getResourceType();

    /**
     * The JCR item addressed by the {@link #getURI() resource URI} or
     * <code>null</code> if the resource URI does not actually address an item
     * in a repository.
     */
    Item getItem();

    /**
     * Returns the object mapped from the {@link #getItem() item} or
     * <code>null</code>, if the item cannot be mapped or mapping is not
     * supported by the implementation.
     */
    Object getData();

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
}
