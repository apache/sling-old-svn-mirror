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

import org.apache.sling.api.resource.ResourceProvider;

public class ResourceProviderEntry implements Comparable<ResourceProviderEntry> {

    private final String path;

    private final String prefix;

    private final ResourceProvider provider;

    private final ResourceProviderEntry parentEntry;

    private ResourceProviderEntry[] entries;

    public ResourceProviderEntry(String path, ResourceProvider provider,
            ResourceProviderEntry parentEntry) {
        this(path, provider, parentEntry, null);
    }

    public ResourceProviderEntry(String path, ResourceProvider provider,
            ResourceProviderEntry parentEntry, ResourceProviderEntry[] entries) {
        if (path.endsWith("/")) {
            this.path = path.substring(0, path.length() - 1);
            this.prefix = path;
        } else {
            this.path = path;
            this.prefix = path + "/";
        }
        this.provider = provider;
        this.parentEntry = parentEntry;
        this.entries = entries;
    }

    public ResourceProviderEntry getParentEntry() {
        return parentEntry;
    }

    public ResourceProvider getResourceProvider() {
        return provider;
    }

    public ResourceProviderEntry[] getEntries() {
        return entries;
    }

    public ResourceProviderEntry getResourceProvider(String path) {
        if (path.equals(this.path)) {
            return this;
        } else if (match(path)) {
            if (entries != null) {

                // consider relative path for further checks
                path = path.substring(this.prefix.length());

                for (ResourceProviderEntry entry : entries) {
                    ResourceProviderEntry test = entry.getResourceProvider(path);
                    if (test != null) {
                        return test;
                    }
                }
            }

            // no more specific provider, return mine
            return this;
        }

        // no match for my prefix, return null
        return null;
    }

    public boolean match(String path) {
        return path.startsWith(prefix);
    }

    public boolean addResourceProvider(String prefix, ResourceProvider provider) {
        if (prefix.equals(this.path)) {
            throw new IllegalStateException(
                "ResourceProviderEntry for prefix already exists");
        } else if (match(prefix)) {

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
                            prefix, provider, this);
                        newEntry.addResourceProvider(entry.path, entry.provider);
                        entries[i] = newEntry;
                        return true;
                    }

                }
            }

            // none found, so add it here
            ResourceProviderEntry entry = new ResourceProviderEntry(prefix,
                provider, this);
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
        } else if (match(prefix)) {
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
