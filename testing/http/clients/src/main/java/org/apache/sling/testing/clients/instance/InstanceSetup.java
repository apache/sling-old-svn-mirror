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
package org.apache.sling.testing.clients.instance;

import org.apache.sling.testing.clients.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for getting the current instance setup
 */
public final class InstanceSetup {

    private static final Logger LOG = LoggerFactory.getLogger(InstanceSetup.class);
    private static InstanceSetup SINGLETON;

    /**
     * @return  the current setup object.
     */
    public static InstanceSetup get() {
        if ( SINGLETON == null ) {
            SINGLETON = new InstanceSetup();
        }
        return SINGLETON;
    }

    private final List<InstanceConfiguration> configs = new ArrayList<InstanceConfiguration>();

    private InstanceSetup() {
        final int number = Integer.valueOf(System.getProperty(Constants.CONFIG_PROP_PREFIX + "instances", "0"));
        for (int i=1; i<=number; i++ ) {
            URI url;
            try {
                url = new URI(System.getProperty(Constants.CONFIG_PROP_PREFIX + "instance.url." + String.valueOf(i)));
            } catch (URISyntaxException e) {
                LOG.error("Could not read URL for instance");
                continue;
            }
            final String runmode = System.getProperty(Constants.CONFIG_PROP_PREFIX + "instance.runmode." + String.valueOf(i));

            final InstanceConfiguration qc = new InstanceConfiguration(url, runmode);

            this.configs.add(qc);
        }
    }

    /**
     * @return all instance configurations.
     */
    public List<InstanceConfiguration> getConfigurations() {
        return this.configs;
    }

    /**
     * Get the list of all InstanceConfiguration with a specific {@code runmode}
     *
     * @param runmode the desired runmode
     * @return all instance configurations filtered by runmode.
     */
    public List<InstanceConfiguration> getConfigurations(final String runmode) {
        final List<InstanceConfiguration> result = new ArrayList<InstanceConfiguration>();
        for(final InstanceConfiguration qc : this.configs) {
            if ( runmode == null || runmode.equals(qc.getRunmode()) ) {
                result.add(qc);
            }
        }
        return result;
    }
}
