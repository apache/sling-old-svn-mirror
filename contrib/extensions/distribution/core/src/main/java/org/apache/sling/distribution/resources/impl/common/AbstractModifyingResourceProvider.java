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
package org.apache.sling.distribution.resources.impl.common;

import javax.jcr.Session;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * a modifying {@link org.apache.sling.api.resource.ResourceProvider} for distribution.
 */
public abstract class AbstractModifyingResourceProvider extends AbstractReadableResourceProvider
        implements ModifyingResourceProvider {

    private final Map<String, Map<String, Object>> changedResources = new HashMap<String, Map<String, Object>>();
    private final Set<String> deletedResources = new HashSet<String>();

    protected AbstractModifyingResourceProvider(String resourceRoot) {
        super(resourceRoot);

    }

    public Resource create(ResourceResolver resolver, String path, Map<String, Object> properties) throws PersistenceException {
        SimplePathInfo pathInfo = extractPathInfo(path);

        if (pathInfo == null) {
            throw new PersistenceException("Invalid path: " + path, null);
        }

        if (!hasPermission(resolver, pathInfo.getResourcePath(), Session.ACTION_ADD_NODE)) {
            throw new PersistenceException("Not enough permissions");
        }

        String resourceName = pathInfo.getMainResourceName();

        boolean added = addToChangedResources(resolver, resourceName, properties, true);

        if (!added) {
            throw new PersistenceException("Resource already exists at " + path, null, resourceName, null);
        }

        return buildMainResource(resolver, pathInfo, properties);
    }

    public void delete(ResourceResolver resolver, String requestPath) throws PersistenceException {
        SimplePathInfo pathInfo = extractPathInfo(requestPath);

        if (pathInfo == null) {
            throw new PersistenceException("Invalid path: " + requestPath, null);
        }

        if (!hasPermission(resolver, pathInfo.getResourcePath(), Session.ACTION_REMOVE)) {
            throw new PersistenceException("Not enough permissions");
        }

        String resourceName = pathInfo.getMainResourceName();

        if (!deletedResources.contains(resourceName)) {
            deletedResources.add(resourceName);
            changedResources.remove(resourceName);
        }
    }

    public void revert(ResourceResolver resolver) {
        reset();
    }

    public void commit(ResourceResolver resolver) throws PersistenceException {
        if (!hasChanges(resolver)) {
            return;
        }

        saveInternalResources(resolver, changedResources, deletedResources);

        reset();
        resolver.commit();
    }

    public boolean hasChanges(ResourceResolver resolver) {
        return changedResources.size() > 0 || deletedResources.size() > 0;
    }

    public void change(ResourceResolver resourceResolver, String requestPath, Map<String, Object> properties) {
        SimplePathInfo pathInfo = extractPathInfo(requestPath);

        if (pathInfo == null) {
            return;
        }

        if (!hasPermission(resourceResolver, pathInfo.getResourcePath(), Session.ACTION_SET_PROPERTY)) {
            return;
        }

        String resourceName = pathInfo.getMainResourceName();

        addToChangedResources(resourceResolver, resourceName, properties, false);
    }

    private boolean addToChangedResources(ResourceResolver resolver, String resourceName, Map<String, Object> newProperties, boolean failIfAlreadyExists) {
        final boolean deleted = this.deletedResources.remove(resourceName);

        SimplePathInfo pathInfo = SimplePathInfo.parsePathInfo(resourceRoot, resourceRoot + "/" + resourceName);

        Map<String, Object> existingResource = getResourceProperties(resolver, pathInfo);

        if (failIfAlreadyExists && !deleted && existingResource != null) {
            return false;
        }

        Map<String, Object> properties = new HashMap<String, Object>();
        if (existingResource != null) {
            properties.putAll(existingResource);
        }

        if (newProperties != null) {
            properties.putAll(newProperties);
        }

        this.changedResources.put(resourceName, properties);

        return true;
    }


    @Override
    protected Map<String, Object> getResourceProperties(ResourceResolver resolver, SimplePathInfo pathInfo) {

        if (pathInfo.isMain()) {
            String resourceName = pathInfo.getMainResourceName();
            if (deletedResources.contains(resourceName)) {
                return null;
            }

            if (changedResources.containsKey(resourceName)) {
                return changedResources.get(resourceName);
            }
        }

        return super.getResourceProperties(resolver, pathInfo);
    }

    @Override
    Resource buildMainResource(ResourceResolver resourceResolver,
                               SimplePathInfo pathInfo,
                               Map<String, Object> properties,
                               Object... adapters) {
        return new SimpleModifiableResource(resourceResolver, this, pathInfo.getResourcePath(), properties);
    }

    private void reset() {
        changedResources.clear();
        deletedResources.clear();
    }


    protected abstract void saveInternalResources(ResourceResolver resourceResolver,
                                                  Map<String, Map<String, Object>> changedResources,
                                                  Set<String> deletedResources) throws PersistenceException;


}
