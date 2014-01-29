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
package org.apache.sling.resourcemerger.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;

/**
 * {@inheritDoc}
 */
public class MergedResource extends AbstractResource {

    private final ResourceResolver resolver;
    private final String mergeRootPath;
    private final String relativePath;
    private final List<String> mappedResources = new ArrayList<String>();

    /**
     * Constructor
     *
     * @param resolver      Resource resolver
     * @param mergeRootPath Merge root path
     * @param relativePath  Relative path
     */
    MergedResource(ResourceResolver resolver, String mergeRootPath, String relativePath) {
        this.resolver = resolver;
        this.mergeRootPath = mergeRootPath;
        this.relativePath = relativePath;
    }

    /**
     * Constructor
     *
     * @param resolver      Resource resolver
     * @param mergeRootPath   Merge root path
     * @param relativePath    Relative path
     * @param mappedResources List of physical mapped resources' paths
     */
    MergedResource(ResourceResolver resolver, String mergeRootPath, String relativePath, List<String> mappedResources) {
        this.resolver = resolver;
        this.mergeRootPath = mergeRootPath;
        this.relativePath = relativePath;
        this.mappedResources.addAll(mappedResources);
    }


    // ---- MergedResource interface ------------------------------------------

    public String getRelativePath() {
        return relativePath;
    }

    /**
     * {@inheritDoc}
     */
    public void addMappedResource(String path) {
        mappedResources.add(path);
    }

    /**
     * {@inheritDoc}
     */
    public Iterable<String> getMappedResources() {
        return mappedResources;
    }


    // ---- Resource interface ------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String getPath() {
        return ResourceUtil.normalize(mergeRootPath + "/" + relativePath);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Resource> listChildren() {
        return resolver.listChildren(this);
    }

    /**
     * {@inheritDoc}
     */
    public String getResourceType() {
        return relativePath;
    }

    /**
     * {@inheritDoc}
     */
    public String getResourceSuperType() {
        // So far, there's no concept of resource super type for a merged resource
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceMetadata getResourceMetadata() {
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.put(ResourceMetadata.RESOLUTION_PATH, getPath());
        metadata.put("sling.mergedResource", true);
        metadata.put("sling.mappedResources", mappedResources.toArray(new String[mappedResources.size()]));
        return metadata;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceResolver getResourceResolver() {
        return resolver;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == ValueMap.class) {
            return (AdapterType) new MergedValueMap(this);
        }

        return null;
    }


    // ---- Object ------------------------------------------------------------

    /**
     * Merged resources are considered equal if their paths are equal,
     * regardless of the list of mapped resources.
     *
     * @param o Object to compare with
     * @return Returns <code>true</code> if the two merged resources have the
     *         same path.
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o.getClass() != getClass()) {
            return false;
        }

        Resource r = (Resource) o;
        return r.getPath().equals(getPath());
    }

}
