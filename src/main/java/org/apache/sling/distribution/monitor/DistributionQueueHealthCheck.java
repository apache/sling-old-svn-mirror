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
package org.apache.sling.distribution.monitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.distribution.agent.DistributionAgent;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueEntry;
import org.apache.sling.distribution.queue.DistributionQueueItem;
import org.apache.sling.distribution.queue.DistributionQueueItemStatus;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link HealthCheck} that checks if distribution queues' first item has been retried more than a configurable amount
 * of times
 */
@Component(immediate = true,
        metatype = true,
        label = "Apache Sling Distribution Queue Health Check")
@Properties({
        @Property(name = HealthCheck.NAME, value = "SlingDistributionQueueHC", description = "Health Check name", label = "Name"),
        @Property(name = HealthCheck.TAGS, unbounded = PropertyUnbounded.ARRAY, description = "Health Check tags", label = "Tags"),
        @Property(name = HealthCheck.MBEAN_NAME, value = "slingDistributionQueue", description = "Health Check MBean name", label = "MBean name")
})
@References({
        @Reference(name = "distributionAgent",
                referenceInterface = DistributionAgent.class,
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
                policy = ReferencePolicy.DYNAMIC)
})

@Service(value = HealthCheck.class)
public class DistributionQueueHealthCheck implements HealthCheck {

    private static final Logger log = LoggerFactory.getLogger(DistributionQueueHealthCheck.class);

    private static final int DEFAULT_NUMBER_OF_RETRIES_ALLOWED = 3;

    private int numberOfRetriesAllowed;

    @Property(intValue = DEFAULT_NUMBER_OF_RETRIES_ALLOWED, description = "Number of allowed retries", label = "Allowed retries")
    private static final String NUMBER_OF_RETRIES_ALLOWED = "numberOfRetriesAllowed";

    private final List<DistributionAgent> distributionAgents = new CopyOnWriteArrayList<DistributionAgent>();

    @Activate
    public void activate(final Map<String, Object> properties) {
        numberOfRetriesAllowed = PropertiesUtil.toInteger(properties.get(NUMBER_OF_RETRIES_ALLOWED), DEFAULT_NUMBER_OF_RETRIES_ALLOWED);
        log.info("Activated, numberOfRetriesAllowed={}", numberOfRetriesAllowed);
    }

    @Deactivate
    protected void deactivate() {
        distributionAgents.clear();
    }

    void bindDistributionAgent(final DistributionAgent distributionAgent) {
        distributionAgents.add(distributionAgent);

        log.debug("Registering distribution agent {} ", distributionAgent);
    }

    protected void unbindDistributionAgent(final DistributionAgent distributionAgent) {
        distributionAgents.remove(distributionAgent);
        log.debug("Unregistering distribution agent {} ", distributionAgent);
    }

    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();
        Map<String, Integer> failures = new HashMap<String, Integer>();
        if (distributionAgents.size() > 0) {

            for (DistributionAgent distributionAgent : distributionAgents) {
                for (String queueName : distributionAgent.getQueueNames()) {
                    try {
                        DistributionQueue q = distributionAgent.getQueue(queueName);

                        DistributionQueueEntry entry = q.getHead();
                        if (entry != null) {
                            DistributionQueueItem item = entry.getItem();
                            DistributionQueueItemStatus status = entry.getStatus();
                            if (status.getAttempts() <= numberOfRetriesAllowed) {
                                resultLog.debug("Queue: [{}], first item: [{}], number of retries: {}", q.getName(), item.getId(), status.getAttempts());
                            } else {
                                // the no. of attempts is higher than the configured threshold
                                resultLog.warn("Queue: [{}], first item: [{}], number of retries: {}, expected number of retries <= {}",
                                        q.getName(), item.getId(), status.getAttempts(), numberOfRetriesAllowed);
                                failures.put(q.getName(), status.getAttempts());
                            }
                        } else {
                            resultLog.debug("No items in queue [{}]", q.getName());
                        }

                    } catch (Exception e) {
                        resultLog.warn("Exception while inspecting distribution queue [{}]: {}", queueName, e);
                    }
                }
            }
        } else {
            resultLog.debug("No distribution queue providers found");
        }

        if (failures.size() > 0) {
            // a specific log entry (using markdown) to provide a recommended user action
            for (Map.Entry<String, Integer> entry : failures.entrySet()) {
                resultLog.warn("Distribution queue {}'s first item in the default queue has been retried {} times (threshold: {})",
                        entry.getKey(), entry.getValue(), numberOfRetriesAllowed);
            }
        }

        return new Result(resultLog);
    }

}