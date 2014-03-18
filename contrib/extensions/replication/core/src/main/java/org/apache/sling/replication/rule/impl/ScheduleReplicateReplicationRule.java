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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.replication.agent.AgentReplicationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.rule.ReplicationRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link org.apache.sling.replication.rule.ReplicationRule} to schedule replications on a certain {@link org.apache.sling.replication.agent.ReplicationAgent}
 */
@Component(immediate = true, label = "Rule for Scheduled Replications")
@Service(value = ReplicationRule.class)
public class ScheduleReplicateReplicationRule implements ReplicationRule {
    private static final String SIGNATURE = "scheduled {action} [on ${path} ]every {seconds} sec";

    private static final String SIGNATURE_REGEX = "(scheduled)\\s(add|delete|poll)+\\s((on)\\s(\\/\\w+)+\\s)?(every)\\s(\\d+)\\s(sec)";

    private static final Pattern signaturePattern = Pattern.compile(SIGNATURE_REGEX);

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private Scheduler scheduler;

    public String getSignature() {
        return SIGNATURE;
    }

    public boolean signatureMatches(String ruleString) {
        return ruleString.matches(SIGNATURE_REGEX);
    }

    public void apply(String ruleString, ReplicationAgent agent) {
        if (signatureMatches(ruleString)) {
            Matcher matcher = signaturePattern.matcher(ruleString);
            if (matcher.find()) {
                String action = matcher.group(2);
                ReplicationActionType actionType = ReplicationActionType.fromName(action.toUpperCase());
                String path = matcher.group(5); // can be null
                int seconds = Integer.parseInt(matcher.group(7));
                ScheduleOptions options = scheduler.NOW(-1, seconds);
                options.name(agent.getName() + " " + ruleString);
                scheduler.schedule(new ScheduledReplication(agent, actionType, path), options);
            }
        }
    }

    public void undo(String ruleString, ReplicationAgent agent) {
        if (signatureMatches(ruleString)) {
            scheduler.unschedule(agent.getName() + " " + ruleString);
        }
    }

    private class ScheduledReplication implements Runnable {
        private final ReplicationAgent agent;
        private final ReplicationActionType action;
        private final String path;

        public ScheduledReplication(ReplicationAgent agent, ReplicationActionType action, String path) {
            this.agent = agent;
            this.action = action;
            this.path = path != null ? path : "/";
        }

        public void run() {
            try {
                agent.send(new ReplicationRequest(System.currentTimeMillis(), action, path));
            } catch (AgentReplicationException e) {
                if (log.isErrorEnabled()) {
                    log.error("failed scheduled replication {} on agent {} for path {}", new Object[]{
                            action.name(), agent.getName(), path}, e);
                }
            }
        }
    }
}
