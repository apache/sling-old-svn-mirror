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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.query.Query;
import org.apache.sling.api.resource.query.QueryInstructions;
import org.apache.sling.resourceresolver.impl.providers.ResourceProviderInfo;
import org.apache.sling.spi.resource.provider.QueryResult;

public interface StatefulResourceProvider {

    ResourceProviderInfo getInfo();

    ResourceResolver getResourceResolver();

    void logout();

    void refresh();

    boolean isLive();

    @CheckForNull
    Resource getParent(final @Nonnull Resource child);

    @CheckForNull
    Resource getResource(@Nonnull final String path, @CheckForNull final Resource parent, final Map<String, String> parameters, final boolean isResolve);

    @CheckForNull
    Iterator<Resource> listChildren(final @Nonnull Resource parent);

    Collection<String> getAttributeNames();

    Object getAttribute(final @Nonnull String name);

    Resource create(final String path, final Map<String, Object> properties) throws PersistenceException;

    void delete(final @Nonnull Resource resource) throws PersistenceException;

    void revert();

    void commit() throws PersistenceException;

    boolean hasChanges();

    @CheckForNull
    QueryResult find(@Nonnull Query q, @Nonnull QueryInstructions qi);

    @CheckForNull
    String[] getSupportedLanguages();

    @CheckForNull
    Iterator<Resource> findResources(String query, String language);

    @CheckForNull
    Iterator<Map<String, Object>> queryResources(String query, String language);

    @CheckForNull
    <AdapterType> AdapterType adaptTo(final @Nonnull Class<AdapterType> type);

    boolean copy(final String srcAbsPath, final String destAbsPath) throws PersistenceException;

    boolean move(final String srcAbsPath, final String destAbsPath) throws PersistenceException;

    StatefulResourceProvider clone(Map<String, Object> authenticationInfo, ResourceResolver resolver) throws LoginException;
}
