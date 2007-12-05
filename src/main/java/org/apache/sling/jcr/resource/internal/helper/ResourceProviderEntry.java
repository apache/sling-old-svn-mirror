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

public class ResourceProviderEntry implements Comparable<ResourceProviderEntry> {

    private final String path;

    private final String prefix;

    private final ResourceProvider provider;

    private ResourceProviderEntry[] entries;

    public ResourceProviderEntry(String path, ResourceProvider provider) {
        if (path.endsWith("/")) {
            this.path = path.substring(0, path.length() - 1);
            this.prefix = path;
        } else {
            this.path = path;
            this.prefix = path + "/";
        }
        this.provider = provider;
    }

    public void addResourceProvider(ResourceProvider provider) {
        String[] roots = provider.getRoots();
        for (int i = 0; i < roots.length; i++) {
            addResourceProvider(roots[i], provider);
        }
    }

    public void removeResourceProvider(ResourceProvider provider) {
        String[] roots = provider.getRoots();
        for (int i = 0; i < roots.length; i++) {
            removeResourceProvider(roots[i]);
        }
    }

    public ResourceProvider getResourceProvider(String path) {
        if (path.equals(this.path)) {
            return provider;
        } else if (match(path)) {
            if (entries != null) {

                // consider relative path for further checks
                path = path.substring(this.prefix.length());

                for (ResourceProviderEntry entry : entries) {
                    ResourceProvider provider = entry.getResourceProvider(path);
                    if (provider != null) {
                        return provider;
                    }
                }
            }

            // no more specific provider, return mine
            return provider;
        }

        // no match for my prefix, return null
        return null;
    }

    public boolean match(String path) {
        return path.startsWith(prefix);
    }

    // ---------- Comparable<ResourceProviderEntry> interface ------------------

    public int compareTo(ResourceProviderEntry o) {
        return prefix.compareTo(o.prefix);
    }

    // ---------- internal -----------------------------------------------------

    private boolean addResourceProvider(String prefix, ResourceProvider provider) {
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

    private boolean removeResourceProvider(String prefix) {
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
}
