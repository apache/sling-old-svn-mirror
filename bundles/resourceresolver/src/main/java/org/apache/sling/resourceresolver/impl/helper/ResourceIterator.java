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
package org.apache.sling.resourceresolver.impl.helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.resourceresolver.impl.tree.ProviderHandler;
import org.apache.sling.resourceresolver.impl.tree.ResourceProviderEntry;
import org.apache.sling.resourceresolver.impl.tree.RootResourceProviderEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ResourceIterator</code> implements the
 * <code>Iterator&lt;Resource&gt;</code> returned from the
 * <code>ResourceResolver.listChidlren(Resource)</code> method.
 * <p>
 * Note: This iterator is created by the
 * <code>JcrResourceResolver.listChildren(Resource)</code> and is not intended
 * for general use by any other code. This class uses internal API of the
 * {@link ResourceProviderEntry} class.
 */
public class ResourceIterator implements Iterator<Resource> {

    /** Logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceIterator.class);

    /**
     * The resource whose children are listed
     */
    private final Resource parentResource;

    /**
     * The root {@link ResourceProviderEntry} used to walk down the resource
     * tree to collect entries which might provide children for the
     * {@link #parentResource}.
     */
    private final RootResourceProviderEntry rootProviderEntry;

    /**
     * <code>ResourceProvider</code> objects registered as nodes above the
     * {@link #parentResource} up to the root of the resource tree
     */
    private final Iterator<ProviderHandler> providers;

    /**
     * The child {@link ResourceProviderEntry} registered at the node of the
     * {@link #parentResource} in the resource tree. This may be
     * <code>null</code> if there is no provider entry registered at that
     * location and will be set to <code>null</code> once all entries have been
     * processed.
     */
    private Iterator<ResourceProviderEntry> baseEntryValues;

    /**
     * An iterator of child resources provided by the current provider entry of
     * the {@link #providers} iterator.
     */
    private Iterator<Resource> resources;

    /**
     * The next resource to be returned from the {@link #next()} method. If this
     * is <code>null</code> the {@link #hasNext()} returns <code>false</code>.
     */
    private Resource nextResource;

    /**
     * Map of synthetic resources returned from resource providers while
     * scanning for children of the {@link #parentResource}. These delayed
     * entries are returned after all non-synthetic resources have been
     * returned. Any delayed entry whose path matches the path of a
     * non-synthetic resource will not returned.
     */
    private final Map<String, Resource> delayed;

    /**
     * Set of paths of resources already returned. This is used to prevent
     * duplicate return of resources.
     */
    private final Set<String> visited;

    /**
     * The absolute path prefix of the {@link #parentResource} resource with a
     * trailing slash to build the absolute path of child resources.
     */
    private final String iteratorPath;

    /**
     * Iterator on the map of {@link #delayed} synthetic resources
     */
    private Iterator<Resource> delayedIter;

    private final ResourceResolverContext resourceResolverContext;

    public ResourceIterator(final ResourceResolverContext ctx,
            final Resource parentResource,
            final RootResourceProviderEntry rootProviderEntry) {
        this.resourceResolverContext = ctx;
        this.parentResource = parentResource;
        this.rootProviderEntry = rootProviderEntry;

        LOGGER.debug("Child Iterator for {}", parentResource.getPath());

        String path = parentResource.getPath();
        if (!path.endsWith("/")) {
            path += "/";
        }

        // gather the providers in linked set, such that we keep
        // the order of addition and make sure we only get one entry
        // for each resource provider
        final Set<ProviderHandler> providersSet = new LinkedHashSet<ProviderHandler>();
        final ResourceProviderEntry atPath = getResourceProviders(path, providersSet);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(" Provider Set for path {} {} ", path, Arrays
                    .toString(providersSet.toArray(new ProviderHandler[providersSet.size()])));
        }
        this.iteratorPath = path;
        providers = providersSet.iterator();
        baseEntryValues = (atPath != null) ? atPath.values().iterator() : null;
        delayed = new LinkedHashMap<String, Resource>();
        visited = new HashSet<String>();
        nextResource = seek();
    }

    @Override
    public boolean hasNext() {
        return nextResource != null;
    }

    @Override
    public Resource next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        final Resource result = nextResource;
        nextResource = seek();
        LOGGER.debug("  Child resource [{}] [{}] ", iteratorPath, result.getPath());
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    private Resource seek() {
        while (delayedIter == null) {
            while ((resources == null || !resources.hasNext())
                    && providers.hasNext()) {
                final ProviderHandler provider = providers.next();
                resources = provider.listChildren(this.resourceResolverContext, parentResource);
                LOGGER.debug("     Checking Provider {} ", provider);
            }

            if (resources != null && resources.hasNext()) {
                final Resource res = resources.next();
                final String resPath = res.getPath();

                if (visited.contains(resPath)) {

                    // ignore a path, we have already visited and
                    // ensure it will not be listed as a delayed
                    // resource at the end
                    delayed.remove(resPath);

                } else if (res instanceof SyntheticResource) {

                    // don't return synthetic resources right away,
                    // since a concrete resource for the same path
                    // may be provided later on
                    delayed.put(resPath, res);

                } else {

                    // we use this concrete, unvisited resource but
                    // mark it as visited and remove from delayed
                    visited.add(resPath);
                    delayed.remove(resPath);
                    LOGGER.debug("      resource {} {}", resPath, res.getClass());

                    res.getResourceMetadata().setResolutionPath(res.getPath());
                    return res;

                }

            } else if (baseEntryValues != null) {

                while (baseEntryValues.hasNext()) {
                    final ResourceProviderEntry rpw = baseEntryValues.next();
                    final String resPath = iteratorPath + rpw.getPath();
                    if (!visited.contains(resPath)) {
                        final ResourceResolver rr = parentResource.getResourceResolver();
                        final Resource res = rpw.getResourceFromProviders(this.resourceResolverContext, rr,
                                resPath, Collections.<String,String>emptyMap());
                        if (res == null) {
                            if (!delayed.containsKey(resPath)) {
                                delayed.put(
                                        resPath,
                                        new SyntheticResource(
                                                rr,
                                                resPath,
                                                ResourceProvider.RESOURCE_TYPE_SYNTHETIC));
                            }
                        } else {
                            // return the real resource immediately, add
                            // to the visited keys and ensure delayed
                            // does not contain it
                            delayed.remove(resPath);
                            visited.add(resPath);
                            LOGGER.debug("   B  resource {} {}", resPath,
                                    res.getClass());
                            res.getResourceMetadata().setResolutionPath(res.getPath());
                            return res;
                        }
                    }
                }

                baseEntryValues = null;

            } else {

                // all resource providers and baseEntryValues have
                // exhausted, so we should continue returning the
                // delayed (synthetic resources)
                delayedIter = delayed.values().iterator();
            }
        }

        // we exhausted all resource providers with their concrete
        // resources. now lets do the delayed (synthetic) resources
        final Resource res = delayedIter.hasNext() ? delayedIter.next() : null;
        if (res != null) {
            LOGGER.debug("   D  resource {} {}", res.getPath(), res.getClass());
            res.getResourceMetadata().setResolutionPath(res.getPath());
        }
        return res;
    }

    /**
     * Returns all resource providers which provider resources whose prefix is
     * the given path.
     *
     * @param path
     *            The prefix path to match the resource provider roots against
     * @param providers
     *            The set of already found resource providers to which any
     *            additional resource providers are added.
     * @return The ResourceProviderEntry at the node identified with the path or
     *         <code>null</code> if there is no entry at the given location
     */
    private ResourceProviderEntry getResourceProviders(final String path,
            final Set<ProviderHandler> providers) {

        // collect providers along the ancestor path segements
        final String[] elements = ResourceProviderEntry.split(path);
        ResourceProviderEntry base = rootProviderEntry;
        for (final String element : elements) {
            if (base.containsKey(element)) {
                base = base.get(element);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Loading from {}  {} ", element,
                            base.getResourceProviders().length);
                }
                for (final ProviderHandler rp : base.getResourceProviders()) {
                    LOGGER.debug("Adding {} for {} ", rp, path);
                    providers.add(rp);
                }
            } else {
                LOGGER.debug("No container for {} ", element);
                base = null;
                break;
            }
        }

        // add in providers at this node in the tree, ie the root provider
        for (final ProviderHandler rp : rootProviderEntry.getResourceProviders()) {
            LOGGER.debug("Loading All at {} ", path);
            providers.add(rp);
        }
        return base;
    }

}
