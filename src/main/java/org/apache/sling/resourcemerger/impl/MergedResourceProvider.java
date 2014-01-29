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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;

/**
 * The <code>MergedResourceProvider</code> is the resource provider providing
 * access to {@link MergedResource} objects.
 */
public class MergedResourceProvider implements ResourceProvider {

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

    /**
     * {@inheritDoc}
     */
    public Resource getResource(final ResourceResolver resolver, final String path) {

        if (resolver.getSearchPath() != null) {
            final String relativePath = getRelativePath(path);

            if ( relativePath != null ) {
                final List<String> mappedResources = new ArrayList<String>();
                // Loop over provided base paths
                for (final String basePath : resolver.getSearchPath()) {
                    // Try to get the corresponding physical resource for this base path
                    final Resource baseRes = resolver.getResource(basePath + "/" + relativePath);
                    if (baseRes != null) {
                        // check if resource is hidden
                        boolean hidden = false;
                        final ValueMap props = ResourceUtil.getValueMap(baseRes);
                        if ( props.get(MergedResourceConstants.PN_HIDE_RESOURCE, Boolean.FALSE) ) {
                            hidden = true;
                        }
                        if ( !hidden ) {
                            // check parent
                            final ValueMap parentProps = ResourceUtil.getValueMap(baseRes.getParent());
                            final String[] childrenToHideArray = parentProps.get(MergedResourceConstants.PN_HIDE_CHILDREN, String[].class);
                            if ( childrenToHideArray != null ) {
                                for(final String name : childrenToHideArray ) {
                                    if ( name.equals(baseRes.getName()) ) {
                                        hidden = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if ( !hidden ) {
                            // Physical resource exists, add it to the list of mapped resources
                            mappedResources.add(0, baseRes.getPath());
                        } else {
                            mappedResources.clear();
                        }
                    }
                }

                if (!mappedResources.isEmpty()) {
                    // Create a new merged resource based on the list of mapped physical resources
                    return new MergedResource(resolver, mergeRootPath, relativePath, mappedResources);
                }
            }
        }

        // Either base paths were not defined, or the resource does not exist in any of them
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Resource> listChildren(final Resource resource) {
        if (resource instanceof MergedResource) {
            final MergedResource mergedResource = (MergedResource) resource;
            final ResourceResolver resolver = mergedResource.getResourceResolver();
            final List<Resource> children = new ArrayList<Resource>();

            for (final String mappedResourcePath : mergedResource.getMappedResources()) {
                final Resource mappedResource = resolver.getResource(mappedResourcePath);

                // Check if the resource exists
                if (mappedResource == null) {
                    continue;
                }

                // Check if some previously defined children have to be ignored
                final ValueMap mappedResourceProps = mappedResource.adaptTo(ValueMap.class);
                final String[] childrenToHideArray = mappedResourceProps.get(MergedResourceConstants.PN_HIDE_CHILDREN, String[].class);
                if ( childrenToHideArray != null ) {
                    final List<String> childrenToHide = Arrays.asList(childrenToHideArray);
                    if ( childrenToHide.contains("*") ) {
                        // Clear current children list
                        children.clear();
                    } else {
                        // Go through current children in order to hide them individually
                        final Iterator<Resource> it = children.iterator();
                        while (it.hasNext()) {
                            if (childrenToHide.contains(it.next().getName())) {
                                it.remove();
                            }
                        }
                    }
                }

                // Browse children of current physical resource
                for (final Resource child : mappedResource.getChildren()) {
                    final String childRelativePath = mergedResource.getRelativePath() + "/" + child.getName();

                    if (ResourceUtil.getValueMap(child).get(MergedResourceConstants.PN_HIDE_RESOURCE, Boolean.FALSE)) {
                        // Child resource has to be hidden
                        children.remove(new MergedResource(resolver, mergeRootPath, childRelativePath));

                    } else {
                        // Check if the child resource already exists in the children list
                        MergedResource mergedResChild = new MergedResource(resolver, mergeRootPath, childRelativePath);
                        int mergedResChildIndex = -1;
                        if (children.contains(mergedResChild)) {
                            // Get current index of the merged resource's child
                            mergedResChildIndex = children.indexOf(mergedResChild);
                            mergedResChild = (MergedResource) children.get(mergedResChildIndex);
                        }
                        // Add a new mapped resource to the merged resource
                        mergedResChild.addMappedResource(child.getPath());
                        boolean mergedResChildExists = mergedResChildIndex > -1;

                        // Check if children need reordering
                        int orderBeforeIndex = -1;
                        String orderBefore = ResourceUtil.getValueMap(child).get(MergedResourceConstants.PN_ORDER_BEFORE, String.class);
                        if (orderBefore != null && !orderBefore.equals(mergedResChild.getName())) {
                            // Get a dummy merged resource just to know the index of that merged resource
                            MergedResource orderBeforeRes = new MergedResource(resolver, mergeRootPath, mergedResource.getRelativePath() + "/" + orderBefore);
                            if (children.contains(orderBeforeRes)) {
                                orderBeforeIndex = children.indexOf(orderBeforeRes);
                            }
                        }

                        if (orderBeforeIndex > -1) {
                            // Add merged resource's child at the right position
                            children.add(orderBeforeIndex, mergedResChild);
                            if (mergedResChildExists) {
                                children.remove(mergedResChildIndex > orderBeforeIndex ? ++mergedResChildIndex : mergedResChildIndex);
                            }
                        } else if (!mergedResChildExists) {
                            // Only add the merged resource's child if it did not exist yet
                            children.add(mergedResChild);
                        }
                    }
                }
            }

            return children.iterator();
        }

        // Return null for resources that aren't a MergedResource
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

}
