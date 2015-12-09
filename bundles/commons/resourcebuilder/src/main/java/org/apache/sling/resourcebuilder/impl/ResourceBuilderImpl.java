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
package org.apache.sling.resourcebuilder.impl;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.resourcebuilder.api.ResourceBuilder;

/** ResourceBuilder implementation */
public class ResourceBuilderImpl implements ResourceBuilder {
    private final Resource originalParent;
    private final ResourceResolver resourceResolver;
    private Resource currentParent;
    private String intermediatePrimaryType;
    private boolean hierarchyMode;
    
    public static final String JCR_PRIMARYTYPE = "jcr:primaryType";
    
    public ResourceBuilderImpl(Resource parent) {
        if(parent == null) {
            throw new IllegalArgumentException("Parent resource is null");
        }
        originalParent = parent;
        resourceResolver = originalParent.getResourceResolver();
        withIntermediatePrimaryType(null);
        resetParent();
        hierarchyMode();
    }

    @Override
    public Resource getCurrentParent() {
        return currentParent;
    }

    @Override
    public ResourceBuilder resetParent() {
        currentParent = originalParent;
        return this;
    }

    @Override
    public ResourceBuilder resource(String relativePath, Object... properties) {
        Resource created = null;
        if(relativePath.startsWith("/")) {
            throw new IllegalArgumentException("Path is not relative:" + relativePath);
        }
        
        final String fullPath = currentParent.getPath() + "/" + relativePath;
        final String parentPath = ResourceUtil.getParent(fullPath);
        final Resource myParent = ensureResourceExists(parentPath);
        
        try {
            created = currentParent.getResourceResolver().create(myParent, 
                    ResourceUtil.getName(relativePath), MapArgsConverter.toMap(properties));
        } catch(PersistenceException pex) {
            throw new RuntimeException(
                    "PersistenceException while creating Resource " + relativePath 
                    + " under " + currentParent.getPath(), pex);
        }
        
        if(created == null) {
            throw new RuntimeException("Failed to created resource " + relativePath 
                    + " under " + currentParent.getPath());
        } else if(hierarchyMode) {
            currentParent = created;
        }
        return this;
    }
    
    /** Create a Resource at the specified path if none exists yet,
     *  using the current intermediate primary type. "Stolen" from
     *  the sling-mock module's ContentBuilder class.
     *  @param path Resource path
     *  @return Resource at path (existing or newly created)
     */
    protected final Resource ensureResourceExists(String path) {
        if(path == null || path.length() == 0 || path.equals("/")) {
            return resourceResolver.getResource("/");
        }
        Resource resource = resourceResolver.getResource(path);
        if (resource != null) {
            return resource;
        }
        String parentPath = ResourceUtil.getParent(path);
        String name = ResourceUtil.getName(path);
        Resource parentResource = ensureResourceExists(parentPath);
        try {
            resource = resourceResolver.create(
                    parentResource, 
                    name, 
                    MapArgsConverter.toMap(JCR_PRIMARYTYPE, intermediatePrimaryType));
            return resource;
        } catch (PersistenceException ex) {
            throw new RuntimeException("Unable to create intermediate resource at " + path, ex);
        }
    }

    @Override
    public ResourceBuilder withIntermediatePrimaryType(String primaryType) {
        intermediatePrimaryType = primaryType == null ? DEFAULT_PRIMARY_TYPE : primaryType;
        return this;
    }

    @Override
    public ResourceBuilder siblingsMode() {
        hierarchyMode = false;
        return this;
    }

    @Override
    public ResourceBuilder hierarchyMode() {
        hierarchyMode = true;
        return this;
    }
    
    @Override
    public ResourceBuilder commit() {
        try {
            resourceResolver.commit();
        } catch (PersistenceException ex) {
            throw new RuntimeException("Unable to commit", ex);
        }
        return this;
    }
}