/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.osgi;

import org.apache.sling.testing.ClientException;
import org.apache.sling.testing.SlingClient;
import org.apache.sling.testing.util.config.InstanceConfig;
import org.apache.sling.testing.util.config.InstanceConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * <p>Allows saving and restoring the OSGiConfig to be used before and after altering OSGi configurations for tests</p>
 * <p>See {@link InstanceConfig}</p>
 */
public class OsgiInstanceConfig implements InstanceConfig {

    /**
     * Number of retries for retrieving the current osgi config for save() and restore()
     */
    protected int waitCount = 20;

    private static final Logger LOG = LoggerFactory.getLogger(OsgiInstanceConfig.class);
    private final OsgiConsoleClient osgiClient;
    private final String configPID;
    private Map<String, Object> config;


    /**
     *
     * @param client The Granite Client to be used internally
     * @param configPID The PID for the OSGi configuration
     * @param <T> The type of the Granite Client
     * @throws ClientException if the client cannot be initialized
     * @throws InstanceConfigException if the config cannot be saved
     */
    public <T extends SlingClient> OsgiInstanceConfig(T client, String configPID) throws ClientException, InstanceConfigException {
        this.osgiClient = client.adaptTo(OsgiConsoleClient.class);
        this.configPID = configPID;

        // Save the configuration
        save();
    }

    /**
     * Save the current OSGi configuration for the PID defined in the constructor
     *
     * @throws InstanceConfigException if the config cannot be saved
     */
    public InstanceConfig save() throws InstanceConfigException {
        try {
            this.config = osgiClient.getConfigurationWithWait(waitCount, this.configPID);
            LOG.info("Saved OSGi config for {}. It is currently this: {}", this.configPID, this.config);
        } catch (ClientException e) {
            throw new InstanceConfigException("Error getting config", e);
        } catch (InterruptedException e) {
            throw new InstanceConfigException("Saving configuration was interrupted ", e);
        }
        return this;
    }

    /**
     * Restore the current OSGi configuration for the PID defined in the constructor
     *
     * @throws InstanceConfigException if the config cannot be restored
     */
    public InstanceConfig restore() throws InstanceConfigException {
        try {
            osgiClient.editConfigurationWithWait(waitCount, this.configPID, null, config);
            LOG.info("restored OSGi config for {}. It is now this: {}", this.configPID, this.config);
        } catch (ClientException e) {
            throw new InstanceConfigException("Could not edit OSGi configuration", e);
        } catch (InterruptedException e) {
            throw new InstanceConfigException("Restoring configuration was interrupted", e);
        }
        return this;
    }
}
