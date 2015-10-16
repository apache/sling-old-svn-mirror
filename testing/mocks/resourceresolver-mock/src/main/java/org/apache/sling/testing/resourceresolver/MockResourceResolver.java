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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.event.Event;

public class MockResourceResolver extends SlingAdaptable implements ResourceResolver {

    private final Map<String, Map<String, Object>> resources;

    private final Map<String, Map<String, Object>> temporaryResources = new LinkedHashMap<String, Map<String,Object>>();

    private final Set<String> deletedResources = new HashSet<String>();

    private final MockResourceResolverFactoryOptions options;

    private final MockResourceResolverFactory factory;
    
    private final Map<String,Object> attributes;

    public MockResourceResolver(final MockResourceResolverFactoryOptions options,
            final MockResourceResolverFactory factory,
            final Map<String, Map<String, Object>> resources) {
        this(options, factory, resources, Collections.<String,Object>emptyMap());
    }

    public MockResourceResolver(final MockResourceResolverFactoryOptions options,
            final MockResourceResolverFactory factory,
            final Map<String, Map<String, Object>> resources,
            final Map<String,Object> attributes) {
        this.factory = factory;
        this.options = options;
        this.resources = resources;
        this.attributes = attributes;
    }

    @Override
    public Resource resolve(final HttpServletRequest request, final String absPath) {
        String path = absPath;
        if (path == null) {
            path = "/";
        }

        // split off query string or fragment that may be appendend to the URL
        String urlRemainder = null;
        int urlRemainderPos = Math.min(path.indexOf('?'), path.indexOf('#'));
        if (urlRemainderPos >= 0) {
          urlRemainder = path.substring(urlRemainderPos);
          path = path.substring(0, urlRemainderPos);
        }
        
        // unmangle namespaces
        if (options.isMangleNamespacePrefixes()) {
            path = NamespaceMangler.unmangleNamespaces(path);
        }

        // build full path again
        path = path + (urlRemainder != null ? urlRemainder : "");

        return this.getResource(path);
    }

    @Override
    public Resource resolve(final String absPath) {
        return resolve(null, absPath);
    }

    @Override
    public String map(final String resourcePath) {
        return map(null, resourcePath);
    }

    @Override
    public String map(final HttpServletRequest request, final String resourcePath) {
        String path = resourcePath;

        // split off query string or fragment that may be appendend to the URL
        String urlRemainder = null;
        int urlRemainderPos = Math.min(path.indexOf('?'), path.indexOf('#'));
        if (urlRemainderPos >= 0) {
          urlRemainder = path.substring(urlRemainderPos);
          path = path.substring(0, urlRemainderPos);
        }
        
        // mangle namespaces
        if (options.isMangleNamespacePrefixes()) {
            path = NamespaceMangler.mangleNamespaces(path);
        }

        // build full path again
        return path + (urlRemainder != null ? urlRemainder : "");
    }
    
    @Override
    public Resource getResource(final String path) {
        Resource resource = getResourceInternal(path);
        
        // if not resource found check if this is a reference to a property
        if (resource == null && path != null) {
            String parentPath = ResourceUtil.getParent(path);
            if (parentPath != null) {
                String name = ResourceUtil.getName(path);
                Resource parentResource = getResourceInternal(parentPath);
                if (parentResource!=null) {
                    ValueMap props = ResourceUtil.getValueMap(parentResource);
                    if (props.containsKey(name)) {
                        return new MockPropertyResource(path, props, this);
                    }
                }
            }
        }
        
        return resource;
    }
    
    private Resource getResourceInternal(final String path) {
        if (path == null) {
            return null;
        }
        
        String normalizedPath = ResourceUtil.normalize(path);
        if (normalizedPath == null) {
            return null;
        } else if ( normalizedPath.startsWith("/") ) {
            if ( this.deletedResources.contains(normalizedPath) ) {
                return null;
            }
            final Map<String, Object> tempProps = this.temporaryResources.get(normalizedPath);
            if ( tempProps != null ) {
                final Resource rsrc = new MockResource(normalizedPath, tempProps, this);
                return rsrc;
            }
            synchronized ( this.resources ) {
                final Map<String, Object> props = this.resources.get(normalizedPath);
                if ( props != null ) {
                    final Resource rsrc = new MockResource(normalizedPath, props, this);
                    return rsrc;
                }
            }
        } else {
            for(final String s : this.getSearchPath() ) {
                final Resource rsrc = this.getResource(s + '/' + normalizedPath);
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
        return this.options.getSearchPaths();
    }

    @Override
    public Iterator<Resource> listChildren(final Resource parent) {
        final String pathPrefix = "/".equals(parent.getPath()) ? "" : parent.getPath();
        final Pattern childPathMatcher = Pattern.compile("^" + Pattern.quote(pathPrefix) + "/[^/]+$");
        final Map<String, Map<String, Object>> candidates = new LinkedHashMap<String, Map<String,Object>>();
        synchronized ( this.resources ) {
            for(final Map.Entry<String, Map<String, Object>> e : this.resources.entrySet()) {
                if (childPathMatcher.matcher(e.getKey()).matches()) {
                    if ( !this.deletedResources.contains(e.getKey()) ) {
                        candidates.put(e.getKey(), e.getValue());
                    }
                }
            }
            for(final Map.Entry<String, Map<String, Object>> e : this.temporaryResources.entrySet()) {
                if (childPathMatcher.matcher(e.getKey()).matches()) {
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

    // part of Resource API 2.5.0
    public Iterable<Resource> getChildren(final Resource parent) {
        return new Iterable<Resource>() {
            @Override
            public Iterator<Resource> iterator() {
                return listChildren(parent);
            }
        };
    }

    @Override
    public boolean isLive() {
        return true;
    }

    @Override
    public void close() {
        this.factory.closed(this);
    }

    @Override
    public String getUserID() {
        return null;
    }

    @Override
    public Iterator<String> getAttributeNames() {
        return attributes.keySet().iterator();
    }

    @Override
    public Object getAttribute(final String name) {
        return attributes.get(name);
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
            if ( this.resources.containsKey(path) && !this.deletedResources.contains(path) ) {
                throw new PersistenceException("Path already exists: " + path);
            }
        }
        this.deletedResources.remove(path);
        if ( properties == null ) {
            properties = new HashMap<String, Object>();
        }
        
        Resource mockResource = new MockResource(path, properties, this);
        this.temporaryResources.put(path, ResourceUtil.getValueMap(mockResource));
        return mockResource;
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
                if ( this.resources.remove(path) != null && this.options.getEventAdmin() != null ) {
                    final Dictionary<String, Object> props = new Hashtable<String, Object>();
                    props.put(SlingConstants.PROPERTY_PATH, path);
                    final Event e = new Event(SlingConstants.TOPIC_RESOURCE_REMOVED, props);
                    this.options.getEventAdmin().sendEvent(e);
                }
                this.temporaryResources.remove(path);
            }
            for(final String path : this.temporaryResources.keySet() ) {
                final boolean changed = this.resources.containsKey(path);
                this.resources.put(path, this.temporaryResources.get(path));
                if ( this.options.getEventAdmin() != null ) {
                    final Dictionary<String, Object> props = new Hashtable<String, Object>();
                    props.put(SlingConstants.PROPERTY_PATH, path);
                    if ( this.resources.get(path).get(ResourceResolver.PROPERTY_RESOURCE_TYPE) != null ) {
                        props.put(SlingConstants.PROPERTY_RESOURCE_TYPE, this.resources.get(path).get(ResourceResolver.PROPERTY_RESOURCE_TYPE));
                    }
                    final Event e = new Event(changed ? SlingConstants.TOPIC_RESOURCE_CHANGED : SlingConstants.TOPIC_RESOURCE_ADDED, props);
                    this.options.getEventAdmin().sendEvent(e);
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

    // part of Resource API 2.6.0
    public boolean hasChildren(Resource resource) {
        return this.listChildren(resource).hasNext();
    }


    // --- unsupported operations ---

    @Override
    @Deprecated
    public Resource resolve(final HttpServletRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getParentResourceType(Resource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getParentResourceType(String resourceType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Resource> findResources(final String query, final String language) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Map<String, Object>> queryResources(String query, String language) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceResolver clone(Map<String, Object> authenticationInfo) throws LoginException {
        throw new UnsupportedOperationException();
    }

    public Resource getParent(Resource child) {
        final String parentPath = ResourceUtil.getParent(child.getPath());
        if (parentPath == null) {
            return null;
        }
        return this.getResource(parentPath);
    }

    public void copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
        throw new UnsupportedOperationException();
    }

    public void move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        throw new UnsupportedOperationException();
    }

}
