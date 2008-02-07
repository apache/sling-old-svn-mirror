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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceProvider;

/**
 * The <code>SyntheticResource</code> class is a simple implementation of the
 * <code>Resource</code> interface which may be used to provide a resource
 * object which has no actual resource data.
 */
public class SyntheticResource implements Resource {

    /** The path of the synthetic resource */
    private final String path;

    /** The type this synthetic resource assumes */
    private final String resourceType;

    /** The metadat of this resource just containig the resource path */
    private final ResourceMetadata resourceMetadata;

    /**
     * Creates a synthetic resource with the given <code>path</code> and
     * <code>resourceType</code>.
     */
    public SyntheticResource(String path, String resourceType) {
        this.path = path;
        this.resourceType = resourceType;
        this.resourceMetadata = new ResourceMetadata();
        this.resourceMetadata.put(ResourceMetadata.RESOLUTION_PATH, path);
    }

    public String getPath() {
        return path;
    }

    public String getResourceType() {
        return resourceType;
    }

    /**
     * Returns a resource metadata object containing just the path of this
     * resource as the {@link ResourceMetadata#RESOLUTION_PATH} property.
     */
    public ResourceMetadata getResourceMetadata() {
        return resourceMetadata;
    }

    /**
     * Returns <code>null</code> because synthetic resources have no actual
     * data and are not provided by any {@link ResourceProvider}.
     */
    public ResourceProvider getResourceProvider() {
        return null;
    }

    /**
     * Returns <code>null</code> because synthetic resources have no actual
     * data and thus may not adapt to anything else.
     */
    public <Type> Type adaptTo(Class<Type> type) {
        return null;
    }

}
