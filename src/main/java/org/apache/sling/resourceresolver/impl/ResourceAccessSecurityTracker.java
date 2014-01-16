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
package org.apache.sling.resourceresolver.impl;

import org.apache.sling.api.security.ResourceAccessSecurity;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This internal helper class keeps track of the resource access security services
 * and always returns the one with the highest service ranking.
 */
public class ResourceAccessSecurityTracker {

    private final ServiceTracker resourceAccessSecurityTracker;

    public ResourceAccessSecurityTracker(final BundleContext bundleContext) {
        resourceAccessSecurityTracker = new ServiceTracker(bundleContext, ResourceAccessSecurity.class.getName(), null);
        resourceAccessSecurityTracker.open();
    }

    public void dispose() {
        this.resourceAccessSecurityTracker.close();
    }

    public ResourceAccessSecurity getApplicationResourceAccessSecurity() {
        return (ResourceAccessSecurity) this.resourceAccessSecurityTracker.getService();
    }

    public ResourceAccessSecurity getProviderResourceAccessSecurity() {
        return (ResourceAccessSecurity) this.resourceAccessSecurityTracker.getService();
    }
}
