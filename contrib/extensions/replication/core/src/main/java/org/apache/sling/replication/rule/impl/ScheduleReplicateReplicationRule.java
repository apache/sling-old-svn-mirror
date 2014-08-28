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
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.replication.agent.AgentReplicationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationRequest;
import org.apache.sling.replication.rule.ReplicationRequestHandler;
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

    @Property(label = "Name", value = "schedule", propertyPrivate = true)
    private static final String NAME = "name";

    @Reference
    private Scheduler scheduler;

    public String getSignature() {
        return SIGNATURE;
    }

    public boolean signatureMatches(String ruleString) {
        return ruleString.matches(SIGNATURE_REGEX);
    }

    public void apply(String handleId, ReplicationRequestHandler agent, String ruleString) {
        if (signatureMatches(ruleString)) {
            Matcher matcher = signaturePattern.matcher(ruleString);
            if (matcher.find()) {
                String action = matcher.group(2);
                ReplicationActionType actionType = ReplicationActionType.fromName(action.toUpperCase());
                String path = matcher.group(5); // can be null
                int seconds = Integer.parseInt(matcher.group(7));
                ScheduleOptions options = scheduler.NOW(-1, seconds);
                options.name(handleId);
                scheduler.schedule(new ScheduledReplication(agent, actionType, path), options);
            }
        }
    }

    public void undo(String handleId) {
        scheduler.unschedule(handleId);
    }

    private class ScheduledReplication implements Runnable {
        private final ReplicationRequestHandler agent;
        private final ReplicationActionType action;
        private final String path;

        public ScheduledReplication(ReplicationRequestHandler agent, ReplicationActionType action, String path) {
            this.agent = agent;
            this.action = action;
            this.path = path != null ? path : "/";
        }

        public void run() {
            log.debug("agent {}: scheduling {} replication of {}", new Object[]{agent, action, path});

            agent.execute(new ReplicationRequest(System.currentTimeMillis(), action, path));
        }
    }
}
