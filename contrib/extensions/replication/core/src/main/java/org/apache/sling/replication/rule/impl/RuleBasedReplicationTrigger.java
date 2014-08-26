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

import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.queue.ReplicationQueueDistributionStrategy;
import org.apache.sling.replication.rule.ReplicationRequestHandler;
import org.apache.sling.replication.rule.ReplicationRule;
import org.apache.sling.replication.rule.ReplicationRuleEngine;
import org.apache.sling.replication.rule.ReplicationTrigger;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

/**
 * Trigger based on rules
 */
@Component(immediate = true, label = "Rule for Chain Replication", configurationFactory = true)
@Service(value = ReplicationTrigger.class)
public class RuleBasedReplicationTrigger implements ReplicationTrigger {

    @Property(label = "Name")
    private static final String NAME = "name";

    @Property(label = "Rule String")
    private static final String RULE_STRING = "ruleString";

    @Property(label = "Target ReplicationRule", name = "ReplicationRule.target")
    @Reference(name = "ReplicationRule", policy = ReferencePolicy.DYNAMIC)
    private ReplicationRule replicationRule;


    String ruleString;

    @Activate
    public void activate(BundleContext context, Map<String, ?> config) throws Exception {
        String name = PropertiesUtil
                .toString(config.get(NAME), String.valueOf(new Random().nextInt(1000)));

        ruleString = PropertiesUtil.toString(config.get(RULE_STRING), null);
    }


    public void register(String handlerId, ReplicationRequestHandler requestHandler) {
        replicationRule.apply(handlerId, requestHandler, ruleString);
    }

    public void unregister(String handlerId) {
        replicationRule.undo(handlerId);
    }
}
