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
package org.apache.sling.discovery.commons.providers.spi.impl;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockedResource extends SyntheticResource {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final MockedResourceResolver mockedResourceResolver;
    private Session session;

    public MockedResource(MockedResourceResolver resourceResolver, String path,
            String resourceType) {
        super(resourceResolver, path, resourceType);
        mockedResourceResolver = resourceResolver;

        resourceResolver.register(this);
    }

    private Session getSession() {
        synchronized (this) {
            if (session == null) {
                try {
                    session = mockedResourceResolver.getSession();
                } catch (RepositoryException e) {
                    throw new RuntimeException("RepositoryException: " + e, e);
                }
            }
            return session;
        }
    }

    @Override
    protected void finalize() throws Throwable {
//        close();
        super.finalize();
    }

    public void close() {
        synchronized (this) {
            if (session != null) {
                if (session.isLive()) {
                    session.logout();
                }
                session = null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type.equals(Node.class)) {
            try {
                return (AdapterType) getSession().getNode(getPath());
            } catch (Exception e) {
                logger.error("Exception occurred: "+e, e);
                throw new RuntimeException("Exception occurred: " + e, e);
            }
        } else if (type.equals(ValueMap.class)) {
            try {
                Session session = getSession();
                Node node = session.getNode(getPath());
                HashMap<String, Object> map = new HashMap<String, Object>();

                PropertyIterator properties = node.getProperties();
                while (properties.hasNext()) {
                    Property p = properties.nextProperty();
                    if (p.getType() == PropertyType.BOOLEAN) {
                        map.put(p.getName(), p.getBoolean());
                    } else if (p.getType() == PropertyType.STRING) {
                        map.put(p.getName(), p.getString());
                    } else if (p.getType() == PropertyType.DATE) {
                        map.put(p.getName(), p.getDate().getTime());
                    } else if (p.getType() == PropertyType.NAME) {
                        map.put(p.getName(), p.getName());
                    } else if (p.getType() == PropertyType.LONG) {
                        map.put(p.getName(), p.getLong());
                    } else {
                        throw new RuntimeException(
                                "Unsupported property type: " + p.getType());
                    }
                }
                ValueMap valueMap = new ValueMapDecorator(map);
                return (AdapterType) valueMap;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else if (type.equals(ModifiableValueMap.class)) {
            return (AdapterType) new ModifiableValueMap() {
                
                public Collection<Object> values() {
                    throw new UnsupportedOperationException();
                }
                
                public int size() {
                    throw new UnsupportedOperationException();
                }
                
                public Object remove(Object arg0) {
                    Session session = getSession();
                    try{
                        final Node node = session.getNode(getPath());
                        final Property p = node.getProperty(String.valueOf(arg0));
                        if (p!=null) {
                        	p.remove();
                        }
                        // this is not according to the spec - but OK for tests since
                        // the return value is never used
                        return null;
                    } catch(PathNotFoundException pnfe) {
                    	// perfectly fine
                    	return null;
                    } catch(RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
                
                public void putAll(Map<? extends String, ? extends Object> arg0) {
                    throw new UnsupportedOperationException();
                }
                
                public Object put(String arg0, Object arg1) {
                    Session session = getSession();
                    try{
                        final Node node = session.getNode(getPath());
                        Object result = null;
                        if (node.hasProperty(arg0)) {
                            final Property previous = node.getProperty(arg0);
                            if (previous==null) {
                                // null
                            } else if (previous.getType() == PropertyType.STRING) {
                                result = previous.getString();
                            } else if (previous.getType() == PropertyType.DATE) {
                                result = previous.getDate();
                            } else if (previous.getType() == PropertyType.BOOLEAN) {
                                result = previous.getBoolean();
                            } else {
                                throw new UnsupportedOperationException();
                            }
                        }
                        if (arg1 instanceof String) {
                            node.setProperty(arg0, (String)arg1);
                        } else if (arg1 instanceof Calendar) {
                            node.setProperty(arg0, (Calendar)arg1);
                        } else if (arg1 instanceof Boolean) {
                            node.setProperty(arg0, (Boolean)arg1);
                        } else if (arg1 instanceof Date) {
                            final Calendar c = Calendar.getInstance();
                            c.setTime((Date)arg1);
                            node.setProperty(arg0, c);
                        } else if (arg1 instanceof Number) {
                            Number n = (Number)arg1;
                            node.setProperty(arg0, n.longValue());
                        } else {
                            throw new UnsupportedOperationException();
                        }
                        return result;
                    } catch(RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
                
                public Set<String> keySet() {
                    Session session = getSession();
                    try {
                        final Node node = session.getNode(getPath());
                        final PropertyIterator pi = node.getProperties();
                        final Set<String> result = new HashSet<String>();
                        while(pi.hasNext()) {
                            final Property p = pi.nextProperty();
                            result.add(p.getName());
                        }
                        return result;
                    } catch (RepositoryException e) {
                        throw new RuntimeException(e);
                    }
                }
                
                public boolean isEmpty() {
                    throw new UnsupportedOperationException();
                }
                
                public Object get(Object arg0) {
                    Session session = getSession();
                    try{
                        final Node node = session.getNode(getPath());
                        final String key = String.valueOf(arg0);
                        if (node.hasProperty(key)) {
                            return node.getProperty(key);
                        } else {
                            return null;
                        }
                    } catch(RepositoryException re) {
                        throw new RuntimeException(re);
                    }
                }
                
                public Set<Entry<String, Object>> entrySet() {
                    throw new UnsupportedOperationException();
                }
                
                public boolean containsValue(Object arg0) {
                    throw new UnsupportedOperationException();
                }
                
                public boolean containsKey(Object arg0) {
                    Session session = getSession();
                    try{
                        final Node node = session.getNode(getPath());
                        return node.hasProperty(String.valueOf(arg0));
                    } catch(RepositoryException re) {
                        throw new RuntimeException(re);
                    }
                }
                
                public void clear() {
                    throw new UnsupportedOperationException();
                }
                
                public <T> T get(String name, T defaultValue) {
                    throw new UnsupportedOperationException();
                }
                
                public <T> T get(String name, Class<T> type) {
                    Session session = getSession();
                    try{
                        final Node node = session.getNode(getPath());
                        if (node==null) {
                        	return null;
                        }
                        if (!node.hasProperty(name)) {
                            return null;
                        }
                        Property p = node.getProperty(name);
                        if (p==null) {
                        	return null;
                        }
                        if (type.equals(Calendar.class)) {
                        	return (T) p.getDate();
                        } else if (type.equals(String.class)) {
                        	return (T) p.getString();
                        } else {
                            throw new UnsupportedOperationException();
                        }
                    } catch(RepositoryException e) {
                    	throw new RuntimeException(e);
                    }
                }
            };
        } else {
            return super.adaptTo(type);
        }
    }

}
