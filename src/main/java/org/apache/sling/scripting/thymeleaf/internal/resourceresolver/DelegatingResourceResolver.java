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
package org.apache.sling.scripting.thymeleaf.internal.resourceresolver;

import java.util.Iterator;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelegatingResourceResolver implements ResourceResolver {

    private final ResourceResolver resourceResolver;

    private final Logger logger = LoggerFactory.getLogger(DelegatingResourceResolver.class);

    public DelegatingResourceResolver(final ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    void closeInternal() {
        resourceResolver.close();
    }

    @Override
    @Nonnull
    public Resource resolve(@Nonnull HttpServletRequest httpServletRequest,@Nonnull String s) {
        return resourceResolver.resolve(httpServletRequest, s);
    }

    @Override
    @Nonnull
    public Resource resolve(@Nonnull String s) {
        return resourceResolver.resolve(s);
    }

    @Override
    @Deprecated
    @Nonnull
    public Resource resolve(@Nonnull HttpServletRequest httpServletRequest) {
        return resourceResolver.resolve(httpServletRequest);
    }

    @Override
    @Nonnull
    public String map(@Nonnull String s) {
        return resourceResolver.map(s);
    }

    @Override
    @CheckForNull
    public String map(@Nonnull HttpServletRequest httpServletRequest, @Nonnull String s) {
        return resourceResolver.map(httpServletRequest, s);
    }

    @Override
    @CheckForNull
    public Resource getResource(@Nonnull String s) {
        return resourceResolver.getResource(s);
    }

    @Override
    @CheckForNull
    public Resource getResource(Resource resource, @Nonnull String s) {
        return resourceResolver.getResource(resource, s);
    }

    @Override
    @Nonnull
    public String[] getSearchPath() {
        return resourceResolver.getSearchPath();
    }

    @Override
    @Nonnull
    public Iterator<Resource> listChildren(@Nonnull Resource resource) {
        return resourceResolver.listChildren(resource);
    }

    @Override
    @CheckForNull
    public Resource getParent(@Nonnull Resource resource) {
        return resourceResolver.getParent(resource);
    }

    @Override
    @Nonnull
    public Iterable<Resource> getChildren(@Nonnull Resource resource) {
        return resourceResolver.getChildren(resource);
    }

    @Override
    @Nonnull
    public Iterator<Resource> findResources(@Nonnull String s, String s1) {
        return resourceResolver.findResources(s, s1);
    }

    @Override
    @Nonnull
    public Iterator<Map<String, Object>> queryResources(@Nonnull String s, String s1) {
        return resourceResolver.queryResources(s, s1);
    }

    @Override
    public boolean hasChildren(@Nonnull Resource resource) {
        return resourceResolver.hasChildren(resource);
    }

    @Override
    @Nonnull
    public ResourceResolver clone(Map<String, Object> map) throws LoginException {
        return resourceResolver.clone(map);
    }

    @Override
    public boolean isLive() {
        return resourceResolver.isLive();
    }

    @Override
    public void close() {
        // do not close
    }

    @Override
    @CheckForNull
    public String getUserID() {
        return resourceResolver.getUserID();
    }

    @Override
    @Nonnull
    public Iterator<String> getAttributeNames() {
        return resourceResolver.getAttributeNames();
    }

    @Override
    @CheckForNull
    public Object getAttribute(@Nonnull String s) {
        return resourceResolver.getAttribute(s);
    }

    @Override
    public void delete(@Nonnull Resource resource) throws PersistenceException {
        resourceResolver.delete(resource);
    }

    @Override
    @Nonnull
    public Resource create(@Nonnull Resource resource, @Nonnull String s, Map<String, Object> map) throws PersistenceException {
        return resourceResolver.create(resource, s, map);
    }

    @Override
    public void revert() {
        resourceResolver.revert();
    }

    @Override
    public void commit() throws PersistenceException {
        resourceResolver.commit();
    }

    @Override
    public boolean hasChanges() {
        return resourceResolver.hasChanges();
    }

    @Override
    @CheckForNull
    public String getParentResourceType(Resource resource) {
        return resourceResolver.getParentResourceType(resource);
    }

    @Override
    @CheckForNull
    public String getParentResourceType(String s) {
        return resourceResolver.getParentResourceType(s);
    }

    @Override
    public boolean isResourceType(Resource resource, String s) {
        return resourceResolver.isResourceType(resource, s);
    }

    @Override
    public void refresh() {
        resourceResolver.refresh();
    }

    @Override
    public Resource copy(String s, String s1) throws PersistenceException {
        return resourceResolver.copy(s, s1);
    }

    @Override
    public Resource move(String s, String s1) throws PersistenceException {
        return resourceResolver.move(s, s1);
    }

    @Override
    @CheckForNull
    public <AdapterType> AdapterType adaptTo(@Nonnull Class<AdapterType> aClass) {
        return resourceResolver.adaptTo(aClass);
    }

}
