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

import javax.servlet.ServletRequest;

import org.apache.sling.api.SlingException;

/**
 * The <code>ResourceResolver</code> defines the service API which may be used
 * to resolve {@link Resource} objects. The resource resolver is available to
 * the request processing servlet through the
 * {@link org.apache.sling.api.SlingHttpServletRequest#getResourceResolver()}
 * method.
 */
public interface ResourceResolver {

    /**
     * Resolves the resource from the given <code>ServletRequest</code>.
     * <p>
     * If the request cannot be resolved to an existing resource, a
     * {@link Resource} object is returned whose
     * {@link Resource#getResourceType() resource type} is set to
     * {@link Resource#RESOURCE_TYPE_NON_EXISTING} and the
     * {@link Resource#getURI() resource URI} set to the request URI. Both the
     * {@link Resource#getRawData() raw data} and
     * {@link Resource#getObject() object} fields will be <code>null</code>.
     *
     * @param request The servlet request object used to resolve the resource
     *            for.
     * @return The {@link Resource} for the request.
     * @throws SlingException May be thrown if another error occurrs.
     */
    Resource resolve(ServletRequest request) throws SlingException;

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
     *            <code>ResourceNotFoundException</code> is thrown.
     * @return The <code>Resource</code> object loaded from the path or
     *         <code>null</code> if the path does not resolve to a resource.
     * @throws java.security.AccessControlException if an item exists at the
     *             <code>path</code> but the session of this resource manager
     *             has no read access to the item.
     * @throws SlingException If an error occurrs trying to load the resource
     *             object from the path or if <code>base</code> is
     *             <code>null</code> and <code>path</code> is relative.
     */
    Resource getResource(String path) throws SlingException;

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
     * @throws java.security.AccessControlException if an item exists at the
     *             <code>path</code> but the session of this resource manager
     *             has no read access to the item.
     * @throws ResourceNotFoundException If no resource can be resolved at the
     *             resolved path.
     * @throws SlingException If an error occurrs trying to load the resource
     *             object from the path or if <code>base</code> is
     *             <code>null</code> and <code>path</code> is relative.
     */
    Resource getResource(Resource base, String path) throws SlingException;

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
     *            If <code>null</code> the children of this request's Resource
     *            are returned.
     * @return An <code>Iterator</code> of {@link Resource} objects.
     * @throws NullPointerException If <code>parent</code> is
     *             <code>null</code>.
     * @throws SlingException If any error occurs acquiring the child resource
     *             iterator.
     */
    Iterator<Resource> listChildren(Resource parent) throws SlingException;

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
     * @throws SlingException If an error occurrs querying for the resources.
     */
    Iterator<Resource> findResources(String query, String language)
            throws SlingException;

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
     * @throws SlingException If an error occurrs querying for the resources.
     */
    Iterator<Map<String, Object>> queryResources(String query, String language)
            throws SlingException;
}
