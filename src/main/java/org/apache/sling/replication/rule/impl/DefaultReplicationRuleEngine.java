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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.rule.ReplicationRule;
import org.apache.sling.replication.rule.ReplicationRuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * default implementation of {@link ReplicationRuleEngine}
 */
@Component
@References({
        @Reference(name = "replicationRule",
                referenceInterface = ReplicationRule.class,
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
                policy = ReferencePolicy.DYNAMIC,
                bind = "bindReplicationRule",
                unbind = "unbindReplicationRule")
})
@Service(value = ReplicationRuleEngine.class)
public class DefaultReplicationRuleEngine implements ReplicationRuleEngine {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Collection<ReplicationRule> replicationRules = new LinkedList<ReplicationRule>();

    @Deactivate
    protected void deactivate() {
        replicationRules.clear();
    }

    protected void bindReplicationRule(final ReplicationRule replicationRule,
                                       Map<String, Object> properties) {
        synchronized (replicationRules) {
            replicationRules.add(replicationRule);
        }
        log.debug("Registering replication rule {} ", replicationRule);
    }

    protected void unbindReplicationRule(final ReplicationRule replicationRule,
                                         Map<String, Object> properties) {
        synchronized (replicationRules) {
            replicationRules.remove(replicationRule);
        }
        log.debug("Unregistering replication rule {} ", replicationRule);
    }

    public void applyRules(ReplicationAgent agent, String... ruleStrings) {
        for (String ruleString : ruleStrings) {

            for (ReplicationRule rule : replicationRules) {
                if (rule.signatureMatches(ruleString)) {
                    if (log.isInfoEnabled()) {
                        log.info("applying rule {} with string {} to agent {}", new Object[]{rule, ruleString, agent});
                    }
                    rule.apply(ruleString, agent);
                    break;
                }
            }

        }
    }

    public void unapplyRules(ReplicationAgent agent, String... ruleStrings) {
        for (String ruleString : ruleStrings) {

            for (ReplicationRule rule : replicationRules) {
                if (rule.signatureMatches(ruleString)) {
                    if (log.isInfoEnabled()) {
                        log.info("un-applying rule {} with string {} to agent {}", new Object[]{rule, ruleString, agent});
                    }
                    rule.undo(ruleString, agent);
                    break;
                }
            }

        }
    }

}
