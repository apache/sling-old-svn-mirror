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
package org.apache.sling.discovery.impl.setup;

import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

public class MockedResource extends SyntheticResource {

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
                    session = mockedResourceResolver.createSession();
                } catch (RepositoryException e) {
                    throw new RuntimeException("RepositoryException: " + e, e);
                }
            }
            return session;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
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
                    } else {
                        throw new RuntimeException(
                                "Unsupported property type: " + p.getType());
                    }
                }
                ValueMap valueMap = new ValueMapDecorator(map);
                return (AdapterType) valueMap;
            } catch (Exception e) {
                throw new RuntimeException("Exception occurred: " + e, e);
            }
        } else {
            return super.adaptTo(type);
        }
    }

}
