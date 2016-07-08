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

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit rule for setting up and tearing down Sling context objects for unit
 * tests.
 */
public final class SlingContext extends SlingContextImpl implements TestRule {

    private final SlingContextCallback beforeSetUpCallback;
    private final SlingContextCallback afterSetUpCallback;
    private final SlingContextCallback beforeTearDownCallback;
    private final SlingContextCallback afterTearDownCallback;
    private final TestRule delegate;

    /**
     * Initialize Sling context with default resource resolver type:
     * {@link org.apache.sling.testing.mock.sling.MockSling#DEFAULT_RESOURCERESOLVER_TYPE}.
     */
    public SlingContext() {
        this(null, null, null);
    }

    /**
     * Initialize Sling context with resource resolver type.
     * @param resourceResolverType Resource resolver type.
     */
    public SlingContext(final ResourceResolverType resourceResolverType) {
        this(null, null, resourceResolverType);
    }

    /**
     * Initialize Sling context with default resource resolver type:
     * {@link org.apache.sling.testing.mock.sling.MockSling#DEFAULT_RESOURCERESOLVER_TYPE}.
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     */
    public SlingContext(final SlingContextCallback afterSetUpCallback) {
        this(afterSetUpCallback, null, null);
    }

    /**
     * Initialize Sling context with resource resolver type.
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @param resourceResolverType Resource resolver type.
     */
    public SlingContext(final SlingContextCallback afterSetUpCallback, final ResourceResolverType resourceResolverType) {
        this(afterSetUpCallback, null, resourceResolverType);
    }

    /**
     * Initialize Sling context with default resource resolver type:
     * {@link org.apache.sling.testing.mock.sling.MockSling#DEFAULT_RESOURCERESOLVER_TYPE}.
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     */
    public SlingContext(final SlingContextCallback afterSetUpCallback, final SlingContextCallback beforeTearDownCallback) {
        this(afterSetUpCallback, beforeTearDownCallback, null);
    }
    
    /**
     * Initialize Sling context with resource resolver type.
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     * @param resourceResolverType Resource resolver type.
     */
    public SlingContext(final SlingContextCallback afterSetUpCallback, final SlingContextCallback beforeTearDownCallback,
            final ResourceResolverType resourceResolverType) {
        this(null, afterSetUpCallback, beforeTearDownCallback, null, null, resourceResolverType);
    }
    
    /**
     * Initialize Sling context with resource resolver type.
     * @param beforeSetUpCallback Allows the application to register an own callback function that is called before the built-in setup rules are executed.
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     * @param afterTearDownCallback Allows the application to register an own callback function that is after before the built-in teardown rules are executed.
     * @param resourceResolverFactoryActivatorProps Allows to override OSGi configuration parameters for the Resource Resolver Factory Activator service.
     * @param resourceResolverType Resource resolver type.
     */
    SlingContext(final SlingContextCallback beforeSetUpCallback, final SlingContextCallback afterSetUpCallback,
            final SlingContextCallback beforeTearDownCallback, final SlingContextCallback afterTearDownCallback,
            final Map<String, Object> resourceResolverFactoryActivatorProps,
            final ResourceResolverType resourceResolverType) {

        this.beforeSetUpCallback = beforeSetUpCallback;
        this.afterSetUpCallback = afterSetUpCallback;
        this.beforeTearDownCallback = beforeTearDownCallback;
        this.afterTearDownCallback = afterTearDownCallback;
        setResourceResolverFactoryActivatorProps(resourceResolverFactoryActivatorProps);

        // set resource resolver type in parent context
        setResourceResolverType(resourceResolverType);

        // wrap {@link ExternalResource} rule executes each test method once
        this.delegate = new ExternalResource() {
            @Override
            protected void before() {
                SlingContext.this.executeBeforeSetUpCallback();
                SlingContext.this.setUp();
                SlingContext.this.executeAfterSetUpCallback();
            }

            @Override
            protected void after() {
                SlingContext.this.executeBeforeTearDownCallback();
                SlingContext.this.tearDown();
                SlingContext.this.executeAfterTearDownCallback();
            }
        };
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return this.delegate.apply(base, description);
    }

    private void executeBeforeSetUpCallback() {
        if (this.beforeSetUpCallback != null) {
            try {
                this.beforeSetUpCallback.execute(this);
            } catch (Throwable ex) {
                throw new RuntimeException("Before setup failed: " + ex.getMessage(), ex);
            }
        }
    }

    private void executeAfterSetUpCallback() {
        if (this.afterSetUpCallback != null) {
            try {
                this.afterSetUpCallback.execute(this);
            } catch (Throwable ex) {
                throw new RuntimeException("After setup failed: " + ex.getMessage(), ex);
            }
        }
    }

    private void executeBeforeTearDownCallback() {
        if (this.beforeTearDownCallback != null) {
            try {
                this.beforeTearDownCallback.execute(this);
            } catch (Throwable ex) {
                throw new RuntimeException("Before teardown failed: " + ex.getMessage(), ex);
            }
        }
    }

    private void executeAfterTearDownCallback() {
        if (this.afterTearDownCallback != null) {
            try {
                this.afterTearDownCallback.execute(this);
            } catch (Throwable ex) {
                throw new RuntimeException("After teardown failed: " + ex.getMessage(), ex);
            }
        }
    }

}
