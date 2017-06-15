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

import org.apache.sling.testing.mock.osgi.context.ContextCallback;
import org.apache.sling.testing.mock.osgi.context.ContextPlugins;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.osgi.annotation.versioning.ProviderType;

/**
 * JUnit rule for setting up and tearing down OSGi context for unit tests.
 */
@ProviderType
public final class OsgiContext extends OsgiContextImpl implements TestRule {

    private final ContextPlugins plugins;
    private final TestRule delegate;

    /**
     * Initialize OSGi context.
     */
    public OsgiContext() {
        this(new ContextPlugins());
    }

    /**
     * Initialize OSGi context.
     * @param <T> context type
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     */
    public <T extends OsgiContextImpl> OsgiContext(final ContextCallback<T> afterSetUpCallback) {
        this(new ContextPlugins(afterSetUpCallback));
    }

    /**
     * Initialize OSGi context.
     * @param <U> context type
     * @param <V> context type
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     */
    public <U extends OsgiContextImpl, V extends OsgiContextImpl> OsgiContext(final ContextCallback<U> afterSetUpCallback, final ContextCallback<V> beforeTearDownCallback) {
        this(new ContextPlugins(afterSetUpCallback, beforeTearDownCallback));
    }

    /**
     * Initialize OSGi context with resource resolver type.
     * @param contextPlugins Context plugins
     */
    OsgiContext(final ContextPlugins contextPlugins) {
        this.plugins = contextPlugins;

        // wrap {@link ExternalResource} rule executes each test method once
        this.delegate = new ExternalResource() {
            @Override
            protected void before() {
                plugins.executeBeforeSetUpCallback(OsgiContext.this);
                OsgiContext.this.setUp();
                plugins.executeAfterSetUpCallback(OsgiContext.this);
            }

            @Override
            protected void after() {
                plugins.executeBeforeTearDownCallback(OsgiContext.this);
                OsgiContext.this.tearDown();
                plugins.executeAfterTearDownCallback(OsgiContext.this);
            }
        };
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return this.delegate.apply(base, description);
    }

}
