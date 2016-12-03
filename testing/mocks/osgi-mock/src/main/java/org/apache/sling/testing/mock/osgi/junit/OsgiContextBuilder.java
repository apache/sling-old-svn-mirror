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

/**
 * Builder class for creating {@link OsgiContext} instances with different sets of parameters.
 */
public final class OsgiContextBuilder {
    
    private final CallbackParams callbackParams = new CallbackParams();
    
    /**
     * Create builder with default resource resolver type.
     */
    public OsgiContextBuilder() {}
    
    /**
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @return this
     */
    public OsgiContextBuilder setUp(ContextCallback... afterSetUpCallback) {
        return afterSetUp(afterSetUpCallback);
    }

    /**
     * @param beforeSetUpCallback Allows the application to register an own callback function that is called before the built-in setup rules are executed.
     * @return this
     */
    public OsgiContextBuilder beforeSetUp(ContextCallback... beforeSetUpCallback) {
        callbackParams.beforeSetUpCallback = beforeSetUpCallback;
        return this;
    }

    /**
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @return this
     */
    public OsgiContextBuilder afterSetUp(ContextCallback... afterSetUpCallback) {
        callbackParams.afterSetUpCallback = afterSetUpCallback;
        return this;
    }

    /**
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     * @return this
     */
    public OsgiContextBuilder tearDown(ContextCallback... beforeTearDownCallback) {
        return beforeTearDown(beforeTearDownCallback);
    }

    /**
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     * @return this
     */
    public OsgiContextBuilder beforeTearDown(ContextCallback... beforeTearDownCallback) {
        callbackParams.beforeTearDownCallback = beforeTearDownCallback;
        return this;
    }

    /**
     * @param afterTearDownCallback Allows the application to register an own callback function that is after before the built-in teardown rules are executed.
     * @return this
     */
    public OsgiContextBuilder afterTearDown(ContextCallback... afterTearDownCallback) {
        callbackParams.afterTearDownCallback = afterTearDownCallback;
        return this;
    }

    /**
     * @return Build {@link OsgiContext} instance.
     */
    public OsgiContext build() {
        return new OsgiContext(callbackParams);
    }
    
}
