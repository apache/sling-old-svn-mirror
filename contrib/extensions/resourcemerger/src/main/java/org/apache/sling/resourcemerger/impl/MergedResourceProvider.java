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

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;

/**
 * The <code>MergedResourceProvider</code> is the resource provider providing
 * access to {@link MergedResource} objects.
 */
public class MergedResourceProvider
    implements ResourceProvider, ModifyingResourceProvider {

    private final String mergeRootPath;

    public MergedResourceProvider(final String mergeRootPath) {
        this.mergeRootPath = mergeRootPath;
    }

    /**
     * {@inheritDoc}
     */
    public Resource getResource(final ResourceResolver resolver, final HttpServletRequest request, final String path) {
        return getResource(resolver, path);
    }

    private static final class ExcludeEntry {

        public final String name;
        public final boolean exclude;

        public ExcludeEntry(final String value) {
            if ( value.startsWith("!!") ) {
                this.name = value.substring(1);
                this.exclude = false;
            } else if ( value.startsWith("!") ) {
                this.name = value.substring(1);
                this.exclude = true;
            } else {
                this.name = value;
                this.exclude = false;
            }
        }
    }

    private static final class ParentHidingHandler {

        private final List<ExcludeEntry> entries = new ArrayList<MergedResourceProvider.ExcludeEntry>();

        public ParentHidingHandler(final Resource parent) {
            final ValueMap parentProps = ResourceUtil.getValueMap(parent);
            final String[] childrenToHideArray = parentProps.get(MergedResourceConstants.PN_HIDE_CHILDREN, String[].class);
            if ( childrenToHideArray != null ) {
                for(final String value : childrenToHideArray) {
                    final ExcludeEntry entry = new ExcludeEntry(value);
                    this.entries.add(entry);
                }
            }
        }

        public boolean isHidden(final String name) {
            boolean hidden = false;
            for(final ExcludeEntry entry : this.entries) {
                if ( entry.name.equals("*") || entry.name.equals(name) ) {
                    hidden = !entry.exclude;
                    break;
                }
            }
            return hidden;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Resource getResource(final ResourceResolver resolver, final String path) {
        final String relativePath = getRelativePath(path);

        if ( relativePath != null ) {
            final ResourceHolder holder = new ResourceHolder(ResourceUtil.getName(path));

            // Loop over provided base paths, start with least import
            final String[] searchPaths = resolver.getSearchPath();
            for(int i=searchPaths.length-1; i >= 0; i--) {
                final String basePath = searchPaths[i];

                // Try to get the corresponding physical resource for this base path
                final String fullPath = basePath + relativePath;

                // check parent for hiding
                final Resource parent = resolver.getResource(ResourceUtil.getParent(fullPath));
                if ( parent != null ) {
                    final boolean hidden = new ParentHidingHandler(parent).isHidden(holder.name);
                    if ( hidden ) {
                        holder.resources.clear();
                    } else {
                        final Resource baseRes = resolver.getResource(fullPath);
                        if (baseRes != null) {
                            holder.resources.add(baseRes);
                        }
                    }
                }
            }
            return createMergedResource(resolver, relativePath, holder);
        }

        return null;
    }

    private static final class ResourceHolder {
        public final String name;
        public final List<Resource> resources = new ArrayList<Resource>();
        public final List<ValueMap> valueMaps = new ArrayList<ValueMap>();

        public ResourceHolder(final String n) {
            this.name = n;
        }
    }

    /**
     * Create the merged resource based on the provided resources
     */
    private Resource createMergedResource(final ResourceResolver resolver,
            final String relativePath,
            final ResourceHolder holder) {
        int index = 0;
        while ( index < holder.resources.size() ) {
            final Resource baseRes = holder.resources.get(index);
            // check if resource is hidden
            final ValueMap props = ResourceUtil.getValueMap(baseRes);
            holder.valueMaps.add(props);
            if ( props.get(MergedResourceConstants.PN_HIDE_RESOURCE, Boolean.FALSE) ) {
                // clear everything up to now
                for(int i=0;i<=index;i++) {
                    holder.resources.remove(0);
                }
                holder.valueMaps.clear();
                index = 0; // start at zero
            } else {
                index++;
            }
        }

        if (!holder.resources.isEmpty()) {
            // create a new merged resource based on the list of mapped physical resources
            return new MergedResource(resolver, mergeRootPath, relativePath, holder.resources, holder.valueMaps, this.mergeRootPath);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Resource> listChildren(final Resource resource) {
        final ResourceResolver resolver = resource.getResourceResolver();

        final String relativePath = getRelativePath(resource.getPath());

        if ( relativePath != null ) {
            final List<ResourceHolder> candidates = new ArrayList<ResourceHolder>();

            // Loop over provided base paths, start with least import
            final String[] searchPaths = resolver.getSearchPath();
            for(int i=searchPaths.length-1; i >= 0; i--) {
                final String basePath = searchPaths[i];
                final Resource parentResource = resolver.getResource(basePath + relativePath);
                if ( parentResource != null ) {
                    final ParentHidingHandler handler = new ParentHidingHandler(parentResource);
                    for(final Resource child : parentResource.getChildren()) {
                        final String rsrcName = child.getName();
                        ResourceHolder holder = null;
                        for(final ResourceHolder current : candidates) {
                            if ( current.name.equals(rsrcName) ) {
                                holder = current;
                                break;
                            }
                        }
                        if ( holder == null ) {
                            holder = new ResourceHolder(rsrcName);
                            candidates.add(holder);
                        }
                        holder.resources.add(child);

                        // Check if children need reordering
                        int orderBeforeIndex = -1;
                        final ValueMap vm = ResourceUtil.getValueMap(child);
                        final String orderBefore = vm.get(MergedResourceConstants.PN_ORDER_BEFORE, String.class);
                        if (orderBefore != null && !orderBefore.equals(rsrcName)) {
                            // search entry
                            int index = 0;
                            while (index < candidates.size()) {
                                final ResourceHolder current = candidates.get(index);
                                if ( current.name.equals(orderBefore) ) {
                                    orderBeforeIndex = index;
                                    break;
                                }
                                index++;
                            }
                        }

                        if (orderBeforeIndex > -1) {
                            candidates.add(orderBeforeIndex, holder);
                            candidates.remove(candidates.size() - 1);
                        }
                    }
                    final Iterator<ResourceHolder> iter = candidates.iterator();
                    while ( iter.hasNext() ) {
                        final ResourceHolder holder = iter.next();
                        if ( handler.isHidden(holder.name) ) {
                            iter.remove();
                        }
                    }
                }
            }
            final List<Resource> children = new ArrayList<Resource>();
            for(final ResourceHolder holder : candidates) {
                final Resource mergedResource = this.createMergedResource(resolver, (relativePath.length() == 0 ? holder.name : relativePath + '/' + holder.name), holder);
                if ( mergedResource != null ) {
                    children.add(mergedResource);
                }
            }
            return children.iterator();
        }

        return null;
    }

    /**
     * Gets the relative path out of merge root path
     *
     * @param path Absolute path
     * @return Relative path
     */
    private String getRelativePath(String path) {
        if ( path.startsWith(mergeRootPath) ) {
            path = path.substring(mergeRootPath.length());
            if ( path.length() == 0 ) {
                return path;
            } else if ( path.charAt(0) == '/' ) {
                return path.substring(1);
            }
        }
        return null;
    }

    private ResourceHolder getAllResources(final ResourceResolver resolver,
            final String path,
            final String relativePath) {
        final ResourceHolder holder = new ResourceHolder(ResourceUtil.getName(path));

        // Loop over provided base paths, start with least import
        final String[] searchPaths = resolver.getSearchPath();
        for(int i=searchPaths.length-1; i >= 0; i--) {
            final String basePath = searchPaths[i];

            // Try to get the corresponding physical resource for this base path
            final String fullPath = basePath + relativePath;

            // check parent for hiding
            final Resource parent = resolver.getResource(ResourceUtil.getParent(fullPath));
            if ( parent != null ) {
                final boolean hidden = new ParentHidingHandler(parent).isHidden(holder.name);
                if ( hidden ) {
                    holder.resources.clear();
                } else {
                    final Resource baseRes = resolver.getResource(fullPath);
                    if (baseRes != null) {
                        holder.resources.add(baseRes);
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
        // we only support modifications if there is more than one search path
        final String[] searchPaths = resolver.getSearchPath();
        if ( searchPaths.length < 2 ) {
            throw new PersistenceException("Modifying is only supported with at least two search paths", null, path, null);
        }
        // check if the resource exists
        final Resource mountResource = this.getResource(resolver, path);
        if ( mountResource != null ) {
            throw new PersistenceException("Resource at " + path + " already exists.", null, path, null);
        }
        // creating of the root mount resource is not supported
        final String relativePath = getRelativePath(path);
        if ( relativePath == null || relativePath.length() == 0 ) {
            throw new PersistenceException("Resource at " + path + " can't be created.", null, path, null);
        }

        final String lastSearchPath = searchPaths[searchPaths.length-1];
        final ResourceHolder holder = this.getAllResources(resolver, path, relativePath);
        if ( holder.resources.size() == 0 || holder.resources.size() == 1 && holder.resources.get(0).getPath().startsWith(lastSearchPath) ) {
            final String useSearchPath = searchPaths[searchPaths.length-2];

            final String createPath = useSearchPath + path.substring(this.mergeRootPath.length() + 1);
            final Resource parentResource = ResourceUtil.getOrCreateResource(resolver, ResourceUtil.getParent(createPath), (String)null, null, false);
            resolver.create(parentResource, ResourceUtil.getName(createPath), properties);
        }
        // TODO check hiding flag
        return this.getResource(resolver, path);
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#delete(org.apache.sling.api.resource.ResourceResolver, java.lang.String)
     */
    public void delete(final ResourceResolver resolver, final String path)
    throws PersistenceException {
        // we only support modifications if there is more than one search path
        final String[] searchPaths = resolver.getSearchPath();
        if ( searchPaths.length < 2 ) {
            throw new PersistenceException("Modifying is only supported with at least two search paths");
        }
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
        final ResourceHolder holder = this.getAllResources(resolver, path, relativePath);
        final String lastSearchPath = searchPaths[searchPaths.length-1];

        int deleted = 0;
        for(final Resource rsrc : holder.resources) {
            final String p = rsrc.getPath();
            if ( !p.startsWith(lastSearchPath) ) {
                resolver.delete(rsrc);
                deleted++;
            }
        }
        if ( deleted < holder.resources.size() ) {
            // create overlay resource which is hiding the other
            final String prefix = searchPaths[searchPaths.length-2];
            final String createPath = prefix + path.substring(this.mergeRootPath.length() + 1);
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
        // the provider for the search paths will revert
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#commit(org.apache.sling.api.resource.ResourceResolver)
     */
    public void commit(final ResourceResolver resolver) throws PersistenceException {
        // the provider for the search paths will commit
    }

    /**
     * @see org.apache.sling.api.resource.ModifyingResourceProvider#hasChanges(org.apache.sling.api.resource.ResourceResolver)
     */
    public boolean hasChanges(final ResourceResolver resolver) {
        // the provider for the search paths will return in case of changes
        return false;
    }
}
