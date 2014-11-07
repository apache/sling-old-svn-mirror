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
package org.apache.sling.distribution.trigger.impl;

import java.util.Dictionary;

import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link ResourceEventDistributionTrigger}
 */
public class ResourceEventDistributionTriggerTest {

    @Test
    public void testRegister() throws Exception {
        String path = "/some/path";
        BundleContext bundleContext = mock(BundleContext.class);
        ServiceRegistration registration = mock(ServiceRegistration.class);
        when(bundleContext.registerService(any(String.class), any(Object.class), any(Dictionary.class))).thenReturn(registration);
        ResourceEventDistributionTrigger resourceEventdistributionTrigger = new ResourceEventDistributionTrigger(path, bundleContext);
        DistributionRequestHandler handler = mock(DistributionRequestHandler.class);
        resourceEventdistributionTrigger.register(handler);
    }

    @Test
    public void testUnregister() throws Exception {
        String path = "/some/path";
        BundleContext bundleContext = mock(BundleContext.class);
        ResourceEventDistributionTrigger resourceEventdistributionTrigger = new ResourceEventDistributionTrigger(path, bundleContext);
        DistributionRequestHandler handler = mock(DistributionRequestHandler.class);
        resourceEventdistributionTrigger.unregister(handler);
    }

    @Test
    public void testEnable() throws Exception {
        String path = "/some/path";
        BundleContext bundleContext = mock(BundleContext.class);
        ResourceEventDistributionTrigger resourceEventdistributionTrigger = new ResourceEventDistributionTrigger(path, bundleContext);
        resourceEventdistributionTrigger.enable();
    }

    @Test
    public void testDisable() throws Exception {
        String path = "/some/path";
        BundleContext bundleContext = mock(BundleContext.class);
        ResourceEventDistributionTrigger resourceEventdistributionTrigger = new ResourceEventDistributionTrigger(path, bundleContext);
        resourceEventdistributionTrigger.disable();
    }
}