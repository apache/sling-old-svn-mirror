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
import org.apache.sling.replication.agent.ReplicationAgent;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.rule.impl.ScheduleReplicateReplicationRule}
 */
public class ScheduleReplicateReplicationRuleTest {

    @Test
    public void testSignatureNotNull() throws Exception {
        ScheduleReplicateReplicationRule scheduleReplicateReplicationRule = new ScheduleReplicateReplicationRule();
        assertNotNull(scheduleReplicateReplicationRule.getSignature());
    }

    @Test
    public void testSignatureMatching() throws Exception {
        ScheduleReplicateReplicationRule scheduleReplicateReplicationRule = new ScheduleReplicateReplicationRule();
        assertFalse(scheduleReplicateReplicationRule.signatureMatches(""));
        assertTrue(scheduleReplicateReplicationRule.signatureMatches("scheduled poll on /apps every 1 sec"));
        assertTrue(scheduleReplicateReplicationRule.signatureMatches("scheduled add on /system every 12 sec"));
        assertTrue(scheduleReplicateReplicationRule.signatureMatches("scheduled delete on /libs every 331 sec"));
        assertTrue(scheduleReplicateReplicationRule.signatureMatches("scheduled poll every 30 sec"));
    }

    @Test
    public void testApplyWithNonMatchingString() throws Exception {
        ScheduleReplicateReplicationRule scheduleReplicateReplicationRule = new ScheduleReplicateReplicationRule();
        String ruleString = "";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        scheduleReplicateReplicationRule.apply(ruleString, replicationAgent);
    }

    @Test
    public void testApplyWithMatchingString() throws Exception {
        ScheduleReplicateReplicationRule scheduleReplicateReplicationRule = new ScheduleReplicateReplicationRule();
        Field schedulerField = scheduleReplicateReplicationRule.getClass().getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        Scheduler scheduler = mock(Scheduler.class);
        ScheduleOptions options = mock(ScheduleOptions.class);
        when(scheduler.NOW(any(Integer.class), any(Integer.class))).thenReturn(options);
        schedulerField.set(scheduleReplicateReplicationRule, scheduler);

        String ruleString = "scheduled add on /system every 12 sec";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        scheduleReplicateReplicationRule.apply(ruleString, replicationAgent);
    }

    @Test
    public void testApplyWithMatchingStringAndRegisteredContext() throws Exception {
        ScheduleReplicateReplicationRule scheduleReplicateReplicationRule = new ScheduleReplicateReplicationRule();
        Field schedulerField = scheduleReplicateReplicationRule.getClass().getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        Scheduler scheduler = mock(Scheduler.class);
        ScheduleOptions options = mock(ScheduleOptions.class);
        when(scheduler.NOW(any(Integer.class), any(Integer.class))).thenReturn(options);
        schedulerField.set(scheduleReplicateReplicationRule, scheduler);

        String ruleString = "scheduled add on /system every 12 sec";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        scheduleReplicateReplicationRule.apply(ruleString, replicationAgent);
    }

    @Test
    public void testApplyWithNonMatchingStringAndRegisteredContext() throws Exception {
        ScheduleReplicateReplicationRule scheduleReplicateReplicationRule = new ScheduleReplicateReplicationRule();
        Field schedulerField = scheduleReplicateReplicationRule.getClass().getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        Scheduler scheduler = mock(Scheduler.class);
        ScheduleOptions options = mock(ScheduleOptions.class);
        when(scheduler.NOW(any(Integer.class), any(Integer.class))).thenReturn(options);
        schedulerField.set(scheduleReplicateReplicationRule, scheduler);

        String ruleString = "scheduled add on /system every 12 sec";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        scheduleReplicateReplicationRule.apply(ruleString, replicationAgent);
    }

    @Test
    public void testUndoWithNonMatchingString() throws Exception {
        ScheduleReplicateReplicationRule scheduleReplicateReplicationRule = new ScheduleReplicateReplicationRule();
        Field schedulerField = scheduleReplicateReplicationRule.getClass().getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        Scheduler scheduler = mock(Scheduler.class);
        ScheduleOptions options = mock(ScheduleOptions.class);
        when(scheduler.NOW(any(Integer.class), any(Integer.class))).thenReturn(options);
        schedulerField.set(scheduleReplicateReplicationRule, scheduler);

        String ruleString = "";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        scheduleReplicateReplicationRule.undo(ruleString, replicationAgent);
    }

    @Test
    public void testUndoWithMatchingString() throws Exception {
        ScheduleReplicateReplicationRule scheduleReplicateReplicationRule = new ScheduleReplicateReplicationRule();
        Field schedulerField = scheduleReplicateReplicationRule.getClass().getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        Scheduler scheduler = mock(Scheduler.class);
        ScheduleOptions options = mock(ScheduleOptions.class);
        when(scheduler.NOW(any(Integer.class), any(Integer.class))).thenReturn(options);
        schedulerField.set(scheduleReplicateReplicationRule, scheduler);

        String ruleString = "scheduled add on /system every 12 sec";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        scheduleReplicateReplicationRule.undo(ruleString, replicationAgent);
    }

    @Test
    public void testUndoWithMatchingStringAndRegisteredContext() throws Exception {
        ScheduleReplicateReplicationRule scheduleReplicateReplicationRule = new ScheduleReplicateReplicationRule();
        Field schedulerField = scheduleReplicateReplicationRule.getClass().getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        Scheduler scheduler = mock(Scheduler.class);
        ScheduleOptions options = mock(ScheduleOptions.class);
        when(scheduler.NOW(any(Integer.class), any(Integer.class))).thenReturn(options);
        schedulerField.set(scheduleReplicateReplicationRule, scheduler);

        String ruleString = "scheduled add on /system every 12 sec";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        scheduleReplicateReplicationRule.undo(ruleString, replicationAgent);
    }

    @Test
    public void testUndoWithNonMatchingStringAndRegisteredContext() throws Exception {
        ScheduleReplicateReplicationRule scheduleReplicateReplicationRule = new ScheduleReplicateReplicationRule();
        Field schedulerField = scheduleReplicateReplicationRule.getClass().getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        Scheduler scheduler = mock(Scheduler.class);
        ScheduleOptions options = mock(ScheduleOptions.class);
        when(scheduler.NOW(any(Integer.class), any(Integer.class))).thenReturn(options);
        schedulerField.set(scheduleReplicateReplicationRule, scheduler);

        String ruleString = "scheduled add on /system every 12 sec";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        scheduleReplicateReplicationRule.undo(ruleString, replicationAgent);
    }
}
