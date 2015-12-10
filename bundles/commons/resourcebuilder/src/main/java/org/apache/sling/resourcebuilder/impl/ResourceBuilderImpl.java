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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ModifiableValueMap;
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
    public static final String JCR_MIMETYPE = "jcr:mimeType";
    public static final String JCR_LASTMODIFIED = "jcr:lastModified";
    public static final String JCR_DATA = "jcr:data";
    public static final String JCR_CONTENT = "jcr:content";
    public static final String NT_RESOURCE = "nt:resource";
    
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
        Resource r = null;
        if(relativePath.startsWith("/")) {
            throw new IllegalArgumentException("Path is not relative:" + relativePath);
        }
        
        final String fullPath = currentParent.getPath() + "/" + relativePath;
        final String parentPath = ResourceUtil.getParent(fullPath);
        final Resource myParent = ensureResourceExists(parentPath);
        
        try {
            r = currentParent.getResourceResolver().getResource(fullPath);
            final Map<String, Object> props = MapArgsConverter.toMap(properties);
            if(r == null) {
                r = currentParent.getResourceResolver().create(myParent, 
                        ResourceUtil.getName(relativePath), props);
            } else {
                // Resource exists, set our properties
                final ModifiableValueMap mvm = r.adaptTo(ModifiableValueMap.class);
                if(mvm == null) {
                    throw new IllegalStateException("Cannot modify properties of " + r.getPath());
                }
                for(Map.Entry <String, Object> e : props.entrySet()) {
                    mvm.put(e.getKey(), e.getValue());
                }
            }
        } catch(PersistenceException pex) {
            throw new RuntimeException(
                    "PersistenceException while creating Resource " + relativePath 
                    + " under " + currentParent.getPath(), pex);
        }
        
        if(r == null) {
            throw new RuntimeException("Failed to get or create resource " + relativePath 
                    + " under " + currentParent.getPath());
        } else if(hierarchyMode) {
            currentParent = r;
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
    public ResourceBuilder file(String filename, InputStream data, String mimeType, long lastModified) {
        Resource file = null;
        final ResourceResolver resolver = currentParent.getResourceResolver();
        final String name = ResourceUtil.getName(filename);
        
        if(!filename.equals(name)) {
            throw new IllegalArgumentException("Filename must not be a path:" + filename + " -> " + name);
        }
        if(data == null) {
            throw new IllegalArgumentException("Data is null for file " + filename);
        }
        
        try {
            final String fullPath = currentParent.getPath() + "/" + name;
            if(resolver.getResource(fullPath) != null) {
                throw new IllegalStateException("Resource already exists:" + fullPath);
            }
            file = resolver.create(currentParent, name, null);
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put(JCR_PRIMARYTYPE, NT_RESOURCE);
            // TODO get mime type from MimeTypeService
            props.put(JCR_MIMETYPE, mimeType);
            props.put(JCR_LASTMODIFIED, lastModified >= 0 ? lastModified : System.currentTimeMillis());
            props.put(JCR_DATA, data);
            resolver.create(file, JCR_CONTENT, props); 
        } catch(PersistenceException pex) {
            throw new RuntimeException("Unable to create file under " + currentParent.getPath(), pex);
        }
        
        if(file == null) {
            throw new RuntimeException("Unable to get or created file resource " + filename + " under " + currentParent.getPath());
        }
        if(hierarchyMode) {
            currentParent = file;
        }
        
        return this;
    }

    @Override
    public ResourceBuilder file(String filename, InputStream data) {
        return file(filename, data, null, -1);
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