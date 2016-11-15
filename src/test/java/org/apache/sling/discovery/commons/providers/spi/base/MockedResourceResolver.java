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
package org.apache.sling.discovery.commons.providers.spi.base;

import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.jcr.api.SlingRepository;

public class MockedResourceResolver implements ResourceResolver {

	private final SlingRepository repository;

	private Session session;

    private List<MockedResource> resources = new LinkedList<MockedResource>();

    public MockedResourceResolver() throws RepositoryException {
    	this(null);
    }

    public MockedResourceResolver(SlingRepository repositoryOrNull) throws RepositoryException {
    	if (repositoryOrNull==null) {
    		this.repository = RepositoryProvider.instance().getRepository();
    		Session adminSession = null;
    		try {
    		    adminSession = this.repository.loginAdministrative(null);
                RepositoryUtil.registerSlingNodeTypes(adminSession);
    		} catch ( final IOException ioe ) {
    		    throw new RepositoryException(ioe);
    		} finally {
    		    if ( adminSession != null ) {
    		        adminSession.logout();
    		    }
    		}
    	} else {
    		this.repository = repositoryOrNull;
    	}
    }

    public Session getSession() throws RepositoryException {
        synchronized (this) {
            if (session != null) {
                return session;
            }
            session = createSession();
            return session;
        }
    }

    private Repository getRepository() {
    	return repository;
    }

    private Session createSession() throws RepositoryException {
        final Credentials credentials = new SimpleCredentials("admin",
                "admin".toCharArray());
        return repository.login(credentials, "default");
    }


    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type.equals(Session.class)) {
            try {
                return (AdapterType) getSession();
            } catch (RepositoryException e) {
                throw new RuntimeException("RepositoryException: " + e, e);
            }
        } else if (type.equals(Repository.class)) {
        	return (AdapterType) getRepository();
        }
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Resource resolve(HttpServletRequest request, String absPath) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Resource resolve(String absPath) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    @Deprecated
    public Resource resolve(HttpServletRequest request) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String map(String resourcePath) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String map(HttpServletRequest request, String resourcePath) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Resource getResource(String path) {
        Session session;
        try {
            session = getSession();
            session.getNode(path);
        } catch (PathNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException("RepositoryException: " + e, e);
        }
        return new MockedResource(this, path, "nt:unstructured");
    }

    @Override
    public Resource getResource(Resource base, String path) {
        if (base.getPath().equals("/")) {
            return getResource("/" + path);
        } else {
            return getResource(base.getPath() + "/" + path);
        }
    }

    @Override
    public String[] getSearchPath() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Iterator<Resource> listChildren(Resource parent) {
        try {
            Node node = parent.adaptTo(Node.class);
            final NodeIterator nodes = node.getNodes();
            return new Iterator<Resource>() {

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Resource next() {
                    Node next = nodes.nextNode();
                    try {
                        return new MockedResource(MockedResourceResolver.this,
                                next.getPath(), "nt:unstructured");
                    } catch (RepositoryException e) {
                        throw new RuntimeException("RepositoryException: " + e,
                                e);
                    }
                }

                @Override
                public boolean hasNext() {
                    return nodes.hasNext();
                }
            };
        } catch (RepositoryException e) {
            throw new RuntimeException("RepositoryException: " + e, e);
        }
    }

    @Override
    public Iterable<Resource> getChildren(Resource parent) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Iterator<Resource> findResources(String query, String language) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Iterator<Map<String, Object>> queryResources(String query,
            String language) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ResourceResolver clone(Map<String, Object> authenticationInfo)
            throws LoginException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean isLive() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void close() {
        Iterator<MockedResource> it = resources.iterator();
        while (it.hasNext()) {
            MockedResource r = it.next();
            r.close();
        }
        if (session != null) {
            if (session.isLive()) {
                session.logout();
            }
            session = null;
        }
    }

    public void register(MockedResource mockedResource) {
        resources.add(mockedResource);
    }

    @Override
    public String getUserID() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Iterator<String> getAttributeNames() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Object getAttribute(String name) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void delete(Resource resource) throws PersistenceException {
        if (resources.contains(resource)) {
            resources.remove(resource);
            Node node = resource.adaptTo(Node.class);
            try {
                node.remove();
            } catch (RepositoryException e) {
                throw new PersistenceException("RepositoryException: "+e, e);
            }
        } else {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    @Override
    public Resource create(Resource parent, String name,
            Map<String, Object> properties) throws PersistenceException {
        final Node parentNode = parent.adaptTo(Node.class);
        try {
            final Node child;
            if (properties!=null && properties.containsKey("jcr:primaryType")) {
                child = parentNode.addNode(name, (String) properties.get("jcr:primaryType"));
            } else {
                child = parentNode.addNode(name);
            }
            if (properties!=null) {
                final Iterator<Entry<String, Object>> it = properties.entrySet().iterator();
                while(it.hasNext()) {
                    final Entry<String, Object> entry = it.next();
                    if (entry.getKey().equals("jcr:primaryType")) {
                        continue;
                    }
                    if (entry.getValue() instanceof String) {
                        child.setProperty(entry.getKey(), (String)entry.getValue());
                    } else if (entry.getValue() instanceof Boolean) {
                        child.setProperty(entry.getKey(), (Boolean)entry.getValue());
                    } else if (entry.getValue() instanceof Calendar) {
                        child.setProperty(entry.getKey(), (Calendar)entry.getValue());
                    } else {
                        throw new UnsupportedOperationException("Not implemented (entry.getValue(): "+entry.getValue()+")");
                    }
                }
            }
            return getResource(parent, name);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void revert() {
        try {
            this.session.refresh(false);
        } catch (final RepositoryException re) {
            throw new RuntimeException("Unable to commit changes.", re);
        }
    }

    @Override
    public void commit() throws PersistenceException {
        try {
            this.session.save();
        } catch (final RepositoryException re) {
            throw new PersistenceException("Unable to commit changes.", re);
        }
    }

    @Override
    public boolean hasChanges() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String getParentResourceType(Resource resource) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getParentResourceType(String resourceType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isResourceType(Resource resource, String resourceType) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void refresh() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean hasChildren(Resource arg0) {
        Iterable<Resource> iter = this.getChildren(arg0);
        return iter.iterator().hasNext();
    }

    @Override
    public Resource getParent(Resource child) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Resource copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Resource move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        throw new UnsupportedOperationException("Not implemented");
    }

}
