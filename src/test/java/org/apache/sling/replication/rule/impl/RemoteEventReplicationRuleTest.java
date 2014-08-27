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
package org.apache.sling.replication.rule.impl;

import java.lang.reflect.Field;

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.replication.rule.ReplicationRequestHandler;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.rule.impl.RemoteEventReplicationRule}
 */
public class RemoteEventReplicationRuleTest {

    @Test
    public void testSignatureNotNull() throws Exception {
        RemoteEventReplicationRule remoteEventReplicationRule = new RemoteEventReplicationRule();
        assertNotNull(remoteEventReplicationRule.getSignature());
    }

    @Test
    public void testSignatureMatching() throws Exception {
        RemoteEventReplicationRule remoteEventReplicationRule = new RemoteEventReplicationRule();
        assertFalse(remoteEventReplicationRule.signatureMatches(""));
        assertTrue(remoteEventReplicationRule.signatureMatches("remote trigger on localhost with user foo and password bar"));
        assertTrue(remoteEventReplicationRule.signatureMatches("remote trigger on http://localhost with user foo and password bar"));
        assertTrue(remoteEventReplicationRule.signatureMatches("remote trigger on localhost with user foo-bar and password bar-foo"));
    }

    @Test
    public void testApplyWithNonMatchingString() throws Exception {
        RemoteEventReplicationRule remoteEventReplicationRule = new RemoteEventReplicationRule();
        String ruleString = "";
        ReplicationRequestHandler requestHandler = mock(ReplicationRequestHandler.class);
        remoteEventReplicationRule.apply("", requestHandler, ruleString);
    }

    @Test
    public void testApplyWithMatchingString() throws Exception {
        RemoteEventReplicationRule remoteEventReplicationRule = new RemoteEventReplicationRule();
        Field schedulerField = remoteEventReplicationRule.getClass().getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        Scheduler scheduler = mock(Scheduler.class);
        ScheduleOptions options = mock(ScheduleOptions.class);
        when(scheduler.NOW()).thenReturn(options);
        when(scheduler.NOW(any(Integer.class), any(Integer.class))).thenReturn(options);
        schedulerField.set(remoteEventReplicationRule, scheduler);

        String ruleString = "remote trigger on localhost with user foo and password bar";
        ReplicationRequestHandler requestHandler = mock(ReplicationRequestHandler.class);
        remoteEventReplicationRule.apply("", requestHandler, ruleString);
    }

    @Test
    public void testUndoWithNonMatchingString() throws Exception {
        RemoteEventReplicationRule remoteEventReplicationRule = new RemoteEventReplicationRule();
        String ruleString = "";
        ReplicationRequestHandler requestHandler = mock(ReplicationRequestHandler.class);
        remoteEventReplicationRule.apply("", requestHandler, ruleString);
    }

    @Test
    public void testUndoWithMatchingString() throws Exception {
        RemoteEventReplicationRule remoteEventReplicationRule = new RemoteEventReplicationRule();
        remoteEventReplicationRule.activate();
        remoteEventReplicationRule.undo("");
    }

}
