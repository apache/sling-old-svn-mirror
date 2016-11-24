/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.core.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.scripting.api.resolver.ScriptingResourceResolverProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptingResourceResolver implements ResourceResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptingResourceResolver.class);

    private ResourceResolver delegate;
    private boolean shouldLogClosing = false;

    public ScriptingResourceResolver(boolean shouldLogClosing, ResourceResolver delegate) {
        this.shouldLogClosing = shouldLogClosing;
        this.delegate = delegate;
    }

    @Nonnull
    @Override
    public Resource resolve(@Nonnull HttpServletRequest request, @Nonnull String absPath) {
        return delegate.resolve(request, absPath);
    }

    @Nonnull
    @Override
    public Resource resolve(@Nonnull String absPath) {
        return delegate.resolve(absPath);
    }

    @Nonnull
    @Override
    public Resource resolve(@Nonnull HttpServletRequest request) {
        return delegate.resolve(request);
    }

    @Nonnull
    @Override
    public String map(@Nonnull String resourcePath) {
        return delegate.map(resourcePath);
    }

    @Override
    public String map(@Nonnull HttpServletRequest request, @Nonnull String resourcePath) {
        return delegate.map(request, resourcePath);
    }

    @Override
    public Resource getResource(@Nonnull String path) {
        return delegate.getResource(path);
    }

    @Override
    public Resource getResource(Resource base, @Nonnull String path) {
        return delegate.getResource(base, path);
    }

    @Nonnull
    @Override
    public String[] getSearchPath() {
        return delegate.getSearchPath();
    }

    @Nonnull
    @Override
    public Iterator<Resource> listChildren(@Nonnull Resource parent) {
        return delegate.listChildren(parent);
    }

    @Override
    public Resource getParent(@Nonnull Resource child) {
        return delegate.getParent(child);
    }

    @Nonnull
    @Override
    public Iterable<Resource> getChildren(@Nonnull Resource parent) {
        return delegate.getChildren(parent);
    }

    @Nonnull
    @Override
    public Iterator<Resource> findResources(@Nonnull String query, String language) {
        return delegate.findResources(query, language);
    }

    @Nonnull
    @Override
    public Iterator<Map<String, Object>> queryResources(@Nonnull String query, String language) {
        return delegate.queryResources(query, language);
    }

    @Override
    public boolean hasChildren(@Nonnull Resource resource) {
        return delegate.hasChildren(resource);
    }

    @Nonnull
    @Override
    public ResourceResolver clone(Map<String, Object> authenticationInfo) throws LoginException {
        return new ScriptingResourceResolver(shouldLogClosing, delegate.clone(null));
    }

    @Override
    public boolean isLive() {
        return delegate.isLive();
    }

    @Override
    public void close() {
        LOGGER.warn("Attempted to call close on the scripting per-request resource resolver. This is handled automatically by the {}.",
                ScriptingResourceResolverProvider.class.getName());
        if (shouldLogClosing) {
            StringWriter writer = new StringWriter();
            Throwable t = new Throwable();
            t.printStackTrace(new PrintWriter(writer));
            LOGGER.warn("The following code attempted to close the per-request resource resolver: {}", writer.toString());
        }
    }

    void _close() {
        delegate.close();
    }

    @Override
    public String getUserID() {
        return delegate.getUserID();
    }

    @Nonnull
    @Override
    public Iterator<String> getAttributeNames() {
        return delegate.getAttributeNames();
    }

    @Override
    public Object getAttribute(@Nonnull String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public void delete(@Nonnull Resource resource) throws PersistenceException {
        delegate.delete(resource);
    }

    @Nonnull
    @Override
    public Resource create(@Nonnull Resource parent, @Nonnull String name, Map<String, Object> properties) throws PersistenceException {
        return delegate.create(parent, name, properties);
    }

    @Override
    public void revert() {
        delegate.revert();
    }

    @Override
    public void commit() throws PersistenceException {
        delegate.commit();
    }

    @Override
    public boolean hasChanges() {
        return delegate.hasChanges();
    }

    @Override
    public String getParentResourceType(Resource resource) {
        return delegate.getParentResourceType(resource);
    }

    @Override
    public String getParentResourceType(String resourceType) {
        return delegate.getParentResourceType(resourceType);
    }

    @Override
    public boolean isResourceType(Resource resource, String resourceType) {
        return delegate.isResourceType(resource, resourceType);
    }

    @Override
    public void refresh() {
        delegate.refresh();
    }

    @Override
    public Resource copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return delegate.copy(srcAbsPath, destAbsPath);
    }

    @Override
    public Resource move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return delegate.move(srcAbsPath, destAbsPath);
    }

    @Override
    public <AdapterType> AdapterType adaptTo(@Nonnull Class<AdapterType> type) {
        return delegate.adaptTo(type);
    }
}
