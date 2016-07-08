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

/**
 * Builder class for creating {@link SlingContext} instances with different sets of parameters.
 */
public final class SlingContextBuilder {
    
    private ResourceResolverType resourceResolverType;
    private SlingContextCallback beforeSetUpCallback;
    private SlingContextCallback afterSetUpCallback;
    private SlingContextCallback beforeTearDownCallback;
    private SlingContextCallback afterTearDownCallback;
    private Map<String, Object> resourceResolverFactoryActivatorProps;
    
    /**
     * Create builder with default resource resolver type.
     */
    public SlingContextBuilder() {}
    
    /**
     * Create builder with given resource resolver type.
     * @param resourceResolverType Resource resolver type.
     */
    public SlingContextBuilder(ResourceResolverType resourceResolverType) {
        this.resourceResolverType(resourceResolverType);
    }
    
    /**
     * @param resourceResolverType Resource resolver type.
     * @return this
     */
    public SlingContextBuilder resourceResolverType(ResourceResolverType resourceResolverType) {
        this.resourceResolverType = resourceResolverType;
        return this;
    }
    
    /**
     * @param beforeSetUpCallback Allows the application to register an own callback function that is called before the built-in setup rules are executed.
     * @return this
     */
    public SlingContextBuilder beforeSetUp(SlingContextCallback beforeSetUpCallback) {
        this.beforeSetUpCallback = beforeSetUpCallback;
        return this;
    }

    /**
     * @param afterSetUpCallback Allows the application to register an own callback function that is called after the built-in setup rules are executed.
     * @return this
     */
    public SlingContextBuilder afterSetUp(SlingContextCallback afterSetUpCallback) {
        this.afterSetUpCallback = afterSetUpCallback;
        return this;
    }

    /**
     * @param beforeTearDownCallback Allows the application to register an own callback function that is called before the built-in teardown rules are executed.
     * @return this
     */
    public SlingContextBuilder beforeTearDown(SlingContextCallback beforeTearDownCallback) {
        this.beforeTearDownCallback = beforeTearDownCallback;
        return this;
    }

    /**
     * @param afterTearDownCallback Allows the application to register an own callback function that is after before the built-in teardown rules are executed.
     * @return this
     */
    public SlingContextBuilder afterTearDown(SlingContextCallback afterTearDownCallback) {
        this.afterTearDownCallback = afterTearDownCallback;
        return this;
    }

    /**
     * Allows to override OSGi configuration parameters for the Resource Resolver Factory Activator service.
     * @param props Configuration properties
     * @return this
     */
    public SlingContextBuilder resourceResolverFactoryActivatorProps(Map<String, Object> props) {
      this.resourceResolverFactoryActivatorProps = props;
      return this;
    }

    /**
     * @return Build {@link SlingContext} instance.
     */
    public SlingContext build() {
        return new SlingContext(this.beforeSetUpCallback, this.afterSetUpCallback,
                this.beforeTearDownCallback, this.afterTearDownCallback,
                this.resourceResolverFactoryActivatorProps,
                this.resourceResolverType);
    }
    
}
