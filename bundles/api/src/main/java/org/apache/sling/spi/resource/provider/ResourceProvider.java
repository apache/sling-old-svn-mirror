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

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;

import aQute.bnd.annotation.ConsumerType;

/**
 * API for providers of resources. Used by the {@link ResourceResolver} to
 * transparently access resources from different locations such as a JCR
 * repository, a database, or bundle resources.
 * <p>
 * This extension point is defined by an abstract class (in contrast to
 * an interface) as this will allow to add new functionality in new versions
 * without breaking any implementation.
 * <p>
 * This service is intended to be implemented by providers of resource
 * instances on behalf of the {@link ResourceResolver}. It
 * is not intended to be used by client applications directly. A resource
 * provider implements this service by extending this class.
 * <p>
 * A resource provider must set the {@link #PROPERTY_ROOT} property with an
 * absolute path. This is the mount point of the resource provider. If there
 * is more than one provider registering for the same root, only the one
 * with the highest service ranking is used.
 * <p>
 * If a provider is used in the resource tree, it gets activated through
 * a call of the {@link #activate(ProviderContext)} method. If the
 * provider is not used anymore within the resource tree, the
 * {@link #deactivate(ProviderContext)} method is called. Whenever
 * information concerning the provider is changed while the provider
 * is used, the {@link #update(ProviderContext)} method is called.
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
 * is called, when the resource resolver is closed.
 * <p>
 * Each method gets the {@link ResolveContext} which gives access to
 * further functionality.
 * <p>
 * TODO - query
 * TODO - observation
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
     * The default for this value is <code>false</code>.
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
     * String service property, default value is <code>{@link #AUTHENTICATE_NO}</code>.
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
     * Boolean service property, default value is <code>false</true>.
     * (value is "provider.modifiable")
     */
    public static final String PROPERTY_MODIFIABLE = "provider.modifiable";

    /**
     * If this flag is set to {@code true}, the resource resolver will use this provider
     * for the adaptTo() operation.
     */
    public static final String PROPERTY_ADAPTABLE = "provider.adaptable";

    /**
     * If this flag is set to {@code true}, the resource resolver will call {@link #refresh(ResolveContext)}
     * when it's refreshed itself.
     */
    public static final String PROPERTY_REFRESHABLE = "provider.refreshable";

    /**
     * If this flag is set to {@code true}, the resource resolver will try to get the attribute
     * names and the attribute values from this provider.
     */
    public static final String PROPERTY_ATTRIBUTABLE = "provider.attributable";

    /**
     * If this flag is set to {@code true}, the resource resolver will use this provider
     * for JCR-style queries.
     */
    public static final String PROPERTY_SUPPORTS_JCR_QUERY = "provider.jcr.query";

    /**
     * If this flag is set to {@code true}, the resource resolver will use this provider
     * for Sling-style queries.
     */
    public static final String PROPERTY_SUPPORTS_NATIVE_QUERY = "provider.native.query";

    /**
     * The authentication information property referring to the bundle
     * providing a service for which a resource provider is to be retrieved. If
     * this property is provided, the
     * {@link ResourceResolverFactory#SUBSERVICE} property may also be
     * present.
     * <p>
     * {@link ResourceResolverFactory} implementations must provide this
     * property if their implementation of the
     * {@link ResourceResolverFactory#getServiceResourceResolver(Map)} method
     * use a resource provider factory.
     * <p>
     * The type of this property, if present, is
     * <code>org.osgi.framework.Bundle</code>.
     */
    public static final String AUTH_SERVICE_BUNDLE = "sling.service.bundle";

    /**
     * The authentication information property indicating to use an
     * administrative login. This property must be set of the resource
     * resolver is created through {@link ResourceResolverFactory#getAdministrativeResourceResolver(Map)}.
     */
    public static final String AUTH_ADMIN = "provider.auth.admin";

    /**
     * The resource type be set on resources returned by the
     * {@link #listChildren(Resource)} method to enable traversing the
     * resource
     * tree down to a deeply nested provided resource which has no concrete
     * parent hierarchy (value is"sling:syntheticResourceProviderResource").
     *
     * @see #listChildren(Resource)
     */
    public static final String RESOURCE_TYPE_SYNTHETIC = "sling:syntheticResourceProviderResource";

    /** The context for this provider. */
    private volatile ProviderContext ctx;

    /**
     * With a call to this method, the provider implementation is notified that
     * it is used in the resource tree.
     * @param ctx The context for this provider.
     */
    public void activate(@Nonnull ProviderContext ctx) {
        this.ctx = ctx;
    }

    /**
     * With a call to this method, the provider implementation is notified
     * that it is not used anymore in the resource tree.
     * @param ctx The context for this provider.
     */
    public void deactivate(@Nonnull ProviderContext ctx) {
        this.ctx = null;
    }

    /**
     * With a call to this method, the provider implementation is notified
     * that any information regarding the registration of the provider
     * has changed. For example, observation listeners might have changed.
     * This method is only called while the provider is used in the resource
     * tree.
     * @param ctx The context for this provider.
     */
    public void update(@Nonnull ProviderContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Get the current provider context.
     * @return The provider context or {@code null} if the provider is currently
     *         not used in the resource tree.
     */
    protected ProviderContext getProviderContext() {
        return this.ctx;
    }

    /**
     * Authenticate against the resource provider.
     * <p>
     * Returns a provider context object if authentication is successful. The
     * context object is passed to the resource provider in all messages through
     * the {@link ResolveContext}. A valid implementation might return {@code null}
     * as the context information.
     * <p>
     * If authentication fails a {@code LoginException} must be thrown.
     * <p>
     * The returned context object grants access to the provided resources with
     * privileges assigned to the service provided by the calling bundle.
     * <p>
     * The <code>authenticationInfo</code> map will in general contain the same
     * information as provided to the respective {@link ResourceResolver}
     * method. For
     * <p>
     * The provided {@code authenticationInfo} map may be used to provide
     * additional information such as the {@link #AUTH_SERVICE_BUNDLE}.
     * If this property is provided, additional information like
     * {@link ResourceResolverFactory#SUBSERVICE} might be provided, but the
     * {@link #USER} and {@link #PASSWORD} properties provided in the map must
     * be ignored.
     * <p>
     * The {@link #USER_IMPERSONATION} property is obeyed but requires that the
     * authenticated user has permission to impersonate as the requested user.
     * If such permission is missing, a {@code LoginException} is thrown.
     * <p>
     * The resource resolver implementation will call the {@link #logout(Object)}
     * method once the resource resolver is closed.
     *
     * @param authenticationInfo A map of further credential information which
     *            may be used by the implementation to parameterize how the
     *            resource resolver is created.
     * @return A context data object according to the
     *         <code>authenticationInfo</code>.
     * @throws LoginException If an error occurs authenticating with the
     *            provided credential data.
     *
     * @see <a
     *      href="http://sling.apache.org/documentation/the-sling-engine/service-authentication.html">Service
     *      Authentication</a>
     */
    @Nonnull public T authenticate(final @Nonnull Map<String, Object> authenticationInfo)
    throws LoginException {
        return null;
    }

    /**
     * If the provider requires authentication, this method is called with the state of the user
     * once the resource resolver is closed.
     * @param state The user state.
     */
    public void logout(final @Nonnull T state) {
        // do nothing
    }

    /**
     * The provider is updated to reflect the latest state.
     * Resources which have changes pending are not discarded.
     * {@link #revert(ResolveContext)} can be called to discard changes.
     *
     * @param ctx The {@link ResolveContext} to which the returned {@link Resource} is attached.
     */
    public void refresh(final @Nonnull ResolveContext<T> ctx) {
        // nothing to do here
    }

    /**
     * Returns <code>true</code> if this resource provider has not been closed
     * yet and can still be used.
     * <p>
     * This method will never throw an exception
     * even after the resource provider has been closed
     *
     * @param ctx The {@link ResolveContext} to which the returned {@link Resource} is attached.
     * @return <code>true</code> if the resource provider has not been closed
     *         yet and is still active.. Once the resource provider has been closed
     *         or is not active anymore, this method returns <code>false</code>.
     */
    public boolean isLive(final @Nonnull ResolveContext<T> ctx) {
        return true;
    }

    /**
     * Returns the parent resource from this resource provider or <code>null</code> if
     * the resource provider cannot find it.
     *
     * The resource provider must not return cached instances for a resource as
     * the resource resolver will update the resource metadata of the resource
     * at the end of the resolution process and this metadata might be different
     * depending on the full path of resource resolution passed into the
     * resource resolver.
     *
     * @param ctx The {@link ResolveContext} to which the returned {@link Resource} is attached.
     * @param child The child resource.
     * @return <code>null</code> if this provider does not have a resource for
     *         the parent.
     * @throws org.apache.sling.api.SlingException
     *             may be thrown in case of any problem creating the <code>Resource</code> instance.
     */
    public @CheckForNull Resource getParent(final @Nonnull ResolveContext<T> ctx, final  @Nonnull Resource child) {
        final String parentPath = ResourceUtil.getParent(child.getPath());
        if (parentPath == null) {
            return null;
        }
        return this.getResource(ctx, parentPath, null);
    }

    /**
     * Returns a resource from this resource provider or <code>null</code> if
     * the resource provider cannot find it. The path must have the {@link #PROPERTY_ROOT}
     * strings as its prefix.
     *
     * The resource provider must not return cached instances for a resource as
     * the resource resolver will update the resource metadata of the resource
     * at the end of the resolution process and this metadata might be different
     * depending on the full path of resource resolution passed into the
     * resource resolver.
     *
     * @param resourceResolver
     *            The {@link ResourceResolver} to which the returned {@link Resource} is attached.
     * @param path The full path of the resource.
     * @param parent Optional parent resource
     * @return <code>null</code> If this provider does not have a resource for
     *         the path.
     * @throws org.apache.sling.api.SlingException
     *             may be thrown in case of any problem creating the <code>Resource</code> instance.
     */
    public abstract @CheckForNull Resource getResource(@Nonnull final ResolveContext<T> ctx,
            @Nonnull final String path,
            @CheckForNull final Resource parent);

    /**
     * Returns an <code>Iterator</code> of {@link Resource} objects loaded from
     * the children of the given <code>Resource</code>. The returned {@link Resource} instances
     *  are attached to the same
     * {@link ResourceResolver} as the given <code>parent</code> resource.
     * <p>
     * This method may be called for resource providers whose root path list contains a path such
     * that the resource path is a
     * prefix of the list entry. This allows for the enumeration of deeply nested provided resources
     * for which no actual parent
     * hierarchy exists.
     * <p>
     * The returned iterator may in turn contain resources which do not actually exist but are required
     *  to traverse the resource
     * tree. Such resources SHOULD be {@link SyntheticResource} objects whose resource type MUST be set to
     * {@link #RESOURCE_TYPE_SYNTHETIC}.
     *
     * As with {@link #getResource(ResourceResolver, String)} the returned Resource objects must
     * not be cached objects.
     *
     * @param ctx The {@link ResolveContext} to which the returned {@link Resource} is attached.
     * @param parent
     *            The {@link Resource Resource} whose children are requested.
     * @return An <code>Iterator</code> of {@link Resource} objects or <code>null</code> if the resource
     *         provider has no children for the given resource.
     * @throws NullPointerException
     *             If <code>parent</code> is <code>null</code>.
     * @throws SlingException
     *             If any error occurs acquiring the child resource iterator.
     */
    public abstract @CheckForNull Iterator<Resource> listChildren(final @Nonnull ResolveContext<T> ctx, final @Nonnull Resource parent);

    /**
     * Returns a collection of attribute names whose value can be retrieved
     * calling the {@link #getAttribute(ResourceResolver, String)} method.
     *
     * @param ctx The {@link ResolveContext} to which the returned {@link Resource} is attached.
     * @return A collection of attribute names or <code>null</code>
     * @throws IllegalStateException if this resource provider has already been
     *                               closed.
     */
    public Collection<String> getAttributeNames(final @Nonnull ResolveContext<T> ctx) {
        return null;
    }

    /**
     * Returns the value of the given resource provider attribute or <code>null</code>
     * if the attribute is not set or not visible (as e.g. security
     * sensitive attributes).
     *
     * @param ctx The {@link ResolveContext} to which the returned {@link Resource} is attached.
     * @param name
     *            The name of the attribute to access
     * @return The value of the attribute or <code>null</code> if the attribute
     *         is not set or not accessible.
     * @throws IllegalStateException
     *             if this resource provider has already been closed.
     */
    public Object getAttribute(final @Nonnull ResolveContext<T> ctx, final @Nonnull String name) {
        return null;
    }

    /**
     * Create a new resource at the given path.
     * The new resource is put into the transient space of this provider
     * until {@link #commit(ResourceResolver)} is called.
     *
     * A resource provider should value {@link ResourceResolver#PROPERTY_RESOURCE_TYPE}
     * to set the resource type of a resource.
     *
     * @param ctx The {@link ResolveContext} to which the returned {@link Resource} is attached.
     * @param path The resource path.
     * @param properties Optional properties
     * @return The new resource.
     *
     * @throws PersistenceException If anything fails
     */
    public Resource create(final @Nonnull ResolveContext<T> ctx, final String path, final Map<String, Object> properties)
    throws PersistenceException {
        throw new PersistenceException("create is not supported.");
    }

    /**
     * Delete the resource at the given path.
     * This change is kept in the transient space of this provider
     * until {@link #commit(ResourceResolver)} is called.
     *
     * @param ctx The {@link ResolveContext} to which the returned {@link Resource} is attached.
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
     *
     * @param ctx The {@link ResolveContext} to which the returned {@link Resource} is attached.
     */
    public void revert(final @Nonnull ResolveContext<T> ctx) {
        // nothing to do here
    }

    /**
     * Commit all transient changes: create, delete and updates
     *
     * @param ctx The {@link ResolveContext} to which the returned {@link Resource} is attached.
     *
     * @throws PersistenceException If anything fails
     */
    public void commit(final @Nonnull ResolveContext<T> ctx)
    throws PersistenceException {
        // nothing to do here
    }

    /**
     * Are there any transient changes?
     *
     * @param ctx The {@link ResolveContext} to which the returned {@link Resource} is attached.
     */
    public boolean hasChanges(final @Nonnull ResolveContext<T> ctx) {
        return false;
    }

    /**
     * Get the optional query provider.
     *
     * @return A query provider if this resource provider supports this type of querying.
     */
    public @CheckForNull QueryProvider<T> getQueryProvider() {
        return null;
    }

    /**
     * Get the optional JCR query provider.
     *
     * @return A JCR query provider if this resource provider supports this type of querying.
     */
    public @CheckForNull JCRQueryProvider<T> getJCRQueryProvider() {
        return null;
    }

    /**
     * Adapts the provider to another type.
     * <p>
     * Please not that it is explicitly left as an implementation detail whether
     * each call to this method with the same <code>type</code> yields the same
     * object or a new object on each call.
     * <p>
     * Implementations of this method should document their adapted types as
     * well as their behaviour with respect to returning newly created or not
     * instance on each call.
     *
     * @param ctx The {@link ResolveContext}.
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
     * <code>srcAbsPath</code> to the new location at <code>destAbsPath</code> and
     * adds it as a child node of the resource at <code>destAbsPath</code>.
     *
     * Both resources are resources from this provider and the full tree is
     * provided by this provider as well.
     *
     * The resource at <code>destAbsPath</code> needs to exist, if not a {@code PersistenceException}
     * is thrown. If a child resource with the same name already exists at <code>destAbsPath</code>
     * a {@code PersistenceException} is thrown.
     *
     * @param srcAbsPath  the path of the resource to be copied.
     * @param destAbsPath the location to which the resource at
     *                    <code>srcAbsPath</code> is to be copied.
     * @throws PersistenceException If an error occurs.
     * @return {@code true} if the provider can perform the copy
     */
    public boolean copy(final  @Nonnull ResolveContext<T> ctx,
              final String srcAbsPath,
              final String destAbsPath) throws PersistenceException {
        return false;
    }

    /**
     * This method moves the subgraph rooted at, and including, the resource at
     * <code>srcAbsPath</code> to the new location at <code>destAbsPath</code> and
     * adds it as a child node of the resource at <code>destAbsPath</code>.
     *
     * Both resources are resources from this provider and the full tree is
     * provided by this provider as well.
     *
     * The resource at <code>destAbsPath</code> needs to exist, if not a {@code PersistenceException}
     * is thrown. If a child resource with the same name already exists at <code>destAbsPath</code>
     * a {@code PersistenceException} is thrown.
     *
     * @param srcAbsPath  the path of the resource to be copied.
     * @param destAbsPath the location to which the resource at
     *                    <code>srcAbsPath</code> is to be moved.
     * @throws PersistenceException If an error occurs.
     * @return {@code true} if the provider can perform the move
     */

    public boolean move(final  @Nonnull ResolveContext<T> ctx,
              final String srcAbsPath,
              final String destAbsPath) throws PersistenceException {
        return false;
    }
}
