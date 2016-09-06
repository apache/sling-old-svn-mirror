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
import org.apache.sling.commons.mime.MimeTypeService;
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
    public static final String NT_FILE = "nt:file";
    
    public static final String CANNOT_RESTART =
            "Cannot reset the parent resource or resource resolver, please create a new "
            + "builder using the ResourceBuilder service";
    
    private final MimeTypeService mimeTypeService;
    
    public ResourceBuilderImpl(Resource parent, MimeTypeService mts) {
        mimeTypeService = mts;
        if(parent == null) {
            throw new IllegalArgumentException("Parent resource is null");
        }
        originalParent = parent;
        resourceResolver = originalParent.getResourceResolver();
        intermediatePrimaryType = DEFAULT_PRIMARY_TYPE;
        currentParent = parent;
        hierarchyMode = true;
    }
    
    private ResourceBuilderImpl cloneResourceBuilder(Resource newCurrentParent,
            String newIntermediatePrimaryType, boolean newHierarchyMode) {
        ResourceBuilderImpl clone = new ResourceBuilderImpl(originalParent, mimeTypeService);
        clone.currentParent = newCurrentParent;
        clone.intermediatePrimaryType = newIntermediatePrimaryType;
        clone.hierarchyMode = newHierarchyMode;
        return clone;
    }
    
    @Override
    public Resource getCurrentParent() {
        return currentParent;
    }

    @Override
    public ResourceBuilder atParent() {
        return cloneResourceBuilder(originalParent, this.intermediatePrimaryType, true);
    }
    
    private boolean isAbsolutePath(String path) {
        return path.startsWith("/") && !path.contains("..");
    }

    private void checkRelativePath(String path) {
        if(path.startsWith("/")) {
            throw new IllegalArgumentException("Path is not relative:" + path);
        }
        if(path.contains("..")) {
            throw new IllegalArgumentException("Path contains invalid pattern '..': " + path);
        }
    }

    private String parentPath(String relativePath) {
        final String parentPath = currentParent.getPath();
        final String fullPath = 
            parentPath.endsWith("/")  ? 
            parentPath + relativePath : 
            parentPath + "/" + relativePath;
        return ResourceUtil.getParent(fullPath);
    }
    
    @Override
    public ResourceBuilder resource(String path, Object... properties) {
        Resource r = null;
        
        final String parentPath;
        final String fullPath;
        if (isAbsolutePath(path)) {
            parentPath = ResourceUtil.getParent(path);
            fullPath = path;
        }
        else {
            checkRelativePath(path);
            parentPath = parentPath(path);
            fullPath = currentParent.getPath() + "/" + path;
        }
        final Resource myParent = ensureResourceExists(parentPath);
        
        try {
            r = currentParent.getResourceResolver().getResource(fullPath);
            final Map<String, Object> props = MapArgsConverter.toMap(properties);
            if(r == null) {
                r = currentParent.getResourceResolver().create(myParent, 
                        ResourceUtil.getName(fullPath), props);
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
                    "PersistenceException while creating Resource " + fullPath, pex);
        }
        
        if (r == null) {
            throw new RuntimeException("Failed to get or create resource " + fullPath);
        } else if(hierarchyMode) {
            return cloneResourceBuilder(r, this.intermediatePrimaryType, this.hierarchyMode);
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
    
    protected String getMimeType(String filename, String userSuppliedMimeType) {
        if(userSuppliedMimeType != null) {
            return userSuppliedMimeType;
        }
        return mimeTypeService.getMimeType(filename);
    }
    
    protected long getLastModified(long userSuppliedValue) {
        if(userSuppliedValue < 0) {
            return System.currentTimeMillis();
        }
        return userSuppliedValue;
    }
    
    @Override
    public ResourceBuilder file(String relativePath, InputStream data, String mimeType, long lastModified) {
        checkRelativePath(relativePath);
        final String name = ResourceUtil.getName(relativePath);
        if(data == null) {
            throw new IllegalArgumentException("Data is null for file " + name);
        }
        
        Resource file = null;
        final ResourceResolver resolver = currentParent.getResourceResolver();
        final String parentPath = parentPath(relativePath);
        
        final Resource parent = ensureResourceExists(parentPath);
        try {
            final String fullPath = currentParent.getPath() + "/" + name;
            if(resolver.getResource(fullPath) != null) {
                throw new IllegalStateException("Resource already exists:" + fullPath);
            }
            final Map<String, Object> fileProps = new HashMap<String, Object>();
            fileProps.put(JCR_PRIMARYTYPE, NT_FILE);
            file = resolver.create(parent, name, fileProps);
            
            final Map<String, Object> contentProps = new HashMap<String, Object>();
            contentProps.put(JCR_PRIMARYTYPE, NT_RESOURCE);
            contentProps.put(JCR_MIMETYPE, getMimeType(name, mimeType));
            contentProps.put(JCR_LASTMODIFIED, getLastModified(lastModified));
            contentProps.put(JCR_DATA, data);
            resolver.create(file, JCR_CONTENT, contentProps); 
        } catch(PersistenceException pex) {
            throw new RuntimeException("Unable to create file under " + currentParent.getPath(), pex);
        }
        
        if(file == null) {
            throw new RuntimeException("Unable to get or created file resource " + relativePath + " under " + currentParent.getPath());
        }
        if(hierarchyMode) {
            return cloneResourceBuilder(file, this.intermediatePrimaryType, this.hierarchyMode);
        }
        return this;
    }

    @Override
    public ResourceBuilder file(String filename, InputStream data) {
        return file(filename, data, null, -1);
    }

    @Override
    public ResourceBuilder withIntermediatePrimaryType(String primaryType) {
        String intermediatePrimaryType = primaryType == null ? DEFAULT_PRIMARY_TYPE : primaryType;
        return cloneResourceBuilder(currentParent, intermediatePrimaryType, hierarchyMode);
    }

    @Override
    public ResourceBuilder siblingsMode() {
        return cloneResourceBuilder(currentParent, intermediatePrimaryType, false);
    }

    @Override
    public ResourceBuilder hierarchyMode() {
        return cloneResourceBuilder(currentParent, intermediatePrimaryType, true);
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
