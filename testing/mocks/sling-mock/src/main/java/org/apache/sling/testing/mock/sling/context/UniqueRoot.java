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
package org.apache.sling.testing.mock.sling.context;

import static org.apache.sling.jcr.resource.JcrResourceConstants.NT_SLING_ORDERED_FOLDER;

import java.util.UUID;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * Manages unique root paths in JCR repository.
 * This is important for resource resolver types like JCR_JACKRABBIT  
 * where the repository is not cleaned for each test run. This class provides
 * unique root paths for each run, and cleans them up when done.
 */
public class UniqueRoot {
    
    private final SlingContextImpl context;
    
    protected final String uniquePathPart;
    
    private Resource contentRoot;
    private Resource appsRoot;
    private Resource libsRoot;
    
    private static final Logger log = LoggerFactory.getLogger(UniqueRoot.class);
    
    protected UniqueRoot(SlingContextImpl context) {
        this.context = context;
        // generate unique path part by using a UUID
        uniquePathPart = UUID.randomUUID().toString();
    }
    
    /**
     * Get or create resource with given JCR primary type
     * @param path Path
     * @param primaryType JCR primary type
     * @return Resource (never null)
     */
    protected final Resource getOrCreateResource(String path, String primaryType) {
        try {
            return ResourceUtil.getOrCreateResource(context.resourceResolver(), path, 
                    ImmutableMap.<String,Object>of(JcrConstants.JCR_PRIMARYTYPE, primaryType),
                    null, true);
        }
        catch (PersistenceException ex) {
            throw new RuntimeException("Unable to create resource at " + path + ": " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Gets (and creates if required) a unique path at <code>/content/xxx</code>.
     * The path (incl. all children) is automatically removed when the unit test completes.
     * @return Unique content path
     */
    public final String content() {
        if (contentRoot == null) {
            contentRoot = getOrCreateResource("/content/" + uniquePathPart, NT_SLING_ORDERED_FOLDER);
        }
        return contentRoot.getPath();
    }
        
    /**
     * Gets (and creates if required) a unique path at <code>/apps/xxx</code>.
     * The path (incl. all children) is automatically removed when the unit test completes.
     * @return Unique content path
     */
    public final String apps() {
        if (appsRoot == null) {
            appsRoot = getOrCreateResource("/apps/" + uniquePathPart, NT_SLING_ORDERED_FOLDER);
        }
        return appsRoot.getPath();
    }
        
    /**
     * Gets (and creates if required) a unique path at <code>/libs/xxx</code>.
     * The path (incl. all children) is automatically removed when the unit test completes.
     * @return Unique content path
     */
    public final String libs() {
        if (libsRoot == null) {
            libsRoot = getOrCreateResource("/libs/" + uniquePathPart, NT_SLING_ORDERED_FOLDER);
        }
        return libsRoot.getPath();
    }
    
    /**
     * Cleanup is called when the unit test rule completes a unit test run.
     * All resources created have to be removed.
     */
    protected void cleanUp() {
        deleteResources(contentRoot, appsRoot, libsRoot);
    }
    
    /**
     * Deletes the given set of resources and commits afterwards.
     * @param resources Resources to be deleted
     */
    protected final void deleteResources(Resource... resources) {
        for (Resource resource : resources) {
            if (resource != null && context.resourceResolver.getResource(resource.getPath()) != null) {
                try {
                    context.resourceResolver().delete(resource);
                }
                catch (PersistenceException ex) {
                    log.warn("Unable to delete root path " + resource.getPath(), ex);
                }
            }
        }
        try {
            context.resourceResolver().commit();
        }
        catch (PersistenceException ex) {
            log.warn("Unable to commit root path deletions.", ex);
        }
            
    }
    
}
