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
package org.apache.sling.jcr.resource.internal.helper;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * The <code>ResourceProviderEntry</code> class represents a node in the tree
 * of resource providers spanned by the root paths of the provider resources.
 * <p>
 * This class is comparable to itself to help keep the child entries list sorted
 * by their prefix.
 */
public class ResourceProviderEntry implements Comparable<ResourceProviderEntry> {

    // the path to resources provided by the resource provider of this
    // entry. this path is relative to the path of the parent resource
    // provider entry and has no trailing slash.
    private final String path;

    // the path to resources provided by the resource provider of this
    // entry. this is the same path as the path field but with a trailing
    // slash to be used as a prefix match resource paths to resolve
    private final String prefix;

    // the resource provider kept in this entry supporting resources at and
    // below the path of this entry.
    private final ResourceProvider provider;

    // child resource provider entries sharing the same path prefix of this
    // entry but with more path elements.
    private ResourceProviderEntry[] entries;

    /**
     * Creates an instance of this class with the given path relative to the
     * parent resource provider entry, encapsulating the given ResourceProvider.
     *
     * @param path The relative path supported by the provider
     * @param provider The resource provider to encapsulate by this entry.
     */
    public ResourceProviderEntry(String path, ResourceProvider provider) {
        this(path, provider, null);
    }

    /**
     * Creates an instance of this class with the given path relative to the
     * parent resource provider entry, encapsulating the given ResourceProvider,
     * and a number of inital child entries.
     *
     * @param path The relative path supported by the provider
     * @param provider The resource provider to encapsulate by this entry.
     */
    public ResourceProviderEntry(String path, ResourceProvider provider,
            ResourceProviderEntry[] entries) {
        if (path.endsWith("/")) {
            this.path = path.substring(0, path.length() - 1);
            this.prefix = path;
        } else {
            this.path = path;
            this.prefix = path + "/";
        }
        this.provider = provider;
        this.entries = entries;
    }

    /**
     * Returns the resource provider contained in this entry
     */
    public ResourceProvider getResourceProvider() {
        return provider;
    }

    /**
     * Returns the child resource provider entries of this entry
     */
    public ResourceProviderEntry[] getEntries() {
        return entries;
    }

    /**
     * Returns the resource with the given path or <code>null</code> if
     * neither the resource provider of this entry nor the resource provider of
     * any of the child entries can provide the resource.
     *
     * @param path The path to the resource to return.
     * @return The resource for the path or <code>null</code> if no resource
     *         can be found.
     * @throws org.apache.sling.api.SlingException if an error occurrs trying to access an existing
     *             resource.
     */
    public Resource getResource(ResourceResolver resourceResolver, String path) {
        return getResource(resourceResolver, path, path);
    }

    /**
     * Returns the resource with the given path or <code>null</code> if
     * neither the resource provider of this entry nor the resource provider of
     * any of the child entries can provide the resource.
     * <p>
     * This method implements the {@link #getResource(String)} method
     * recursively calling itself on any child provide entries matching the
     * path.
     *
     * @param path The path to the resource to return relative to the parent
     *            resource provider entry, which called this method.
     * @param fullPath The actual path to the resource as provided to the
     *            {@link #getResource(String)} method.
     * @return The resource for the path or <code>null</code> if no resource
     *         can be found.
     * @throws SlingException if an error occurrs trying to access an existing
     *             resource.
     */
    private Resource getResource(ResourceResolver resourceResolver,
            String path, String fullPath) {
        if (path.equals(this.path)) {
            return getResourceProvider().getResource(resourceResolver, fullPath);
        } else if (path.startsWith(this.prefix)) {
            if (entries != null) {

                // consider relative path for further checks
                path = path.substring(this.prefix.length());

                for (ResourceProviderEntry entry : entries) {
                    Resource test = entry.getResource(resourceResolver, path,
                        fullPath);
                    if (test != null) {
                        return test;
                    }
                }
            }

            // no more specific provider, return mine
            return getResourceProvider().getResource(resourceResolver, fullPath);
        }

        // no match for my prefix, return null
        return null;
    }

    public boolean addResourceProvider(String prefix, ResourceProvider provider) {
        if (prefix.equals(this.path)) {
            throw new IllegalStateException(
                "ResourceProviderEntry for prefix already exists");
        } else if (prefix.startsWith(this.prefix)) {

            // consider relative path for further checks
            prefix = prefix.substring(this.prefix.length());

            // check whether there is a better suited place
            if (entries != null) {
                for (int i = 0; i < entries.length; i++) {
                    ResourceProviderEntry entry = entries[i];
                    if (entry.addResourceProvider(prefix, provider)) {
                        return true;
                    } else if (entry.prefix.startsWith(prefix)
                        && entry.prefix.charAt(prefix.length()) == '/') {
                        ResourceProviderEntry newEntry = new ResourceProviderEntry(
                            prefix, provider);
                        newEntry.addResourceProvider(entry.path, entry.provider);
                        entries[i] = newEntry;
                        return true;
                    }

                }
            }

            // none found, so add it here
            ResourceProviderEntry entry = new ResourceProviderEntry(prefix,
                provider);
            if (entries == null) {
                entries = new ResourceProviderEntry[] { entry };
            } else {
                SortedSet<ResourceProviderEntry> set = new TreeSet<ResourceProviderEntry>();
                set.addAll(Arrays.asList(entries));
                set.add(entry);
                entries = set.toArray(new ResourceProviderEntry[set.size()]);
            }

            return true;
        }

        // the prefix does not match this prefix
        return false;
    }

    public boolean removeResourceProvider(String prefix) {
        if (prefix.equals(path)) {
            return true;
        } else if (prefix.startsWith(this.prefix)) {
            // consider relative path for further checks
            prefix = prefix.substring(this.prefix.length());

            // check whether there is a better suited place
            if (entries != null) {
                for (int i = 0; i < entries.length; i++) {
                    ResourceProviderEntry entry = entries[i];
                    if (entry.removeResourceProvider(prefix)) {
                        if (entries.length == 1) {
                            entries = null;
                        } else {
                            int newEntriesLen = entries.length - 1;
                            ResourceProviderEntry[] newEntries = new ResourceProviderEntry[newEntriesLen];
                            if (i > 0) {
                                System.arraycopy(entries, 0, newEntries, 0, i);
                            }
                            if (i < newEntriesLen) {
                                System.arraycopy(entries, i + 1, newEntries, i,
                                    newEntriesLen - i);
                            }
                            entries = newEntries;
                        }

                        // reinsert children
                        ResourceProviderEntry[] children = entry.entries;
                        if (children != null) {
                            String pathPrefix = this.prefix + entry.prefix;
                            for (ResourceProviderEntry child : children) {
                                String path = pathPrefix + child.path;
                                addResourceProvider(path, child.provider);
                            }
                        }

                        return false;
                    }

                }
            }
        }

        return false;
    }

    // ---------- Comparable<ResourceProviderEntry> interface ------------------

    public int compareTo(ResourceProviderEntry o) {
        return prefix.compareTo(o.prefix);
    }

}
