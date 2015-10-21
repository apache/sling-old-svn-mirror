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

import static org.apache.commons.collections.IteratorUtils.chainedIterator;
import static org.apache.commons.collections.IteratorUtils.transformedIterator;
import static org.apache.sling.api.resource.ResourceUtil.getName;
import static org.apache.sling.spi.resource.provider.ResourceProvider.RESOURCE_TYPE_SYNTHETIC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.query.Query;
import org.apache.sling.api.resource.query.QueryInstructions;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderHandler;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderInfo;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderStorage;
import org.apache.sling.resourceresolver.impl.providers.tree.Node;
import org.apache.sling.spi.resource.provider.QueryResult;
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

    private static final StatefulResourceProvider EMPTY_PROVIDER = new EmptyResourceProvider();

    private final ResourceProviderStorage storage;

    private final ResourceResolver resolver;

    private final ResourceProviderAuthenticator authenticator;

    public CombinedResourceProvider(ResourceProviderStorage storage, ResourceResolver resolver, ResourceProviderAuthenticator authenticator) {
        this.storage = storage;
        this.resolver = resolver;
        this.authenticator = authenticator;
    }

    /**
     * Logouts from all providers.
     */
    public void logout() {
        for (StatefulResourceProvider p : authenticator.getAllUsedAuthenticated()) {
            p.logout();
        }
    }

    /**
     * Refreshes all providers.
     */
    public void refresh() {
        for (StatefulResourceProvider p : authenticator.getAll(storage.getRefreshableHandlers())) {
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
        String path = child.getPath();
        List<StatefulResourceProvider> matching = getMatchingProviders(path);
        Resource parentCandidate = head(matching).getParent(child, tail(matching));
        if (parentCandidate != null) {
            return parentCandidate;
        }
        String parentPath = ResourceUtil.getParent(path);
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
            List<StatefulResourceProvider> matching = getMatchingProviders(path);
            Resource resourceCandidate = head(matching).getResource(path, parent, parameters, isResolve, tail(matching));
            if (resourceCandidate != null) {
                return resourceCandidate;
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
        } catch (SlingException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Unexpected exception while trying to get resource for " + path, e);
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
        List<StatefulResourceProvider> matching = getMatchingProviders(parent.getPath());
        Iterator<Resource> realChildren = head(matching).listChildren(parent, tail(matching));
        Iterator<Resource> syntheticChildren = getSyntheticChildren(parent).iterator();
        Iterator<Resource> allChildren;
        if (realChildren == null) {
            allChildren = syntheticChildren;
        } else {
            allChildren = new UniqueIterator(chainedIterator(realChildren, syntheticChildren));
        } 
        return transformedIterator(allChildren, new Transformer() {
            @Override
            public Object transform(Object input) {
                Resource resource = (Resource) input;
                resource.getResourceMetadata().setResolutionPath(resource.getPath());
                return resource;
            }
        });
    }

    private List<Resource> getSyntheticChildren(Resource parent) {
        Node<ResourceProviderHandler> node = storage.getTree().getNode(parent.getPath());
        if (node == null) {
            return Collections.emptyList();
        }
        List<Resource> children = new ArrayList<Resource>();
        for (Entry<String, Node<ResourceProviderHandler>> entry : node.getChildren().entrySet()) {
            final String name = entry.getKey();
            final ResourceProviderHandler handler = entry.getValue().getValue();
            final String childPath = new StringBuilder(parent.getPath()).append('/').append(name).toString();
            final Resource child;
            if (handler == null) {
                child = new SyntheticResource(resolver, childPath, RESOURCE_TYPE_SYNTHETIC);
            } else { 
                child = authenticator.getStateful(handler).getResource(childPath, parent, null, false, null);
            }
            if (child != null) {
                children.add(child);
            }
        }
        return children;
    }

    /**
     * Returns the union of all attribute names.
     */
    public Collection<String> getAttributeNames() {
        final Set<String> names = new LinkedHashSet<String>();
        for (StatefulResourceProvider p : authenticator.getAll(storage.getAttributableHandlers())) {
            Collection<String> newNames = p.getAttributeNames();
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
        for (StatefulResourceProvider p : authenticator.getAll(storage.getAttributableHandlers())) {
            Object attribute = p.getAttribute(name);
            if (attribute != null) {
                return attribute;
            }
        }
        return null;
    }

    /**
     * Create a resource. Iterate over all modifiable ResourceProviders
     * stopping at the first one which creates the resource and return the
     * created resource.
     *
     * @throws UnsupportedOperationException
     *             If creation is not allowed/possible
     * @throws PersistenceException
     *             If creation fails
     * @return The new resource
     */
    public Resource create(String path, Map<String, Object> properties) throws PersistenceException {
        List<StatefulResourceProvider> matching = getMatchingModifiableProviders(path);
        Resource creationResultResource = head(matching).create(path, properties, tail(matching));
        if (creationResultResource != null) {
            return creationResultResource;
        }
        // If none of the viable handlers could create the resource or if the
        // list of handlers was empty, throw an Exception
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
    public void delete(Resource resource) throws PersistenceException {
        final String path = resource.getPath();
        final Map<String, String> parameters = resource.getResourceMetadata().getParameterMap();
        boolean anyProviderAttempted = false;

        // Give all viable handlers a chance to delete the resource
        for (StatefulResourceProvider p : getMatchingModifiableProviders(path)) {
            Resource providerResource = p.getResource(path, null, parameters, false, null);
            if (providerResource != null) {
                anyProviderAttempted = true;
                p.delete(providerResource, null);
            }
        }
        // If none of the viable handlers could delete the resource or if the
        // list of handlers was empty, throw an Exception
        if (!anyProviderAttempted) {
            throw new UnsupportedOperationException("delete at '" + path + "'");
        }
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
     * Queries all resource providers and combines the results.
     */
    public QueryResult find(final Query q, final QueryInstructions qi) {
        return new CombinedQueryResult(q, qi);
    }

    /**
     * Return the union of query languages supported by the providers.
     */
    public String[] getSupportedLanguages() {
        Set<String> supportedLanguages = new LinkedHashSet<String>();
        for (StatefulResourceProvider p : authenticator.getAll(storage.getJcrQuerableHandlers())) {
            supportedLanguages.addAll(Arrays.asList(p.getSupportedLanguages()));
        }
        return supportedLanguages.toArray(new String[supportedLanguages.size()]);
    }

    /**
     * Queries all resource providers and combines the results.
     */
    public Iterator<Resource> findResources(final String query, final String language) {
        List<StatefulResourceProvider> querableRP = getQuerableProviders(language);
        List<Iterator<Resource>> iterators = new ArrayList<Iterator<Resource>>(querableRP.size());
        for (StatefulResourceProvider p : querableRP) {
            iterators.add(p.findResources(query, language));
        }
        return new ChainedIterator<Resource>(iterators.iterator());
    }

    private List<StatefulResourceProvider> getQuerableProviders(String language) {
        List<StatefulResourceProvider> querableProviders = new ArrayList<StatefulResourceProvider>();
        for (StatefulResourceProvider p : authenticator.getAll(storage.getJcrQuerableHandlers())) {
            if (ArrayUtils.contains(p.getSupportedLanguages(), language)) {
                querableProviders.add(p);
            }
        }
        return querableProviders;
    }

    /**
     * Queries all resource providers and combines the results.
     */
    public Iterator<Map<String, Object>> queryResources(final String query, final String language) {
        List<StatefulResourceProvider> querableRP = getQuerableProviders(language);
        List<Iterator<Map<String, Object>>> iterators = new ArrayList<Iterator<Map<String, Object>>>(querableRP.size());
        for (StatefulResourceProvider p : querableRP) {
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
        for (StatefulResourceProvider p : authenticator.getAll(storage.getAdaptableHandlers())) {
            final Object adaptee = p.adaptTo(type);
            if (adaptee != null) {
                return (AdapterType) adaptee;
            }
        }
        return null;
    }

    /**
     * Tries to find a resource provider accepting both paths and invokes
     * {@link StatefulResourceProvider#copy(String, String)} method on it.
     * Returns false if there's no such provider.
     */
    public boolean copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
        List<StatefulResourceProvider> srcProviders = getMatchingProviders(srcAbsPath);
        List<StatefulResourceProvider> dstProviders = getMatchingModifiableProviders(destAbsPath);
        @SuppressWarnings("unchecked")
        List<StatefulResourceProvider> intersection = ListUtils.intersection(srcProviders, dstProviders);
        return head(intersection).copy(srcAbsPath, destAbsPath, tail(intersection));
    }

    /**
     * Tries to find a resource provider accepting both paths and invokes
     * {@link StatefulResourceProvider#move(String, String)} method on it.
     * Returns false if there's no such provider.
     */
    public boolean move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        List<StatefulResourceProvider> srcProviders = getMatchingModifiableProviders(srcAbsPath);
        List<StatefulResourceProvider> dstProviders = getMatchingModifiableProviders(destAbsPath);
        @SuppressWarnings("unchecked")
        List<StatefulResourceProvider> intersection = ListUtils.intersection(srcProviders, dstProviders);
        return head(intersection).move(srcAbsPath, destAbsPath, tail(intersection));
    }

    private List<StatefulResourceProvider> getMatchingProviders(String path) {
        List<ResourceProviderHandler> handlers = storage.getTree().getMatchingNodes(path);
        StatefulResourceProvider[] matching = new StatefulResourceProvider[handlers.size()];
        int i = matching.length - 1;
        for (ResourceProviderHandler h : handlers) {
            matching[i--] = authenticator.getStateful(h); // reverse order
        }
        return Arrays.asList(matching);
    }

    private List<StatefulResourceProvider> getMatchingModifiableProviders(String path) {
        List<ResourceProviderHandler> handlers = storage.getTree().getMatchingNodes(path);
        List<StatefulResourceProvider> matching = new ArrayList<StatefulResourceProvider>(handlers.size());
        for (ResourceProviderHandler h : handlers) {
            if (h.getInfo().getModifiable()) {
                matching.add(authenticator.getStateful(h));
            }
        }
        Collections.reverse(matching);
        return matching;
    }

    private static StatefulResourceProvider head(List<StatefulResourceProvider> list) {
        if (list.isEmpty()) {
            return EMPTY_PROVIDER;
        } else {
            return list.get(0);
        }
    }

    private static <T> List<T> tail(List<T> list) {
        if (list.isEmpty()) {
            return Collections.emptyList();
        } else {
            return list.subList(1, list.size());
        }
    }

    private class CombinedQueryResult extends QueryResult implements Iterable<Resource> {

        private final Query q;

        private final QueryInstructions qi;

        public CombinedQueryResult(Query q, QueryInstructions qi) {
            this.q = q;
            this.qi = qi;
        }

        @Override
        public Iterable<Resource> getResources() {
            return this;
        }

        @Override
        public Iterator<Resource> iterator() {
            @SuppressWarnings("unchecked")
            Iterator<Iterator<Resource>> iterators = IteratorUtils.transformedIterator(authenticator.getAll(storage.getNativeQuerableHandlers()).iterator(),
                    new Transformer() {
                        @Override
                        public Object transform(Object input) {
                            StatefulResourceProvider rp = (StatefulResourceProvider) input;
                            return rp.find(q, qi).getResources().iterator();
                        }
                    });
            return new ChainedIterator<Resource>(iterators);
        }
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

        private final List<String> visited;

        private final Map<String, Resource> delayed;

        private Iterator<Resource> delayedIterator;

        public UniqueIterator(Iterator<Resource> input) {
            this.input = input;
            this.visited = new ArrayList<String>();
            this.delayed = new LinkedHashMap<String, Resource>();
        }

        @Override
        protected Resource seek() {
            while (input.hasNext()) {
                Resource next = input.next();
                String path = next.getPath();

                if (visited.contains(path)) {
                    continue;
                } else if (next instanceof SyntheticResource) {
                    delayed.put(path, next);
                } else {
                    visited.add(path);
                    delayed.remove(path);
                    return next;
                }
            }

            if (delayedIterator == null) {
                delayedIterator = delayed.values().iterator();
            }
            if (delayedIterator.hasNext()) {
                return delayedIterator.next();
            }
            return null;
        }
    }
}
