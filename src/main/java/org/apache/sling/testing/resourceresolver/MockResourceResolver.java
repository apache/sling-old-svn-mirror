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
package org.apache.sling.testing.resourceresolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class MockResourceResolver implements ResourceResolver {

    private final Map<String, Map<String, Object>> resources;

    private final Map<String, Map<String, Object>> temporaryResources = new LinkedHashMap<String, Map<String,Object>>();

    private final Set<String> deletedResources = new HashSet<String>();

    private final EventAdmin eventAdmin;

    public MockResourceResolver(final EventAdmin eventAdmin,
            final Map<String, Map<String, Object>> resources) {
        this.eventAdmin = eventAdmin;
        this.resources = resources;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return null;
    }

    @Override
    public Resource resolve(final HttpServletRequest request, final String absPath) {
        return this.getResource(absPath);
    }

    @Override
    public Resource resolve(final String absPath) {
        return this.getResource(absPath);
    }

    @Override
    @Deprecated
    public Resource resolve(final HttpServletRequest request) {
        return null;
    }

    @Override
    public String map(final String resourcePath) {
        return resourcePath;
    }

    @Override
    public String map(final HttpServletRequest request, final String resourcePath) {
        return resourcePath;
    }

    @Override
    public Resource getResource(final String path) {
        if ( path.startsWith("/") ) {
            if ( this.deletedResources.contains(path) ) {
                return null;
            }
            final Map<String, Object> tempProps = this.temporaryResources.get(path);
            if ( tempProps != null ) {
                final Resource rsrc = new MockResource(path, tempProps, this);
                return rsrc;
            }
            synchronized ( this.resources ) {
                final Map<String, Object> props = this.resources.get(path);
                if ( props != null ) {
                    final Resource rsrc = new MockResource(path, props, this);
                    return rsrc;
                }
            }
        } else {
            for(final String s : this.getSearchPath() ) {
                final Resource rsrc = this.getResource(s + '/' + path);
                if ( rsrc != null ) {
                    return rsrc;
                }
            }
        }
        return null;
    }

    @Override
    public Resource getResource(Resource base, String path) {
        if ( path == null || path.length() == 0 ) {
            path = "/";
        }
        if ( path.startsWith("/") ) {
            return getResource(path);
        }
        if ( base.getPath().equals("/") ) {
            return getResource(base.getPath() + path);
        }
        return getResource(base.getPath() + '/' + path);
    }

    @Override
    public String[] getSearchPath() {
        return new String[] {"/apps", "/libs"};
    }

    @Override
    public Iterator<Resource> listChildren(final Resource parent) {
        final String prefixPath = parent.getPath() + "/";
        final Map<String, Map<String, Object>> candidates = new HashMap<String, Map<String,Object>>();
        synchronized ( this.resources ) {
            for(final Map.Entry<String, Map<String, Object>> e : this.resources.entrySet()) {
                if (e.getKey().startsWith(prefixPath) && e.getKey().lastIndexOf('/') < prefixPath.length() ) {
                    if ( !this.deletedResources.contains(e.getKey()) ) {
                        candidates.put(e.getKey(), e.getValue());
                    }
                }
            }
            for(final Map.Entry<String, Map<String, Object>> e : this.temporaryResources.entrySet()) {
                if (e.getKey().startsWith(prefixPath) && e.getKey().lastIndexOf('/') < prefixPath.length() ) {
                    if ( !this.deletedResources.contains(e.getKey()) ) {
                        candidates.put(e.getKey(), e.getValue());
                    }
                }
            }
        }
        final List<Resource> children = new ArrayList<Resource>();
        for(final Map.Entry<String, Map<String, Object>> e : candidates.entrySet()) {
            children.add(new MockResource(e.getKey(), e.getValue(), this));
        }
        return children.iterator();
    }

    @Override
    public Iterable<Resource> getChildren(final Resource parent) {
        return new Iterable<Resource>() {

            @Override
            public Iterator<Resource> iterator() {
                return listChildren(parent);
            }
        };
    }

    @Override
    public Iterator<Resource> findResources(final String query, final String language) {
        final List<Resource> emptyList = Collections.emptyList();
        return emptyList.iterator();
    }

    @Override
    public Iterator<Map<String, Object>> queryResources(String query,
            String language) {
        final List<Map<String, Object>> emptyList = Collections.emptyList();
        return emptyList.iterator();
    }

    @Override
    public ResourceResolver clone(Map<String, Object> authenticationInfo)
            throws LoginException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isLive() {
        return true;
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public String getUserID() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterator<String> getAttributeNames() {
        final List<String> emptyList = Collections.emptyList();
        return emptyList.iterator();
    }

    @Override
    public Object getAttribute(final String name) {
        return null;
    }

    @Override
    public void delete(final Resource resource) throws PersistenceException {
        this.deletedResources.add(resource.getPath());
        this.temporaryResources.remove(resource.getPath());
        final String prefixPath = resource.getPath() + '/';
        synchronized ( this.resources ) {
            for(final Map.Entry<String, Map<String, Object>> e : this.resources.entrySet()) {
                if (e.getKey().startsWith(prefixPath)) {
                    this.deletedResources.add(e.getKey());
                }
            }
            final Iterator<Map.Entry<String, Map<String, Object>>> i = this.temporaryResources.entrySet().iterator();
            while ( i.hasNext() ) {
                final Map.Entry<String, Map<String, Object>> e = i.next();
                if (e.getKey().startsWith(prefixPath) ) {
                    i.remove();
                }
            }
        }
    }

    @Override
    public Resource create(Resource parent, String name,
            Map<String, Object> properties) throws PersistenceException {
        final String path = (parent.getPath().equals("/") ? parent.getPath() + name : parent.getPath() + '/' + name);
        if ( this.temporaryResources.containsKey(path) ) {
            throw new PersistenceException("Path already exists: " + path);
        }
        synchronized ( this.resources ) {
            if ( this.resources.containsKey(path) ) {
                throw new PersistenceException("Path already exists: " + path);
            }
        }
        this.deletedResources.remove(path);
        if ( properties == null ) {
            properties = new HashMap<String, Object>();
        }
        this.temporaryResources.put(path, properties);

        return new MockResource(path, properties, this);
    }

    @Override
    public void revert() {
        this.deletedResources.clear();
        this.temporaryResources.clear();
    }

    @Override
    public void commit() throws PersistenceException {
        synchronized ( this.resources ) {
            for(final String path : this.deletedResources ) {
                if ( this.resources.remove(path) != null && this.eventAdmin != null ) {
                    final Map<String, Object> props = new HashMap<String, Object>();
                    props.put(SlingConstants.PROPERTY_PATH, path);
                    final Event e = new Event(SlingConstants.TOPIC_RESOURCE_REMOVED, props);
                    this.eventAdmin.sendEvent(e);
                }
                this.temporaryResources.remove(path);
            }
            for(final String path : this.temporaryResources.keySet() ) {
                final boolean changed = this.resources.containsKey(path);
                this.resources.put(path, this.temporaryResources.get(path));
                if ( this.eventAdmin != null ) {
                    final Map<String, Object> props = new HashMap<String, Object>();
                    props.put(SlingConstants.PROPERTY_PATH, path);
                    if ( this.resources.get(path).get(ResourceResolver.PROPERTY_RESOURCE_TYPE) != null ) {
                        props.put(SlingConstants.PROPERTY_RESOURCE_TYPE, this.resources.get(path).get(ResourceResolver.PROPERTY_RESOURCE_TYPE));
                    }
                    final Event e = new Event(changed ? SlingConstants.TOPIC_RESOURCE_CHANGED : SlingConstants.TOPIC_RESOURCE_ADDED, props);
                    this.eventAdmin.sendEvent(e);
                }
            }
        }
        this.revert();
    }

    @Override
    public boolean hasChanges() {
        return this.temporaryResources.size() > 0 || this.deletedResources.size() > 0;
    }

    @Override
    public String getParentResourceType(Resource resource) {
        return null;
    }

    @Override
    public String getParentResourceType(String resourceType) {
        return null;
    }

    @Override
    public boolean isResourceType(Resource resource, String resourceType) {
        return resource.getResourceType().equals(resourceType);
    }

    @Override
    public void refresh() {
        // nothing to do
    }

    public void addChanged(final String path, final Map<String, Object> props) {
        this.temporaryResources.put(path, props);
    }

    @Override
    public boolean hasChildren(Resource resource) {
        return this.listChildren(resource).hasNext();
    }
}
