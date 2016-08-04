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

import java.util.Iterator;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import org.osgi.annotation.versioning.ConsumerType;


/**
 * The JCR query provider supports querying based on the JCR spec.
 * Calls from {@link ResourceResolver#findResources(String, String)} and
 * {@link ResourceResolver#queryResources(String, String)} are passed on
 * to this object.
 *
 * @since 1.0.0 (Sling API Bundle 2.11.0)
 */
@ConsumerType
public interface QueryLanguageProvider<T> {

    /**
     * The supported languages of the resource provider.
     * @param ctx The resolve context
     * @return The array of supported languages
     */
    String[] getSupportedLanguages(@Nonnull ResolveContext<T> ctx);

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
     * @param ctx The resolve context
     * @param query The query string to use to find the resources.
     * @param language The language in which the query is formulated.
     * @return An <code>Iterator</code> of {@link Resource} objects matching the
     *         query. If no resources match, <code>null</code> might be
     *         returned instead of an empty iterator.
     * @throws QuerySyntaxException If the query is not syntactically correct
     *             according to the query language indicator or if the query
     *             language is not supported as specified in {@link #getSupportedLanguages(ResolveContext)}.
     * @throws org.apache.sling.api.SlingException If an error occurs querying
     *             for the resources.
     * @throws IllegalStateException if this resource provider has already been
     *             closed.
     */
    Iterator<Resource> findResources(@Nonnull ResolveContext<T> ctx, String query, String language);

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
     * @param ctx The resolve context
     * @param query The query string to use to find the resources.
     * @param language The language in which the query is formulated.
     * @return An <code>Iterator</code> of <code>Map</code> instances providing
     *         access to the query result. If no resources match, <code>null</code>
     *         might be returned instead of an empty iterator.
     * @throws QuerySyntaxException If the query is not syntactically correct
     *             according to the query language indicator or if the query
     *             language is not supported as specified in {@link #getSupportedLanguages(ResolveContext)}.
     * @throws org.apache.sling.api.SlingException If an error occurs querying
     *             for the resources.
     * @throws IllegalStateException if this resource provider has already been
     *             closed.
     */
    Iterator<ValueMap> queryResources(@Nonnull ResolveContext<T> ctx, String query, String language);
}
