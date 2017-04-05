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
package org.apache.sling.testing.junit.rules.instance;

import org.apache.sling.testing.junit.rules.instance.util.ConfigurationPool;
import org.apache.sling.testing.clients.instance.InstanceConfiguration;
import org.junit.Assume;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

class ExistingInstanceStatement extends Statement {

    private final Logger logger = LoggerFactory.getLogger(ExistingInstanceStatement.class);

    private final ConfigurationPool configurationPool = new ConfigurationPool();

    private final Description description;

    private final Statement statement;

    private final String runMode;

    private Integer index;

    private InstanceConfiguration configuration;

    private final InstanceConfiguration defaultConfiguration;

    public ExistingInstanceStatement(Description description, Statement statement,
                                     String runMode, InstanceConfiguration defaultConfiguration) {
        this.description = description;
        this.statement = statement;
        this.runMode = runMode;
        this.defaultConfiguration = defaultConfiguration;
    }

    @Override
    public void evaluate() throws Throwable {
        takeMatchingQuickstart();

        assumeMatchingQuickstartIsFound();

        try {
            statement.evaluate();
        } finally {
            returnQuickstart();
        }
    }

    private void assumeMatchingQuickstartIsFound() {
        if (this.index == null) {
            Assume.assumeNotNull(this.defaultConfiguration);
            this.configuration = defaultConfiguration;
            logger.info("Using default InstanceConfiguration provided (URL: {}, runmode: {}) for test {}",
                    configuration.getUrl(), configuration.getRunmode(), description);
        }
    }

    private void takeMatchingQuickstart() {
        List<InstanceConfiguration> configurations = configurationPool.getConfigurations();

        for (int i = 0; i < configurations.size(); i++) {
            InstanceConfiguration configuration = configurations.get(i);

            // Does the configuration match the requested run mode?
            if (!runMode.equals(configuration.getRunmode())) {
                continue;
            }

            // Is the configuration already used by the current test?
            if (configurationPool.isTaken(i)) {
                continue;
            }

            // The configuration is valid, save the index
            logger.info("InstanceConfiguration (URL: {}, runmode: {}) found for test {}", configuration.getUrl(), runMode, description);

            takeQuickstart(i);

            return;
        }
    }

    private void takeQuickstart(Integer index) {
        this.configuration = configurationPool.takeConfiguration(description, index);
        this.index = index;
    }

    private void returnQuickstart() {
        if (index == null) {
            return;
        }
        configurationPool.returnConfiguration(description, index);
        index = null;
    }

    public InstanceConfiguration getConfiguration() {
        return configuration;
    }

}
