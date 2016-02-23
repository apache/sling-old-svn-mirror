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

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.osgi.framework.BundleContext;

/**
 * Create resolve resolver instance and initialize it depending on it's type.
 */
final class ContextResourceResolverFactory {

    private ContextResourceResolverFactory() {
        // static methods only
    }

    public static ResourceResolverFactory get(final ResourceResolverType resourceResolverType,
            final BundleContext bundleContext) {
        ResourceResolverType type = resourceResolverType;
        if (type == null) {
            type = MockSling.DEFAULT_RESOURCERESOLVER_TYPE;
        }
        try {
            ResourceResolverFactory factory = MockSling.newResourceResolverFactory(type, bundleContext);

            switch (type) {
            case JCR_MOCK:
                initializeJcrMock(factory);
                break;
            case JCR_OAK:
                initializeJcrOak(factory);
                break;
            case RESOURCERESOLVER_MOCK:
                initializeResourceResolverMock(factory);
                break;
            case NONE:
                initializeResourceResolverNone(factory);
                break;
            default:
                throw new IllegalArgumentException("Invalid resource resolver type: " + type);
            }

            return factory;
        } catch (Throwable ex) {
            throw new RuntimeException("Unable to initialize " + type + " resource resolver factory: " + ex.getMessage(), ex);
        }
    }

    private static void initializeJcrMock(ResourceResolverFactory factory) throws RepositoryException, LoginException {
        // nothing to do
    }

    private static void initializeJcrOak(ResourceResolverFactory factory) {
        // register sling node types?
    }

    private static void initializeResourceResolverMock(ResourceResolverFactory factory) {
        // nothing to do
    }

    private static void initializeResourceResolverNone(ResourceResolverFactory factory) {
        // nothing to do
    }

}
