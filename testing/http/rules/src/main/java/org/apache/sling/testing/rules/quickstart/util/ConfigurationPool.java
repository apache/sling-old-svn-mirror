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
package org.apache.sling.testing.rules.quickstart.util;

import org.apache.sling.testing.clients.quickstart.QuickstartConfiguration;
import org.apache.sling.testing.clients.quickstart.QuickstartSetup;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class ConfigurationPool {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationPool.class);

    private static final List<QuickstartConfiguration> configurations = initialConfigurations();

    private static final Set<Integer> takenConfigurations = new HashSet<Integer>();

    private static final Map<Description, Set<Integer>> takenConfigurationsByDescription = new HashMap<Description, Set<Integer>>();

    private static List<QuickstartConfiguration> initialConfigurations() {

        // If we are using the Quickstart Maven Plugin, use initial configurations from the plugin

        LOG.info("Reading initial configurations from the Quickstart Maven Plugin");

        List<QuickstartConfiguration> configurationsFromPlugin = QuickstartSetup.get().getConfigurations();

        if (!configurationsFromPlugin.isEmpty()) {

            LOG.info("Found {} quickstart configuration(s) from the system properties", configurationsFromPlugin.size());

            return configurationsFromPlugin;
        }

        LOG.info("No quickstart configurations found from the system properties");

        List<QuickstartConfiguration> configurations = new ArrayList<QuickstartConfiguration>();

        // If no quickstart JAR is specified, assume that two standard instances are already running

        LOG.info("Check if tests are running in development mode");

        if (Options.JAR.isNotSpecified()) {

            LOG.info("Tests are running in development mode, adding default QuickstartConfiguration (URL: http://localhost:8080), runmode: default");

            try {
                configurations.add(new QuickstartConfiguration(new URI("http://localhost:8080"), "default"));
            } catch (URISyntaxException e) {
                LOG.warn("Couldn't parse URI", e);
            }
        }

        return configurations;
    }

    public List<QuickstartConfiguration> getConfigurations() {
        return new ArrayList<QuickstartConfiguration>(configurations);
    }

    public boolean isTaken(int configurationIndex) {
        synchronized (ConfigurationPool.class) {
            return takenConfigurations.contains(configurationIndex);
        }
    }

    public QuickstartConfiguration takeConfiguration(Description description, int configurationIndex) {
        synchronized (ConfigurationPool.class) {
            if (isTaken(configurationIndex)) {
                throw new IllegalStateException("Requested configuration is already taken");
            }

            Set<Integer> indices = takenConfigurationsByDescription.get(description);

            if (indices == null) {
                indices = new HashSet<Integer>();
            }

            indices.add(configurationIndex);

            LOG.debug("Test {} took configuration with index {}", description, configurationIndex);

            takenConfigurationsByDescription.put(description, indices);

            takenConfigurations.add(configurationIndex);

            return configurations.get(configurationIndex);
        }
    }

    public void returnConfiguration(Description description, int configurationIndex) {
        synchronized (ConfigurationPool.class) {
            if (!isTaken(configurationIndex)) {
                throw new IllegalStateException("Returned configuration is not taken by anyone");
            }

            Set<Integer> indices = takenConfigurationsByDescription.get(description);

            if (indices == null) {
                throw new IllegalStateException("The test didn't take any configuration");
            }

            if (!indices.contains(configurationIndex)) {
                throw new IllegalStateException("The returned configuration was not taken by the test");
            }

            indices.remove(configurationIndex);

            LOG.debug("Test {} returned configuration with index {}", description, configurationIndex);

            if (indices.isEmpty()) {
                takenConfigurationsByDescription.remove(description);
            }

            takenConfigurations.remove(configurationIndex);
        }
    }

    public Integer addAndTakeConfiguration(Description description, QuickstartConfiguration configuration) {
        synchronized (ConfigurationPool.class) {
            configurations.add(configuration);

            Integer index = configurations.size() - 1;

            takeConfiguration(description, index);

            return index;
        }
    }

}
