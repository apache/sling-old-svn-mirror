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

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.query.Query;
import org.apache.sling.api.resource.query.QueryInstructions;
import org.apache.sling.spi.resource.provider.QueryResult;

public class StatefulResourceProviderWrapper implements StatefulResourceProvider {

    protected final StatefulResourceProvider rp;

    public StatefulResourceProviderWrapper(StatefulResourceProvider rp) {
        this.rp = rp;
    }

    @Override
    public void logout() {
        rp.logout();
    }

    @Override
    public void refresh() {
        rp.refresh();
    }

    @Override
    public boolean isLive() {
        return rp.isLive();
    }

    @Override
    public Resource getParent(Resource child) {
        return rp.getParent(child);
    }

    @Override
    public Resource getResource(String path, Resource parent, Map<String, String> parameters, boolean isResolve) {
        return rp.getResource(path, parent, parameters, isResolve);
    }

    @Override
    public Iterator<Resource> listChildren(Resource parent) {
        return rp.listChildren(parent);
    }

    @Override
    public Collection<String> getAttributeNames() {
        return rp.getAttributeNames();
    }

    @Override
    public Object getAttribute(String name) {
        return rp.getAttribute(name);
    }

    @Override
    public Resource create(String path, Map<String, Object> properties) throws PersistenceException {
        return rp.create(path, properties);
    }

    @Override
    public void delete(Resource resource) throws PersistenceException {
        rp.delete(resource);
    }

    @Override
    public void revert() {
        rp.revert();
    }

    @Override
    public void commit() throws PersistenceException {
        rp.commit();
    }

    @Override
    public boolean hasChanges() {
        return rp.hasChanges();
    }

    @Override
    public QueryResult find(Query q, QueryInstructions qi) {
        return rp.find(q, qi);
    }

    @Override
    public String[] getSupportedLanguages() {
        return rp.getSupportedLanguages();
    }

    @Override
    public Iterator<Resource> findResources(String query, String language) {
        return rp.findResources(query, language);
    }

    @Override
    public Iterator<Map<String, Object>> queryResources(String query, String language) {
        return rp.queryResources(query, language);
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return rp.adaptTo(type);
    }

    @Override
    public boolean copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return rp.copy(srcAbsPath, destAbsPath);
    }

    @Override
    public boolean move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return rp.move(srcAbsPath, destAbsPath);
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return rp.getResourceResolver();
    }
}
