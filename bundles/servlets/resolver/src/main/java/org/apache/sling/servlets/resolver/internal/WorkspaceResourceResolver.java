/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.servlets.resolver.internal;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * This is a facade around ResourceResolver in order to provide access to the
 * workspace name.
 *
 * TODO - should ResourceResolver have a getWorkspaceName() method?
 */
public class WorkspaceResourceResolver implements ResourceResolver {

    private ResourceResolver delegate;
    private String workspaceName;

    public WorkspaceResourceResolver(ResourceResolver delegate, String workspaceName) {
        this.delegate = delegate;
        this.workspaceName = workspaceName;
    }

    /** {@inheritDoc} */
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return delegate.adaptTo(type);
    }

    /** {@inheritDoc} */
    public Iterator<Resource> findResources(String query, String language) {
        return delegate.findResources(query, language);
    }

    /** {@inheritDoc} */
    public Resource getResource(Resource base, String path) {
        return delegate.getResource(base, path);
    }

    /** {@inheritDoc} */
    public Resource getResource(String path) {
        return delegate.getResource(path);
    }

    /** {@inheritDoc} */
    public String[] getSearchPath() {
        return delegate.getSearchPath();
    }

    /** {@inheritDoc} */
    public Iterator<Resource> listChildren(Resource parent) {
        return delegate.listChildren(parent);
    }

    /** {@inheritDoc} */
    public String map(HttpServletRequest request, String resourcePath) {
        return delegate.map(request, resourcePath);
    }

    /** {@inheritDoc} */
    public String map(String resourcePath) {
        return delegate.map(resourcePath);
    }

    /** {@inheritDoc} */
    public Iterator<Map<String, Object>> queryResources(String query, String language) {
        return delegate.queryResources(query, language);
    }

    /** {@inheritDoc} */
    public Resource resolve(HttpServletRequest request) {
        return delegate.resolve(request);
    }

    /** {@inheritDoc} */
    public Resource resolve(HttpServletRequest request, String absPath) {
        return delegate.resolve(request, absPath);
    }

    /** {@inheritDoc} */
    public Resource resolve(String absPath) {
        return delegate.resolve(absPath);
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

}
