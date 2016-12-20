/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.api.wrappers;

import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * The {@code ResourceResolverWrapper} is a wrapper for any {@code ResourceResolver}, delegating all method calls to the wrapped resource
 * resolver by default. Extensions of this class may overwrite any method to return different values as appropriate.
 */
@ConsumerType
public class ResourceResolverWrapper implements ResourceResolver {

    private ResourceResolver wrapped;

    /**
     * Creates a new wrapper instance, delegating all calls to the given {@code resolver}.
     *
     * @param resolver the wrapped resource resolver
     */
    public ResourceResolverWrapper(ResourceResolver resolver) {
        wrapped = resolver;
    }

    /**
     * Wraps and returns the {@code Resource} obtained by calling {@code resolve} on the wrapped resource resolver.
     *
     * @param request The http servlet request object providing more hints at
     *                how to resolve the <code>absPath</code>. This parameter may be
     *                <code>null</code> in which case the implementation should use
     *                reasonable defaults.
     * @param absPath The absolute path to be resolved to a resource. If this
     *                parameter is <code>null</code>, it is assumed to address the
     *                root of the resource tree. If the path is relative it is
     *                assumed relative to the root, that is a slash is prepended to
     *                the path before resolving it.
     * @return a wrapped resource obtained through the wrapped resource resolver
     */
    @Nonnull
    @Override
    public Resource resolve(@Nonnull HttpServletRequest request, @Nonnull String absPath) {
        return ResourceResolverResourceWrapper.wrap(this, wrapped.resolve(request, absPath));
    }

    /**
     * Wraps and returns the {@code Resource} obtained by calling {@code resolve} on the wrapped resource resolver.
     *
     * @param absPath The absolute path to be resolved to a resource. If this
     *                parameter is <code>null</code>, it is assumed to address the
     *                root of the resource tree. If the path is relative it is
     *                assumed relative to the root, that is a slash is prepended to
     *                the path before resolving it.
     * @return a wrapped resource obtained through the wrapped resource resolver
     */
    @Nonnull
    @Override
    public Resource resolve(@Nonnull String absPath) {
        return ResourceResolverResourceWrapper.wrap(this, wrapped.resolve(absPath));
    }

    /**
     * Wraps and returns the {@code Resource} obtained by calling {@code resolve} on the wrapped resource resolver.
     *
     * @param request The http servlet request object used to resolve the
     *                resource for. This must not be <code>null</code>.
     * @return a wrapped resource obtained through the wrapped resource resolver
     */
    @SuppressWarnings("deprecation")
    @Nonnull
    @Override
    public Resource resolve(@Nonnull HttpServletRequest request) {
        return ResourceResolverResourceWrapper.wrap(this, wrapped.resolve(request));
    }

    @Nonnull
    @Override
    public String map(@Nonnull String resourcePath) {
        return wrapped.map(resourcePath);
    }

    @Override
    public String map(@Nonnull HttpServletRequest request, @Nonnull String resourcePath) {
        return wrapped.map(request, resourcePath);
    }

    /**
     * Wraps and returns the {@code Resource} obtained by calling {@code getResource} on the wrapped resource resolver.
     *
     * @param path The absolute path to the resource object to be loaded. The
     *             path may contain relative path specifiers like <code>.</code>
     *             (current location) and <code>..</code> (parent location),
     *             which are resolved by this method. If the path is relative,
     *             that is the first character is not a slash, implementations
     *             are expected to apply a search path algorithm to resolve the
     *             relative path to a resource.
     * @return a wrapped resource obtained through the wrapped resource resolver
     */
    @Override
    public Resource getResource(@Nonnull String path) {
        return ResourceResolverResourceWrapper.wrap(this, wrapped.getResource(path));
    }

    /**
     * Wraps and returns the {@code Resource} obtained by calling {@code getResource} on the wrapped resource resolver.
     *
     * @param base The base {@link Resource} against which a relative path
     *             argument given by <code>path</code> is resolved. This
     *             parameter may be <code>null</code> if the <code>path</code> is
     *             known to be absolute.
     * @param path The path to the resource object to be loaded. If the path is
     *             relative, i.e. does not start with a slash (<code>/</code>),
     *             the resource relative to the given <code>base</code> resource
     *             is returned. The path may contain relative path specifiers
     *             like <code>.</code> (current location) and <code>..</code>
     *             (parent location), which are resolved by this method.
     * @return a wrapped resource obtained through the wrapped resource resolver
     */
    @Override
    public Resource getResource(Resource base, @Nonnull String path) {
        return ResourceResolverResourceWrapper.wrap(this, wrapped.getResource(base, path));
    }

    @Nonnull
    @Override
    public String[] getSearchPath() {
        return wrapped.getSearchPath();
    }

    /**
     * Wraps and returns the {@code Iterator} obtained by calling {@code listChildren} on the wrapped resource resolver.
     *
     * @param parent The {@link Resource Resource} whose children are requested.
     * @return a wrapped iterator obtained through the wrapped resource resolver
     */
    @Nonnull
    @Override
    public Iterator<Resource> listChildren(@Nonnull Resource parent) {
        return new ResourceIteratorWrapper(this, wrapped.listChildren(parent));
    }

    /**
     * Wraps and returns the {@code Resource} obtained by calling {@code getParent} on the wrapped resource resolver.
     *
     * @param child The {@link Resource Resource} whose parent is requested.
     * @return a wrapped resource obtained through the wrapped resource resolver
     */
    @Override
    public Resource getParent(@Nonnull Resource child) {
        return ResourceResolverResourceWrapper.wrap(this, wrapped.getParent(child));
    }

    /**
     * Wraps and returns the {@code Iterable} obtained by calling {@code getChildren} on the wrapped resource resolver.
     *
     * @param parent The {@link Resource Resource} whose children are requested.
     * @return a wrapped iterable obtained through the wrapped resource resolver
     */
    @Nonnull
    @Override
    public Iterable<Resource> getChildren(@Nonnull final Resource parent) {
        final ResourceResolverWrapper resourceResolverWrapper = this;
        return new Iterable<Resource>() {
            @Override
            public Iterator<Resource> iterator() {
                return new ResourceIteratorWrapper(resourceResolverWrapper, wrapped.getChildren(parent).iterator());
            }
        };
    }

    /**
     * Wraps and returns the {@code Iterator} obtained by calling {@code findResources} on the wrapped resource resolver.
     *
     * @param query    The query string to use to find the resources.
     * @param language The language in which the query is formulated. The
     *                 language should always be specified. However for
     *                 compatibility with older version, if no language
     *                 is specified, "xpath" is used.
     * @return a wrapped iterator obtained through the wrapped resource resolver
     */
    @Nonnull
    @Override
    public Iterator<Resource> findResources(@Nonnull String query, String language) {
        return new ResourceIteratorWrapper(this, wrapped.findResources(query, language));
    }

    @Nonnull
    @Override
    public Iterator<Map<String, Object>> queryResources(@Nonnull String query, String language) {
        return wrapped.queryResources(query, language);
    }

    @Override
    public boolean hasChildren(@Nonnull Resource resource) {
        return wrapped.hasChildren(resource);
    }

    /**
     * Wraps and returns the {@code ResourceResolver} obtained by calling {@code clone} on the wrapped resource resolver.
     *
     * @param authenticationInfo The map or credential data to overlay the
     *                           original credential data with for the creation of a new
     *                           resource resolver. This may be <code>null</code> in which case
     *                           the same credential data is used as was used to create this
     *                           instance.
     * @return a wrapped resource resolver
     */
    @Nonnull
    @Override
    public ResourceResolver clone(Map<String, Object> authenticationInfo) throws LoginException {
        ResourceResolver toWrap = wrapped.clone(authenticationInfo);
        return new ResourceResolverWrapper(toWrap);
    }

    @Override
    public boolean isLive() {
        return wrapped.isLive();
    }

    @Override
    public void close() {
        wrapped.close();
    }

    @Override
    public String getUserID() {
        return wrapped.getUserID();
    }

    @Nonnull
    @Override
    public Iterator<String> getAttributeNames() {
        return wrapped.getAttributeNames();
    }

    @Override
    public Object getAttribute(@Nonnull String name) {
        return wrapped.getAttribute(name);
    }

    @Override
    public void delete(@Nonnull Resource resource) throws PersistenceException {
        wrapped.delete(resource);
    }

    /**
     * Wraps the {@code Resource} obtained by calling {@code create} on the wrapped resource resolver.
     *
     * @param parent     The parent resource
     * @param name       The name of the child resource - this is a plain name, not a path!
     * @param properties Optional properties for the resource
     * @return a wrapped resource obtained through the wrapped resource resolver
     */
    @Nonnull
    @Override
    public Resource create(@Nonnull Resource parent, @Nonnull String name, Map<String, Object> properties) throws PersistenceException {
        return ResourceResolverResourceWrapper.wrap(this, wrapped.create(parent, name, properties));
    }

    @Override
    public void revert() {
        wrapped.revert();
    }

    @Override
    public void commit() throws PersistenceException {
        wrapped.commit();
    }

    @Override
    public boolean hasChanges() {
        return wrapped.hasChanges();
    }

    @Override
    public String getParentResourceType(Resource resource) {
        return wrapped.getParentResourceType(resource);
    }

    @Override
    public String getParentResourceType(String resourceType) {
        return wrapped.getParentResourceType(resourceType);
    }

    @Override
    public boolean isResourceType(Resource resource, String resourceType) {
        return wrapped.isResourceType(resource, resourceType);
    }

    @Override
    public void refresh() {
        wrapped.refresh();
    }

    /**
     * Wraps the {@code Resource} obtained by calling {@code copy} on the wrapped resource resolver.
     *
     * @param srcAbsPath  the path of the resource to be copied.
     * @param destAbsPath the location to which the resource at
     *                    <code>srcAbsPath</code> is to be copied.
     * @return a wrapped resource obtained through the wrapped resource resolver
     */
    @Override
    public Resource copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return ResourceResolverResourceWrapper.wrap(this, wrapped.copy(srcAbsPath, destAbsPath));
    }

    /**
     * Wraps the {@code Resource} obtained by calling {@code move} on the wrapped resource resolver.
     *
     * @param srcAbsPath  the path of the resource to be moved.
     * @param destAbsPath the location to which the resource at
     *                    <code>srcAbsPath</code> is to be moved.
     * @return a wrapped resource obtained through the wrapped resource resolver
     */
    @Override
    public Resource move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return ResourceResolverResourceWrapper.wrap(this, wrapped.move(srcAbsPath, destAbsPath));
    }

    @Override
    public <AdapterType> AdapterType adaptTo(@Nonnull Class<AdapterType> type) {
        return wrapped.adaptTo(type);
    }

    /**
     * {@code ResourceWrapper} that overwrites the {@link #getResourceResolver()} to return the {@link ResourceResolverWrapper}.
     */
    private static class ResourceResolverResourceWrapper extends ResourceWrapper {

        private final ResourceResolverWrapper resolver;

        /**
         * Creates a new wrapper instance delegating all method calls to the given {@code resource}.
         *
         * @param resolver the wrapping resource resolver
         * @param resource the wrapped resource
         */
        ResourceResolverResourceWrapper(ResourceResolverWrapper resolver, Resource resource) {
            super(resource);
            this.resolver = resolver;
        }

        /**
         * Returns the wrapping resource resolver from which this resource was obtained.
         *
         * @return the wrapping resource resolver
         */
        @Override
        public ResourceResolver getResourceResolver() {
            return resolver;
        }

        private static Resource wrap(ResourceResolverWrapper resolver, Resource resource) {
            if (resource != null) {
                return new ResourceResolverResourceWrapper(resolver, resource);
            }
            return null;
        }
    }

    /**
     * {@code IteratorWrapper} that wraps every returned {@code Resource} with the {@code ResourceResolverResourceWrapper}.
     *
     * @see ResourceResolverResourceWrapper
     */
    private static class ResourceIteratorWrapper extends IteratorWrapper<Resource> {

        private final ResourceResolverWrapper resolver;

        public ResourceIteratorWrapper(ResourceResolverWrapper resolver, Iterator<Resource> wrappedIterator) {
            super(wrappedIterator);
            this.resolver = resolver;
        }

        @Override
        public Resource next() {
            return ResourceResolverResourceWrapper.wrap(resolver, super.next());
        }
    }
}
