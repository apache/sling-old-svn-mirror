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
package org.apache.sling.replication.trigger.impl;

import org.apache.sling.replication.trigger.ReplicationTriggerRequestHandler;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.mockito.Mockito.mock;

/**
 * Testcase for {@link org.apache.sling.replication.trigger.impl.ResourceEventReplicationTrigger}
 */
public class ResourceEventReplicationTriggerTest {

    @Test
    public void testRegister() throws Exception {
        String path = "/some/path";
        BundleContext bundleContext = mock(BundleContext.class);
        ResourceEventReplicationTrigger resourceEventReplicationTrigger = new ResourceEventReplicationTrigger(path, bundleContext);
        String handlerId = "handlder-id-123";
        ReplicationTriggerRequestHandler handler = mock(ReplicationTriggerRequestHandler.class);
        resourceEventReplicationTrigger.register(handlerId, handler);
    }

    @Test
    public void testUnregister() throws Exception {
        String path = "/some/path";
        BundleContext bundleContext = mock(BundleContext.class);
        ResourceEventReplicationTrigger resourceEventReplicationTrigger = new ResourceEventReplicationTrigger(path, bundleContext);
        String handlerId = "handlder-id-123";
        resourceEventReplicationTrigger.unregister(handlerId);
    }

    @Test
    public void testEnable() throws Exception {
        String path = "/some/path";
        BundleContext bundleContext = mock(BundleContext.class);
        ResourceEventReplicationTrigger resourceEventReplicationTrigger = new ResourceEventReplicationTrigger(path, bundleContext);
        resourceEventReplicationTrigger.enable();
    }

    @Test
    public void testDisable() throws Exception {
        String path = "/some/path";
        BundleContext bundleContext = mock(BundleContext.class);
        ResourceEventReplicationTrigger resourceEventReplicationTrigger = new ResourceEventReplicationTrigger(path, bundleContext);
        resourceEventReplicationTrigger.disable();
    }
}