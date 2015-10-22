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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MergingResourceProvider extends ResourceProvider<Void> {

    protected final String mergeRootPath;

    protected final MergedResourcePicker picker;

    private final boolean readOnly;

    protected final boolean traverseHierarchie;

    MergingResourceProvider(final String mergeRootPath,
            final MergedResourcePicker picker,
            final boolean readOnly,
            final boolean traverseHierarchie) {
        this.mergeRootPath = mergeRootPath;
        this.picker = picker;
        this.readOnly = readOnly;
        this.traverseHierarchie = traverseHierarchie;
    }

    protected static final class ExcludeEntry {

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

    /**
     * Class to check whether a child resource must be hidden. It should not be instantiated for the underlying resource
     * tree (which is /libs by default) because this check is expensive.
     */
    protected static final class ParentHidingHandler {

        private List<ExcludeEntry> entries = new ArrayList<ExcludeEntry>();

        public ParentHidingHandler(final Resource parent, final boolean traverseParent) {
            final ValueMap parentProps = parent.getValueMap();
            final String[] childrenToHideArray = parentProps.get(MergedResourceConstants.PN_HIDE_CHILDREN, String[].class);
            if (childrenToHideArray != null) {
                for (final String value : childrenToHideArray) {
                    final ExcludeEntry entry = new ExcludeEntry(value);
                    this.entries.add(entry);
                }
            }
            if (parent != null) {
                Resource ancestor = parent.getParent();
                String previousAncestorName = parent.getName();
                while (ancestor != null) {
                    final ValueMap ancestorProps = ancestor.getValueMap();
                    final String[] ancestorChildrenToHideArray = ancestorProps.get(MergedResourceConstants.PN_HIDE_CHILDREN, String[].class);
                    if (ancestorChildrenToHideArray != null) {
                        for (final String value : ancestorChildrenToHideArray) {
                            final ExcludeEntry entry = new ExcludeEntry(value);
                            final Boolean hides = hides(entry, previousAncestorName);
                            if (hides != null && hides.booleanValue() == true) {
                                this.entries.add(new ExcludeEntry("*"));
                                break;
                            }
                        }
                    }
                    if ( !traverseParent ) {
                        break;
                    }
                    previousAncestorName = ancestor.getName();
                    ancestor = ancestor.getParent();
                }
            }
        }

        public boolean isHidden(final String name) {
            boolean hidden = false;
            if ( this.entries != null ) {
                for(final ExcludeEntry entry : this.entries) {
                    Boolean result = hides(entry, name);
                    if (result != null) {
                        hidden = result.booleanValue();
                        break;
                    }
                }
            }
            return hidden;
        }

        /**
         * Determine if an entry should hide the named resource.
         *
         * @return a non-null value if the entry matches; a null value if it does not
         */
        private Boolean hides(final ExcludeEntry entry, final String name) {
            Boolean result = null;
            if (entry.name.equals("*") || entry.name.equals(name)) {
                result = Boolean.valueOf(!entry.exclude);
            }
            return result;
        }

    }

    protected static final class ResourceHolder {
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
    private Resource createMergedResource(final ResourceResolver resolver, final String relativePath,
            final ResourceHolder holder) {
        int index = 0;
        while (index < holder.resources.size()) {
            final Resource baseRes = holder.resources.get(index);
            // check if resource is hidden
            final ValueMap props = baseRes.getValueMap();
            holder.valueMaps.add(props);
            if (props.get(MergedResourceConstants.PN_HIDE_RESOURCE, Boolean.FALSE)) {
                // clear everything up to now
                for (int i = 0; i <= index; i++) {
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
            if ( this.readOnly ) {
                return new MergedResource(resolver, mergeRootPath, relativePath, holder.resources, holder.valueMaps);
            }
            return new CRUDMergedResource(resolver, mergeRootPath, relativePath, holder.resources, holder.valueMaps, this.picker);
        }
        return null;
    }

    /**
     * Gets the relative path out of merge root path
     *
     * @param path Absolute path
     * @return Relative path
     */
    protected String getRelativePath(String path) {
        if (path.startsWith(mergeRootPath)) {
            path = path.substring(mergeRootPath.length());
            if (path.length() == 0) {
                return path;
            } else if (path.charAt(0) == '/') {
                return path.substring(1);
            }
        }
        return null;
    }

    @Override
    public Resource getParent(ResolveContext<Void> ctx, Resource child) {
        final String parentPath = ResourceUtil.getParent(child.getPath());
        if (parentPath == null) {
            return null;
        }
        return this.getResource(ctx, parentPath, child);
    }

    /**
     * {@inheritDoc}
     */
    public Resource getResource(final ResolveContext<Void> ctx, final String path, final Resource parent) {
        final String relativePath = getRelativePath(path);

        if (relativePath != null) {
            final ResourceHolder holder = new ResourceHolder(ResourceUtil.getName(path));

            final ResourceResolver resolver = ctx.getResourceResolver();
            final Iterator<Resource> resources = picker.pickResources(resolver, relativePath, parent).iterator();

            if (!resources.hasNext()) {
                return null;
            }

            boolean isUnderlying = true;
            while (resources.hasNext()) {
                final Resource resource = resources.next();

                final boolean hidden;
                if (isUnderlying) {
                    hidden = false;
                    isUnderlying = false;
                } else {
                    // check parent for hiding
                    // SLING 3521 : if parent is not readable, nothing is hidden
                    final Resource resourceParent = resource.getParent();
                    hidden = resourceParent != null && new ParentHidingHandler(resourceParent, this.traverseHierarchie).isHidden(holder.name);

                    // TODO Usually, the parent does not exist if the resource is a NonExistingResource. Ideally, this
                    // common case should be optimised
                }
                if (hidden) {
                    holder.resources.clear();
                } else if (!ResourceUtil.isNonExistingResource(resource)) {
                    holder.resources.add(resource);
                }
            }
            return createMergedResource(resolver, relativePath, holder);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Resource> listChildren(final ResolveContext<Void> ctx, final Resource parent) {
        final ResourceResolver resolver = parent.getResourceResolver();

        final String relativePath = getRelativePath(parent.getPath());

        if (relativePath != null) {
            final List<ResourceHolder> candidates = new ArrayList<ResourceHolder>();

            final Iterator<Resource> resources = picker.pickResources(resolver, relativePath, parent).iterator();

            boolean isUnderlying = true;
            while (resources.hasNext()) {
                Resource parentResource = resources.next();
                final ParentHidingHandler handler = !isUnderlying ? new ParentHidingHandler(parentResource, this.traverseHierarchie) : null;
                isUnderlying = false;

                for (final Resource child : parentResource.getChildren()) {
                    final String rsrcName = child.getName();
                    ResourceHolder holder = null;
                    for (final ResourceHolder current : candidates) {
                        if (current.name.equals(rsrcName)) {
                            holder = current;
                            break;
                        }
                    }
                    if (holder == null) {
                        holder = new ResourceHolder(rsrcName);
                        candidates.add(holder);
                    }
                    holder.resources.add(child);

                    // Check if children need reordering
                    int orderBeforeIndex = -1;
                    final ValueMap vm = child.getValueMap();
                    final String orderBefore = vm.get(MergedResourceConstants.PN_ORDER_BEFORE, String.class);
                    if (orderBefore != null && !orderBefore.equals(rsrcName)) {
                        // search entry
                        int index = 0;
                        while (index < candidates.size()) {
                            final ResourceHolder current = candidates.get(index);
                            if (current.name.equals(orderBefore)) {
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
                if (handler != null) {
                    final Iterator<ResourceHolder> iter = candidates.iterator();
                    while (iter.hasNext()) {
                        final ResourceHolder holder = iter.next();
                        if (handler.isHidden(holder.name)) {
                            iter.remove();
                        }
                    }
                }
            }
            final List<Resource> children = new ArrayList<Resource>();
            for (final ResourceHolder holder : candidates) {
                final Resource mergedResource = this.createMergedResource(resolver,
                        (relativePath.length() == 0 ? holder.name : relativePath + '/' + holder.name), holder);
                if (mergedResource != null) {
                    children.add(mergedResource);
                }
            }
            return children.iterator();
        }

        return null;
    }

}
