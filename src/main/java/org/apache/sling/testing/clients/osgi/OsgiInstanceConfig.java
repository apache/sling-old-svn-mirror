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
package org.apache.sling.testing.clients.osgi;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.util.config.InstanceConfig;
import org.apache.sling.testing.clients.util.config.InstanceConfigException;
import org.apache.sling.testing.clients.SlingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * <p>Allows saving and restoring the OSGiConfig to be used before and after altering OSGi configurations for tests</p>
 * <p>See {@link InstanceConfig}</p>
 */
public class OsgiInstanceConfig implements InstanceConfig {
    private static final Logger LOG = LoggerFactory.getLogger(OsgiInstanceConfig.class);

    /**
     * Time im ms to wait for retrieving the current osgi config for save() and restore()
     */
    private static final long WAIT_TIMEOUT = 20000;  // in ms

    private final OsgiConsoleClient osgiClient;
    private final String configPID;
    private Map<String, Object> config;

    @Deprecated
    protected int waitCount = 20;

    /**
     *
     * @param client The Granite Client to be used internally
     * @param configPID The PID for the OSGi configuration
     * @param <T> The type of the Granite Client
     * @throws ClientException if the client cannot be initialized
     * @throws InstanceConfigException if the config cannot be saved
     */
    public <T extends SlingClient> OsgiInstanceConfig(T client, String configPID)
            throws ClientException, InstanceConfigException, InterruptedException {
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
    public InstanceConfig save() throws InstanceConfigException, InterruptedException {
        try {
            this.config = osgiClient.waitGetConfiguration(WAIT_TIMEOUT, this.configPID);
            LOG.info("Saved OSGi config for {}. It is currently this: {}", this.configPID, this.config);
        } catch (ClientException e) {
            throw new InstanceConfigException("Error getting config", e);
        } catch (TimeoutException e) {
            throw new InstanceConfigException("Timeout of " + WAIT_TIMEOUT + " ms was reached while waiting for the configuration", e);
        }
        return this;
    }

    /**
     * Restore the current OSGi configuration for the PID defined in the constructor
     *
     * @throws InstanceConfigException if the config cannot be restored
     */
    public InstanceConfig restore() throws InstanceConfigException, InterruptedException {
        try {
            osgiClient.waitEditConfiguration(WAIT_TIMEOUT, this.configPID, null, config);
            LOG.info("restored OSGi config for {}. It is now this: {}", this.configPID, this.config);
        } catch (ClientException e) {
            throw new InstanceConfigException("Could not edit OSGi configuration", e);
        } catch (TimeoutException e) {
            throw new InstanceConfigException("Timeout of " + WAIT_TIMEOUT + " ms was reached while waiting for the configuration", e);
        }
        return this;
    }
}
