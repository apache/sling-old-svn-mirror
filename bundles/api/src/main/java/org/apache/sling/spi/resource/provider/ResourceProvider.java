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
package org.apache.sling.spi.resource.provider;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * API for providers of resources. Used by the {@link org.apache.sling.api.resource.ResourceResolver} to
 * transparently access resources from different locations such as a JCR
 * repository, a database, or bundle resources.
 * <p>
 * This extension point is defined by an abstract class (in contrast to
 * an interface) as this allows to add new functionality in new versions
 * without breaking any implementation.
 * <p>
 * This service is intended to be implemented by providers of resource
 * instances on behalf of the {@link org.apache.sling.api.resource.ResourceResolver}. It
 * is not intended to be used by client applications directly. A resource
 * provider implements this service by extending this class.
 * <p>
 * A resource provider must set the {@link #PROPERTY_ROOT} property with an
 * absolute path. This is the mount point of the resource provider. If there
 * is more than one provider registering for the same root, only the one
 * with the highest service ranking is used.
 * <p>
 * If a provider is used in the resource tree, it gets activated through
 * a call of the {@link #start(ProviderContext)} method. If the
 * provider is not used anymore within the resource tree, the
 * {@link #stop()} method is called. Whenever
 * information concerning the provider is changed while the provider
 * is used, the {@link #update(long)} method is called. The provider context
 * instance which is passed to the {@link #start(ProviderContext)} method
 * contains the updated state.
 * <p>
 * Some resource providers might require (user) authentication. For example
 * the JCR resource provider uses authenticated sessions. If a provider
 * requires authentication it must indicate this by setting the service
 * registration property {@link #PROPERTY_AUTHENTICATE} to either
 * {@link #AUTHENTICATE_LAZY} or {@link #AUTHENTICATE_REQUIRED}. In these
 * cases, the resource resolver calls {@link #authenticate(Map)} and on
 * successful authentication the provider returns a state object for
 * the current user. This object is passed into the provider with
 * every method through {@link ResolveContext#getProviderState()}.
 * If a provider requires authentication, the {@link #logout(Object)} method
 * is called, when the resource resolver is closed. If the provider
 * does not set this service property or sets it to {@link #AUTHENTICATE_NO}
 * the {@link #authenticate(Map)} and {@link #logout(Object)} method
 * are never called and therefore {@link ResolveContext#getProviderState()}
 * will return {@code null}.
 * <p>
 * Each method gets the {@link ResourceContext} which gives access to
 * further functionality.
 *
 * @since 1.0.0 (Sling API Bundle 2.11.0)
 */
@ConsumerType
public abstract class ResourceProvider<T> {

    /**
     * The name of the service registration property containing the root path
     * of the resources provided by this provider.
     * If this property is missing, empty or invalid, the provider is ignored.
     * (value is "provider.root")
     */
    public static final String PROPERTY_ROOT = "provider.root";

    /**
     * Optional service registration property setting a name for the provider.
     * The name must not be unique. The name in combination with the root
     * can be used to identify a resource provider.
     */
    public static final String PROPERTY_NAME = "provider.name";

    /**
     * The name of the service registration property containing a boolean
     * flag indicating if the ResourceAccessSecurity service should be used for
     * this provider or not. ResourceAccessSecurity should only be used if the
     * underlying storage does not provide access control
     * The default for this value is {@code false}.
     * (value is "provider.useResourceAccessSecurity")
     */
    public static final String PROPERTY_USE_RESOURCE_ACCESS_SECURITY = "provider.useResourceAccessSecurity";

    /**
     * Default value for {@link #PROPERTY_AUTHENTICATE}
     * @see #PROPERTY_AUTHENTICATE
     */
    public static final String AUTHENTICATE_NO = "no";

    /**
     * Value for {@link #PROPERTY_AUTHENTICATE} indicating authentication is required.
     * @see #PROPERTY_AUTHENTICATE
     */
    public static final String AUTHENTICATE_REQUIRED = "required";

    /**
     * Value for {@link #PROPERTY_AUTHENTICATE} indicating authentication is needed
     * and will be done on demand.
     * @see #PROPERTY_AUTHENTICATE
     */
    public static final String AUTHENTICATE_LAZY = "lazy";

    /**
     * If a resource provider needs the user to be authenticated this property must be set
     * to either {@link #AUTHENTICATE_LAZY} or {@link #AUTHENTICATE_REQUIRED}.
     * If it is set to {@link #AUTHENTICATE_REQUIRED}, the {@link #authenticate(Map)} method
     * is called when the resource resolver is created and only if authentication against
     * all resource providers marked as required is successful, a resource resolver is
     * created. Otherwise the creation fails.
     * If a provider sets this property to {@link #AUTHENTICATE_LAZY}, the authenticate method
     * is only invoked if a resource from this provider is requested. This might also happen
     * for traversal or queries. If the authentication fails, resources from this provider
     * are not accessible.
     * If this property is not set or set to {@link #AUTHENTICATE_NO}, no authentication
     * is required for this provider and the {@link #authenticate(Map)} is never invoked.
     * String service property, default value is {@link #AUTHENTICATE_NO}.
     * (value is "provider.authenticate")
     */
    public static final String PROPERTY_AUTHENTICATE = "provider.authenticate";

    /**
     * A modifiable resource provider is capable of creating, changing and deleting resources.
     * This means the methods {@link #create(ResolveContext, String, Map)},
     * {@link #delete(ResolveContext, Resource)} and adapting a resource to a modifiable
     * value map is supported.
     * If this flag is set to {@code false}, the resource resolver does not take this
     * provider into account for modifications and modification operations to this provider
     * always result in an exception.
     * If this is set to {@code true}, the property {@link ResourceProvider#PROPERTY_AUTHENTICATE}
     * must require authentication, otherwise this provider registration is considered
     * invalid and the provider is not used.
     * Boolean service property, default value is {@code false}.
     * (value is "provider.modifiable")
     */
    public static final String PROPERTY_MODIFIABLE = "provider.modifiable";

    /**
     * If this flag is set to {@code true}, the resource resolver will use this provider
     * for the adaptTo() operation.
     * Boolean service property, default value is {@code false}.
     * (value is "provider.adaptable")
     */
    public static final String PROPERTY_ADAPTABLE = "provider.adaptable";

    /**
     * If this flag is set to {@code true}, the resource resolver will call {@link #refresh(ResolveContext)}
     * when it's refreshed itself.
     * Boolean service property, default value is {@code false}.
     * (value is "provider.refreshable")
     */
    public static final String PROPERTY_REFRESHABLE = "provider.refreshable";

    /**
     * If this flag is set to {@code true}, the resource resolver will try to get the attribute
     * names and the attribute values from this provider.
     * Boolean service property, default value is {@code false}.
     * (value is "provider.attributable")
     */
    public static final String PROPERTY_ATTRIBUTABLE = "provider.attributable";

    /**
     * The authentication information property referring to the bundle
     * providing a service for which a resource provider is to be retrieved. If
     * this property is provided, the
     * {@link org.apache.sling.api.resource.ResourceResolverFactory#SUBSERVICE} property may also be
     * present.
     * <p>
     * {@link org.apache.sling.api.resource.ResourceResolverFactory} implementations must provide this
     * property if their implementation of the
     * {@link org.apache.sling.api.resource.ResourceResolverFactory#getServiceResourceResolver(Map)} method
     * use a resource provider factory.
     * <p>
     * The type of this property, if present, is
     * {@code org.osgi.framework.Bundle}.
     */
    public static final String AUTH_SERVICE_BUNDLE = "sling.service.bundle";

    /**
     * The authentication information property indicating to use an
     * administrative login. This property must be set of the resource
     * resolver is created through {@link org.apache.sling.api.resource.ResourceResolverFactory#getAdministrativeResourceResolver(Map)}.
     */
    public static final String AUTH_ADMIN = "provider.auth.admin";

    /**
     * The resource type be set on resources returned by the
     * {@link #listChildren(ResolveContext, Resource)} method to enable traversing the
     * resource
     * tree down to a deeply nested provided resource which has no concrete
     * parent hierarchy (value is"sling:syntheticResourceProviderResource").
     *
     * @see #listChildren(ResolveContext, Resource)
     */
    public static final String RESOURCE_TYPE_SYNTHETIC = "sling:syntheticResourceProviderResource";

    /** The context for this provider. */
    private volatile ProviderContext ctx;

    /**
     * With a call to this method, the provider implementation is notified that
     * it is used in the resource tree.
     * @param ctx The context for this provider.
     */
    public void start(@Nonnull ProviderContext ctx) {
        this.ctx = ctx;
    }

    /**
     * With a call to this method, the provider implementation is notified
     * that it is not used anymore in the resource tree.
     */
    public void stop() {
        this.ctx = null;
    }

    /**
     * With a call to this method, the provider implementation is notified
     * that any information regarding the registration of the provider
     * has changed. For example, observation listeners might have changed.
     * This method is only called while the provider is used in the resource
     * tree.
     * @param changeSet A bit set of provider info that has changed.
     * @see ProviderContext#OBSERVATION_LISTENER_CHANGED
     * @see ProviderContext#EXCLUDED_PATHS_CHANGED
     */
    public void update(final long changeSet) {
        // nothing to do here
    }

    /**
     * Get the current provider context.
     * @return The provider context or {@code null} if the provider is currently
     *         not used in the resource tree.
     */
    protected @CheckForNull ProviderContext getProviderContext() {
        return this.ctx;
    }

    /**
     * Authenticate against the resource provider.
     * <p>
     * Returns a provider context object if authentication is successful. The
     * context object is passed to the resource provider in all messages through
     * the {@link ResourceContext}. A valid implementation might return {@code null}
     * as the context information.
     * <p>
     * If authentication fails a {@code LoginException} must be thrown.
     * <p>
     * The returned context object grants access to the provided resources with
     * privileges assigned to the service provided by the calling bundle.
     * <p>
     * The {@code authenticationInfo} map will in general contain the same
     * information as provided to the respective {@link org.apache.sling.api.resource.ResourceResolver}
     * method. For
     * <p>
     * The provided {@code authenticationInfo} map may be used to provide
     * additional information such as the {@link #AUTH_SERVICE_BUNDLE}.
     * If this property is provided, additional information like
     * {@link org.apache.sling.api.resource.ResourceResolverFactory#SUBSERVICE} might be provided, but the
     * {@link org.apache.sling.api.resource.ResourceResolverFactory#USER} and
     * {@link org.apache.sling.api.resource.ResourceResolverFactory#PASSWORD}
     * properties provided in the map must be ignored.
     * <p>
     * The {@link org.apache.sling.api.resource.ResourceResolverFactory#USER_IMPERSONATION}
     * property is obeyed but requires that the
     * authenticated user has permission to impersonate as the requested user.
     * If such permission is missing, a {@code LoginException} is thrown.
     * <p>
     * The resource resolver implementation will call the {@link #logout(Object)}
     * method once the resource resolver is closed. However, if the resource
     * provider is already unregistered when the resource resolver is closed,
     * logout can't be called. Therefore the returned state object might
     * implement {@link java.io.Closeable}. In this case close is called
     * on the state object.
     *
     * @param authenticationInfo A map of further credential information which
     *            may be used by the implementation to parameterize how the
     *            resource resolver is created.
     * @return A context data object according to the
     *         {@code authenticationInfo}.
     * @throws LoginException If an error occurs authenticating with the
     *            provided credential data.
     *
     * @see <a
     *      href="http://sling.apache.org/documentation/the-sling-engine/service-authentication.html">Service
     *      Authentication</a>
     */
    public @CheckForNull T authenticate(final @Nonnull Map<String, Object> authenticationInfo)
    throws LoginException {
        return null;
    }

    /**
     * If the provider requires authentication, this method is called with the state of the user
     * returned by {@link #authenticate(Map)} once the resource resolver is closed.
     *
     * @param state The provider state returned by {@link #authenticate(Map)}.
     */
    public void logout(final @CheckForNull T state) {
        // do nothing
    }

    /**
     * The provider is updated to reflect the latest state.
     * Resources which have changes pending are not discarded.
     * {@link #revert(ResolveContext)} can be called to discard changes.
     * <p>
     * This method is only called if the provider supports this and indicates
     * it by setting the {@link #PROPERTY_REFRESHABLE} to the value {@code true}.
     *
     * @param ctx The {@link ResolveContext}.
     */
    public void refresh(final @Nonnull ResolveContext<T> ctx) {
        // nothing to do here
    }

    /**
     * Returns {@code true} if this resource provider has not been closed
     * yet and can still be used.
     * <p>
     * This method will never throw an exception
     * even after the resource provider has been closed
     * <p>
     * This method is only called for resource providers which have a state and
     * require authentication.
     *
     * @param ctx The {@link ResolveContext}.
     * @return {@code true} if the resource provider has not been closed
     *         yet and is still active.. Once the resource provider has been closed
     *         or is not active anymore, this method returns {@code false}.
     */
    public boolean isLive(final @Nonnull ResolveContext<T> ctx) {
        return true;
    }

    /**
     * Returns the parent resource from this resource provider or {@code null} if
     * the resource provider cannot find it.
     *
     * The resource provider must not return cached instances for a resource as
     * the resource resolver will update the resource meta data of the resource
     * at the end of the resolution process and this meta data might be different
     * depending on the full path of resource resolution passed into the
     * resource resolver.
     *
     * @param ctx The {@link ResolveContext}.
     * @param child The child resource.
     * @return {@code null} if this provider does not have a resource for
     *         the parent.
     * @throws org.apache.sling.api.SlingException
     *             may be thrown in case of any problem creating the {@code Resource} instance.
     */
    public @CheckForNull Resource getParent(final @Nonnull ResolveContext<T> ctx, final @Nonnull Resource child) {
        final String parentPath = ResourceUtil.getParent(child.getPath());
        if (parentPath == null) {
            return null;
        }
        return this.getResource(ctx, parentPath, ResourceContext.EMPTY_CONTEXT, null);
    }

    /**
     * Returns a resource from this resource provider or {@code null} if
     * the resource provider cannot find it. The path must have the {@link #PROPERTY_ROOT}
     * strings as its prefix.
     *
     * The resource provider must not return cached instances for a resource as
     * the resource resolver will update the resource meta data of the resource
     * at the end of the resolution process and this meta data might be different
     * depending on the full path of resource resolution passed into the
     * resource resolver.
     *
     * @param ctx The {@link ResolveContext}.
     * @param path The full path of the resource.
     * @param resourceContext Additional information for resolving the resource
     * @param parent Optional parent resource
     * @return {@code null} If this provider does not have a resource for
     *         the path.
     * @throws org.apache.sling.api.SlingException
     *             may be thrown in case of any problem creating the {@code Resource} instance.
     */
    public abstract @CheckForNull Resource getResource(@Nonnull final ResolveContext<T> ctx,
            @Nonnull final String path,
            @Nonnull final ResourceContext resourceContext,
            @CheckForNull final Resource parent);

    /**
     * Returns an {@code Iterator} of {@link Resource} objects loaded from
     * the children of the given {@code Resource}. The returned {@link Resource} instances
     * are attached to the same
     * {@link org.apache.sling.api.resource.ResourceResolver} as the given {@code parent} resource.
     * <p>
     * This method may be called for resource providers whose root path list contains a path such
     * that the resource path is a
     * prefix of the list entry. This allows for the enumeration of deeply nested provided resources
     * for which no actual parent
     * hierarchy exists.
     * <p>
     * The returned iterator may in turn contain resources which do not actually exist but are required
     *  to traverse the resource
     * tree. Such resources SHOULD be {@link org.apache.sling.api.resource.SyntheticResource} objects whose resource type MUST be set to
     * {@link #RESOURCE_TYPE_SYNTHETIC}.
     *
     * As with {@link #getResource(ResolveContext, String, ResourceContext, Resource)} the returned Resource objects must
     * not be cached objects.
     *
     * @param ctx The {@link ResolveContext}.
     * @param parent
     *            The {@link Resource Resource} whose children are requested.
     * @return An {@code Iterator} of {@link Resource} objects or {@code null} if the resource
     *         provider has no children for the given resource.
     * @throws NullPointerException
     *             If {@code parent} is {@code null}.
     * @throws org.apache.sling.api.SlingException
     *             If any error occurs acquiring the child resource iterator.
     */
    public abstract @CheckForNull Iterator<Resource> listChildren(final @Nonnull ResolveContext<T> ctx, final @Nonnull Resource parent);

    /**
     * Returns a collection of attribute names whose value can be retrieved
     * calling the {@link #getAttribute(ResolveContext, String)} method.
     * <p>
     * This method is only called if the provider supports this and indicates
     * it by setting the {@link #PROPERTY_ATTRIBUTABLE} to the value {@code true}.
     *
     * @param ctx The {@link ResolveContext}.
     * @return A collection of attribute names or {@code null}
     * @throws IllegalStateException if this resource provider has already been
     *                               closed.
     */
    public @CheckForNull Collection<String> getAttributeNames(final @Nonnull ResolveContext<T> ctx) {
        return null;
    }

    /**
     * Returns the value of the given resource provider attribute or {@code null}
     * if the attribute is not set or not visible (as e.g. security
     * sensitive attributes).
     * <p>
     * This method is only called if the provider supports this and indicates
     * it by setting the {@link #PROPERTY_ATTRIBUTABLE} to the value {@code true}.
     *
     * @param ctx The {@link ResolveContext}.
     * @param name  The name of the attribute to access
     * @return The value of the attribute or {@code null} if the attribute
     *         is not set or not accessible.
     * @throws IllegalStateException
     *             if this resource provider has already been closed.
     */
    public @CheckForNull Object getAttribute(final @Nonnull ResolveContext<T> ctx, final @Nonnull String name) {
        return null;
    }

    /**
     * Create a new resource at the given path.
     * The new resource is put into the transient space of this provider
     * until {@link #commit(ResolveContext)} is called.
     * <p>
     * A resource provider must value {@link org.apache.sling.api.resource.ResourceResolver#PROPERTY_RESOURCE_TYPE}
     * to set the resource type of a resource.
     * <p>
     * This method is only called if the provider supports this and indicates
     * it by setting the {@link #PROPERTY_MODIFIABLE} to the value {@code true}.
     *
     * @param ctx The {@link ResolveContext}.
     * @param path The resource path.
     * @param properties Optional properties
     * @return The new resource.
     *
     * @throws PersistenceException If anything fails
     */
    public @Nonnull Resource create(final @Nonnull ResolveContext<T> ctx, final String path, final Map<String, Object> properties)
    throws PersistenceException {
        throw new PersistenceException("create is not supported.");
    }

    /**
     * Delete the resource at the given path.
     * This change is kept in the transient space of this provider
     * until {@link #commit(ResolveContext)} is called.
     * <p>
     * This method is only called if the provider supports this and indicates
     * it by setting the {@link #PROPERTY_MODIFIABLE} to the value {@code true}.
     *
     * @param ctx The {@link ResolveContext}.
     * @param resource The resource to delete.
     *
     * @throws PersistenceException If anything fails
     */
    public void delete(final @Nonnull ResolveContext<T> ctx, final @Nonnull Resource resource)
    throws PersistenceException {
        throw new PersistenceException("delete is not supported.");
    }

    /**
     * Revert all transient changes: create, delete and updates.
     * <p>
     * This method is only called if the provider supports this and indicates
     * it by setting the {@link #PROPERTY_MODIFIABLE} to the value {@code true}.
     *
     * @param ctx The {@link ResolveContext}.
     */
    public void revert(final @Nonnull ResolveContext<T> ctx) {
        // nothing to do here
    }

    /**
     * Commit all transient changes: create, delete and updates
     * <p>
     * This method is only called if the provider supports this and indicates
     * it by setting the {@link #PROPERTY_MODIFIABLE} to the value {@code true}.
     *
     * @param ctx The {@link ResolveContext}.
     * @throws PersistenceException If anything fails
     */
    public void commit(final @Nonnull ResolveContext<T> ctx)
    throws PersistenceException {
        // nothing to do here
    }

    /**
     * Are there any transient changes?
     * <p>
     * This method is only called if the provider supports this and indicates
     * it by setting the {@link #PROPERTY_MODIFIABLE} to the value {@code true}.
     *
     * @param ctx The {@link ResolveContext}.
     * @return {@code true} if there are pending changes.
     */
    public boolean hasChanges(final @Nonnull ResolveContext<T> ctx) {
        return false;
    }

    /**
     * Get the optional query language provider.
     * If the provider supports this kind of query, it must return a query provider implementation
     * if the provider is active. It should not return a query provider if it is not
     * active.
     * This method is called for each query, therefore the provider implementation
     * might cache the provider object.
     *
     * @return A query language provider if this resource provider supports this type of querying.
     */
    public @CheckForNull QueryLanguageProvider<T> getQueryLanguageProvider() {
        return null;
    }

    /**
     * Adapts the provider to another type.
     * <p>
     * Please not that it is explicitly left as an implementation detail whether
     * each call to this method with the same {@code type} yields the same
     * object or a new object on each call.
     * <p>
     * Implementations of this method should document their adapted types as
     * well as their behavior with respect to returning newly created or not
     * instance on each call.
     * <p>
     * This method is only called if the provider supports this and indicates
     * it by setting the {@link #PROPERTY_ADAPTABLE} to the value {@code true}.
     *
     * @param ctx The {@link ResolveContext}.
     * @param <AdapterType> The generic type to which this resource is adapted to.
     * @param type The generic type to which this resource is adapted to.
     * @return The adapter target or {@code null} if the provider cannot
     *         be adapt to the requested type.
     */
    public @CheckForNull <AdapterType> AdapterType adaptTo(final  @Nonnull ResolveContext<T> ctx,
            final @Nonnull Class<AdapterType> type) {
        return null;
    }

    /**
     * This method copies the subgraph rooted at, and including, the resource at
     * {@code srcAbsPath} to the new location at {@code destAbsPath} and
     * adds it as a child node of the resource at {@code destAbsPath}.
     * <p>
     * Both resources are resources from this provider and the full tree is
     * provided by this provider as well.
     * <p>
     * The resource at {@code destAbsPath} needs to exist, if not a {@code PersistenceException}
     * is thrown. If a child resource with the same name already exists at {@code destAbsPath}
     * a {@code PersistenceException} is thrown.
     * <p>
     * This method is only called if the provider supports this and indicates
     * it by setting the {@link #PROPERTY_MODIFIABLE} to the value {@code true}.
     *
     * @param ctx The {@link ResolveContext}.
     * @param srcAbsPath  the path of the resource to be copied.
     * @param destAbsPath the location to which the resource at
     *                    {@code srcAbsPath} is to be copied.
     * @throws PersistenceException If an error occurs.
     * @return {@code true} if the provider can perform the copy
     */
    public boolean copy(final  @Nonnull ResolveContext<T> ctx,
              final @Nonnull String srcAbsPath,
              final @Nonnull String destAbsPath) throws PersistenceException {
        return false;
    }

    /**
     * This method moves the subgraph rooted at, and including, the resource at
     * {@code srcAbsPath} to the new location at {@code destAbsPath} and
     * adds it as a child node of the resource at {@code destAbsPath}.
     * <p>
     * Both resources are resources from this provider and the full tree is
     * provided by this provider as well.
     * <p>
     * The resource at {@code destAbsPath} needs to exist, if not a {@code PersistenceException}
     * is thrown. If a child resource with the same name already exists at {@code destAbsPath}
     * a {@code PersistenceException} is thrown.
     * <p>
     * This method is only called if the provider supports this and indicates
     * it by setting the {@link #PROPERTY_MODIFIABLE} to the value {@code true}.
     *
     * @param ctx The {@link ResolveContext}.
     * @param srcAbsPath  the path of the resource to be copied.
     * @param destAbsPath the location to which the resource at
     *                    {@code srcAbsPath} is to be moved.
     * @throws PersistenceException If an error occurs.
     * @return {@code true} if the provider can perform the move
     */
    public boolean move(final  @Nonnull ResolveContext<T> ctx,
              final @Nonnull String srcAbsPath,
              final @Nonnull String destAbsPath) throws PersistenceException {
        return false;
    }
}
