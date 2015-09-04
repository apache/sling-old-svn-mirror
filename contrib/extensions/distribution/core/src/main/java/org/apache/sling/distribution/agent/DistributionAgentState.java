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

package org.apache.sling.distribution.agent;

/**
 * The state of a distribution agent.
 */
public enum DistributionAgentState {

    /**
     * The agent is active but the queue processing is disabled.
     */
    PAUSED,

    /**
     * The agent is active and it does not have items in its queues.
     */
    IDLE,

    /**
     * The agent is active and there are items in its queues.
     */
    RUNNING,

    /**
     * The agent is active and some items in its queues cannot be processed.
     */
    BLOCKED
}
