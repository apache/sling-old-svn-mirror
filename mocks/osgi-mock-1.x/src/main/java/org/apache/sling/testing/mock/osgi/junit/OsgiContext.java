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
package org.apache.sling.testing.mock.osgi.junit;

import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * JUnit rule for setting up and tearing down OSGi context for unit tests.
 */
public final class OsgiContext extends OsgiContextImpl implements TestRule {

    private final CallbackParams callbackParams;
    private final TestRule delegate;

    /**
     * Initialize OSGi context.
     */
    public OsgiContext() {
        this(new CallbackParams());
    }

    /**
     * Initialize OSGi context.
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     */
    public OsgiContext(final ContextCallback afterSetUpCallback) {
        this(new CallbackParams(afterSetUpCallback));
    }

    /**
     * Initialize OSGi context.
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     */
    public OsgiContext(final ContextCallback afterSetUpCallback, final ContextCallback beforeTearDownCallback) {
        this(new CallbackParams(afterSetUpCallback, beforeTearDownCallback));
    }

    /**
     * Initialize OSGi context with resource resolver type.
     * @param callbackParams Callback parameters
     */
    OsgiContext(final CallbackParams callbackParams) {
        this.callbackParams = callbackParams;

        // wrap {@link ExternalResource} rule executes each test method once
        this.delegate = new ExternalResource() {
            @Override
            protected void before() {
                OsgiContext.this.executeBeforeSetUpCallback();
                OsgiContext.this.setUp();
                OsgiContext.this.executeAfterSetUpCallback();
            }

            @Override
            protected void after() {
                OsgiContext.this.executeBeforeTearDownCallback();
                OsgiContext.this.tearDown();
                OsgiContext.this.executeAfterTearDownCallback();
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
