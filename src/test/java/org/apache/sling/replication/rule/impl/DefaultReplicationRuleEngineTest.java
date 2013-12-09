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

import java.util.Collections;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.rule.ReplicationRule;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link DefaultReplicationRuleEngine}
 */
public class DefaultReplicationRuleEngineTest {

    @Test
    public void testApplyWithNoRules() throws Exception {
        DefaultReplicationRuleEngine defaultReplicationRuleEngine = new DefaultReplicationRuleEngine();
        String ruleString = "do something sometimes";
        ReplicationAgent agent = mock(ReplicationAgent.class);
        defaultReplicationRuleEngine.applyRules(agent, ruleString);
    }

    @Test
    public void testApplyWithNoMatchingRules() throws Exception {
        DefaultReplicationRuleEngine defaultReplicationRuleEngine = new DefaultReplicationRuleEngine();
        String ruleString = "do something sometimes";
        ReplicationRule rule = mock(ReplicationRule.class);
        when(rule.signatureMatches(ruleString)).thenReturn(false);
        defaultReplicationRuleEngine.bindReplicationRule(rule, Collections.<String,Object>emptyMap());
        ReplicationAgent agent = mock(ReplicationAgent.class);
        defaultReplicationRuleEngine.applyRules(agent, ruleString);
    }

    @Test
    public void testApplyWithMatchingRules() throws Exception {
        DefaultReplicationRuleEngine defaultReplicationRuleEngine = new DefaultReplicationRuleEngine();
        String ruleString = "do something sometimes";
        ReplicationRule rule = mock(ReplicationRule.class);
        when(rule.signatureMatches(ruleString)).thenReturn(true);
        defaultReplicationRuleEngine.bindReplicationRule(rule, Collections.<String,Object>emptyMap());
        ReplicationAgent agent = mock(ReplicationAgent.class);
        defaultReplicationRuleEngine.applyRules(agent, ruleString);
    }
}
