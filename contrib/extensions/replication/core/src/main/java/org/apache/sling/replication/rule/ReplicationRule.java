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
package org.apache.sling.replication.rule;

import org.apache.sling.replication.agent.ReplicationAgent;

/**
 * A replication rule
 */
public interface ReplicationRule {

    /**
     * get the signature of this rule
     *
     * @return a <code>String</code> for this rule's signature
     */
    String getSignature();

    /**
     * checks if the given rule <code>String</code> matches this {@link ReplicationRule}'s signature
     *
     * @param ruleString a rule <code>String</code>
     * @return <code>true</code> if the given rule <code>String</code> matches this rule's signature, <code>false</code> otherwise
     */
    boolean signatureMatches(String ruleString);

    /**
     * apply this rule to a replication agent
     *
     * @param ruleString the rule to apply to the agent
     * @param agent      {@link ReplicationAgent agent} the agent to apply the rule to
     */
    void apply(String ruleString, ReplicationAgent agent);

    /**
     * undo the application of this rule to the given agent
     *
     * @param agent the {@link ReplicationAgent agent} on which undoing this rule application
     */
    void undo(String ruleString, ReplicationAgent agent);
}
