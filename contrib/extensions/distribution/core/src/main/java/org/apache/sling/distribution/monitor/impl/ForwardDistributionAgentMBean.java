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
package org.apache.sling.distribution.monitor.impl;

/**
 * The ForwardDistributionAgent MBean definition.
 */
public interface ForwardDistributionAgentMBean {

    /**
     * The name of the agent.
     *
     * @return the Name
     */
    String getName();

    /**
     * The display friendly title of the agent.
     *
     * @return the Title
     */
    String getTitle();

    /**
     * The display friendly details of the agent.
     *
     * @return the Details
     */
    String getDetails();

    /**
     * Whether or not to start the distribution agent.
     *
     * @return the Enabled
     */
    boolean isEnabled();

    /**
     * The name of the service used to access the repository.
     *
     * @return the Service Name
     */
    String getServiceName();

    /**
     * The log level recorded in the transient log accessible via http.
     *
     * @return the Log Level
     */
    String getLogLevel();

    /**
     * If set the agent will allow only distribution requests under the specified roots.
     *
     * @return the Allowed roots
     */
    String getAllowedRoots();

    /**
     * Whether or not the distribution agent should process packages in the queues.
     *
     * @return the Queue Processing Enabled
     */
    boolean isQueueProcessingEnabled();

    /**
     * List of endpoints to which packages are sent (imported). The list can be given as a map in case a queue should be configured for each endpoint, e.g. queueName=http://...
     *
     * @return the Importer Endpoints
     */
    String getPackageImporterEndpoints();

    /**
     * List of queues that should be disabled.These queues will gather all the packages until they are removed explicitly.
     *
     * @return the Passive queues
     */
    String getPassiveQueues();

    /**
     * List of priority queues that should used for specific paths.The selector format is  {queuePrefix}[|{mainQueueMatcher}]={pathMatcher}, e.g. french=/content/fr.*
     *
     * @return the Priority queues
     */
    String getPriorityQueues();

    /**
     * The strategy to apply after a certain number of failed retries.
     *
     * @return the Retry Strategy
     */
    String getRetryStrategy();

    /**
     * The number of times to retry until the retry strategy is applied.
     *
     * @return the Retry attempts
     */
    int getRetryAttempts();

    /**
     * The queue provider implementation.
     *
     * @return the Queue provider
     */
    String getQueueProvider();

    /**
     * Whether or not to use a separate delivery queue to maximize transport throughput when queue has more than 100 items
     *
     * @return the Async delivery
     */
    boolean isAsyncDelivery();

    /**
     * The distribution agent status, usually one of:
     * paused
     * idle
     * running
     * blocked
     *
     *
     * @return the distribution agent status
     * @see org.apache.sling.distribution.agent.DistributionAgentState
     */
    String getStatus();

}
