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

import org.apache.sling.testing.mock.osgi.context.ContextPlugins;
import org.apache.sling.testing.mock.osgi.context.OsgiContextImpl;
import org.apache.sling.testing.mock.osgi.context.ContextCallback;
import org.apache.sling.testing.mock.osgi.context.ContextPlugin;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Builder class for creating {@link OsgiContext} instances with different sets of parameters.
 */
@ProviderType
public final class OsgiContextBuilder {
    
    private final ContextPlugins plugins = new ContextPlugins();
    
    /**
     * Create builder with default resource resolver type.
     */
    public OsgiContextBuilder() {}
    
    /**
     * @param <T> context type
     * @param plugin Context plugin which listens to context lifecycle events.
     * @return this
     */
    @SafeVarargs
    public final <T extends OsgiContextImpl> OsgiContextBuilder plugin(ContextPlugin<T>... plugin) {
        plugins.addPlugin(plugin);
        return this;
    }

    /**
     * @param <T> context type
     * @param beforeSetUpCallback Allows the application to register an own callback function that is called before the built-in setup rules are executed.
     * @return this
     */
    @SafeVarargs
    public final <T extends OsgiContextImpl> OsgiContextBuilder beforeSetUp(ContextCallback<T>... beforeSetUpCallback) {
        plugins.addBeforeSetUpCallback(beforeSetUpCallback);
        return this;
    }

    /**
     * @param <T> context type
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @return this
     */
    @SafeVarargs
    public final <T extends OsgiContextImpl> OsgiContextBuilder afterSetUp(ContextCallback<T>... afterSetUpCallback) {
        plugins.addAfterSetUpCallback(afterSetUpCallback);
        return this;
    }

    /**
     * @param <T> context type
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     * @return this
     */
    @SafeVarargs
    public final <T extends OsgiContextImpl> OsgiContextBuilder beforeTearDown(ContextCallback<T>... beforeTearDownCallback) {
        plugins.addBeforeTearDownCallback(beforeTearDownCallback);
        return this;
    }

    /**
     * @param <T> context type
     * @param afterTearDownCallback Allows the application to register an own callback function that is after before the built-in teardown rules are executed.
     * @return this
     */
    @SafeVarargs
    public final <T extends OsgiContextImpl> OsgiContextBuilder afterTearDown(ContextCallback<T>... afterTearDownCallback) {
        plugins.addAfterTearDownCallback(afterTearDownCallback);
        return this;
    }

    /**
     * @return Build {@link OsgiContext} instance.
     */
    public OsgiContext build() {
        return new OsgiContext(plugins);
    }
    
}
