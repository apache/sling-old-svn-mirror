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

/**
 * The <code>ResourceResolver</code> defines the service API which may be used
 * to resolve {@link Resource} objects. The resource resolver is available to
 * the request processing servlet through the
 * {@link org.apache.sling.api.SlingHttpServletRequest#getResourceResolver()}
 * method.
 * <p>
 * The <code>ResourceResolver</code> is also an {@link Adaptable} to get
 * adapters to other types. A JCR based resource resolver might support adapting
 * to the JCR Session used by the resolver to access the JCR Repository.
 */
public interface ResourceResolver extends Adaptable {

    /**
     * Resolves the resource from the given <code>absPath</code> optionally
     * taking <code>HttpServletRequest</code> into account, such as the value
     * of the <code>Host</code> request header.
     * <p>
     * If the <code>absPath</code> cannot be resolved to an existing resource
     * this method returns a {@link NonExistingResource}. This method is
     * intended to apply the same algorithm to the absolute path as is used by
     * the {@link #resolve(HttpServletRequest, String)} method except for cases
     * where the latter uses request property such as request headers or request
     * parameters to resolve a resource.
     * <p>
     * The difference between this method and the {@link #resolve(String)}
     * method is, that this method may take request properties like the scheme,
     * the host header or request parameters into account to resolve the
     * resource.
     *
     * @param request The http servlet request object providing more hints at
     *            how to resolve the <code>absPath</code>. This parameter may
     *            be <code>null</code> in which case the implementation should
     *            use reasonable defaults.
     * @param absPath The absolute path to be resolved to a resource. If this
     *            parameter is <code>null</code>, it is assumed to address
     *            the root of the resource tree. If the path is relative it is
     *            assumed relative to the root, that is a slash is prepended to
     *            the path before resolving it.
     * @return The {@link Resource} addressed by the <code>absPath</code> or a
     *         {@link NonExistingResource} if no such resource can be resolved.
     * @throws org.apache.sling.api.SlingException Or a subclass thereof may be
     *             thrown if an error occurrs trying to resolve the resource.
     * @since 2.0.4
     */
    Resource resolve(HttpServletRequest request, String absPath);

    /**
     * Resolves the resource from the given <code>HttpServletRequest</code>.
     * <p>
     * If the request cannot be resolved to an existing resource this method
     * returns a {@link NonExistingResource}. This method is intended to apply
     * the same algorithm to the absolute path as is used by the
     * {@link #resolve(HttpServletRequest, String)} method except for cases
     * where the latter uses request property such as request headers or request
     * parameters to resolve a resource.
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
     * @throws NullPointerException If <code>request</code> is
     *             <code>null</code>.
     * @throws org.apache.sling.api.SlingException Or a subclass thereof may be
     *             thrown if an error occurrs trying to resolve the resource.
     * @deprecated as of 2.0.4, use {@link #resolve(HttpServletRequest, String)}
     *             instead.
     */
    @Deprecated
    Resource resolve(HttpServletRequest request);

    /**
     * Resolves the resource from the given absolute path.
     * <p>
     * If the request cannot be resolved to an existing resource this method
     * returns <code>null</code>. This method is intended to apply the same
     * algorithm to the absolute path as is used by the
     * {@link #resolve(HttpServletRequest)} method except for cases where the
     * latter uses request property such as request headers or request
     * parameters to resolve a resource. In this case this method may return
     * <code>null</code>.
     * <p>
     * If the <code>absPath</code> is a relative path, this method returns
     * <code>null</code>.
     *
     * @param absPath The absolute path to be resolved to a resource. If the
     *            path is relative <code>null</code> is returned. This
     *            parameter must not be <code>null</code>.
     * @return The {@link Resource} addressed by the <code>absPath</code> or a
     *         {@link NonExistingResource} if no such resource can be resolved.
     * @throws NullPointerException if <code>absPath</code> is
     *             <code>null</code>.
     * @throws org.apache.sling.api.SlingException Or a subclass thereof may be
     *             thrown if an error occurrs trying to resolve the resource.
     */
    Resource resolve(String absPath);

    /**
     * Returns a path mapped from the (resource) path applying the reverse
     * mapping used by the {@link #resolve(String)} such that when the path is
     * given to the {@link #resolve(String)} method the same resource is
     * returned.
     * <p>
     * Note, that technically the <code>resourcePath</code> need not refer to
     * an existing resource. This method just applies the mappings and returns
     * the resulting string. If the <code>resourcePath</code> does not address
     * an existing resource roundtripping may of course not work and calling
     * {@link #resolve(String)} with the path returned may return
     * <code>null</code>.
     * <p>
     * This method is intended as the reverse operation of the
     * {@link #resolve(String)} method.
     *
     * @param resourcePath The path for which to return a mapped path.
     * @return The mapped path.
     */
    String map(String resourcePath);

    /**
     * Returns an URL mapped from the (resource) path applying the reverse
     * mapping used by the {@link #resolve(HttpServletRequest, String)} such
     * that when the path is given to the
     * {@link #resolve(HttpServletRequest, String)} method the same resource is
     * returned.
     * <p>
     * Note, that technically the <code>resourcePath</code> need not refer to
     * an existing resource. This method just applies the mappings and returns
     * the resulting string. If the <code>resourcePath</code> does not address
     * an existing resource roundtripping may of course not work and calling
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
     * loaded.
     * In contrast to the {@link #resolve(String)} method, this method
     * does not apply any logic to the path, so the path is used as-is to
     * fetch the content.
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
     * @throws org.apache.sling.api.SlingException If an error occurrs trying to
     *             load the resource object from the path.
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
     *            parameter may be <code>null</code> if the <code>path</code>
     *            is known to be absolute.
     * @param path The path to the resource object to be loaded. If the path is
     *            relative, i.e. does not start with a slash (<code>/</code>),
     *            the resource relative to the given <code>base</code>
     *            resource is returned. The path may contain relative path
     *            specifiers like <code>.</code> (current location) and
     *            <code>..</code> (parent location), which are resolved by
     *            this method.
     * @return The <code>Resource</code> object loaded from the path or
     *         <code>null</code> if the path does not resolve to a resource.
     * @throws org.apache.sling.api.SlingException If an error occurrs trying to
     *             load the resource object from the path or if
     *             <code>base</code> is <code>null</code> and
     *             <code>path</code> is relative.
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
     */
    String[] getSearchPath();

    /**
     * Returns an <code>Iterator</code> of {@link Resource} objects loaded
     * from the children of the given <code>Resource</code>.
     * <p>
     * This specification does not define what the term "child" means. This is
     * left to the implementation to define. For example an implementation
     * reading content from a Java Content Repository, the children could be the
     * {@link Resource} objects loaded from child items of the <code>Item</code>
     * of the given <code>Resource</code>.
     *
     * @param parent The {@link Resource Resource} whose children are requested.
     * @return An <code>Iterator</code> of {@link Resource} objects.
     * @throws NullPointerException If <code>parent</code> is
     *             <code>null</code>.
     * @throws org.apache.sling.api.SlingException If any error occurs acquiring
     *             the child resource iterator.
     */
    Iterator<Resource> listChildren(Resource parent);

    /**
     * Searches for resources using the given query formulated in the given
     * language.
     * <p>
     * The semantic meaning of the query and language depend on the actual
     * implementation and storage used for the resources. For JCR repository
     * being used as storage, the query and lanuage parameters are used to
     * create a JCR <code>Query</code> through the <code>QueryManager</code>.
     * The result returned is then based on the <code>NodeIterator</code>
     * provided by the query result.
     *
     * @param query The query string to use to find the resources.
     * @param language The language in which the query is formulated.
     * @return An <code>Iterator</code> of {@link Resource} objects matching
     *         the query.
     * @throws QuerySyntaxException If the query is not syntactically correct
     *             according to the query language indicator of if the query
     *             language is not supported.
     * @throws org.apache.sling.api.SlingException If an error occurrs querying
     *             for the resources.
     */
    Iterator<Resource> findResources(String query, String language);

    /**
     * Queries the storage using the given query formulated in the given
     * language.
     * <p>
     * The semantic meaning of the query and language depend on the actual
     * implementation and storage used for the resources. For JCR repository
     * being used as storage, the query and lanuage parameters are used to
     * create a JCR <code>Query</code> through the <code>QueryManager</code>.
     * The result returned is then based on the <code>RowIterator</code>
     * provided by the query result. The map returned for each row is indexed by
     * the column name and the column value is the JCR <code>Value</code>
     * object converted into the respective Java object, such as
     * <code>Boolean</code> for a value of property type <em>Boolean</em>.
     *
     * @param query The query string to use to find the resources.
     * @param language The language in which the query is formulated.
     * @return An <code>Iterator</code> of <code>Map</code> instances
     *         providing access to the query result.
     * @throws QuerySyntaxException If the query is not syntactically correct
     *             according to the query language indicator of if the query
     *             language is not supported.
     * @throws org.apache.sling.api.SlingException If an error occurrs querying
     *             for the resources.
     */
    Iterator<Map<String, Object>> queryResources(String query, String language);

}
