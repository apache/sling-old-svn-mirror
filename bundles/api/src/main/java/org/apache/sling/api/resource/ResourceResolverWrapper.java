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
package org.apache.sling.api.resource;

import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * The {@code ResourceResolverWrapper} is a wrapper for any {@code ResourceResolver}, delegating all method calls to the wrapped resource
 * resolver by default. Extensions of this class may overwrite any method to return different values as appropriate.
 */
@ConsumerType
public class ResourceResolverWrapper implements ResourceResolver {

    private ResourceResolver wrapped;

    public ResourceResolverWrapper(ResourceResolver resolver) {
        wrapped = resolver;
    }

    @Nonnull
    @Override
    public Resource resolve(@Nonnull HttpServletRequest request, @Nonnull String absPath) {
        return wrapped.resolve(request, absPath);
    }

    @Nonnull
    @Override
    public Resource resolve(@Nonnull String absPath) {
        return wrapped.resolve(absPath);
    }

    @Nonnull
    @Override
    public Resource resolve(@Nonnull HttpServletRequest request) {
        return wrapped.resolve(request);
    }

    @Nonnull
    @Override
    public String map(@Nonnull String resourcePath) {
        return wrapped.map(resourcePath);
    }

    @Override
    public String map(@Nonnull HttpServletRequest request, @Nonnull String resourcePath) {
        return wrapped.map(request, resourcePath);
    }

    @Override
    public Resource getResource(@Nonnull String path) {
        return wrapped.getResource(path);
    }

    @Override
    public Resource getResource(Resource base, @Nonnull String path) {
        return wrapped.getResource(base, path);
    }

    @Nonnull
    @Override
    public String[] getSearchPath() {
        return wrapped.getSearchPath();
    }

    @Nonnull
    @Override
    public Iterator<Resource> listChildren(@Nonnull Resource parent) {
        return wrapped.listChildren(parent);
    }

    @Override
    public Resource getParent(@Nonnull Resource child) {
        return wrapped.getParent(child);
    }

    @Nonnull
    @Override
    public Iterable<Resource> getChildren(@Nonnull Resource parent) {
        return wrapped.getChildren(parent);
    }

    @Nonnull
    @Override
    public Iterator<Resource> findResources(@Nonnull String query, String language) {
        return wrapped.findResources(query, language);
    }

    @Nonnull
    @Override
    public Iterator<Map<String, Object>> queryResources(@Nonnull String query, String language) {
        return wrapped.queryResources(query, language);
    }

    @Override
    public boolean hasChildren(@Nonnull Resource resource) {
        return wrapped.hasChildren(resource);
    }

    @Nonnull
    @Override
    public ResourceResolver clone(Map<String, Object> authenticationInfo) throws LoginException {
        return wrapped.clone(authenticationInfo);
    }

    @Override
    public boolean isLive() {
        return wrapped.isLive();
    }

    @Override
    public void close() {
        wrapped.close();
    }

    @Override
    public String getUserID() {
        return wrapped.getUserID();
    }

    @Nonnull
    @Override
    public Iterator<String> getAttributeNames() {
        return wrapped.getAttributeNames();
    }

    @Override
    public Object getAttribute(@Nonnull String name) {
        return wrapped.getAttribute(name);
    }

    @Override
    public void delete(@Nonnull Resource resource) throws PersistenceException {
        wrapped.delete(resource);
    }

    @Nonnull
    @Override
    public Resource create(@Nonnull Resource parent, @Nonnull String name, Map<String, Object> properties) throws PersistenceException {
        return wrapped.create(parent, name, properties);
    }

    @Override
    public void revert() {
        wrapped.revert();
    }

    @Override
    public void commit() throws PersistenceException {
        wrapped.commit();
    }

    @Override
    public boolean hasChanges() {
        return wrapped.hasChanges();
    }

    @Override
    public String getParentResourceType(Resource resource) {
        return wrapped.getParentResourceType(resource);
    }

    @Override
    public String getParentResourceType(String resourceType) {
        return wrapped.getParentResourceType(resourceType);
    }

    @Override
    public boolean isResourceType(Resource resource, String resourceType) {
        return wrapped.isResourceType(resource, resourceType);
    }

    @Override
    public void refresh() {
        wrapped.refresh();
    }

    @Override
    public Resource copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return wrapped.copy(srcAbsPath, destAbsPath);
    }

    @Override
    public Resource move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return wrapped.move(srcAbsPath, destAbsPath);
    }

    @Override
    public <AdapterType> AdapterType adaptTo(@Nonnull Class<AdapterType> type) {
        return wrapped.adaptTo(type);
    }
}
