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
import static org.apache.sling.spi.resource.provider.ResourceProvider.RESOURCE_TYPE_SYNTHETIC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.query.Query;
import org.apache.sling.api.resource.query.QueryInstructions;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderInfo;
import org.apache.sling.spi.resource.provider.QueryResult;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombinedResourceProvider implements StatefulResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(CombinedResourceProvider.class);

    private final List<StatefulResourceProvider> providers;

    private final ResourceResolver resolver;

    public CombinedResourceProvider(List<StatefulResourceProvider> providers, ResourceResolver resolver) {
        this.providers = new ArrayList<StatefulResourceProvider>(providers);
        // sort descending by paths and rankings (start with longest paths and
        // higher ranks)
        Collections.sort(providers, new Comparator<StatefulResourceProvider>() {
            @Override
            public int compare(StatefulResourceProvider rp1, StatefulResourceProvider rp2) {
                return rp2.getInfo().compareTo(rp1.getInfo());
            }
        });
        this.resolver = resolver;
    }

    @Override
    public ResourceProviderInfo getInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resolver;
    }

    @Override
    public void logout() {
        for (StatefulResourceProvider p : providers) {
            p.logout();
        }
    }

    @Override
    public void refresh() {
        for (StatefulResourceProvider p : providers) {
            p.refresh();
        }
    }

    @Override
    public boolean isLive() {
        for (StatefulResourceProvider p : providers) {
            if (!p.isLive()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Resource getParent(Resource child) {
        String path = child.getPath();
        for (StatefulResourceProvider p : getMatchingProviders(path)) {
            Resource parentCandidate = p.getParent(child);
            if (parentCandidate != null) {
                return parentCandidate;
            }
        }
        String parentPath = ResourceUtil.getParent(path);
        if (parentPath != null && isIntermediatePath(parentPath)) {
            return new SyntheticResource(getResourceResolver(), parentPath, ResourceProvider.RESOURCE_TYPE_SYNTHETIC);
        }
        return null;
    }

    @Override
    public Resource getResource(String path, Resource parent, Map<String, String> parameters, boolean isResolve) {
        if (path == null || path.length() == 0 || path.charAt(0) != '/') {
            logger.debug("Not absolute {}", path);
            return null; // path must be absolute
        }

        try {
            Resource fallbackResource = null;
            for (StatefulResourceProvider p : getMatchingProviders(path)) {
                Resource resource = p.getResource(path, parent, parameters, isResolve);
                if (resource == null) {
                    break;
                }
                if (isContinueResolving(resource)) {
                    logger.debug("Resolved Full {} using {} - continue resolving flag is set!", path, p.getInfo());
                    fallbackResource = resource;
                } else {
                    logger.debug("Resolved Full {} using {}", path, p.getInfo());
                    return resource;
                }
            }
            if (fallbackResource != null) {
                return fallbackResource;
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
                return new SyntheticResource(getResourceResolver(), path, ResourceProvider.RESOURCE_TYPE_SYNTHETIC);
            }
        } catch (SlingException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Unexpected exception while trying to get resource for " + path, e);
        }
        logger.debug("Resource null {} ", path);
        return null;
    }

    private boolean isContinueResolving(Resource resource) {
        return resource.getResourceMetadata() != null
                && resource.getResourceMetadata().containsKey(ResourceMetadata.INTERNAL_CONTINUE_RESOLVING);
    }

    private boolean isIntermediatePath(final String fullPath) {
        for (StatefulResourceProvider p : providers) {
            if (p.getInfo().getPath().startsWith(fullPath)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<Resource> listChildren(final Resource parent) {
        Iterator<Iterator<Resource>> iterators = transformedIterator(providers.iterator(),
                new Transformer() {
                    @Override
                    public Object transform(Object input) {
                        StatefulResourceProvider rp = (StatefulResourceProvider) input;
                        return rp.listChildren(parent);
                    }
                });
        Iterator<Resource> allChildren = new ChainedIterator<Resource>(iterators);
        Iterator<Resource> syntheticChildren = getSyntheticChildren(parent.getPath()).iterator(); 
        Iterator<Resource> uniqueChildren = new UniqueIterator(chainedIterator(allChildren, syntheticChildren));
        return transformedIterator(uniqueChildren, new Transformer() {
            @Override
            public Object transform(Object input) {
                Resource resource = (Resource) input;
                resource.getResourceMetadata().setResolutionPath(resource.getPath());
                return resource;
            }
        });
    }

    private List<Resource> getSyntheticChildren(String parentPath) {
        String parentPathWithSlash = parentPath + "/";
        List<Resource> children = new ArrayList<Resource>();
        for (StatefulResourceProvider p : providers) {
            String providerPath = p.getInfo().getPath();
            if (providerPath.startsWith(parentPathWithSlash)) {
                String childPath = providerPath.substring(0, providerPath.indexOf('/', parentPathWithSlash.length()));
                children.add(new SyntheticResource(getResourceResolver(), childPath, RESOURCE_TYPE_SYNTHETIC));
            }
        }
        return children;
    }

    @Override
    public Collection<String> getAttributeNames() {
        final Set<String> names = new LinkedHashSet<String>();
        for (StatefulResourceProvider p : providers) {
            Collection<String> newNames = p.getAttributeNames();
            if (newNames != null) {
                names.addAll(newNames);
            }
        }
        return names;
    }

    @Override
    public Object getAttribute(String name) {
        for (StatefulResourceProvider p : providers) {
            Object attribute = p.getAttribute(name);
            if (attribute != null) {
                return attribute;
            }
        }
        return null;
    }

    /**
     * Create a resource. Iterate over all viable ModifyingProviderHandlers
     * stopping at the first one which creates the resource and return the
     * created resource.
     *
     * @throws UnsupportedOperationException
     *             If creation is not allowed/possible
     * @throws PersistenceException
     *             If creation fails
     * @return The new resource
     */
    @Override
    public Resource create(String path, Map<String, Object> properties) throws PersistenceException {
        for (StatefulResourceProvider p : getMatchingModifiableProviders(path)) {
            Resource creationResultResource = p.create(path, properties);
            if (creationResultResource != null) {
                return creationResultResource;
            }
        }
        // If none of the viable handlers could create the resource or if the
        // list of handlers was empty, throw an Exception
        throw new UnsupportedOperationException(
                "create '" + ResourceUtil.getName(path) + "' at " + ResourceUtil.getParent(path));
    }

    /**
     * Delete the resource. Iterate over all viable ModifyingProviderHandlers
     * giving each an opportunity to delete the resource if they are able.
     *
     * @throws NullPointerException
     *             if resource is null
     * @throws UnsupportedOperationException
     *             If deletion is not allowed/possible
     * @throws PersistenceException
     *             If deletion fails
     */
    @Override
    public void delete(Resource resource) throws PersistenceException {
        final String path = resource.getPath();
        final Map<String, String> parameters = resource.getResourceMetadata().getParameterMap();
        boolean anyProviderAttempted = false;

        // Give all viable handlers a chance to delete the resource
        for (StatefulResourceProvider p : getMatchingModifiableProviders(path)) {
            Resource providerResource = p.getResource(path, null, parameters, false);
            if (providerResource != null) {
                anyProviderAttempted = true;
                p.delete(providerResource);
            }
        }
        // If none of the viable handlers could delete the resource or if the
        // list of handlers was empty, throw an Exception
        if (!anyProviderAttempted) {
            throw new UnsupportedOperationException("delete at '" + path + "'");
        }
    }

    @Override
    public void revert() {
        for (StatefulResourceProvider p : providers) {
            if (p.getInfo().getModifiable()) {
                p.revert();
            }
        }
    }

    @Override
    public void commit() throws PersistenceException {
        for (StatefulResourceProvider p : providers) {
            if (p.getInfo().getModifiable()) {
                p.commit();
            }
        }
    }

    @Override
    public boolean hasChanges() {
        for (StatefulResourceProvider p : providers) {
            if (p.getInfo().getModifiable() && p.hasChanges()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public QueryResult find(final Query q, final QueryInstructions qi) {
        return new CombinedQueryResult(q, qi);
    }

    @Override
    public String[] getSupportedLanguages() {
        Set<String> supportedLanguages = new LinkedHashSet<String>();
        for (StatefulResourceProvider p : providers) {
            supportedLanguages.addAll(Arrays.asList(p.getSupportedLanguages()));
        }
        return supportedLanguages.toArray(new String[supportedLanguages.size()]);
    }

    @Override
    public Iterator<Resource> findResources(final String query, final String language) {
        List<StatefulResourceProvider> querableRP = getQuerableProviders(language);
        @SuppressWarnings("unchecked")
        Iterator<Iterator<Resource>> iterators = IteratorUtils.transformedIterator(querableRP.iterator(),
                new Transformer() {
                    @Override
                    public Object transform(Object input) {
                        StatefulResourceProvider rp = (StatefulResourceProvider) input;
                        return rp.findResources(query, language);
                    }
                });
        return new ChainedIterator<Resource>(iterators);
    }

    private List<StatefulResourceProvider> getQuerableProviders(String language) {
        List<StatefulResourceProvider> querableProviders = new ArrayList<StatefulResourceProvider>();
        for (StatefulResourceProvider p : providers) {
            if (ArrayUtils.contains(p.getSupportedLanguages(), language)) {
                querableProviders.add(p);
            }
        }
        return querableProviders;
    }

    @Override
    public Iterator<Map<String, Object>> queryResources(final String query, final String language) {
        List<StatefulResourceProvider> querableRP = getQuerableProviders(language);
        @SuppressWarnings("unchecked")
        Iterator<Iterator<Map<String, Object>>> iterators = IteratorUtils.transformedIterator(querableRP.iterator(),
                new Transformer() {
                    @Override
                    public Object transform(Object input) {
                        StatefulResourceProvider rp = (StatefulResourceProvider) input;
                        return rp.queryResources(query, language);
                    }
                });
        return new ChainedIterator<Map<String, Object>>(iterators);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        for (StatefulResourceProvider p : providers) {
            final Object adaptee = p.adaptTo(type);
            if (adaptee != null) {
                return (AdapterType) adaptee;
            }
        }
        return null;
    }

    @Override
    public boolean copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
        List<StatefulResourceProvider> srcProviders = getMatchingProviders(srcAbsPath);
        List<StatefulResourceProvider> dstProviders = getMatchingModifiableProviders(srcAbsPath);
        @SuppressWarnings("unchecked")
        List<StatefulResourceProvider> intersection = ListUtils.intersection(srcProviders, dstProviders);
        for (StatefulResourceProvider p : intersection) {
            if (p.copy(srcAbsPath, destAbsPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        List<StatefulResourceProvider> srcProviders = getMatchingModifiableProviders(srcAbsPath);
        List<StatefulResourceProvider> dstProviders = getMatchingModifiableProviders(srcAbsPath);
        @SuppressWarnings("unchecked")
        List<StatefulResourceProvider> intersection = ListUtils.intersection(srcProviders, dstProviders);
        for (StatefulResourceProvider p : intersection) {
            if (p.move(srcAbsPath, destAbsPath)) {
                return true;
            }
        }
        return false;
    }

    private List<StatefulResourceProvider> getMatchingProviders(String path) {
        List<StatefulResourceProvider> matching = new ArrayList<StatefulResourceProvider>();
        for (StatefulResourceProvider p : providers) {
            if (path.startsWith(p.getInfo().getPath())) {
                matching.add(p);
            }
        }
        return matching;
    }

    private List<StatefulResourceProvider> getMatchingModifiableProviders(String path) {
        List<StatefulResourceProvider> matching = getMatchingProviders(path);
        Iterator<StatefulResourceProvider> it = matching.iterator();
        while (it.hasNext()) {
            if (!it.next().getInfo().getModifiable()) {
                it.remove();
            }
        }
        return matching;
    }

    @Override
    public StatefulResourceProvider clone(Map<String, Object> authenticationInfo, ResourceResolver resolver)
            throws LoginException {
        List<StatefulResourceProvider> list = new ArrayList<StatefulResourceProvider>();
        for (StatefulResourceProvider p : providers) {
            list.add(p.clone(authenticationInfo, resolver));
        }
        return new CombinedResourceProvider(list, resolver);
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
            Iterator<Iterator<Resource>> iterators = IteratorUtils.transformedIterator(providers.iterator(),
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
            this.delayedIterator = delayed.values().iterator();
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

            if (delayedIterator.hasNext()) {
                return delayedIterator.next();
            }
            return null;
        }
    }
}
