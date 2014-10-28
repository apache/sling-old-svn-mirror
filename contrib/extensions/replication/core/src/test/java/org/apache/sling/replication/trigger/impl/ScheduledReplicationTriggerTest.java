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

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.trigger.ReplicationRequestHandler;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.trigger.impl.ScheduledReplicationTrigger}
 */
public class ScheduledReplicationTriggerTest {

    @Test
    public void testRegister() throws Exception {
        for (ReplicationActionType action : ReplicationActionType.values()) {
            String path = "/path/to/somewhere";
            int interval = 10;
            ReplicationRequestHandler handler = mock(ReplicationRequestHandler.class);
            Scheduler scheduler = mock(Scheduler.class);
            ScheduleOptions options = mock(ScheduleOptions.class);
            when(scheduler.NOW(-1, interval)).thenReturn(options);
            when(options.name(handler.toString())).thenReturn(options);
            ScheduledReplicationTrigger scheduledReplicationTrigger = new ScheduledReplicationTrigger(action.name(), path, interval, scheduler);
            scheduledReplicationTrigger.register(handler);
        }
    }

    @Test
    public void testUnregister() throws Exception {
        for (ReplicationActionType action : ReplicationActionType.values()) {
            String path = "/path/to/somewhere";
            int interval = 10;
            Scheduler scheduler = mock(Scheduler.class);
            ScheduledReplicationTrigger scheduledReplicationTrigger = new ScheduledReplicationTrigger(action.name(), path, interval, scheduler);
            ReplicationRequestHandler handlerId = mock(ReplicationRequestHandler.class);
            scheduledReplicationTrigger.unregister(handlerId);
        }
    }
}