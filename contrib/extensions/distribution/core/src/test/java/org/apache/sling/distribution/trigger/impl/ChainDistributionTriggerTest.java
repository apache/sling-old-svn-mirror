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

import org.apache.sling.distribution.trigger.DistributionRequestHandler;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.mockito.Mockito.mock;

/**
 * Testcase for {@link DistributionEventDistributeDistributionTrigger}
 */
public class ChainDistributionTriggerTest {

    @Test
    public void testRegister() throws Exception {
        String pathPrefix = "/prefix";
        BundleContext bundleContext = mock(BundleContext.class);
        DistributionEventDistributeDistributionTrigger chainDistributeDistributionTrigger = new DistributionEventDistributeDistributionTrigger(pathPrefix, bundleContext);
        DistributionRequestHandler handler = mock(DistributionRequestHandler.class);
        chainDistributeDistributionTrigger.register(handler);
    }

    @Test
    public void testUnregister() throws Exception {
        String pathPrefix = "/prefix";
        BundleContext bundleContext = mock(BundleContext.class);
        DistributionEventDistributeDistributionTrigger chainDistributeDistributionTrigger = new DistributionEventDistributeDistributionTrigger(pathPrefix, bundleContext);
        DistributionRequestHandler handler = mock(DistributionRequestHandler.class);
        chainDistributeDistributionTrigger.unregister(handler);
    }


    @Test
    public void testDisable() throws Exception {
        String pathPrefix = "/prefix";
        BundleContext bundleContext = mock(BundleContext.class);
        DistributionEventDistributeDistributionTrigger chainDistributeDistributionTrigger = new DistributionEventDistributeDistributionTrigger(pathPrefix, bundleContext);
        chainDistributeDistributionTrigger.disable();
    }
}