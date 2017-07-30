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
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker2;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

public class MergingResourceProvider extends ResourceProvider<Void> {

    protected final String mergeRootPath;

    protected final MergedResourcePicker2 picker;

    private final boolean readOnly;

    protected final boolean traverseHierarchie;

    MergingResourceProvider(final String mergeRootPath,
            final MergedResourcePicker2 picker,
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
        public final boolean onlyUnderlying; // if only underlying resources should be affected (and not the local ones)

        public ExcludeEntry(final String value, boolean onlyUnderlying) {
            this.onlyUnderlying = onlyUnderlying;
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

        /**
         * 
         * @param parent the underlying resource
         * @param traverseParent if true will also continue with the parent's parent recursively
         */
        public ParentHidingHandler(final Resource parent, final boolean traverseParent) {
            // evaluate the sling:hideChildren property on the current resource
            final ValueMap parentProps = parent.getValueMap();
            final String[] childrenToHideArray = parentProps.get(MergedResourceConstants.PN_HIDE_CHILDREN, String[].class);
            if (childrenToHideArray != null) {
                for (final String value : childrenToHideArray) {
                    final boolean onlyUnderlying;
                    if (value.equals("*")) {
                        onlyUnderlying = true;
                    } else {
                        onlyUnderlying = false;
                    }
                    final ExcludeEntry entry = new ExcludeEntry(value, onlyUnderlying);
                    this.entries.add(entry);
                }
            }
            // also check on the parent's parent whether that was hiding the parent
            if (parent != null) {
                Resource ancestor = parent.getParent();
                String previousAncestorName = parent.getName();
                while (ancestor != null) {
                    final ValueMap ancestorProps = ancestor.getValueMap();
                    final String[] ancestorChildrenToHideArray = ancestorProps.get(MergedResourceConstants.PN_HIDE_CHILDREN, String[].class);
                    if (ancestorChildrenToHideArray != null) {
                        for (final String value : ancestorChildrenToHideArray) {
                            final boolean onlyUnderlying;
                            if (value.equals("*")) {
                                onlyUnderlying = true;
                            } else {
                                onlyUnderlying = false;
                            }
                            final ExcludeEntry entry = new ExcludeEntry(value, onlyUnderlying);
                            // check if this entry is applicable at all (always assuming the worst case, i.e. non local resource)
                            final Boolean hides = hides(entry, previousAncestorName, false);
                            if (hides != null && hides.booleanValue() == true) {
                                this.entries.add(new ExcludeEntry("*", entry.onlyUnderlying));
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

        /**
         * 
         * @param name the name of the resource to check
         * @param isLocalResource {@code true} if the check is on a local resource, {@code false} if the check is on an underlying/inherited resource
         * @return {@code true} if the local/inherited resource should be hidden, otherwise {@code false}
         */
        public boolean isHidden(final String name, boolean isLocalResource) {
            boolean hidden = false;
            if ( this.entries != null ) {
                for(final ExcludeEntry entry : this.entries) {
                    Boolean result = hides(entry, name, isLocalResource);
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
        private Boolean hides(final ExcludeEntry entry, final String name, boolean isLocalResource) {
            Boolean result = null;
            if (entry.name.equals("*") || entry.name.equals(name)) {
                if ((isLocalResource && !entry.onlyUnderlying) || !isLocalResource) {
                    result = Boolean.valueOf(!entry.exclude);
                }
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
        return this.getResource(ctx, parentPath, ResourceContext.EMPTY_CONTEXT, child);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource getResource(final ResolveContext<Void> ctx, final String path, final ResourceContext rCtx, final Resource parent) {
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
                    // SLING-3521 : if parent is not readable, nothing is hidden
                    final Resource resourceParent = resource.getParent();
                    hidden = resourceParent != null && new ParentHidingHandler(resourceParent, this.traverseHierarchie).isHidden(holder.name, true);

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
    @Override
    public Iterator<Resource> listChildren(final ResolveContext<Void> ctx, final Resource parent) {
        final ResourceResolver resolver = parent.getResourceResolver();

        final String relativePath = getRelativePath(parent.getPath());

        if (relativePath != null) {
            // candidates is the list of holders from which the children are being constructed!
            final List<ResourceHolder> candidates = new ArrayList<ResourceHolder>();

            final Iterator<Resource> resources = picker.pickResources(resolver, relativePath, parent).iterator();
            
            // start with the base resource
            boolean isUnderlying = true;
            while (resources.hasNext()) {
                Resource parentResource = resources.next();
                final ParentHidingHandler handler = !isUnderlying ? new ParentHidingHandler(parentResource, this.traverseHierarchie) : null;
                isUnderlying = false;

                // remove the hidden child resources from the underlying resource
                if (handler != null) {
                    final Iterator<ResourceHolder> iter = candidates.iterator();
                    while (iter.hasNext()) {
                        final ResourceHolder holder = iter.next();
                        if (handler.isHidden(holder.name, false)) {
                            iter.remove(); // remove from the candidates list
                        }
                    }
                }
                
                int previousChildPositionInCandidateList = -1;
                
                // get children of current resource (might be overlaid resource)
                for (final Resource child : parentResource.getChildren()) {
                    final String rsrcName = child.getName();
                    // the holder which should end up in the children list
                    ResourceHolder holder = null;
                    int childPositionInCandidateList = -1;
                    
                    // check if this an overlaid resource (i.e. has the resource with the same name already be exposed through the underlying resource)
                    for (int index=0; index < candidates.size(); index++) {
                        ResourceHolder current = candidates.get(index);
                        if (current.name.equals(rsrcName)) {
                            holder = current;
                            childPositionInCandidateList = index;
                            break;
                        }
                    }
                    // for new resources, i.e. no underlying resource found...
                    if (holder == null) {
                        // remove the hidden child resources from the local resource
                        if (handler != null && handler.isHidden(rsrcName, true)) {
                            continue; // skip this child
                        }
                        holder = new ResourceHolder(rsrcName);
                        if (previousChildPositionInCandidateList != -1) {
                            // either add after the previous child position
                            candidates.add(previousChildPositionInCandidateList+1, holder);
                            previousChildPositionInCandidateList++;
                        } else {
                            // or add to the end of the list
                            candidates.add(holder);
                            previousChildPositionInCandidateList = candidates.size() - 1;
                        }
                    }
                    // in all cases the holder should get the current child!
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
                    // either reorder because of explicit reording property
                    if (orderBeforeIndex > -1) {
                        candidates.add(orderBeforeIndex, holder);
                        candidates.remove(candidates.size() - 1);
                        previousChildPositionInCandidateList = orderBeforeIndex;
                    } else {
                        // or reorder because overlaid resource has a different order
                        if (childPositionInCandidateList != -1 && previousChildPositionInCandidateList != -1) {
                            candidates.remove(childPositionInCandidateList);
                            if (childPositionInCandidateList < previousChildPositionInCandidateList) {
                                previousChildPositionInCandidateList--;
                            }
                            candidates.add(previousChildPositionInCandidateList+1, holder);
                            previousChildPositionInCandidateList++;
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
