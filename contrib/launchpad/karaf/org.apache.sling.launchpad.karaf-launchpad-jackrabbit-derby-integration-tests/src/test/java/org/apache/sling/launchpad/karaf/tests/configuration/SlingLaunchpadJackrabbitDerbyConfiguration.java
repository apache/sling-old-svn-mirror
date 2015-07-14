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
package org.apache.sling.launchpad.karaf.tests.configuration;

import org.apache.sling.launchpad.karaf.testing.SlingLaunchpadConfiguration;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

public class SlingLaunchpadJackrabbitDerbyConfiguration extends SlingLaunchpadConfiguration {

    @Configuration
    public Option[] configuration() {
        return OptionUtils.combine(launchpadConfiguration(),
            editConfigurationFilePut("etc/custom.properties", "sling.run.modes", "jackrabbit"),
            addSlingFeatures(
                "sling-jcr-jackrabbit-security",
                "sling-launchpad-jackrabbit-derby"
            ),
            // configurations for tests
            editConfigurationFilePut("etc/integrationTestsConfig.cfg", "message", "This test config should be loaded at startup"),
            editConfigurationFilePut("etc/org.apache.sling.servlets.resolver.SlingServletResolver.cfg", "servletresolver.cacheSize", "0")
        );
    }

}
