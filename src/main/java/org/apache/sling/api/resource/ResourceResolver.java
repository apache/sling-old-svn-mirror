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
package org.apache.sling.api.resource;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.adapter.Adaptable;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>ResourceResolver</code> defines the service API which may be used
 * to resolve {@link Resource} objects. The resource resolver is available to
 * the request processing servlet through the
 * {@link org.apache.sling.api.SlingHttpServletRequest#getResourceResolver()}
 * method.
 * A resource resolver can also be created through the {@link ResourceResolverFactory}.
 * <p>
 * The <code>ResourceResolver</code> is also an {@link Adaptable} to get
 * adapters to other types. A JCR based resource resolver might support adapting
 * to the JCR Session used by the resolver to access the JCR Repository.
 * <p>
 * A <code>ResourceResolver</code> is generally not thread safe! As a
 * consequence, an application which uses the resolver, its returned resources
 * and/or objects resulting from adapting either the resolver or a resource,
 * must provide proper synchronization to ensure no more than one thread
 * concurrently operates against a single resolver, resource or resulting
 * objects.
 * <p>
 * <b>Accessing Resources</b>
 * <p>
 * This interface defines two kinds of methods to access resources: The
 * <code>resolve</code> methods and the <code>getResource</code> methods. The
 * difference lies in the algorithm applied to find the requested resource and
 * in the behavior in case a resource cannot be found:
 * <table>
 * <tr>
 * <th>Method Kind</th>
 * <th>Access Algorithm</th>
 * <th>Missing Resource</th>
 * </tr>
 * <tr>
 * <td>resolve</td>
 * <td>Path is always assumed to be absolute. Uses elaborate resource resolution
 * algorithm. This kind of method is intended to resolve request URLs to
 * resources.</td>
 * <td>Returns {@link NonExistingResource}</td>
 * </tr>
 * <tr>
 * <td>getResource</td>
 * <td>Directly access resources with absolute path. For relative paths, the
 * {@link #getSearchPath() search path} is applied. This method is intended to
 * be used by request processing scripts to access further resources as
 * required.</td>
 * <td>Returns <code>null</code></td>
 * </tr>
 * </table>
 * <p>
 * <b>Lifecycle</b>
 * <p>
 * A Resource Resolver has a life cycle which begins with the creation of the
 * Resource Resolver using any of the factory methods and ends with calling the
 * {@link #close()} method. It is very important to call the {@link #close()}
 * method once the resource resolver is not used any more to ensure any system
 * resources are properly cleaned up.
 * <p>
 * To check whether a Resource Resolver can still be used, the {@link #isLive()}
 * method can be called.
 * <p>
 * <b>Resource Resolver Attributes</b>
 * <p>
 * The authentication info properties provided to the
 * {@link ResourceResolverFactory#getResourceResolver(Map)},
 * {@link ResourceResolverFactory#getAdministrativeResourceResolver(Map)}, or
 * {@link #clone(Map)} are available through the {@link #getAttributeNames()}
 * and {@link #getAttribute(String)} methods with the exception of security
 * sensitive properties like {@link ResourceResolverFactory#PASSWORD} which is
 * not exposed.
 */
@ProviderType
public interface ResourceResolver extends Adaptable {

    /**
     * A request attribute containing the workspace to use for
     * {@link #resolve(HttpServletRequest)} and
     * {@link #resolve(HttpServletRequest, String)} if not the default workspace
     * should be used to resolve the resource.
     *
     * @since 2.1
     * @deprecated
     */
    @Deprecated
    String REQUEST_ATTR_WORKSPACE_INFO = ResourceResolver.class.getName()
        + "/use.workspace";

    /**
     * The name of the resource resolver attribute which is set if the resource
     * resolver has been impersonated as per the
     * {@link ResourceResolverFactory#USER_IMPERSONATION} property. The value of
     * this attribute is the name of the primary user provided to the resource
     * resolver factory method.
     *
     * @since 2.1
     */
    String USER_IMPERSONATOR = "user.impersonator";

    /**
     * This is the suggested property to be used for setting the resource type
     * of a resource during either creation ({@link #create(Resource, String, Map)})
     * or modifying ({@link ModifiableValueMap}).
     * However the exact way to set the resource type of a resource is defined
     * by the underlying resource provider. It should value this property but
     * is not required to do so.
     * @since 2.3
     */
    String PROPERTY_RESOURCE_TYPE = "sling:resourceType";

    /**
     * Resolves the resource from the given <code>absPath</code> optionally
     * taking <code>HttpServletRequest</code> into account, such as the value of
     * the <code>Host</code> request header. Returns a
     * {@link NonExistingResource} if the path cannot be resolved to an existing
     * and accessible resource.
     * <p>
     * The difference between this method and the {@link #resolve(String)}
     * method is, that this method may take request properties like the scheme,
     * the host header or request parameters into account to resolve the
     * resource.
     *
     * @param request The http servlet request object providing more hints at
     *            how to resolve the <code>absPath</code>. This parameter may be
     *            <code>null</code> in which case the implementation should use
     *            reasonable defaults.
     * @param absPath The absolute path to be resolved to a resource. If this
     *            parameter is <code>null</code>, it is assumed to address the
     *            root of the resource tree. If the path is relative it is
     *            assumed relative to the root, that is a slash is prepended to
     *            the path before resolving it.
     * @return The {@link Resource} addressed by the <code>absPath</code> or a
     *         {@link NonExistingResource} if no such resource can be resolved.
     * @throws org.apache.sling.api.SlingException Or a subclass thereof may be
     *             thrown if an error occurs trying to resolve the resource.
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     * @since 2.0.4
     */
    Resource resolve(HttpServletRequest request, String absPath);

    /**
     * Resolves the resource from the given absolute path. Returns a
     * {@link NonExistingResource} if the path cannot be resolved to an existing
     * and accessible resource.
     * <p>
     * This method is intended to apply the same algorithm to the absolute path
     * as is used by the {@link #resolve(HttpServletRequest)} method except for
     * cases where the latter uses request property such as request headers or
     * request parameters to resolve a resource.
     * <p>
     * It is ok for the implementation of this method to just call the
     * {@link #resolve(HttpServletRequest, String)} method with
     * <code>null</code> as the request argument.
     *
     * @param absPath The absolute path to be resolved to a resource. If this
     *            parameter is <code>null</code>, it is assumed to address the
     *            root of the resource tree. If the path is relative it is
     *            assumed relative to the root, that is a slash is prepended to
     *            the path before resolving it.
     * @return The {@link Resource} addressed by the <code>absPath</code> or a
     *         {@link NonExistingResource} if no such resource can be resolved.
     * @throws org.apache.sling.api.SlingException Or a subclass thereof may be
     *             thrown if an error occurs trying to resolve the resource.
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     */
    Resource resolve(String absPath);

    /**
     * Resolves the resource from the given <code>HttpServletRequest</code>.
     * Returns a {@link NonExistingResource} if the path cannot be resolved to
     * an existing and accessible resource.
     * <p>
     * This method is deprecated as of API version 2.0.4 and should not be used
     * anymore. Implementations are expected to implement this method calling
     * the {@link #resolve(HttpServletRequest, String)} where the
     * <code>absPath</code> argument is the result of calling the
     * <code>getPathInfo()</code> on the <code>request</code> object.
     *
     * @param request The http servlet request object used to resolve the
     *            resource for. This must not be <code>null</code>.
     * @return The {@link Resource} addressed by
     *         <code>HttpServletRequest.getPathInfo()</code> or a
     *         {@link NonExistingResource} if no such resource can be resolved.
     * @throws NullPointerException If <code>request</code> is <code>null</code>
     *             .
     * @throws org.apache.sling.api.SlingException Or a subclass thereof may be
     *             thrown if an error occurs trying to resolve the resource.
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     * @deprecated as of 2.0.4, use {@link #resolve(HttpServletRequest, String)}
     *             instead.
     */
    @Deprecated
    Resource resolve(HttpServletRequest request);

    /**
     * Returns a path mapped from the (resource) path applying the reverse
     * mapping used by the {@link #resolve(String)} such that when the path is
     * given to the {@link #resolve(String)} method the same resource is
     * returned.
     * <p>
     * Note, that technically the <code>resourcePath</code> need not refer to an
     * existing resource. This method just applies the mappings and returns the
     * resulting string. If the <code>resourcePath</code> does not address an
     * existing resource roundtripping may of course not work and calling
     * {@link #resolve(String)} with the path returned may return
     * <code>null</code>.
     * <p>
     * This method is intended as the reverse operation of the
     * {@link #resolve(String)} method.
     *
     * @param resourcePath The path for which to return a mapped path.
     * @return The mapped path.
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     */
    String map(String resourcePath);

    /**
     * Returns an URL mapped from the (resource) path applying the reverse
     * mapping used by the {@link #resolve(HttpServletRequest, String)} such
     * that when the path is given to the
     * {@link #resolve(HttpServletRequest, String)} method the same resource is
     * returned.
     * <p>
     * Note, that technically the <code>resourcePath</code> need not refer to an
     * existing resource. This method just applies the mappings and returns the
     * resulting string. If the <code>resourcePath</code> does not address an
     * existing resource roundtripping may of course not work and calling
     * {@link #resolve(HttpServletRequest, String)} with the path returned may
     * return <code>null</code>.
     * <p>
     * This method is intended as the reverse operation of the
     * {@link #resolve(HttpServletRequest, String)} method. As such the URL
     * returned is expected to be an absolute URL including scheme, host, any
     * servlet context path and the actual path used to resolve the resource.
     *
     * @param request The http servlet request object which may be used to apply
     *            more mapping functionality.
     * @param resourcePath The path for which to return a mapped path.
     * @return The mapped URL.
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     * @since 2.0.4
     */
    String map(HttpServletRequest request, String resourcePath);

    /**
     * Returns a {@link Resource} object for data located at the given path.
     * <p>
     * This specification does not define the location for resources or the
     * semantics for resource paths. For an implementation reading content from
     * a Java Content Repository, the path could be a
     * <code>javax.jcr.Item</code> path from which the resource object is
     * loaded. In contrast to the {@link #resolve(String)} method, this method
     * does not apply any logic to the path, so the path is used as-is to fetch
     * the content.
     *
     * @param path The absolute path to the resource object to be loaded. The
     *            path may contain relative path specifiers like <code>.</code>
     *            (current location) and <code>..</code> (parent location),
     *            which are resolved by this method. If the path is relative,
     *            that is the first character is not a slash, implementations
     *            are expected to apply a search path algorithm to resolve the
     *            relative path to a resource.
     * @return The <code>Resource</code> object loaded from the path or
     *         <code>null</code> if the path does not resolve to a resource.
     * @throws org.apache.sling.api.SlingException If an error occurs trying to
     *             load the resource object from the path.
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     */
    Resource getResource(String path);

    /**
     * Returns a {@link Resource} object for data located at the given path.
     * <p>
     * This specification does not define the location for resources or the
     * semantics for resource paths. For an implementation reading content from
     * a Java Content Repository, the path could be a
     * <code>javax.jcr.Item</code> path from which the resource object is
     * loaded.
     *
     * @param base The base {@link Resource} against which a relative path
     *            argument given by <code>path</code> is resolved. This
     *            parameter may be <code>null</code> if the <code>path</code> is
     *            known to be absolute.
     * @param path The path to the resource object to be loaded. If the path is
     *            relative, i.e. does not start with a slash (<code>/</code>),
     *            the resource relative to the given <code>base</code> resource
     *            is returned. The path may contain relative path specifiers
     *            like <code>.</code> (current location) and <code>..</code>
     *            (parent location), which are resolved by this method.
     * @return The <code>Resource</code> object loaded from the path or
     *         <code>null</code> if the path does not resolve to a resource.
     * @throws org.apache.sling.api.SlingException If an error occurs trying to
     *             load the resource object from the path or if
     *             <code>base</code> is <code>null</code> and <code>path</code>
     *             is relative.
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     */
    Resource getResource(Resource base, String path);

    /**
     * Returns the search path used by the {@link #getResource(String)} method
     * to search for resources by relative path. If no search path is set an
     * empty array is returned.
     * <p>
     * The returns array of Strings is a copy of the internal value, so
     * modifications to this array have no influence on the operation of the
     * ResourceResolver.
     * <p>
     * Each entry in the array is an absolute path terminated with a slash
     * character. Thus to create an absolute path from a search path entry and a
     * relative path, the search path entry and relative path may just be
     * concatenated.
     *
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     */
    String[] getSearchPath();

    /**
     * Returns an <code>Iterator</code> of {@link Resource} objects loaded from
     * the children of the given <code>Resource</code>.
     * <p>
     * This specification does not define what the term "child" means. This is
     * left to the implementation to define. For example an implementation
     * reading content from a Java Content Repository, the children could be the
     * {@link Resource} objects loaded from child items of the <code>Item</code>
     * of the given <code>Resource</code>.
     *
     * @param parent The {@link Resource Resource} whose children are requested.
     * @return An <code>Iterator</code> of {@link Resource} objects.
     * @throws NullPointerException If <code>parent</code> is <code>null</code>.
     * @throws org.apache.sling.api.SlingException If any error occurs acquiring
     *             the child resource iterator.
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     */
    Iterator<Resource> listChildren(Resource parent);

    /**
     * Returns an <code>Iterable</code> of {@link Resource} objects loaded from
     * the children of the given <code>Resource</code>.
     * <p>
     * This specification does not define what the term "child" means. This is
     * left to the implementation to define. For example an implementation
     * reading content from a Java Content Repository, the children could be the
     * {@link Resource} objects loaded from child items of the <code>Item</code>
     * of the given <code>Resource</code>.
     *
     * @param parent The {@link Resource Resource} whose children are requested.
     * @return An <code>Iterable</code> of {@link Resource} objects.
     * @throws NullPointerException If <code>parent</code> is <code>null</code>.
     * @throws org.apache.sling.api.SlingException If any error occurs acquiring
     *             the child resource iterator.
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     * @since 2.2
     */
    Iterable<Resource> getChildren(Resource parent);

    /**
     * Searches for resources using the given query formulated in the given
     * language.
     * <p>
     * The semantic meaning of the query and language depend on the actual
     * implementation and storage used for the resources. For JCR repository
     * being used as storage, the query and language parameters are used to
     * create a JCR <code>Query</code> through the <code>QueryManager</code>.
     * The result returned is then based on the <code>NodeIterator</code>
     * provided by the query result.
     *
     * @param query The query string to use to find the resources.
     * @param language The language in which the query is formulated. The
     *                 language should always be specified. However for
     *                 compatibility with older version, if no language
     *                 is specified, "xpath" is used.
     * @return An <code>Iterator</code> of {@link Resource} objects matching the
     *         query.
     * @throws QuerySyntaxException If the query is not syntactically correct
     *             according to the query language indicator.
     * @throws org.apache.sling.api.SlingException If an error occurs querying
     *             for the resources.
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     */
    Iterator<Resource> findResources(String query, String language);

    /**
     * Queries the storage using the given query formulated in the given
     * language.
     * <p>
     * The semantic meaning of the query and language depend on the actual
     * implementation and storage used for the resources. For JCR repository
     * being used as storage, the query and language parameters are used to
     * create a JCR <code>Query</code> through the <code>QueryManager</code>.
     * The result returned is then based on the <code>RowIterator</code>
     * provided by the query result. The map returned for each row is indexed by
     * the column name and the column value is the JCR <code>Value</code> object
     * converted into the respective Java object, such as <code>Boolean</code>
     * for a value of property type <em>Boolean</em>.
     *
     * @param query The query string to use to find the resources.
     * @param language The language in which the query is formulated. The
     *                 language should always be specified. However for
     *                 compatibility with older version, if no language
     *                 is specified, "xpath" is used.
     * @return An <code>Iterator</code> of <code>Map</code> instances providing
     *         access to the query result.
     * @throws QuerySyntaxException If the query is not syntactically correct
     *             according to the query language indicator.
     * @throws org.apache.sling.api.SlingException If an error occurs querying
     *             for the resources.
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     */
    Iterator<Map<String, Object>> queryResources(String query, String language);

    /**
     * Checks if the specified resource has any direct child resources.
     * 
     * @param resource
     *            the resource to check for direct children
     * @return <code>true</code> if the resource has any child resources
     * @since 2.4.4
     */
    boolean hasChildren(Resource resource);
    
    /**
     * Returns a new <code>ResourceResolver</code> instance based on the given
     * <code>authenticationInfo</code> map and the original authentication info
     * used to create this instance.
     * <p>
     * The new resource resolver is created according to the following
     * algorithm:
     *
     * <pre>
     * Map&lt;String, Object&gt; newAuthenticationInfo = new HashMap(
     *     authenticationInfoOfThisInstance);
     * newAuthenticationInfo.addAll(authenticationInfo);
     * return resourceResolverFactory.getResourceResolver(newAuthenticationInfo);
     * </pre>
     *
     * @param authenticationInfo The map or credential data to overlay the
     *            original credential data with for the creation of a new
     *            resource resolver. This may be <code>null</code> in which case
     *            the same credential data is used as was used to create this
     *            instance.
     * @return A new <code>ResourceResolver</code>
     * @throws LoginException If an error occurs creating the new
     *             <code>ResourceResolver</code> with the provided credential
     *             data.
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     * @since 2.1
     */
    ResourceResolver clone(Map<String, Object> authenticationInfo)
            throws LoginException;

    /**
     * Returns <code>true</code> if this resource resolver has not been closed
     * yet.
     * <p>
     * Unlike the other methods defined in this interface, this method will
     * never throw an exception even after the resource resolver has been
     * {@link #close() closed}.
     *
     * @return <code>true</code> if the resource resolver has not been closed
     *         yet. Once the resource resolver has been closed, this method
     *         returns <code>false</code>.
     * @since 2.1
     */
    boolean isLive();

    /**
     * Close this resource resolver. This method should be called by clients
     * when the resource resolver is not used anymore. Once this method has been
     * called, the resource resolver is considered unusable and will throw
     * exceptions if still used - with the exception of this method, which
     * can be called several times with no ill effects.
     *
     * @since 2.1
     */
    void close();

    /**
     * Get the user ID, if any, associated with this resource resolver. The
     * meaning of this identifier is an implementation detail defined by the
     * underlying repository. This method may return null.
     *
     * @return the user ID
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     * @since 2.1
     */
    String getUserID();

    /**
     * Returns an iterator of attribute names whose value can be retrieved
     * calling the {@link #getAttribute(String)} method. This iterator will not
     * include any attributes which are not accessible.
     *
     * @return An iterator of attribute names
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     */
    Iterator<String> getAttributeNames();

    /**
     * Returns the value of the given resource resolver attribute or
     * <code>null</code> if the attribute is not set (or not visible as is the
     * case of the {@link ResourceResolverFactory#PASSWORD} or other security
     * sensitive attributes).
     *
     * @param name The name of the attribute to access
     * @return The value of the attribute or <code>null</code> if the attribute
     *         is not set or not accessible.
     * @throws NullPointerException if <code>name</code> is <code>null</code>.
     * @throws IllegalStateException if this resource resolver has already been
     *             {@link #close() closed}.
     */
    Object getAttribute(String name);

    /**
     * Delete the resource
     *
     * Deleting a non existing resource leads to no operation nor exception.
     *
     * @param resource The resource to delete
     *
     * @throws NullPointerException if the resource parameter is null
     * @throws UnsupportedOperationException If the resource provider does not allow to
     *                                       delete this resource.
     * @throws PersistenceException If the operation fails.
     * @since 2.2
     */
    void delete(Resource resource)
    throws PersistenceException;

    /**
     * Add a child resource to the given parent resource
     * @param parent The parent resource
     * @param name   The name of the child resource - this is a plain name, not a path!
     * @param properties Optional properties for the resource
     * @return The new resource
     *
     * @throws NullPointerException if the resource parameter or name parameter is null
     * @throws IllegalArgumentException if the name contains a slash
     * @throws UnsupportedOperationException If the resource provider does not allow to
     *                                       create a resource at that location.
     * @throws PersistenceException If the operation fails.
     * @since 2.2
     */
    Resource create(Resource parent, String name, Map<String, Object> properties)
    throws PersistenceException;

    /**
     * Revert all pending changes.
     * @since 2.2
     */
    void revert();

    /**
     * Persist all pending changes.
     *
     * @throws PersistenceException
     * @since 2.2
     */
    void commit() throws PersistenceException;

    /**
     * Are there any pending changes?
     * @since 2.2
     */
    boolean hasChanges();

    /**
     * Returns the super type of the given resource. This method checks first if
     * the resource itself knows its super type by calling
     * {@link Resource#getResourceSuperType()}. If that returns
     * <code>null</code> {@link #getParentResourceType(String)}
     * is invoked with the resource type of the resource.
     *
     * @param resource The resource to return the resource super type for.
     * @return The resource super type or <code>null</code>. This
     *         method also returns <code>null</code> if the
     *         provided resource is <code>null</code>
     * @since 2.3
     */
    String getParentResourceType(final Resource resource);

    /**
     * Returns the super type of the given resource type. This method converts
     * the resource type to a resource path and checks the corresponding resource.
     * If the resource exists, the {@link Resource#getResourceSuperType()} method
     * is called.
     *
     * @param resourceType The resource type whose super type is to be returned.
     * @return the super type of the <code>resourceType</code> or
     *         <code>null</code> if the resource type does not exist or returns
     *         <code>null</code> for its super type. It also returns
     *         <code>null</code> if <code>resourceType> is null.
     * @since 2.3
     */
    public String getParentResourceType(final String resourceType);

    /**
     * Returns <code>true</code> if the resource type or any of the resource's
     * super type(s) equals the given resource type.
     *
     * @param resource The resource to check
     * @param resourceType The resource type to check this resource against.
     * @return <code>true</code> if the resource type or any of the resource's
     *         super type(s) equals the given resource type. <code>false</code>
     *         is also returned if <code>resource</code> or<code>resourceType</code>
     *         are <code>null</code>.
     * @since 2.3
     */
    boolean isResourceType(final Resource resource, final String resourceType);

    /**
     * The resolver is updated to reflect the latest state.
     * Resources which have changes pending are not discarded.
     * @since 2.3
     */
    void refresh();

}
