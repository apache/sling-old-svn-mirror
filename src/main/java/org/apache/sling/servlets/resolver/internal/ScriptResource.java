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
package org.apache.sling.servlets.resolver.internal;

import java.lang.ref.WeakReference;
import java.util.Iterator;

import javax.servlet.Servlet;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * ScriptResource is a resource wrapper of a resource fetched by a
 * per thread resource resolver.
 * As the script engines usually keep a reference to this resource
 * for a longer time, the resource switches internally to
 * a resource fetched by the shared resource resolver.
 *
 * We can't extend ResourceWrapper as its not possible to clear
 * the wrapped resource field.
 */
public class ScriptResource extends AbstractResource {

    private Resource activeResource;

    private final ResourceResolver sharedResourceResolver;

    private WeakReference<ResourceResolver> perThreadResourceResolver;

    public ScriptResource(final Resource resource, final ResourceResolver sharedResourceResolver) {
        this.perThreadResourceResolver = new WeakReference<ResourceResolver>(resource.getResourceResolver());
        this.sharedResourceResolver = sharedResourceResolver;
        this.activeResource = resource;
    }

    private Resource getActiveResource() {
        if ( this.perThreadResourceResolver != null ) {
            final ResourceResolver rr = this.perThreadResourceResolver.get();
            if ( rr == null || !rr.isLive() ) {
                this.perThreadResourceResolver = null;
                this.activeResource = this.sharedResourceResolver.getResource(this.activeResource.getPath());
            }
        }
        return this.activeResource;
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceType()
     */
    public String getResourceType() {
        return this.getActiveResource().getResourceType();
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceSuperType()
     */
    public String getResourceSuperType() {
        return this.getActiveResource().getResourceSuperType();
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceResolver()
     */
    public ResourceResolver getResourceResolver() {
        return this.getActiveResource().getResourceResolver();
    }

    /**
     * @see org.apache.sling.api.adapter.SlingAdaptable#adaptTo(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
        if ( type == Servlet.class ) {
            final Servlet s = (Servlet)super.adaptTo(type);
            if ( s != null ) {
                return (AdapterType)s;
            }
        }
        return this.getActiveResource().adaptTo(type);
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getPath()
     */
    public String getPath() {
        return this.getActiveResource().getPath();
    }

    /**
     * @see org.apache.sling.api.resource.Resource#getResourceMetadata()
     */
    public ResourceMetadata getResourceMetadata() {
        return this.getActiveResource().getResourceMetadata();
    }

    /**
     * @see org.apache.sling.api.resource.AbstractResource#getName()
     */
    @Override
    public String getName() {
        return this.getActiveResource().getName();
    }

    /**
     * @see org.apache.sling.api.resource.AbstractResource#getParent()
     */
    @Override
    public Resource getParent() {
        return this.getActiveResource().getParent();
    }

    /**
     * @see org.apache.sling.api.resource.AbstractResource#getChild(java.lang.String)
     */
    @Override
    public Resource getChild(String relPath) {
        return this.getActiveResource().getChild(relPath);
    }

    /**
     * @see org.apache.sling.api.resource.AbstractResource#listChildren()
     */
    @Override
    public Iterator<Resource> listChildren() {
        return this.getActiveResource().listChildren();
    }

    /**
     * @see org.apache.sling.api.resource.AbstractResource#getChildren()
     */
    @Override
    public Iterable<Resource> getChildren() {
        return this.getActiveResource().getChildren();
    }

    /**
     * @see org.apache.sling.api.resource.AbstractResource#isResourceType(java.lang.String)
     */
    @Override
    public boolean isResourceType(String resourceType) {
        return this.getActiveResource().isResourceType(resourceType);
    }
}
