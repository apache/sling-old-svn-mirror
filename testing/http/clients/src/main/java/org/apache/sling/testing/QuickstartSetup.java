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
package org.apache.sling.testing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for getting the current quickstart setup
 */
public final class QuickstartSetup {

    private static final Logger LOG = LoggerFactory.getLogger(QuickstartSetup.class);
    private static QuickstartSetup SINGLETON;

    /**
     * @return  the current setup object.
     */
    public static QuickstartSetup get() {
        if ( SINGLETON == null ) {
            SINGLETON = new QuickstartSetup();
        }
        return SINGLETON;
    }

    private final List<QuickstartConfiguration> configs = new ArrayList<QuickstartConfiguration>();

    private QuickstartSetup() {
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

            final QuickstartConfiguration qc = new QuickstartConfiguration(url, runmode);

            this.configs.add(qc);
        }
    }

    /**
     * @return all quickstart configurations.
     */
    public List<QuickstartConfiguration> getConfigurations() {
        return this.configs;
    }

    /**
     * Get the list of all QuickstartConfiguration with a specific {@code runmode}
     *
     * @param runmode the desired runmode
     * @return all quickstart configurations filtered by runmode.
     */
    public List<QuickstartConfiguration> getConfigurations(final String runmode) {
        final List<QuickstartConfiguration> result = new ArrayList<QuickstartConfiguration>();
        for(final QuickstartConfiguration qc : this.configs) {
            if ( runmode == null || runmode.equals(qc.getRunmode()) ) {
                result.add(qc);
            }
        }
        return result;
    }
}
