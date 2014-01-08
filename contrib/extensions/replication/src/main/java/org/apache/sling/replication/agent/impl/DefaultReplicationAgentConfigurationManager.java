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
package org.apache.sling.replication.agent.impl;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.replication.agent.AgentConfigurationException;
import org.apache.sling.replication.agent.ReplicationAgent;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.agent.ReplicationAgentConfigurationManager;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ReplicationAgentConfigurationManager}
 */
@Component(immediate = true)
@Service(value = ReplicationAgentConfigurationManager.class)
public class DefaultReplicationAgentConfigurationManager implements
        ReplicationAgentConfigurationManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ConfigurationAdmin configAdmin;

    public ReplicationAgentConfiguration getConfiguration(ReplicationAgent replicationAgent)
            throws AgentConfigurationException {
        if (log.isInfoEnabled()) {
            log.info("retrieving configuration for agent {}", replicationAgent);
        }
        try {
            Configuration configuration = getOsgiConfiguration(replicationAgent);
            if (log.isInfoEnabled()) {
                log.info("configuration for agent {} found {}", replicationAgent, configuration);
            }
            return new ReplicationAgentConfiguration(configuration.getProperties());

        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("configuration for agent {} cannot be found", replicationAgent.getName());
            }
            throw new AgentConfigurationException(e);
        }
    }

    private Configuration getOsgiConfiguration(ReplicationAgent replicationAgent) throws Exception {

        String filter = "(name=" + replicationAgent.getName() + ")";
        Configuration[] configurations = configAdmin.listConfigurations(filter);
        if (configurations == null) {
            throw new Exception("no configuration found");
        } else if (configurations.length == 1) {
            if (log.isInfoEnabled()) {
                log.info("found configuration {} for agent {}", configurations[0],
                        replicationAgent.getName());
            }
            return configurations[0];
        } else {
            if (log.isErrorEnabled()) {
                log.error("{} configurations for agent {} found", configurations.length,
                        replicationAgent.getName());
            }
            throw new Exception("too many configurations found");
        }
    }

    public ReplicationAgentConfiguration updateConfiguration(ReplicationAgent replicationAgent,
                                                             Map<String, Object> updateProperties) throws AgentConfigurationException {
        try {
            Configuration configuration = getOsgiConfiguration(replicationAgent);
            @SuppressWarnings("unchecked")
            Dictionary<String, Object> configurationProperties = configuration.getProperties();
            for (Map.Entry<String, Object> entry : updateProperties.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("X-replication-")) {
                    key = key.substring(0, 14);
                }
                configurationProperties.put(key, entry.getValue());
            }
            configuration.update(configurationProperties);
            return new ReplicationAgentConfiguration(configuration.getProperties());
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("configuration for agent {} was not found", replicationAgent.getName());
            }
            throw new AgentConfigurationException(e);
        }

    }

    public void createAgentConfiguration(Map<String, Object> properties) throws AgentConfigurationException {

        Object name = properties.get("name");
        if (name != null) {
            try {
                Configuration configuration = configAdmin.createFactoryConfiguration(ReplicationAgentServiceFactory.SERVICE_PID + "-" + parseString(name));
                @SuppressWarnings("unchecked")
                Dictionary<String, Object> configurationProperties = new Hashtable<String, Object>();

                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    String key = entry.getKey();
                    if (key.startsWith("X-replication-")) {
                        key = key.substring(0, 14);
                    }
                    String value = parseString(entry.getValue());
                    configurationProperties.put(key, value);
                }
                configuration.update(configurationProperties);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("cannot create agent {} ", name);
                }
                throw new AgentConfigurationException(e);
            }
        } else {
            throw new AgentConfigurationException("a (unique) name is needed in order to create an agent");
        }
    }

    private String parseString(Object object) {
        String value;
        if (object instanceof String[]) {
            String arrayString = Arrays.toString((String[]) object);
            value = arrayString.substring(1, arrayString.length() - 1);
        } else {
            value = String.valueOf(object);
        }
        return value;
    }

}
