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
package org.apache.sling.testing.mock.sling;

import org.osgi.framework.BundleContext;

/**
 * Mock {@link ResourceResolverFactory} implementation.
 * Uses real Sling ResourceResolverFactory in simulated OSGi environment.
 * Resource Resolver factory has no ResourceProvider registered; you have to registere one yourself to do anything useful with it. 
 */
class MockNoneResourceResolverFactory extends AbstractMockResourceResolverFactory {

    public MockNoneResourceResolverFactory(BundleContext bundleContext) {
        super(bundleContext);
    }
}
