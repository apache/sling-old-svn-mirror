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

import org.apache.sling.testing.mock.osgi.junit.ContextCallback;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit rule for setting up and tearing down Sling context objects for unit tests.
 * You can use {@link SlingContextBuilder} alternatively to the constructors on this class - it offers
 * more options and fine-grained control about setting up the test context.
 */
public final class SlingContext extends SlingContextImpl implements TestRule {

    private final CallbackParams callbackParams;
    private final TestRule delegate;

    /**
     * Initialize Sling context with default resource resolver type:
     * {@link org.apache.sling.testing.mock.sling.MockSling#DEFAULT_RESOURCERESOLVER_TYPE}.
     */
    public SlingContext() {
        this(new CallbackParams(), null, null);
    }

    /**
     * Initialize Sling context with resource resolver type.
     * @param resourceResolverType Resource resolver type.
     */
    public SlingContext(final ResourceResolverType resourceResolverType) {
        this(new CallbackParams(), null, resourceResolverType);
    }

    /**
     * Initialize Sling context with default resource resolver type:
     * {@link org.apache.sling.testing.mock.sling.MockSling#DEFAULT_RESOURCERESOLVER_TYPE}.
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     */
    public SlingContext(final ContextCallback<?> afterSetUpCallback) {
        this(new CallbackParams(afterSetUpCallback), null, null);
    }

    /**
     * Initialize Sling context with resource resolver type.
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @param resourceResolverType Resource resolver type.
     */
    public SlingContext(final ContextCallback<?> afterSetUpCallback, final ResourceResolverType resourceResolverType) {
        this(new CallbackParams(afterSetUpCallback), null, resourceResolverType);
    }

    /**
     * Initialize Sling context with default resource resolver type:
     * {@link org.apache.sling.testing.mock.sling.MockSling#DEFAULT_RESOURCERESOLVER_TYPE}.
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     */
    public SlingContext(final ContextCallback<?> afterSetUpCallback, final ContextCallback<?> beforeTearDownCallback) {
        this(new CallbackParams(afterSetUpCallback, beforeTearDownCallback), null, null);
    }
    
    /**
     * Initialize Sling context with resource resolver type.
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     * @param resourceResolverType Resource resolver type.
     */
    public SlingContext(final ContextCallback<?> afterSetUpCallback, final ContextCallback<?> beforeTearDownCallback,
            final ResourceResolverType resourceResolverType) {
        this(new CallbackParams(afterSetUpCallback, beforeTearDownCallback), null, resourceResolverType);
    }
    
    /**
     * Initialize Sling context with resource resolver type.
     * @param callbackParams Callback parameters
     * @param resourceResolverFactoryActivatorProps Allows to override OSGi configuration parameters for the Resource Resolver Factory Activator service.
     * @param resourceResolverType Resource resolver type.
     */
    SlingContext(final CallbackParams callbackParams,
            final Map<String, Object> resourceResolverFactoryActivatorProps,
            final ResourceResolverType resourceResolverType) {

        this.callbackParams = callbackParams;
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

    @SuppressWarnings("unchecked")
    private void executeBeforeSetUpCallback() {
        if (callbackParams.beforeSetUpCallback != null) {
            try {
                for (ContextCallback callback : callbackParams.beforeSetUpCallback) {
                    callback.execute(this);
                }
            } catch (Throwable ex) {
                throw new RuntimeException("Before setup failed: " + ex.getMessage(), ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void executeAfterSetUpCallback() {
        if (callbackParams.afterSetUpCallback != null) {
            try {
                for (ContextCallback callback : callbackParams.afterSetUpCallback) {
                    callback.execute(this);
                }
            } catch (Throwable ex) {
                throw new RuntimeException("After setup failed: " + ex.getMessage(), ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void executeBeforeTearDownCallback() {
        if (callbackParams.beforeTearDownCallback != null) {
            try {
                for (ContextCallback callback : callbackParams.beforeTearDownCallback) {
                    callback.execute(this);
                }
            } catch (Throwable ex) {
                throw new RuntimeException("Before teardown failed: " + ex.getMessage(), ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void executeAfterTearDownCallback() {
        if (callbackParams.afterTearDownCallback != null) {
            try {
                for (ContextCallback callback : callbackParams.afterTearDownCallback) {
                    callback.execute(this);
                }
            } catch (Throwable ex) {
                throw new RuntimeException("After teardown failed: " + ex.getMessage(), ex);
            }
        }
    }

}
