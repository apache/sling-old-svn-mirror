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

import org.apache.sling.replication.agent.ReplicationAgent;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Testcase for {@link ChainReplicateReplicationRule}
 */
public class ChainReplicateReplicationRuleTest {

    @Test
    public void testSignatureNotNull() throws Exception {
        ChainReplicateReplicationRule chainPathReplicationRule = new ChainReplicateReplicationRule();
        assertNotNull(chainPathReplicationRule.getSignature());
    }

    @Test
    public void testSignatureMatching() throws Exception {
        ChainReplicateReplicationRule chainPathReplicationRule = new ChainReplicateReplicationRule();
        assertFalse(chainPathReplicationRule.signatureMatches(""));
        assertTrue(chainPathReplicationRule.signatureMatches("chain on path: /apps"));
        assertTrue(chainPathReplicationRule.signatureMatches("chain on path: /apps/sling"));
        assertFalse(chainPathReplicationRule.signatureMatches("chain on path: /"));
        assertFalse(chainPathReplicationRule.signatureMatches("chain on path: foo bar"));
    }

    @Test
    public void testApplyWithNonMatchingString() throws Exception {
        ChainReplicateReplicationRule chainPathReplicationRule = new ChainReplicateReplicationRule();
        String ruleString = "";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        chainPathReplicationRule.apply(ruleString, replicationAgent);
    }

    @Test
    public void testApplyWithMatchingString() throws Exception {
        ChainReplicateReplicationRule chainPathReplicationRule = new ChainReplicateReplicationRule();
        String ruleString = "chain on path: /foo/bar";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        chainPathReplicationRule.apply(ruleString, replicationAgent);
    }

    @Test
    public void testApplyWithMatchingStringAndRegisteredContext() throws Exception {
        ChainReplicateReplicationRule chainPathReplicationRule = new ChainReplicateReplicationRule();
        String ruleString = "chain on path: /foo/bar";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        BundleContext context = mock(BundleContext.class);
        chainPathReplicationRule.activate(context);
        chainPathReplicationRule.apply(ruleString, replicationAgent);
    }

    @Test
    public void testApplyWithNonMatchingStringAndRegisteredContext() throws Exception {
        ChainReplicateReplicationRule chainPathReplicationRule = new ChainReplicateReplicationRule();
        String ruleString = "chain on path: 1 2 3";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        BundleContext context = mock(BundleContext.class);
        chainPathReplicationRule.activate(context);
        chainPathReplicationRule.apply(ruleString, replicationAgent);
    }

    @Test
    public void testUndoWithNonMatchingString() throws Exception {
        ChainReplicateReplicationRule chainPathReplicationRule = new ChainReplicateReplicationRule();
        String ruleString = "";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        chainPathReplicationRule.undo(ruleString, replicationAgent);
    }

    @Test
    public void testUndoWithMatchingString() throws Exception {
        ChainReplicateReplicationRule chainPathReplicationRule = new ChainReplicateReplicationRule();
        String ruleString = "chain on path: /foo/bar";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        chainPathReplicationRule.undo(ruleString, replicationAgent);
    }

    @Test
    public void testUndoWithMatchingStringAndRegisteredContext() throws Exception {
        ChainReplicateReplicationRule chainPathReplicationRule = new ChainReplicateReplicationRule();
        String ruleString = "chain on path: /foo/bar";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        BundleContext context = mock(BundleContext.class);
        chainPathReplicationRule.activate(context);
        chainPathReplicationRule.undo(ruleString, replicationAgent);
    }

    @Test
    public void testUndoWithNonMatchingStringAndRegisteredContext() throws Exception {
        ChainReplicateReplicationRule chainPathReplicationRule = new ChainReplicateReplicationRule();
        String ruleString = "chain on path: 1 2 3";
        ReplicationAgent replicationAgent = mock(ReplicationAgent.class);
        BundleContext context = mock(BundleContext.class);
        chainPathReplicationRule.activate(context);
        chainPathReplicationRule.undo(ruleString, replicationAgent);
    }
}
