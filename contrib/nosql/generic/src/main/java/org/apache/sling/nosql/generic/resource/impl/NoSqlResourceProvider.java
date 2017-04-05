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
package org.apache.sling.nosql.generic.resource.impl;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.QueriableResourceProvider;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.nosql.generic.adapter.NoSqlAdapter;
import org.apache.sling.nosql.generic.adapter.NoSqlData;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Generic implementation of a NoSQL resource provider.
 * The mapping to the NoSQL database implementation details is done via the provided {@link NoSqlAdapter}.
 */
public class NoSqlResourceProvider implements ResourceProvider, ModifyingResourceProvider, QueriableResourceProvider {
    
    private static final String ROOT_PATH = "/";
    
    private final NoSqlAdapter adapter;
    private final EventAdmin eventAdmin;
    private final Map<String, NoSqlData> changedResources = new LinkedHashMap<String, NoSqlData>();
    private final Set<String> deletedResources = new HashSet<String>();
    
    public NoSqlResourceProvider(NoSqlAdapter adapter, EventAdmin eventAdmin) {
        this.adapter = new ValueMapConvertingNoSqlAdapter(adapter);
        this.eventAdmin = eventAdmin;
    }

    
    // ### READONLY ACCESS ###
    
    public Resource getResource(ResourceResolver resourceResolver, String path) {
        if (!adapter.validPath(path)) {
            return null;
        }
        if (!this.deletedResources.isEmpty()) {
            for (String deletedPath : deletedResources) {
                Pattern deletedPathPattern = PathUtil.getSameOrDescendantPathPattern(deletedPath);
                if (deletedPathPattern.matcher(path).matches()) {
                    return null;
                }
            }
        }
        if (this.changedResources.containsKey(path)) {
            return new NoSqlResource(this.changedResources.get(path), resourceResolver, this);
        }
        NoSqlData data = adapter.get(path);
        if (data != null) {
            return new NoSqlResource(data, resourceResolver, this);
        }
        else if (ROOT_PATH.equals(path)) {
            // root path exists implicitly - bot not yet in nosql store - return a "virtual" resource until something is stored in it
            NoSqlData rootData = new NoSqlData(ROOT_PATH, new HashMap<String, Object>());
            return new NoSqlResource(rootData, resourceResolver, this);
        }
        return null;
    }

    public Resource getResource(ResourceResolver resourceResolver, HttpServletRequest request, String path) {
        return getResource(resourceResolver, path);
    }

    public Iterator<Resource> listChildren(Resource parent) {
        
        // use map to consolidate data from adapter minus deleted plus changed resources
        // always sorty result alphabetically to have a consistent ordering - the nosql data source does not support ordering
        SortedMap<String, Resource> children = new TreeMap<String, Resource>();
        
        Iterator<NoSqlData> fromAdapter = adapter.getChildren(parent.getPath());
        while (fromAdapter.hasNext()) {
            NoSqlData item = fromAdapter.next();
            if (isDeleted(item.getPath()) || changedResources.containsKey(item.getPath())) {
                continue;
            }
            children.put(item.getPath(), new NoSqlResource(item, parent.getResourceResolver(), this));
        }
        
        Pattern childPathPattern = PathUtil.getChildPathPattern(parent.getPath());
        for (NoSqlData item : changedResources.values()) {
            if (childPathPattern.matcher(item.getPath()).matches()) {
                children.put(item.getPath(), new NoSqlResource(item, parent.getResourceResolver(), this));
            }
        }
        
        return children.values().iterator();
    }

    private boolean isDeleted(String path) {
        for (String deletedPath : deletedResources) {
            if (path.equals(deletedPath) || path.equals(deletedPath + "/")) {
                return true;
            }
        }
        return false;
    }

    
    // ### WRITE ACCESS ###
    
    public Resource create(ResourceResolver resolver, String path, Map<String, Object> properties)
            throws PersistenceException {
        if (ROOT_PATH.equals(path) || !adapter.validPath(path)) {
            throw new PersistenceException("Illegal path - unable to create resource at " + path, null, path, null);
        }

        // check if already exists
        boolean deleted = this.deletedResources.remove(path);
        boolean exists = changedResources.containsKey(path) || this.adapter.get(path) != null;
        if (!deleted && exists) {
            throw new PersistenceException("Resource already exists at " + path, null, path, null);
        }
        
        // create new resource in changeset
        Map<String, Object> writableMap = properties != null ? new HashMap<String, Object>(properties) : new HashMap<String, Object>();
        NoSqlData data = new NoSqlData(path, NoSqlValueMap.convertForWriteAll(writableMap));
        changedResources.put(path, data);
        return new NoSqlResource(data, resolver, this);
    }
    
    public void delete(ResourceResolver resolver, String path) throws PersistenceException {
        if (ROOT_PATH.equals(path) || !adapter.validPath(path)) {
            throw new PersistenceException("Unable to delete resource at {}" + path, null, path, null);
        }

        Pattern pathsToDeletePattern = PathUtil.getSameOrDescendantPathPattern(path);

        // remove all existing path and probably descendant paths from list of deleted paths
        Iterator<String> deletedResourcesIterator = deletedResources.iterator();
        while (deletedResourcesIterator.hasNext()) {
            String deletedPath = deletedResourcesIterator.next();
            if (pathsToDeletePattern.matcher(deletedPath).matches()) {
                deletedResourcesIterator.remove();
            }
        }
        
        // remove all changed descendant items from changeset
        Iterator<Map.Entry<String, NoSqlData>> changeResourcesIterator = changedResources.entrySet().iterator();
        while (changeResourcesIterator.hasNext()) {
            Map.Entry<String, NoSqlData> entry = changeResourcesIterator.next();
            if (pathsToDeletePattern.matcher(entry.getKey()).matches()) {
                changeResourcesIterator.remove();
            }
        }
        
        // add path to delete
        deletedResources.add(path);
    }
    
    public void revert(ResourceResolver resolver) {
        changedResources.clear();
        deletedResources.clear();
    }
    
    public void commit(ResourceResolver resolver) throws PersistenceException {
        try {
            for (String path : deletedResources) {
               adapter.deleteRecursive(path); 
               notifyRemoved(path);
            }
            for (NoSqlData item : changedResources.values()) {
                boolean created = adapter.store(item);
                if (created) {
                    notifyAdded(item.getPath());
                }
                else {
                    notifyUpdated(item.getPath());
                }
            }
        }
        finally {
            this.revert(resolver);
        }
    }
    
    public boolean hasChanges(ResourceResolver resolver) {
        return !(changedResources.isEmpty() && deletedResources.isEmpty());
    }
    
    void markAsChanged(Resource resource) {
        changedResources.put(resource.getPath(), new NoSqlData(resource.getPath(), resource.getValueMap()));
    }
    
    private void notifyAdded(String path) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(SlingConstants.PROPERTY_PATH, path);
        final Event event = new Event(SlingConstants.TOPIC_RESOURCE_ADDED, props);
        this.eventAdmin.postEvent(event);
    }

    private void notifyUpdated(String path) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(SlingConstants.PROPERTY_PATH, path);
        final Event event = new Event(SlingConstants.TOPIC_RESOURCE_CHANGED, props);
        this.eventAdmin.postEvent(event);
    }    

    private void notifyRemoved(String path) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(SlingConstants.PROPERTY_PATH, path);
        final Event event = new Event(SlingConstants.TOPIC_RESOURCE_REMOVED, props);
        this.eventAdmin.postEvent(event);
    }

    
    // ### QUERY ACCESS ###
    
    public Iterator<Resource> findResources(final ResourceResolver resolver, final String query, final String language) {
        final Iterator<NoSqlData> result = adapter.query(query, language);
        if (result == null) {
            return null;
        }
        return new Iterator<Resource>() {
            public boolean hasNext() {
                return result.hasNext();
            }
            public Resource next() {
                return new NoSqlResource(result.next(), resolver, NoSqlResourceProvider.this);
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Iterator<ValueMap> queryResources(final ResourceResolver resolver, final String query, final String language) {
        final Iterator<Resource> result = findResources(resolver, query, language);
        if (result == null) {
            return null;
        }
        return new Iterator<ValueMap>() {
            public boolean hasNext() {
                return result.hasNext();
            }
            public ValueMap next() {
                return result.next().getValueMap();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
}
