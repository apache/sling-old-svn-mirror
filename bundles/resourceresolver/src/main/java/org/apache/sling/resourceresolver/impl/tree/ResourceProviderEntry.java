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
package org.apache.sling.resourceresolver.impl.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.FastTreeMap;
import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ResourceProviderEntry</code> class represents a node in the tree of
 * resource providers spanned by the root paths of the provider resources.
 * <p>
 * This class is comparable to itself to help keep the child entries list sorted by their prefix.
 */
public class ResourceProviderEntry implements Comparable<ResourceProviderEntry> {

    private static Logger LOGGER = LoggerFactory.getLogger(ResourceProviderEntry.class);

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
    private ProviderHandler[] providers = new ProviderHandler[0];

    private final FastTreeMap storageMap = new FastTreeMap();

    private Collection<ResourceProviderEntry> storageMapValues = new ArrayList<ResourceProviderEntry>();

    /**
     * Creates an instance of this class with the given path relative to the
     * parent resource provider entry, encapsulating the given ResourceProvider,
     * and a number of inital child entries.
     *
     * @param path
     *            The relative path supported by the provider
     * @param providerList
     *            The resource provider to encapsulate by this entry.
     */
    public ResourceProviderEntry(final String path, final ProviderHandler[] providerList) {
        if (path.endsWith("/")) {
            this.path = path.substring(0, path.length() - 1);
            this.prefix = path;
        } else {
            this.path = path;
            this.prefix = path + "/";
        }
        if (providerList != null) {
            providers = new ProviderHandler[providerList.length];
            for (int i = 0; i < providerList.length; i++) {
                providers[i] = providerList[i];
            }
        }

        // this will consume slightly more memory but ensures read is fast.
        storageMap.setFast(true);

    }

    public String getPath() {
        return path;
    }

    /**
     * Returns the resource providers contained in this entry
     */
    public ProviderHandler[] getResourceProviders() {
        return providers;
    }

    /**
     * Returns the resource with the given path or <code>null</code> if neither
     * the resource provider of this entry nor the resource provider of any of
     * the child entries can provide the resource.
     *
     * @param path
     *            The path to the resource to return.
     * @return The resource for the path or <code>null</code> if no resource can
     *         be found.
     * @throws org.apache.sling.api.SlingException
     *             if an error occurrs trying to access an existing resource.
     */
    public Resource getResource(final ResourceResolverContext ctx,
                    final ResourceResolver resourceResolver,
                    final String path) {
        return getInternalResource(ctx, resourceResolver, path);
    }

    // ------------------ Map methods, here so that we can delegate 2 maps
    // together
    @SuppressWarnings("unchecked")
    public void put(final String key, final ResourceProviderEntry value) {
        storageMap.put(key, value);
        // get a thread safe copy, the ArrayList constructor does a toArray
        // which is thread safe.
        storageMapValues = new ArrayList<ResourceProviderEntry>(storageMap.values());
    }

    public boolean containsKey(final String key) {
        return storageMap.containsKey(key);
    }

    public ResourceProviderEntry get(final String key) {
        return (ResourceProviderEntry) storageMap.get(key);
    }

    public Collection<ResourceProviderEntry> values() {
        return storageMapValues;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final ResourceProviderEntry o) {
        return prefix.compareTo(o.prefix);
    }

    // ---------- internal -----------------------------------------------------

    /**
     * Adds a list of providers to this entry.
     *
     * No sync required as this is called by a sync method!
     */
    private boolean addInternalProvider(final ProviderHandler provider) {
        final int before = providers.length;
        final Set<ProviderHandler> set = new HashSet<ProviderHandler>();
        set.addAll(Arrays.asList(providers));

        LOGGER.debug("Adding provider {} at {} ", provider, path);
        set.add(provider);
        providers = conditionalSort(set);
        return providers.length > before;
    }

    /**
     * Remove a provider from the list of entries.
     *
     * No sync required as this is called by a sync method!
     */
    private boolean removeInternalProvider(final ProviderHandler provider) {
        final int before = providers.length;
        final Set<ProviderHandler> set = new HashSet<ProviderHandler>();
        set.addAll(Arrays.asList(providers));

        LOGGER.debug("Removing provider {} at {} ", provider, path);
        set.remove(provider);
        providers = conditionalSort(set);
        return providers.length < before;
    }

    /**
     * Adds the given resource provider into the tree for the given prefix.
     *
     * @return <code>true</code> if the provider could be entered into the
     *         subtree below this entry. Otherwise <code>false</code> is
     *         returned.
     */
    protected synchronized boolean addResourceProvider(final String prefix, final ProviderHandler provider) {
        final String[] elements = split(prefix);
        final List<ResourceProviderEntry> entries = new ArrayList<ResourceProviderEntry>();
        this.populateProviderPath(entries, elements);

        // add this=root to the start so if the list is empty
        // we have a position to add to
        entries.add(0, this);
        for (int i = entries.size() - 1; i < elements.length; i++) {
            final String stubPrefix = elements[i];
            final ResourceProviderEntry rpe2 = new ResourceProviderEntry(stubPrefix, new ProviderHandler[0]);
            entries.get(i).put(stubPrefix, rpe2);
            entries.add(rpe2);
        }
        return entries.get(elements.length).addInternalProvider(provider);
    }

    /**
     * Remove the given resource provider from the tree
     */
    protected synchronized boolean removeResourceProvider(final String prefix, final ProviderHandler resourceProvider) {
        final String[] elements = split(prefix);
        final List<ResourceProviderEntry> entries = new ArrayList<ResourceProviderEntry>();
        this.populateProviderPath(entries, elements);

        if (entries.size() > 0 && entries.size() == elements.length) {
            // the last element is a perfect match;
            return entries.get(entries.size() - 1).removeInternalProvider(resourceProvider);
        }
        return false;
    }

    /**
     * Return a sorted array of handlers.
     */
    private ProviderHandler[] conditionalSort(final Set<ProviderHandler> set) {

        final List<ProviderHandler> providerList = new ArrayList<ProviderHandler>(set);

        Collections.sort(providerList);

        return providerList.toArray(new ProviderHandler[providerList.size()]);
    }

    /**
     * Get a list of resource provider entries in reverse order.
     * @param entries List to add the entries to
     * @param elements The path already split into segments.
     */
    private void populateProviderPath(final List<ResourceProviderEntry> entries, final String[] elements) {
        ResourceProviderEntry base = this;
        for (final String element : elements) {
            if (element != null) {
                if (base.containsKey(element)) {
                    base = base.get(element);
                    entries.add(base);
                } else {
                    break;
                }
            }
        }
    }

    /**
     * Resolve a resource from a path into a Resource
     *
     * @param ctx The resource resolver context
     * @param resourceResolver the ResourceResolver.
     * @param fullPath the Full path
     * @return null if no resource was found, a resource if one was found.
     */
    private Resource getInternalResource(final ResourceResolverContext ctx,
                    final ResourceResolver resourceResolver,
                    final String fullPath) {
        try {

            if (fullPath == null || fullPath.length() == 0 || fullPath.charAt(0) != '/') {
                LOGGER.debug("Not absolute {}", fullPath);
                return null; // fullpath must be absolute
            }
            final String[] elements = split(fullPath);
            final List<ResourceProviderEntry> entries = new ArrayList<ResourceProviderEntry>();
            this.populateProviderPath(entries, elements);

            // the path is in reverse order end first
            for (int i = entries.size() - 1; i >= 0; i--) {
                final ProviderHandler[] rps = entries.get(i).getResourceProviders();
                for (final ProviderHandler rp : rps) {

                    final Resource resource = rp.getResource(ctx, resourceResolver, fullPath);
                    if (resource != null) {
                        LOGGER.debug("Resolved Full {} using {} from {} ", new Object[] { fullPath, rp, Arrays.toString(rps) });
                        return resource;
                    }
                }
                // TODO stop handling if provider claims subtree!
            }

            // resolve against this one
            final Resource resource = getResourceFromProviders(ctx, resourceResolver, fullPath);
            if (resource != null) {
                return resource;
            }

            // query: /libs/sling/servlet/default
            // resource Provider: libs/sling/servlet/default/GET.servlet
            // list will match libs, sling, servlet, default
            // and there will be no resource provider at the end
            if (entries.size() > 0 && entries.size() == elements.length) {
                if (entries.get(entries.size() - 1).getResourceProviders().length == 0) {
                    LOGGER.debug("Resolved Synthetic {}", fullPath);
                    return new SyntheticResource(resourceResolver, fullPath, ResourceProvider.RESOURCE_TYPE_SYNTHETIC);
                }
            }

            LOGGER.debug("Resource null {} ", fullPath);
            return null;
        } catch (final Exception ex) {
            LOGGER.debug("Failed! ", ex);
            return null;
        }
    }

    public Resource getResourceFromProviders(final ResourceResolverContext ctx,
                    final ResourceResolver resourceResolver, final String fullPath) {
        final ProviderHandler[] rps = getResourceProviders();
        for (final ProviderHandler rp : rps) {
            final Resource resource = rp.getResource(ctx, resourceResolver, fullPath);
            if (resource != null) {
                LOGGER.debug("Resolved Base {} using {} ", fullPath, rp);
                return resource;
            }
        }
        return null;
    }

    public ModifyingResourceProvider getModifyingProvider(final ResourceResolverContext ctx,
                    final ResourceResolver resourceResolver,
                    final String fullPath) {
        final String[] elements = split(fullPath);
        final List<ResourceProviderEntry> entries = new ArrayList<ResourceProviderEntry>();
        this.populateProviderPath(entries, elements);

        for (int i = entries.size() - 1; i >= 0; i--) {
            final ProviderHandler[] rps = entries.get(i).getResourceProviders();
            for (final ProviderHandler rp : rps) {
                final ResourceProvider provider = rp.getResourceProvider(ctx);
                if ( provider instanceof ModifyingResourceProvider ) {
                    return (ModifyingResourceProvider) provider;
                }
            }
            // TODO stop handling if provider claims subtree!
        }
        throw new UnsupportedOperationException();
    }

    private static final char SPLIT_SEP = '/';
    private static final String[] EMPTY_RESULT = new String[0];

    /**
     * Split the string by slash.
     * This method never returns null.
     * @param st The string to split
     * @return an array of the strings between the separator
     */
    public static String[] split(final String st) {

        if (st == null) {
            return EMPTY_RESULT;
        }
        final char[] pn = st.toCharArray();
        if (pn.length == 0) {
            return EMPTY_RESULT;
        }
        if (pn.length == 1 && pn[0] == SPLIT_SEP) {
            return EMPTY_RESULT;
        }
        int n = 1;
        int start = 0;
        int end = pn.length;
        while (start < end && SPLIT_SEP == pn[start])
            start++;
        while (start < end && SPLIT_SEP == pn[end - 1])
            end--;
        for (int i = start; i < end; i++) {
            if (SPLIT_SEP == pn[i]) {
                n++;
            }
        }
        final String[] e = new String[n];
        int s = start;
        int j = 0;
        for (int i = start; i < end; i++) {
            if (pn[i] == SPLIT_SEP) {
                e[j++] = new String(pn, s, i - s);
                s = i + 1;
            }
        }
        if (s < end) {
            e[j++] = new String(pn, s, end - s);
        }
        return e;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.util.AbstractMap#toString()
     */
    @Override
    public String toString() {
        return this.path;
        // "{path:\"" + this.path +
        // "\", providers:"+Arrays.toString(getResourceProviders())+", map:" +
        // storageMap.toString() + "}";
    }
}
