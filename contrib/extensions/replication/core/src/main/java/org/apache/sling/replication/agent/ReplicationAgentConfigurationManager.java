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
package org.apache.sling.replication.agent;

import java.util.Map;

/**
 * Facade class for dealing with {@link ReplicationAgent}s' {@link ReplicationAgentConfiguration}s
 */
public interface ReplicationAgentConfigurationManager {

    /**
     * retrieve a {@link ReplicationAgentConfiguration} for a given {@link ReplicationAgent}
     *
     * @param agentName the name of a {@link org.apache.sling.replication.agent.ReplicationAgent}
     * @return a {@link ReplicationAgentConfiguration} for the given agent
     * @throws AgentConfigurationException
     */
    ReplicationAgentConfiguration getConfiguration(String agentName)
            throws AgentConfigurationException;

    /**
     * updates the {@link ReplicationAgentConfiguration} of a certain {@link ReplicationAgent}
     *
     * @param agentName  the name of a {@link org.apache.sling.replication.agent.ReplicationAgent}
     * @param properties a {@link java.util.Map} of properties used to update the configuration
     * @return the updated {@link ReplicationAgentConfiguration} for the given agent
     * @throws AgentConfigurationException
     */
    ReplicationAgentConfiguration updateConfiguration(String agentName, Map<String, Object> properties)
            throws AgentConfigurationException;

    /**
     * creates a configuration for a {@link ReplicationAgent}
     *
     *
     * @param agentName name of the agent
     * @param properties the configuration of the agent to create
     * @throws AgentConfigurationException
     */
    void createAgentConfiguration(String agentName, Map<String, Object> properties)
            throws AgentConfigurationException;

    /**
     * deletes a configuration for a {@link ReplicationAgent}       *
     *
     * @param agentName name of the agent
     * @throws AgentConfigurationException
     */
    void deleteAgentConfiguration(String agentName)
            throws AgentConfigurationException;

    /**
     * lists all configurations for {@link ReplicationAgent}
     *
     */
    ReplicationAgentConfiguration[] listAllAgentConfigurations()
            throws AgentConfigurationException;


}
