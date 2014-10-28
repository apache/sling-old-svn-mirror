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

import org.apache.http.client.CredentialsProvider;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.replication.transport.authentication.TransportAuthenticationProvider;
import org.apache.sling.replication.trigger.ReplicationRequestHandler;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.trigger.impl.RemoteEventReplicationTrigger}
 */
public class RemoteEventReplicationTriggerTest {

    @Test
    public void testRegister() throws Exception {
        ReplicationRequestHandler handler = mock(ReplicationRequestHandler.class);
        String endpoint = "";
        TransportAuthenticationProvider<CredentialsProvider, CredentialsProvider> authProvider = mock(TransportAuthenticationProvider.class);
        when(authProvider.canAuthenticate(CredentialsProvider.class)).thenReturn(true);
        Scheduler scheduler = mock(Scheduler.class);
        ScheduleOptions options = mock(ScheduleOptions.class);
        when(options.name(handler.toString())).thenReturn(options);
        when(scheduler.NOW()).thenReturn(options);
        RemoteEventReplicationTrigger remoteEventReplicationTrigger = new RemoteEventReplicationTrigger(
                endpoint, authProvider, scheduler);
        remoteEventReplicationTrigger.register(handler);
    }

    @Test
    public void testUnregister() throws Exception {
        String endpoint = "";
        String handlerId = "handler-id-1";
        TransportAuthenticationProvider<CredentialsProvider, CredentialsProvider> authProvider = mock(TransportAuthenticationProvider.class);
        when(authProvider.canAuthenticate(CredentialsProvider.class)).thenReturn(true);
        Scheduler scheduler = mock(Scheduler.class);
        RemoteEventReplicationTrigger remoteEventReplicationTrigger = new RemoteEventReplicationTrigger(
                endpoint, authProvider, scheduler);
        ReplicationRequestHandler handler = mock(ReplicationRequestHandler.class);
        remoteEventReplicationTrigger.unregister(handler);
    }
}