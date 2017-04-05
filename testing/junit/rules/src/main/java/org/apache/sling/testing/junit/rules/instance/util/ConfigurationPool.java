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
package org.apache.sling.testing.junit.rules.instance.util;

import org.apache.sling.testing.clients.instance.InstanceConfiguration;
import org.apache.sling.testing.clients.instance.InstanceSetup;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ConfigurationPool {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationPool.class);

    private static final List<InstanceConfiguration> configurations = initialConfigurations();

    private static final Set<Integer> takenConfigurations = new HashSet<Integer>();

    private static final Map<Description, Set<Integer>> takenConfigurationsByDescription = new HashMap<Description, Set<Integer>>();

    private static List<InstanceConfiguration> initialConfigurations() {

        LOG.info("Reading initial configurations from the system properties");

        List<InstanceConfiguration> configurationsFromPlugin = InstanceSetup.get().getConfigurations();

        if (!configurationsFromPlugin.isEmpty()) {

            LOG.info("Found {} instance configuration(s) from the system properties", configurationsFromPlugin.size());

            return configurationsFromPlugin;
        }

        LOG.info("No instance configurations found from the system properties");

        List<InstanceConfiguration> configurations = new ArrayList<InstanceConfiguration>();

        return configurations;
    }

    public List<InstanceConfiguration> getConfigurations() {
        return new ArrayList<InstanceConfiguration>(configurations);
    }

    public boolean isTaken(int configurationIndex) {
        synchronized (ConfigurationPool.class) {
            return takenConfigurations.contains(configurationIndex);
        }
    }

    public InstanceConfiguration takeConfiguration(Description description, int configurationIndex) {
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

    public Integer addAndTakeConfiguration(Description description, InstanceConfiguration configuration) {
        synchronized (ConfigurationPool.class) {
            configurations.add(configuration);

            Integer index = configurations.size() - 1;

            takeConfiguration(description, index);

            return index;
        }
    }

}
