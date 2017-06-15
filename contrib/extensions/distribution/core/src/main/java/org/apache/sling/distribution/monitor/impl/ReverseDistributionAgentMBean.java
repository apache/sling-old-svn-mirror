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
 * The ReverseDistributionAgent MBean definition.
 */
public interface ReverseDistributionAgentMBean {

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
     * Whether or not the distribution agent should process packages in the queues.
     *
     * @return the Queue Processing Enabled
     */
    boolean isQueueProcessingEnabled();

    /**
     * List of endpoints from which packages are received (exported).
     *
     * @return the Importer Endpoints
     */
    String getPackageExporterEndpoints();

    /**
     * Number of subsequent pull requests to make.
     *
     * @return the Pull Items
     */
    int getPullItems();

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
