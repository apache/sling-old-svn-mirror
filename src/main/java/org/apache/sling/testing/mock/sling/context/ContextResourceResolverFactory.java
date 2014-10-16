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
package org.apache.sling.testing.mock.sling.context;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;

/**
 * Create resolve resolver instance and initialize it depending on it's type.
 */
final class ContextResourceResolverFactory {

    private ContextResourceResolverFactory() {
        // static methods only
    }

    public static ResourceResolverFactory get(final ResourceResolverType resourceResolverType) {
        try {
            ResourceResolverFactory factory = MockSling.newResourceResolverFactory(resourceResolverType);

            switch (resourceResolverType) {
            case JCR_MOCK:
                initializeJcrMock(factory);
                break;
            case JCR_JACKRABBIT:
                initializeJcrJackrabbit(factory);
                break;
            case RESOURCERESOLVER_MOCK:
                initializeResourceResolverMock(factory);
                break;
            default:
                throw new IllegalArgumentException("Invalid resource resolver type: " + resourceResolverType);
            }

            return factory;
        } catch (Throwable ex) {
            throw new RuntimeException("Unable to initialize " + resourceResolverType + " resource resolver factory.", ex);
        }
    }

    private static void initializeJcrMock(ResourceResolverFactory factory) throws RepositoryException, LoginException {
        // register default namespaces
        ResourceResolver resolver = factory.getResourceResolver(null);
        Session session = resolver.adaptTo(Session.class);
        NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
        namespaceRegistry.registerNamespace("sling", "http://sling.apache.org/jcr/sling/1.0");
    }

    private static void initializeJcrJackrabbit(ResourceResolverFactory factory) {
        // register sling node types?
    }

    private static void initializeResourceResolverMock(ResourceResolverFactory factory) {
        // nothing to do
    }

}
