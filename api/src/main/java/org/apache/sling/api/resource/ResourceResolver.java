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
     * Resolves the resource from the given <code>HttpServletRequest</code>.
     * <p>
     * If the request cannot be resolved to an existing resource, a
     * {@link Resource} object is returned whose
     * {@link Resource#getResourceType() resource type} is set to
     * {@link Resource#RESOURCE_TYPE_NON_EXISTING} and the
     * {@link Resource#getPath() resource path} set to the request URI.
     * {@link Resource#adaptTo(Class) object} returns <code>null</code> for
     * all classes.
     * 
     * @param request The http servlet request object used to resolve the
     *            resource for.
     * @return The {@link Resource} for the request.
     * @throws AccessControlException if the user authenticated with the request
     *             does not have enough rights to access the resource to which
     *             the request maps.
     * @throws SlingException A subclass of this exception is thrown if the
     *             resource to which the request maps cannot be retrieved.
     */
    Resource resolve(HttpServletRequest request);

    /**
     * Returns a {@link Resource} object for data located at the given path.
     * <p>
     * This specification does not define the location for resources or the
     * semantics for resource paths. For an implementation reading content from
     * a Java Content Repository, the path could be a
     * <code>javax.jcr.Item</code> path from which the resource object is
     * loaded.
     * 
     * @param path The absolute path to the resource object to be loaded. The
     *            path may contain relative path specifiers like <code>.</code>
     *            (current location) and <code>..</code> (parent location),
     *            which are resolved by this method. If the path is relative,
     *            that is the first character is not a slash, a
     *            <code>SlingException</code> is thrown.
     * @return The <code>Resource</code> object loaded from the path or
     *         <code>null</code> if the path does not resolve to a resource.
     * @throws AccessControlException if an item exists at the <code>path</code>
     *             but the session of this resource manager has no read access
     *             to the item.
     * @throws SlingException If an error occurrs trying to load the resource
     *             object from the path or if <code>base</code> is
     *             <code>null</code> and <code>path</code> is relative.
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
     * @throws AccessControlException if an item exists at the <code>path</code>
     *             but the session of this resource manager has no read access
     *             to the item.
     * @throws SlingException If an error occurrs trying to load the resource
     *             object from the path or if <code>base</code> is
     *             <code>null</code> and <code>path</code> is relative.
     */
    Resource getResource(Resource base, String path);

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
     * @throws SlingException If any error occurs acquiring the child resource
     *             iterator.
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
     * @throws SlingException If an error occurrs querying for the resources.
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
     * @throws SlingException If an error occurrs querying for the resources.
     */
    Iterator<Map<String, Object>> queryResources(String query, String language);

}
