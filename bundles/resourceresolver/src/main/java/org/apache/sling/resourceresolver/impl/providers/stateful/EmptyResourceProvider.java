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
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.query.Query;
import org.apache.sling.api.resource.query.QueryInstructions;
import org.apache.sling.spi.resource.provider.QueryResult;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

public class EmptyResourceProvider implements StatefulResourceProvider {

    @Override
    public ResourceResolver getResourceResolver() {
        return null;
    }

    @Override
    public void logout() {
    }

    @Override
    public void refresh() {
    }

    @Override
    public boolean isLive() {
        return false;
    }

    @Override
    public Resource getParent(Resource child, List<StatefulResourceProvider> parentProviders) {
        return null;
    }

    @Override
    public Resource getResource(String path, Resource parent, Map<String, String> parameters, boolean isResolve,
            List<StatefulResourceProvider> parentProviders) {
        return null;
    }

    @Override
    public Iterator<Resource> listChildren(Resource parent, List<StatefulResourceProvider> parentProviders) {
        return null;
    }

    @Override
    public Collection<String> getAttributeNames() {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public Resource create(String path, Map<String, Object> properties, List<StatefulResourceProvider> parentProviders)
            throws PersistenceException {
        return null;
    }

    @Override
    public void delete(Resource resource, List<StatefulResourceProvider> parentProviders) throws PersistenceException {
    }

    @Override
    public void revert() {
    }

    @Override
    public void commit() throws PersistenceException {
    }

    @Override
    public boolean hasChanges() {
        return false;
    }

    @Override
    public QueryResult find(Query q, QueryInstructions qi) {
        return null;
    }

    @Override
    public String[] getSupportedLanguages() {
        return null;
    }

    @Override
    public Iterator<Resource> findResources(String query, String language) {
        return null;
    }

    @Override
    public Iterator<Map<String, Object>> queryResources(String query, String language) {
        return null;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return null;
    }

    @Override
    public boolean copy(String srcAbsPath, String destAbsPath, List<StatefulResourceProvider> parentProviders)
            throws PersistenceException {
        return false;
    }

    @Override
    public boolean move(String srcAbsPath, String destAbsPath, List<StatefulResourceProvider> parentProviders)
            throws PersistenceException {
        return false;
    }

    @Override
    public ResourceProvider<Object> getResourceProvider() {
        return null;
    }

    @Override
    public ResolveContext<Object> getContext(Map<String, String> parameters) {
        return null;
    }

}
