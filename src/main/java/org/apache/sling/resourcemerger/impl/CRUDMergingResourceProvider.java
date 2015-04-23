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
import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker;

/**
 * This is a modifiable resource provider.
 */
public class CRUDMergingResourceProvider
    extends MergingResourceProvider
    implements ModifyingResourceProvider {

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
        final Iterator<Resource> iter = this.picker.pickResources(resolver, relativePath).iterator();
        while ( iter.hasNext() ) {
            final Resource rsrc = iter.next();
            holder.count++;
            holder.highestResourcePath = rsrc.getPath();
            if ( !ResourceUtil.isNonExistingResource(rsrc) ) {
                // check parent for hiding
                final Resource parent = rsrc.getParent();
                if ( parent != null ) {
                    final boolean hidden = new ParentHidingHandler(parent, this.traverseHierarchie).isHidden(holder.name);
                    if ( hidden ) {
                        holder.resources.clear();
                    } else {
                        holder.resources.add(rsrc);
                    }
                }
            }
        }

        return holder;
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#create(org.apache.sling.api.resource.ResourceResolver, java.lang.String, java.util.Map)
     */
    public Resource create(final ResourceResolver resolver,
            final String path,
            final Map<String, Object> properties)
    throws PersistenceException {
        // check if the resource exists
        final Resource mountResource = this.getResource(resolver, path);
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
        return this.getResource(resolver, path);
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#delete(org.apache.sling.api.resource.ResourceResolver, java.lang.String)
     */
    public void delete(final ResourceResolver resolver, final String path)
    throws PersistenceException {
        // deleting of the root mount resource is not supported
        final String relativePath = getRelativePath(path);
        if ( relativePath == null || relativePath.length() == 0 ) {
            throw new PersistenceException("Resource at " + path + " can't be created.", null, path, null);
        }

        // check if the resource exists
        final Resource mntResource = this.getResource(resolver, path);
        if ( mntResource == null ) {
            throw new PersistenceException("Resource at " + path + " does not exist", null, path, null);
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

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#revert(org.apache.sling.api.resource.ResourceResolver)
     */
    public void revert(final ResourceResolver resolver) {
        // the provider for the merged resources will revert
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#commit(org.apache.sling.api.resource.ResourceResolver)
     */
    public void commit(final ResourceResolver resolver) throws PersistenceException {
        // the provider for the merged resources will commit
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#hasChanges(org.apache.sling.api.resource.ResourceResolver)
     */
    public boolean hasChanges(final ResourceResolver resolver) {
        // the provider for the merged resources will return changes
        return false;
    }
}
