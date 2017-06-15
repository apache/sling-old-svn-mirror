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
package org.apache.sling.testing.mock.sling.junit;

import java.util.Map;

import org.apache.sling.testing.mock.osgi.context.ContextCallback;
import org.apache.sling.testing.mock.osgi.context.ContextPlugins;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.osgi.annotation.versioning.ProviderType;

/**
 * JUnit rule for setting up and tearing down Sling context objects for unit tests.
 * You can use {@link SlingContextBuilder} alternatively to the constructors on this class - it offers
 * more options and fine-grained control about setting up the test context.
 */
@ProviderType
public final class SlingContext extends SlingContextImpl implements TestRule {

    private final ContextPlugins plugins;
    private final TestRule delegate;

    /**
     * Initialize Sling context with default resource resolver type:
     * {@link org.apache.sling.testing.mock.sling.MockSling#DEFAULT_RESOURCERESOLVER_TYPE}.
     */
    public SlingContext() {
        this(new ContextPlugins(), null, null);
    }

    /**
     * Initialize Sling context with resource resolver type.
     * @param resourceResolverType Resource resolver type.
     */
    public SlingContext(final ResourceResolverType resourceResolverType) {
        this(new ContextPlugins(), null, resourceResolverType);
    }

    /**
     * Initialize Sling context with default resource resolver type:
     * {@link org.apache.sling.testing.mock.sling.MockSling#DEFAULT_RESOURCERESOLVER_TYPE}.
     * @param <T> context type
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     */
    public <T extends OsgiContextImpl> SlingContext(final ContextCallback<T> afterSetUpCallback) {
        this(new ContextPlugins(afterSetUpCallback), null, null);
    }

    /**
     * Initialize Sling context with resource resolver type.
     * @param <T> context type
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @param resourceResolverType Resource resolver type.
     */
    public <T extends OsgiContextImpl> SlingContext(final ContextCallback<T> afterSetUpCallback, final ResourceResolverType resourceResolverType) {
        this(new ContextPlugins(afterSetUpCallback), null, resourceResolverType);
    }

    /**
     * Initialize Sling context with default resource resolver type:
     * {@link org.apache.sling.testing.mock.sling.MockSling#DEFAULT_RESOURCERESOLVER_TYPE}.
     * @param <U> context type
     * @param <V> context type
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     */
    public <U extends OsgiContextImpl, V extends OsgiContextImpl> SlingContext(final ContextCallback<U> afterSetUpCallback, final ContextCallback<V> beforeTearDownCallback) {
        this(new ContextPlugins(afterSetUpCallback, beforeTearDownCallback), null, null);
    }
    
    /**
     * Initialize Sling context with resource resolver type.
     * @param <U> context type
     * @param <V> context type
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     * @param resourceResolverType Resource resolver type.
     */
    public <U extends OsgiContextImpl, V extends OsgiContextImpl> SlingContext(final ContextCallback<U> afterSetUpCallback, final ContextCallback<V> beforeTearDownCallback,
            final ResourceResolverType resourceResolverType) {
        this(new ContextPlugins(afterSetUpCallback, beforeTearDownCallback), null, resourceResolverType);
    }
    
    /**
     * Initialize Sling context with resource resolver type.
     * @param contextPlugins Context plugins
     * @param resourceResolverFactoryActivatorProps Allows to override OSGi configuration parameters for the Resource Resolver Factory Activator service.
     * @param resourceResolverType Resource resolver type.
     */
    SlingContext(final ContextPlugins contextPlugins,
            final Map<String, Object> resourceResolverFactoryActivatorProps,
            final ResourceResolverType resourceResolverType) {

        this.plugins = contextPlugins;
        setResourceResolverFactoryActivatorProps(resourceResolverFactoryActivatorProps);

        // set resource resolver type in parent context
        setResourceResolverType(resourceResolverType);

        // wrap {@link ExternalResource} rule executes each test method once
        this.delegate = new ExternalResource() {
            @Override
            protected void before() {
                plugins.executeBeforeSetUpCallback(SlingContext.this);
                SlingContext.this.setUp();
                plugins.executeAfterSetUpCallback(SlingContext.this);
            }

            @Override
            protected void after() {
                plugins.executeBeforeTearDownCallback(SlingContext.this);
                SlingContext.this.tearDown();
                plugins.executeAfterTearDownCallback(SlingContext.this);
            }
        };
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return this.delegate.apply(base, description);
    }

}
