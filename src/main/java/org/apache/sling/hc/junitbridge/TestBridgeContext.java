/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.junitbridge;

import org.apache.sling.hc.util.HealthCheckFilter;
import org.osgi.framework.BundleContext;

class TestBridgeContext {
    private final String [] tags;
    private final HealthCheckFilter filter;
    private final BundleContext bundleContext;
        
    TestBridgeContext(BundleContext bundleContext, String [] tags) {
        this.bundleContext = bundleContext;
        this.tags = tags;
        this.filter = new HealthCheckFilter(bundleContext);
    }
    
    String [] getTags() {
        return tags;
    }
    
    HealthCheckFilter getFilter() {
        return filter;
    }
    
    BundleContext getBundleContext() {
        return bundleContext;
    }
}