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
package org.apache.sling.resourceresolver.impl.providers.stateful;

import static org.apache.sling.api.resource.ResourceUtil.getName;
import static org.apache.sling.spi.resource.provider.ResourceProvider.RESOURCE_TYPE_SYNTHETIC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.paths.PathBuilder;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderInfo;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderStorage;
import org.apache.sling.resourceresolver.impl.providers.tree.Node;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class takes a number of {@link StatefulResourceProvider} objects and
 * exposes it as one such object. Provider appropriate for the given operation
 * is chosen basing on its {@link ResourceProviderInfo#getPath()} (more specific
 * first) and service ranking.
 */
public class CombinedResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(CombinedResourceProvider.class);

    private final ResourceProviderStorage storage;

    private final ResourceResolver resolver;

    private final ResourceProviderAuthenticator authenticator;

    public CombinedResourceProvider(ResourceProviderStorage storage,
            ResourceResolver resolver,
            ResourceProviderAuthenticator authenticator) {
        this.storage = storage;
        this.resolver = resolver;
        this.authenticator = authenticator;
    }

    /**
     * Logs out from all providers.
     */
    public void logout() {
        for (StatefulResourceProvider p : authenticator.getAllUsedAuthenticated()) {
            p.logout();
        }
    }

    /**
     * Refreshes all refreshable providers.
     */
    public void refresh() {
        for (StatefulResourceProvider p : authenticator.getAllUsedRefreshable()) {
            p.refresh();
        }
    }

    /**
     * Returns {@code true} if all providers are live.
     */
    public boolean isLive() {
        for (StatefulResourceProvider p : authenticator.getAllUsedAuthenticated()) {
            if (!p.isLive()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns parent from the most appropriate resource provider accepting the
     * given children.
     *
     * In some cases the {@link SyntheticResource} can be returned if no
     * resource provider returns parent for this child. See
     * {@link #getResource(String, Resource, Map, boolean)} for more details
     */
    public Resource getParent(Resource child) {
        final String path = child.getPath();
        try {
            final StatefulResourceProvider provider = getBestMatchingProvider(path);
            final Resource parentCandidate = provider.getParent(child);
            if (parentCandidate != null) {
                return parentCandidate;
            }
        } catch ( final LoginException le ) {
            // ignore
        }
        final String parentPath = ResourceUtil.getParent(path);
        if (parentPath != null && isIntermediatePath(parentPath)) {
            return new SyntheticResource(resolver, parentPath, ResourceProvider.RESOURCE_TYPE_SYNTHETIC);
        }
        return null;
    }

    /**
     * Returns resource from the most appropriate resource provider.
     * <br/><br/>
     * If there's no such provider and the path is a part of some resource
     * provider path, then the {@link SyntheticResource} will be returned. For
     * instance, if we have resource provider under
     * {@code /libs/sling/servlet/default/GET.servlet} and no resource provider
     * returns a resource for {@code /libs/sling/servlet/default}, then the
     * {@link SyntheticResource} will be returned to provide a consistent
     * resource tree.
     * <br/><br/>
     * The same behaviour occurs in {@link #getParent(Resource)} and
     * {@link #listChildren(Resource)}.
     */
    public Resource getResource(String path, Resource parent, Map<String, String> parameters, boolean isResolve) {
        if (path == null || path.length() == 0 || path.charAt(0) != '/') {
            logger.debug("Not absolute {}", path);
            return null; // path must be absolute
        }

        try {
            final StatefulResourceProvider provider = this.getBestMatchingProvider(path);
            final Resource resourceCandidate = provider.getResource(path, parent, parameters, isResolve);
            if (resourceCandidate != null) {
                return resourceCandidate;
            }
        } catch ( LoginException le ) {
            // ignore
        }
        // query: /libs/sling/servlet/default
        // resource Provider: libs/sling/servlet/default/GET.servlet
        // list will match libs, sling, servlet, default
        // and there will be no resource provider at the end
        // SLING-3482 : this is only done for getResource but not resolve
        //              as it is important e.g. for servlet resolution
        //              to get the parent resource for resource traversal.
        if (!isResolve && isIntermediatePath(path)) {
            logger.debug("Resolved Synthetic {}", path);
            return new SyntheticResource(resolver, path, ResourceProvider.RESOURCE_TYPE_SYNTHETIC);
        }
        logger.debug("Resource null {} ", path);
        return null;
    }

    private boolean isIntermediatePath(final String fullPath) {
        return storage.getTree().getNode(fullPath) != null;
    }

    /**
     * This method asks all matching resource providers for the children iterators,
     * merges them, adds {@link SyntheticResource}s (see
     * {@link #getResource(String, Resource, Map, boolean)} for more details),
     * filters out the duplicates and returns the resulting iterator. All
     * transformations are done lazily, during the {@link Iterator#hasNext()}
     * invocation on the result.
     */
    @SuppressWarnings("unchecked")
    public Iterator<Resource> listChildren(final Resource parent) {
        final String parentPath = parent.getPath();

        // 3 sources are combined: children of the provider which owns 'parent',
        // providers which are directly mounted at a child path,
        // synthetic resources for providers mounted at a lower level

        // children of the 'parent' provider
        Iterator<Resource> realChildren = null;
        try {
            final StatefulResourceProvider provider = this.getBestMatchingProvider(parentPath);
            realChildren = provider.listChildren(parent);
        } catch ( final LoginException le ) {
            // ignore, realChildren will be null
        }

        final Set<String> visitedNames = new HashSet<String>();

        IteratorChain chain = new IteratorChain();
        if ( realChildren != null ) {
            chain.addIterator(realChildren);
        }

        // synthetic and providers are done in one loop
        final Node<ResourceProviderHandler> node = storage.getTree().getNode(parent.getPath());
        if (node != null) {
            final List<Resource> syntheticList = new ArrayList<Resource>();
            final List<Resource> providerList = new ArrayList<Resource>();

            for (final Entry<String, Node<ResourceProviderHandler>> entry : node.getChildren().entrySet()) {
                final String name = entry.getKey();
                final ResourceProviderHandler handler = entry.getValue().getValue();
                PathBuilder pathBuilder = new PathBuilder(parent.getPath());
                pathBuilder.append(name);
                final String childPath = pathBuilder.toString();
                if (handler == null) {
                    syntheticList.add(new SyntheticResource(resolver, childPath, RESOURCE_TYPE_SYNTHETIC));
                } else {
                    Resource rsrc = null;
                    try {
                        rsrc = authenticator.getStateful(handler, this).getResource(childPath, parent, null, false);
                    } catch ( final LoginException ignore) {
                        // ignore
                    }
                    if ( rsrc != null ) {
                        providerList.add(rsrc);
                    } else {
                        // if there is a child provider underneath, we need to create a synthetic resource
                        // otherwise we need to make sure that no one else is providing this child
                        if ( entry.getValue().getChildren().isEmpty() ) {
                            syntheticList.add(new SyntheticResource(resolver, childPath, RESOURCE_TYPE_SYNTHETIC));
                        } else {
                            visitedNames.add(name);
                        }
                    }
                }
            }
            if ( !providerList.isEmpty() ) {
                chain.addIterator(providerList.iterator());
            }
            if ( !syntheticList.isEmpty() ) {
                chain.addIterator(syntheticList.iterator());
            }
        }
        if ( chain.size() == 0 ) {
            return Collections.EMPTY_LIST.iterator();
        }
        return new UniqueIterator(visitedNames, chain);
    }

    /**
     * Returns the union of all attribute names.
     */
    public Collection<String> getAttributeNames() {
        final Set<String> names = new LinkedHashSet<String>();
        for (StatefulResourceProvider p : authenticator.getAllBestEffort(storage.getAttributableHandlers(), this)) {
            final Collection<String> newNames = p.getAttributeNames();
            if (newNames != null) {
                names.addAll(newNames);
            }
        }
        return names;
    }

    /**
     * Returns the first non-null result of the
     * {@link StatefulResourceProvider#getAttribute(String)} invocation on
     * the providers.
     */
    public Object getAttribute(String name) {
        for (StatefulResourceProvider p : authenticator.getAllBestEffort(storage.getAttributableHandlers(), this)) {
            Object attribute = p.getAttribute(name);
            if (attribute != null) {
                return attribute;
            }
        }
        return null;
    }

    /**
     * Create a resource.
     *
     * @throws UnsupportedOperationException
     *             If creation is not allowed/possible
     * @throws PersistenceException
     *             If creation fails
     * @return The new resource
     */
    public Resource create(String path, Map<String, Object> properties) throws PersistenceException {
        try {
            final StatefulResourceProvider provider = getBestMatchingModifiableProvider(path);
            if ( provider != null ) {
                final Resource creationResultResource = provider.create(resolver, path, properties);
                if (creationResultResource != null) {
                    return creationResultResource;
                }
            }
        } catch (LoginException le) {
            // ignore and throw (see below)
        }
        throw new UnsupportedOperationException("create '" + getName(path) + "' at " + ResourceUtil.getParent(path));
    }

    /**
     * Delete the resource. Iterate over all modifiable ResourceProviders
     * giving each an opportunity to delete the resource if they are able.
     *
     * @throws NullPointerException
     *             if resource is null
     * @throws UnsupportedOperationException
     *             If deletion is not allowed/possible
     * @throws PersistenceException
     *             If deletion fails
     */
    public void delete(final Resource resource) throws PersistenceException {
        final String path = resource.getPath();
        try {
            final StatefulResourceProvider provider = getBestMatchingModifiableProvider(path);
            if ( provider != null ) {
                provider.delete(resource);
                return;
            }
        } catch (LoginException le) {
            // ignore and throw (see below)
        }
        throw new UnsupportedOperationException("delete at '" + path + "'");
    }

    /**
     * Revert changes on all modifiable ResourceProviders.
     */
    public void revert() {
        for (StatefulResourceProvider p : authenticator.getAllUsedModifiable()) {
            p.revert();
        }
    }

    /**
     * Commit changes on all modifiable ResourceProviders.
     */
    public void commit() throws PersistenceException {
        for (StatefulResourceProvider p : authenticator.getAllUsedModifiable()) {
            p.commit();
        }
    }

    /**
     * Check if any modifiable ResourceProvider has uncommited changes.
     */
    public boolean hasChanges() {
        for (StatefulResourceProvider p : authenticator.getAllUsedModifiable()) {
            if (p.hasChanges()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the union of query languages supported by the providers.
     */
    public String[] getSupportedLanguages() {
        Set<String> supportedLanguages = new LinkedHashSet<String>();
        for (StatefulResourceProvider p : authenticator.getAllBestEffort(storage.getLanguageQueryableHandlers(), this)) {
            supportedLanguages.addAll(Arrays.asList(p.getSupportedLanguages()));
        }
        return supportedLanguages.toArray(new String[supportedLanguages.size()]);
    }

    /**
     * Queries all resource providers and combines the results.
     */
    public Iterator<Resource> findResources(final String query, final String language) {
        List<StatefulResourceProvider> queryableRP = getQueryableProviders(language);
        List<Iterator<Resource>> iterators = new ArrayList<Iterator<Resource>>(queryableRP.size());
        for (StatefulResourceProvider p : queryableRP) {
            iterators.add(p.findResources(query, language));
        }
        return new ChainedIterator<Resource>(iterators.iterator());
    }

    private List<StatefulResourceProvider> getQueryableProviders(String language) {
        List<StatefulResourceProvider> queryableProviders = new ArrayList<StatefulResourceProvider>();
        for (StatefulResourceProvider p : authenticator.getAllBestEffort(storage.getLanguageQueryableHandlers(), this)) {
            if (ArrayUtils.contains(p.getSupportedLanguages(), language)) {
                queryableProviders.add(p);
            }
        }
        return queryableProviders;
    }

    /**
     * Queries all resource providers and combines the results.
     */
    public Iterator<Map<String, Object>> queryResources(final String query, final String language) {
        List<StatefulResourceProvider> queryableRP = getQueryableProviders(language);
        List<Iterator<Map<String, Object>>> iterators = new ArrayList<Iterator<Map<String, Object>>>(queryableRP.size());
        for (StatefulResourceProvider p : queryableRP) {
            iterators.add(p.queryResources(query, language));
        }
        return new ChainedIterator<Map<String, Object>>(iterators.iterator());
    }

    /**
     * Returns the first non-null result of the adaptTo() method invoked on the
     * providers.
     */
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        for (StatefulResourceProvider p : authenticator.getAllBestEffort(storage.getAdaptableHandlers(), this)) {
            final Object adaptee = p.adaptTo(type);
            if (adaptee != null) {
                return (AdapterType) adaptee;
            }
        }
        return null;
    }

    private StatefulResourceProvider checkSourceAndDest(final String srcAbsPath, final String destAbsPath) throws PersistenceException {
        // check source
        final Node<ResourceProviderHandler> srcNode = storage.getTree().getBestMatchingNode(srcAbsPath);
        if ( srcNode == null ) {
            throw new PersistenceException("Source resource does not exist.", null, srcAbsPath, null);
        }
        StatefulResourceProvider srcProvider = null;
        try {
            srcProvider = authenticator.getStateful(srcNode.getValue(), this);
        } catch (LoginException e) {
            // ignore
        }
        if ( srcProvider == null ) {
            throw new PersistenceException("Source resource does not exist.", null, srcAbsPath, null);
        }
        final Resource srcResource = srcProvider.getResource(srcAbsPath, null, null, false);
        if ( srcResource == null ) {
            throw new PersistenceException("Source resource does not exist.", null, srcAbsPath, null);
        }

        // check destination
        final Node<ResourceProviderHandler> destNode = storage.getTree().getBestMatchingNode(destAbsPath);
        if ( destNode == null ) {
            throw new PersistenceException("Destination resource does not exist.", null, destAbsPath, null);
        }
        StatefulResourceProvider destProvider = null;
        try {
            destProvider = authenticator.getStateful(destNode.getValue(), this);
        } catch (LoginException e) {
            // ignore
        }
        if ( destProvider == null ) {
            throw new PersistenceException("Destination resource does not exist.", null, destAbsPath, null);
        }
        final Resource destResource = destProvider.getResource(destAbsPath, null, null, false);
        if ( destResource == null ) {
            throw new PersistenceException("Destination resource does not exist.", null, destAbsPath, null);
        }

        // check for sub providers of src and dest
        if ( srcProvider == destProvider && !collectProviders(srcNode) && !collectProviders(destNode) ) {
            return srcProvider;
        }
        return null;
    }

    private boolean collectProviders(final Node<ResourceProviderHandler> parent) {
        boolean hasMoreProviders = false;
        for (final Entry<String, Node<ResourceProviderHandler>> entry : parent.getChildren().entrySet()) {
            if ( entry.getValue().getValue() != null ) {
                try {
                    authenticator.getStateful(entry.getValue().getValue(), this);
                    hasMoreProviders = true;
                } catch ( final LoginException ignore) {
                    // ignore
                }
            }
            if ( collectProviders(entry.getValue())) {
                hasMoreProviders = true;
            }
        }

        return hasMoreProviders;
    }

    private void copy(final Resource src, final String dstPath, final List<Resource> newNodes) throws PersistenceException {
        final ValueMap vm = src.getValueMap();
        final String createPath = new PathBuilder(dstPath).append(src.getName()).toString();
        newNodes.add(this.create(createPath, vm));
        for(final Resource c : src.getChildren()) {
            copy(c, createPath, newNodes);
        }
    }

    /**
     * Tries to find a resource provider accepting both paths and invokes
     * {@link StatefulResourceProvider#copy(String, String)} method on it.
     * Returns false if there's no such provider.
     */
    public Resource copy(final String srcAbsPath, final String destAbsPath) throws PersistenceException {
        final StatefulResourceProvider optimizedSourceProvider = checkSourceAndDest(srcAbsPath, destAbsPath);
        if ( optimizedSourceProvider != null && optimizedSourceProvider.copy(srcAbsPath, destAbsPath) ) {
            return this.getResource(destAbsPath + '/' + ResourceUtil.getName(srcAbsPath), null, null, false);
        }

        final Resource srcResource = this.getResource(srcAbsPath, null, null, false);
        final List<Resource> newResources = new ArrayList<Resource>();
        boolean rollback = true;
        try {
            this.copy(srcResource, destAbsPath, newResources);
            rollback = false;
            return newResources.get(0);
        } finally {
            if ( rollback ) {
                for(final Resource rsrc : newResources) {
                    this.delete(rsrc);
                }
            }
        }
    }

    /**
     * Tries to find a resource provider accepting both paths and invokes
     * {@link StatefulResourceProvider#move(String, String)} method on it.
     * Returns false if there's no such provider.
     */
    public Resource move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        final StatefulResourceProvider optimizedSourceProvider = checkSourceAndDest(srcAbsPath, destAbsPath);
        if ( optimizedSourceProvider != null && optimizedSourceProvider.move(srcAbsPath, destAbsPath) ) {
            return this.getResource(destAbsPath + '/' + ResourceUtil.getName(srcAbsPath), null, null, false);
        }
        final Resource srcResource = this.getResource(srcAbsPath, null, null, false);
        final List<Resource> newResources = new ArrayList<Resource>();
        boolean rollback = true;
        try {
            this.copy(srcResource, destAbsPath, newResources);
            this.delete(srcResource);
            rollback = false;
            return newResources.get(0);
        } finally {
            if ( rollback ) {
                for(final Resource rsrc : newResources) {
                    this.delete(rsrc);
                }
            }
        }
    }

    public ResourceProviderStorage getResourceProviderStorage() {
        return this.storage;
    }

    public @CheckForNull StatefulResourceProvider getStatefulResourceProvider(@Nonnull final ResourceProviderHandler handler)
    throws LoginException {
        if ( handler != null ) {
            return authenticator.getStateful(handler, this);
        }
        return null;
    }

    /**
     * @param path
     * @return
     * @throws LoginException
     */
    private @Nonnull StatefulResourceProvider getBestMatchingProvider(final String path) throws LoginException {
        final Node<ResourceProviderHandler> node = storage.getTree().getBestMatchingNode(path);
        return node == null ? EmptyResourceProvider.SINGLETON : authenticator.getStateful(node.getValue(), this);
    }

    /**
     * @param path
     * @return The modifiable provider or {@code null}
     * @throws LoginException
     */
    private @CheckForNull StatefulResourceProvider getBestMatchingModifiableProvider(final String path) throws LoginException {
        final Node<ResourceProviderHandler> node = storage.getTree().getBestMatchingNode(path);
        if ( node != null && node.getValue().getInfo().isModifiable() ) {
            return authenticator.getStateful(node.getValue(), this);
        }
        return null;
    }

    private static class ChainedIterator<T> extends AbstractIterator<T> {

        private final Iterator<Iterator<T>> iterators;

        private Iterator<T> currentIterator;

        public ChainedIterator(Iterator<Iterator<T>> iterators) {
            this.iterators = iterators;
        }

        @Override
        protected T seek() {
            while (true) {
                if (currentIterator == null) {
                    if (!iterators.hasNext()) {
                        return null;
                    }
                    currentIterator = iterators.next();
                    continue;
                }
                if (currentIterator.hasNext()) {
                    return currentIterator.next();
                } else {
                    currentIterator = null;
                }
            }
        }
    }

    /**
     * This iterator removes duplicated Resource entries. Regular resources
     * overrides the synthetic ones.
     */
    private static class UniqueIterator extends AbstractIterator<Resource> {

        private final Iterator<Resource> input;

        private final Set<String> visited;

        public UniqueIterator(final Set<String> visited, final Iterator<Resource> input) {
            this.input = input;
            this.visited = visited;
        }

        @Override
        protected Resource seek() {
            while (input.hasNext()) {
                final Resource next = input.next();
                final String name = next.getName();

                if (visited.contains(name)) {
                    continue;
                } else {
                    visited.add(name);
                    next.getResourceMetadata().setResolutionPath(next.getPath());
                    return next;
                }
            }

            return null;
        }
    }
}
