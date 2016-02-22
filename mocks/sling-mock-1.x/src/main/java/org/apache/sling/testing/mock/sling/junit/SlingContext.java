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

    private final SlingContextCallback setUpCallback;
    private final SlingContextCallback tearDownCallback;
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
     * @param setUpCallback Allows the application to register an own callback
     *            function that is called after the built-in setup rules are
     *            executed.
     */
    public SlingContext(final SlingContextCallback setUpCallback) {
        this(setUpCallback, null, null);
    }

    /**
     * Initialize Sling context with resource resolver type.
     * @param setUpCallback Allows the application to register an own callback
     *            function that is called after the built-in setup rules are
     *            executed.
     * @param resourceResolverType Resource resolver type.
     */
    public SlingContext(final SlingContextCallback setUpCallback, final ResourceResolverType resourceResolverType) {
        this(setUpCallback, null, resourceResolverType);
    }

    /**
     * Initialize Sling context with default resource resolver type:
     * {@link org.apache.sling.testing.mock.sling.MockSling#DEFAULT_RESOURCERESOLVER_TYPE}.
     * @param setUpCallback Allows the application to register an own callback
     *            function that is called after the built-in setup rules are
     *            executed.
     * @param tearDownCallback Allows the application to register an own
     *            callback function that is called before the built-in teardown
     *            rules are executed.
     */
    public SlingContext(final SlingContextCallback setUpCallback, final SlingContextCallback tearDownCallback) {
        this(setUpCallback, tearDownCallback, null);
    }
    
    /**
     * Initialize Sling context with resource resolver type.
     * @param setUpCallback Allows the application to register an own callback
     *            function that is called after the built-in setup rules are
     *            executed.
     * @param tearDownCallback Allows the application to register an own
     *            callback function that is called before the built-in teardown
     *            rules are executed.
     * @param resourceResolverType Resource resolver type.
     */
    public SlingContext(final SlingContextCallback setUpCallback, final SlingContextCallback tearDownCallback,
            final ResourceResolverType resourceResolverType) {

        this.setUpCallback = setUpCallback;
        this.tearDownCallback = tearDownCallback;

        // set resource resolver type in parent context
        setResourceResolverType(resourceResolverType);

        // wrap {@link ExternalResource} rule executes each test method once
        this.delegate = new ExternalResource() {
            @Override
            protected void before() {
                SlingContext.this.setUp();
                SlingContext.this.executeSetUpCallback();
            }

            @Override
            protected void after() {
                SlingContext.this.executeTearDownCallback();
                SlingContext.this.tearDown();
            }
        };
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return this.delegate.apply(base, description);
    }

    private void executeSetUpCallback() {
        if (this.setUpCallback != null) {
            try {
                this.setUpCallback.execute(this);
            } catch (Throwable ex) {
                throw new RuntimeException("Setup failed: " + ex.getMessage(), ex);
            }
        }
    }

    private void executeTearDownCallback() {
        if (this.tearDownCallback != null) {
            try {
                this.tearDownCallback.execute(this);
            } catch (Throwable ex) {
                throw new RuntimeException("Teardown failed: " + ex.getMessage(), ex);
            }
        }
    }

}
