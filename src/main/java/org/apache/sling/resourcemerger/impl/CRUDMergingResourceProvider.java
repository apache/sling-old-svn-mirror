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
package org.apache.sling.resourcemerger.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker;
import org.apache.sling.spi.resource.provider.ResolveContext;

/**
 * This is a modifiable resource provider.
 */
public class CRUDMergingResourceProvider
    extends MergingResourceProvider {

    public CRUDMergingResourceProvider(final String mergeRootPath,
            final MergedResourcePicker picker,
            final boolean traverseHierarchie) {
        super(mergeRootPath, picker, false, traverseHierarchie);
    }

    private static final class ExtendedResourceHolder {
        public final String name;
        public final List<Resource> resources = new ArrayList<Resource>();
        public int count;
        public String highestResourcePath;

        public ExtendedResourceHolder(final String n) {
            this.name = n;
        }
    }
    private ExtendedResourceHolder getAllResources(final ResourceResolver resolver,
            final String path,
            final String relativePath) {
        final ExtendedResourceHolder holder = new ExtendedResourceHolder(ResourceUtil.getName(path));

        holder.count = 0;

        // Loop over resources
        boolean isUnderlying = true;
        final Iterator<Resource> iter = this.picker.pickResources(resolver, relativePath).iterator();
        while ( iter.hasNext() ) {
            final Resource rsrc = iter.next();
            holder.count++;
            holder.highestResourcePath = rsrc.getPath();

            final boolean hidden;
            if (isUnderlying) {
                isUnderlying = false;
                hidden = false;
            } else {
                // check parent for hiding
                // SLING 3521 : if parent is not readable, nothing is hidden
                final Resource parent = rsrc.getParent();
                hidden = (parent == null ? false : new ParentHidingHandler(parent, this.traverseHierarchie).isHidden(holder.name));
            }
            if (hidden) {
                holder.resources.clear();
            } else if (!ResourceUtil.isNonExistingResource(rsrc)) {
                holder.resources.add(rsrc);
            }
        }

        return holder;
    }

    @Override
    public Resource create(final ResolveContext<Void> ctx, final String path, final Map<String, Object> properties) throws PersistenceException {
        final ResourceResolver resolver = ctx.getResourceResolver();

        // check if the resource exists
        final Resource mountResource = this.getResource(ctx, path, null);
        if ( mountResource != null ) {
            throw new PersistenceException("Resource at " + path + " already exists.", null, path, null);
        }
        // creation of the root mount resource is not supported
        final String relativePath = getRelativePath(path);
        if ( relativePath == null || relativePath.length() == 0 ) {
            throw new PersistenceException("Resource at " + path + " can't be created.", null, path, null);
        }

        final ExtendedResourceHolder holder = this.getAllResources(resolver, path, relativePath);
        // we only support modifications if there is more than one location merged
        if ( holder.count < 2 ) {
            throw new PersistenceException("Modifying is only supported with at least two potentially merged resources.", null, path, null);
        }
        if ( holder.resources.size() == 0
             || (holder.resources.size() < holder.count && !holder.resources.get(holder.resources.size() - 1).getPath().equals(holder.highestResourcePath) )) {
            final String createPath = holder.highestResourcePath;
            final Resource parentResource = ResourceUtil.getOrCreateResource(resolver, ResourceUtil.getParent(createPath), (String)null, null, false);
            resolver.create(parentResource, ResourceUtil.getName(createPath), properties);
        } else {
            final Resource hidingResource = resolver.getResource(holder.highestResourcePath);
            if ( hidingResource != null ) {
                final ModifiableValueMap mvm = hidingResource.adaptTo(ModifiableValueMap.class);
                mvm.remove(MergedResourceConstants.PN_HIDE_RESOURCE);
                mvm.putAll(properties);
            }
            // TODO check parent hiding
        }
        return this.getResource(ctx, path, null);
    }

    @Override
    public void delete(final ResolveContext<Void> ctx, final Resource resource) throws PersistenceException {
        final ResourceResolver resolver = ctx.getResourceResolver();
        final String path = resource.getPath();

        // deleting of the root mount resource is not supported
        final String relativePath = getRelativePath(path);
        if ( relativePath == null || relativePath.length() == 0 ) {
            throw new PersistenceException("Resource at " + path + " can't be deleted.", null, path, null);
        }

        final ExtendedResourceHolder holder = this.getAllResources(resolver, path, relativePath);
        // we only support modifications if there is more than one location merged
        if ( holder.count < 2 ) {
            throw new PersistenceException("Modifying is only supported with at least two potentially merged resources.", null, path, null);
        }

        if ( holder.resources.size() == 1 && holder.resources.get(0).getPath().equals(holder.highestResourcePath) ) {
            // delete the only resource which is the highest one
            resolver.delete(holder.resources.get(0));
        } else {
            // create overlay resource which is hiding the other
            final String createPath = holder.highestResourcePath;
            final Resource parentResource = ResourceUtil.getOrCreateResource(resolver, ResourceUtil.getParent(createPath), (String)null, null, false);
            final Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(MergedResourceConstants.PN_HIDE_RESOURCE, Boolean.TRUE);
            resolver.create(parentResource, ResourceUtil.getName(createPath), properties);
        }
    }

    @Override
    public void revert(final ResolveContext<Void> ctx) {
        // the provider for the merged resources will revert
    }

    @Override
    public void commit(final ResolveContext<Void> ctx) throws PersistenceException {
        // the provider for the merged resources will commit
    }

    @Override
    public boolean hasChanges(final ResolveContext<Void> ctx) {
        // the provider for the merged resources will return changes
        return false;
    }

}
