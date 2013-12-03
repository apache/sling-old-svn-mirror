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
import org.apache.sling.replication.rule.ReplicationRule;

/**
 * a rule for triggering a specific agent upon node / properties being changed under a certain path
 */
public class TriggerPathReplicationRule implements ReplicationRule {

    private final String path;

    public TriggerPathReplicationRule(String path) {
        this.path = path;
    }

    public String getSignature() {
        return "trigger on path: ${path}";
    }

    public void apply(ReplicationAgent agent) {
        // TODO : register an event handler on path which triggers the agent on node / property changes

    }

    public void unapply(ReplicationAgent agent) {
        // TODO : unregister the event handler for the given agent on that path
    }
}
