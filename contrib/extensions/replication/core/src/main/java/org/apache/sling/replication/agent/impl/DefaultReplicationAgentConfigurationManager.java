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
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.replication.agent.AgentConfigurationException;
import org.apache.sling.replication.agent.ReplicationAgentConfiguration;
import org.apache.sling.replication.agent.ReplicationAgentConfigurationManager;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ReplicationAgentConfigurationManager}
 */
@Component(immediate = true, label = "Default Replication Agent Configuration Manager")
@Service(value = ReplicationAgentConfigurationManager.class)
public class DefaultReplicationAgentConfigurationManager implements
        ReplicationAgentConfigurationManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ConfigurationAdmin configAdmin;

    public ReplicationAgentConfiguration getConfiguration(String agentName)
            throws AgentConfigurationException {

        log.info("retrieving configuration for agent {}", agentName);

        try {
            Dictionary agentProperties = getOrSetProperties(agentName, null);
            Dictionary<String, Dictionary> componentProperties = getOrSetComponentProperties(agentName, null);

            log.info("configuration for agent {} found {}", agentName, agentProperties);

            return new ReplicationAgentConfiguration(agentProperties, componentProperties);

        } catch (Exception e) {

            log.error("configuration for agent {} cannot be found", agentName);
            throw new AgentConfigurationException(e);
        }
    }

    private Dictionary getOrSetProperties(String agentName, Dictionary properties) throws Exception {
        Configuration agentConfiguration = getAgentConfiguration(agentName);

        if (agentConfiguration == null)
            throw new Exception("no configuration found");

        if (properties != null)
            agentConfiguration.update(properties);
        return agentConfiguration.getProperties();
    }


    private Dictionary<String, Dictionary> getOrSetComponentProperties(String agentName, Dictionary<String, Dictionary> properties) throws Exception {
        Dictionary<String, Dictionary> result = new Hashtable<String, Dictionary>();

        Configuration agentConfiguration = getAgentConfiguration(agentName);

        for (String component : ReplicationAgentConfiguration.COMPONENTS) {
            Configuration componentConfiguration = getComponentConfiguration(agentConfiguration, component);

            if (componentConfiguration == null)
                continue;

            if (properties != null) {
                Dictionary componentProperties = properties.get(component);
                if (componentProperties != null)
                    componentConfiguration.update(componentProperties);
            }

            result.put(component, componentConfiguration.getProperties());
        }

        return result;
    }

    private Configuration getAgentConfiguration(String agentName) throws Exception {
        String filter = "(name=" + agentName + ")";
        return getOsgiConfiguration(filter);
    }

    private Configuration getComponentConfiguration(Configuration agentConfiguration, String component) throws Exception {
        try {
            String filter = PropertiesUtil.toString(agentConfiguration.getProperties().get(component), "");
            return getOsgiConfiguration(filter);
        } catch (Exception ex) {
            return null;
        }
    }

    private Configuration getOsgiConfiguration(String filter) throws Exception {
        Configuration[] configurations = getAllOsgiConfigurations(filter);
        if (configurations == null || configurations.length == 0) {
            log.info("no configurations for filter {}", filter);
            return null;
        } else if (configurations.length == 1) {
            log.info("found configuration {} for filter {}", configurations[0], filter);
            return configurations[0];
        } else {
            log.error("{} configurations for filter {} found", configurations.length, filter);
            throw new Exception("too many configurations found");
        }
    }

    private Configuration[] getAllOsgiConfigurations(String filter) throws Exception {
        return configAdmin.listConfigurations(filter);
    }

    public ReplicationAgentConfiguration updateConfiguration(String agentName,
                                                             Map<String, Object> updateProperties) throws AgentConfigurationException {
        try {

            String configName = PropertiesUtil.toString(updateProperties.get("name"), "");

            if (agentName == null || agentName.length() == 0)
                throw new Exception("agent name cannot be empty");

            if (!agentName.equals(configName))
                throw new Exception("cannot change name of a configuration");

            Dictionary agentProperties = getOrSetProperties(agentName, null);
            Dictionary<String, Dictionary> componentProperties = getOrSetComponentProperties(agentName, null);

            for (Map.Entry<String, Object> entry : updateProperties.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("X-replication-")) {
                    key = key.substring(0, 14);
                }


                String component = extractComponent(key);
                if (component != null) {
                    key = key.substring(component.length() + 1);
                    Dictionary dictionary = componentProperties.get(component);
                    if (dictionary != null)
                        dictionary.put(key, entry.getValue());
                } else {
                    agentProperties.put(key, entry.getValue());
                }
            }

            agentProperties = getOrSetProperties(agentName, agentProperties);
            componentProperties = getOrSetComponentProperties(agentName, componentProperties);

            return new ReplicationAgentConfiguration(agentProperties, componentProperties);

        } catch (Exception e) {
            log.error("configuration for agent {} was not found", agentName);

            throw new AgentConfigurationException(e);
        }
    }


    String extractComponent(String string) {
        for (String component : ReplicationAgentConfiguration.COMPONENTS) {
            if (string.startsWith(component + "."))
                return component;
        }
        return null;
    }

    public void createAgentConfiguration(String agentName, Map<String, Object> properties) throws AgentConfigurationException {

        if (agentName != null) {
            try {

                Configuration configuration = getAgentConfiguration(agentName);

                if (configuration != null)
                    throw new Exception("the agent name is already in use");

                configuration = configAdmin.createFactoryConfiguration(ReplicationAgentServiceFactory.SERVICE_PID);

                if (configuration == null)
                    throw new Exception("configuration cannot be created");

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
                log.error("cannot create agent {} ", agentName);

                throw new AgentConfigurationException(e);
            }
        } else {
            throw new AgentConfigurationException("a (unique) name is needed in order to create an agent");
        }
    }


    public void deleteAgentConfiguration(String agentName) throws AgentConfigurationException {

        if (agentName != null) {
            try {
                Configuration configuration = getAgentConfiguration(agentName);

                configuration.delete();
            } catch (Exception e) {
                log.error("cannot delete agent {} ", agentName);

                throw new AgentConfigurationException(e);
            }
        } else {
            throw new AgentConfigurationException("a (unique) name is needed in order to create an agent");
        }
    }

    public ReplicationAgentConfiguration[] listAllAgentConfigurations() throws AgentConfigurationException {
        try {
            String filter = "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + ReplicationAgentServiceFactory.SERVICE_PID + ")";
            Configuration[] configurations = getAllOsgiConfigurations(filter);

            ReplicationAgentConfiguration[] result = new ReplicationAgentConfiguration[configurations.length];
            for (int i = 0; i < configurations.length; i++) {
                String agentName = (String) configurations[i].getProperties().get("name");
                result[i] = getConfiguration(agentName);
            }

            return result;

        } catch (Exception e) {
            log.error("configurations for agents cannot be retrieved");
            throw new AgentConfigurationException(e);
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
