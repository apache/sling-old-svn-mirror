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
package org.apache.sling.resourcemerger.api;

import org.apache.sling.api.resource.Resource;

import aQute.bnd.annotation.ProviderType;

/**
 * Service to handle merged resources.
 */
@ProviderType
public interface ResourceMergerService {

    /**
     * Returns the absolute path for the provided relative path.
     *
     * @param relativePath The relative path
     * @return Returns the merged resource path
     * @throw IllegalArgumentException if relativePath is <code>null</code> or absolute
     */
    String getMergedResourcePath(String relativePath);

    /**
     * Returns a merged resource if the provided resource is from one of the search paths
     * and the resource is not hidden.
     * If the resource is <code>null</code>, <code>null</code> is returned.
     *
     * @param resource The resource
     * @return Returns the merged resource or <code>null</code>
     */
    Resource getMergedResource(Resource resource);

    /**
     * Returns <code>true</code> if the provided {@link Resource} is a merged resource.
     * If the resource is <code>null</code>, <code>false</code> is returned.
     * @param resource The resource
     * @return Returns <code>true</code> if the provided {@link Resource} is a merged resource.
     */
    boolean isMergedResource(Resource resource);

    /**
     * Return a resource path by taking the path of the merged resource, removing
     * the mount point and replacing it with the search path.
     *
     * For example, if the provided search path is "/apps/" and the merged resource
     * path is "/mnt/overlay/my/resource", the result will be "/apps/my/resource".
     *
     * @param searchPath The search path, this is an absolute path ending with a slash
     *                   as returned by the resource resolver
     * @param mergedResourcePath An absolute path to a merged resource
     * @return The path to the resource
     * @throws IllegalArgumentException If search path is not absolute or does not end
     *                                  with a slash or if the merged resource path
     *                                  is not within the space of the merged resources.
     * @since 1.1
     */
    String getResourcePath(String searchPath, String mergedResourcePath);
}
