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


/**
 * A queriable resource provider is an extension of a resource provider.
 *
 * This extension is supported for services directly implementing the
 * {@link ResourceProvider} interface and {@link ResourceProvider}s
 * returned through a {@link ResourceProviderFactory}.
 *
 * @since 2.2.0
 */
public interface QueriableResourceProvider extends ResourceProvider {

    /**
     * The name of the service registration property containing the supported
     * languages of the resource provider (value is "provider.query.languages").
     * If the resource provider is delivered by a {@link ResourceProviderFactory}
     * this property should be declared on the factory.
     */
    String LANGUAGES = "provider.query.languages";

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
     * @return An <code>Iterator</code> of {@link Resource} objects matching the
     *         query. If no resources match, <code>null</code> might be
     *         returned instead of an empty iterator.
     * @throws QuerySyntaxException If the query is not syntactically correct
     *             according to the query language indicator or if the query
     *             language is not supported as specified in {@link #LANGUAGES}.
     * @throws org.apache.sling.api.SlingException If an error occurrs querying
     *             for the resources.
     * @throws IllegalStateException if this resource provider has already been
     *             closed.
     */
    Iterator<Resource> findResources(ResourceResolver resolver, String query, String language);

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
     * the column name and the column value is the JCR <code>Value</code> object
     * converted into the respective Java object, such as <code>Boolean</code>
     * for a value of property type <em>Boolean</em>.
     *
     * @param query The query string to use to find the resources.
     * @param language The language in which the query is formulated.
     * @return An <code>Iterator</code> of <code>Map</code> instances providing
     *         access to the query result. If no resources match, <code>null</code>
     *         might be returned instead of an empty iterator.
     * @throws QuerySyntaxException If the query is not syntactically correct
     *             according to the query language indicator or if the query
     *             language is not supported as specified in {@link #LANGUAGES}.
     * @throws org.apache.sling.api.SlingException If an error occurrs querying
     *             for the resources.
     * @throws IllegalStateException if this resource provider has already been
     *             closed.
     */
    Iterator<Map<String, Object>> queryResources(ResourceResolver resolver, String query, String language);
}
