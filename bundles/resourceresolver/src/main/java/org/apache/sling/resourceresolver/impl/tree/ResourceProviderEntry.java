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
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.FastTreeMap;
import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.resourceresolver.impl.helper.ResourceResolverContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ResourceProviderEntry</code> class represents a node in the tree of
 * resource providers spanned by the root paths of the provider resources.
 * <p>
 * That means this class has a map of child ResourceProviderEntries, keyed by the child name
 * and a list of ProviderHandlers that are mapped to the path that this ResourceProviderEntry represents.
 * To locate a list of potential ResourceProviders the path is split into elements and then that list used to
 * walk down the tree of ResourceProviders. eg: for a path /a/b/c/d the list of ProviderHandlers would be accessed
 * by rootProvider.get("a").get("b").get("c").get("d")  assuming the final get("d") was not null. If it was, then the list
 * of ProviderHanders would be rootProvider.get("a").get("b").get("c").
 * <p>
 * This class is comparable to itself to help keep the child entries list sorted by their prefix.
 */
public class ResourceProviderEntry implements Comparable<ResourceProviderEntry> {

    private final Logger logger = LoggerFactory.getLogger(ResourceProviderEntry.class);

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
     * and a number of initial child entries.
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
            final String path,
            final boolean isResolve) {
        return getInternalResource(ctx, resourceResolver, path, isResolve);
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

        logger.debug("Adding provider {} at {} ", provider, path);
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

        logger.debug("Removing provider {} at {} ", provider, path);
        set.remove(provider);
        providers = conditionalSort(set);
        return providers.length < before;
    }

	/**
	 * Adds the given resource provider into the tree for the given prefix. This
	 * will expand the tree of ResourceProviderEntries down the supplied prefix
	 * and add the provider to a ResourceProviderEntry that represents the last
	 * element of the path.
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
        // we have a position to add, this will shift other entries
        // down the list.
        entries.add(0, this);
        // the list may not be complete, so add blank entries from the current size to the end of this path.
        for (int i = entries.size() - 1; i < elements.length; i++) {
            final String stubPrefix = elements[i];
            final ResourceProviderEntry rpe2 = new ResourceProviderEntry(stubPrefix, new ProviderHandler[0]);
            entries.get(i).put(stubPrefix, rpe2);
            entries.add(rpe2);
        }
        // finally add this provider to the last in the list. This might be a new entry, or an existing entry.
        return entries.get(elements.length).addInternalProvider(provider);
    }

    /**
     * Remove the given resource provider from the tree
     */
    protected synchronized boolean removeResourceProvider(final String prefix, final ProviderHandler resourceProvider) {
        boolean result = false;
        final String[] elements = split(prefix);
        final List<ResourceProviderEntry> entries = new ArrayList<ResourceProviderEntry>();
        this.populateProviderPath(entries, elements);

        if(entries.size() == 0) {
            // might be a root provider, try to remove it on this entry
            result = this.removeInternalProvider(resourceProvider);
        } else if (entries.size() > 0 && entries.size() == elements.length) {
            // the last element is a perfect match;
            result = entries.get(entries.size() - 1).removeInternalProvider(resourceProvider);
        }

        if(!result) {
            // bad news - the provider might be an OSGi service being deactivated,
            // so this should be taken care of.
            logger.warn("Unable to remove {} for prefix {}, no matching entry found", resourceProvider, prefix);
        }
        return result;
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
	 * Get a list of resource provider entries navigating down the tree starting
	 * from this provider until there are no providers left in the tree. Given a
	 * sequence of path elements a/b/c/d this function will inspect this
	 * ResourceProviderEntry for a child entry of "a" and if present add it to
	 * the list, then it will inspect that child entry for a child "b", then
	 * child "b" for child "c" etc until the list of elements is exhausted or
	 * the child does not exist.
	 *
	 * @param entries
	 *            List to add the entries to.
	 * @param elements
	 *            The path already split into segments.
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
            final String fullPath,
            final boolean isResolve) {
        try {

            if (fullPath == null || fullPath.length() == 0 || fullPath.charAt(0) != '/') {
                logger.debug("Not absolute {}", fullPath);
                return null; // fullpath must be absolute
            }
            final String[] elements = split(fullPath);
            final List<ResourceProviderEntry> entries = new ArrayList<ResourceProviderEntry>();
            this.populateProviderPath(entries, elements);

            Resource fallbackResource = null;

            // the path is in reverse order end first
            for (int i = entries.size() - 1; i >= 0; i--) {
                final ProviderHandler[] rps = entries.get(i).getResourceProviders();
                for (final ProviderHandler rp : rps) {

                    boolean foundFallback = false;
                    final Resource resource = rp.getResource(ctx, resourceResolver, fullPath);
                    if (resource != null) {
                        if ( resource.getResourceMetadata() != null && resource.getResourceMetadata().get(ResourceMetadata.INTERNAL_CONTINUE_RESOLVING) != null ) {
                            if ( logger.isDebugEnabled() ) {
                                logger.debug("Resolved Full {} using {} from {} - continue resolving flag is set!", new Object[] { fullPath, rp, Arrays.toString(rps) });
                            }
                            fallbackResource = resource;
                            fallbackResource.getResourceMetadata().remove(ResourceMetadata.INTERNAL_CONTINUE_RESOLVING);
                            foundFallback = true;
                        } else {
                            if ( logger.isDebugEnabled() ) {
                                logger.debug("Resolved Full {} using {} from {} ", new Object[] { fullPath, rp, Arrays.toString(rps) });
                            }
                            return resource;
                        }
                    }
                    if ( rp.ownsRoots() && !foundFallback ) {
                        logger.debug("Resource null {} ", fullPath);
                        return fallbackResource;
                    }
                }
            }

            // resolve against this one
            final Resource resource = getResourceFromProviders(ctx, resourceResolver, fullPath);
            if (resource != null) {
                return resource;
            }

            if ( fallbackResource != null ) {
                logger.debug("Using first found resource {} for {}", fallbackResource, fullPath);
                return fallbackResource;
            }

            // query: /libs/sling/servlet/default
            // resource Provider: libs/sling/servlet/default/GET.servlet
            // list will match libs, sling, servlet, default
            // and there will be no resource provider at the end
            // SLING-3482 : this is only done for getResource but not resolve
            //              as it is important e.g. for servlet resolution
            //              to get the parent resource for resource traversal.
            if ( !isResolve ) {
                if (entries.size() > 0 && entries.size() == elements.length) {
                    final ResourceProviderEntry lastEntry = entries.get(entries.size() - 1);
                    if (lastEntry.getResourceProviders().length == 0) {
                        logger.debug("Resolved Synthetic {}", fullPath);
                        return new SyntheticResource(resourceResolver, fullPath, ResourceProvider.RESOURCE_TYPE_SYNTHETIC);
                    }
                }
            }
            logger.debug("Resource null {} ", fullPath);
            return null;
        } catch (final Exception ex) {
            logger.debug("Failed! ", ex);
            return null;
        }
    }

    public Resource getResourceFromProviders(final ResourceResolverContext ctx,
            final ResourceResolver resourceResolver,
            final String fullPath) {
        Resource fallbackResource = null;
        final ProviderHandler[] rps = getResourceProviders();
        for (final ProviderHandler rp : rps) {
            boolean foundFallback = false;

            final Resource resource = rp.getResource(ctx, resourceResolver, fullPath);
            if (resource != null) {
                if ( resource.getResourceMetadata() != null && resource.getResourceMetadata().get(ResourceMetadata.INTERNAL_CONTINUE_RESOLVING) != null ) {
                    logger.debug("Resolved Base {} using {} - continue resolving flag is set!", fullPath, rp);
                    fallbackResource = resource;
                    fallbackResource.getResourceMetadata().remove(ResourceMetadata.INTERNAL_CONTINUE_RESOLVING);
                    foundFallback = true;
                } else {
                    logger.debug("Resolved Base {} using {} ", fullPath, rp);
                    return resource;
                }
            }
            if ( rp.ownsRoots() && !foundFallback ) {
                logger.debug("Resource null {} ", fullPath);
                return fallbackResource;
            }
        }
        return fallbackResource;
    }

    /**
     * Internal method getting the provider handler containing a modifying resource provider
     * for the given path
     * @return The provider handler if such a provider exists or <code>null</code>
     */
    private ProviderHandler getModifyingProviderHandler(final ResourceResolverContext ctx,
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
                    return rp;
                }
                if ( rp.ownsRoots() ) {
                    return null;
                }
            }
        }
        // try this one
        for(final ProviderHandler rp : this.providers) {
            final ResourceProvider provider = rp.getResourceProvider(ctx);
            if ( provider instanceof ModifyingResourceProvider) {
                return rp;
            }
        }
        return null;
    }

    /**
     * Delete the resource
     * @throws NullPointerException if resource is null
     * @throws UnsupportedOperationException If deletion is not allowed/possible
     * @throws PersistenceException If deletion fails
     */
    public void delete(final ResourceResolverContext ctx,
            final ResourceResolver resourceResolver,
            final Resource resource) throws PersistenceException {
        final String fullPath = resource.getPath();
        final ProviderHandler handler = this.getModifyingProviderHandler(ctx, resourceResolver, fullPath);
        if ( handler == null || !handler.canDelete(ctx, resource) ) {
            throw new UnsupportedOperationException("delete at '" + fullPath + "'");
        }
        final ModifyingResourceProvider mrp = (ModifyingResourceProvider) handler.getResourceProvider(ctx);
        mrp.delete(resourceResolver, fullPath);
    }

    /**
     * Create a resource
     * @throws UnsupportedOperationException If creation is not allowed/possible
     * @throws PersistenceException If creation fails
     * @return The new resource
     */
    public Resource create(final ResourceResolverContext ctx,
            final ResourceResolver resourceResolver,
            final String fullPath,
            final Map<String, Object> properties) throws PersistenceException {
        final ProviderHandler handler = this.getModifyingProviderHandler(ctx, resourceResolver, fullPath);
        if ( handler == null || !handler.canCreate(ctx, resourceResolver, fullPath) ) {
            throw new UnsupportedOperationException("create '" + ResourceUtil.getName(fullPath) + "' at " + ResourceUtil.getParent(fullPath));
        }
        final ModifyingResourceProvider mrp = (ModifyingResourceProvider) handler.getResourceProvider(ctx);
        return mrp.create(resourceResolver, fullPath, properties);
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
