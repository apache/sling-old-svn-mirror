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

    private final OsgiContextCallback setUpCallback;
    private final OsgiContextCallback tearDownCallback;
    private final TestRule delegate;

    /**
     * Initialize OSGi context.
     */
    public OsgiContext() {
        this(null, null);
    }

    /**
     * Initialize OSGi context.
     * @param setUpCallback Allows the application to register an own callback
     *            function that is called after the built-in setup rules are
     *            executed.
     */
    public OsgiContext(final OsgiContextCallback setUpCallback) {
        this(setUpCallback, null);
    }

    /**
     * Initialize OSGi context.
     * @param setUpCallback Allows the application to register an own callback
     *            function that is called after the built-in setup rules are
     *            executed.
     * @param tearDownCallback Allows the application to register an own
     *            callback function that is called before the built-in teardown
     *            rules are executed.
     */
    public OsgiContext(final OsgiContextCallback setUpCallback, final OsgiContextCallback tearDownCallback) {

        this.setUpCallback = setUpCallback;
        this.tearDownCallback = tearDownCallback;

        // wrap {@link ExternalResource} rule executes each test method once
        this.delegate = new ExternalResource() {
            @Override
            protected void before() {
                OsgiContext.this.setUp();
                OsgiContext.this.executeSetUpCallback();
            }

            @Override
            protected void after() {
                OsgiContext.this.executeTearDownCallback();
                OsgiContext.this.tearDown();
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
