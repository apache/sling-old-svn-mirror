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
package org.apache.sling.launchpad.karaf.testing;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

public abstract class SlingLaunchpadConfiguration extends KarafTestSupport {

    public Option[] launchpadConfiguration() {
        final int httpPort = 8888; // TODO findFreePort();
        return OptionUtils.combine(baseConfiguration(),
            editConfigurationFilePut("etc/users.properties", "admin", "admin,_g_:admingroup"), // Sling’s default admin credentials used in tests
            editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", Integer.toString(httpPort)),
            addSlingFeatures(
                "sling-launchpad-content",
                "sling-auth-form",
                "sling-auth-openid",
                "sling-auth-selector",
                "sling-scripting-groovy",
                "sling-scripting-javascript",
                "sling-scripting-jsp",
                "sling-installer-provider-jcr"
            ),
            // misc (legacy, snapshots, ...) stuff
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.servlets.compat").version("1.0.3-SNAPSHOT"),
            // test support
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.junit.core").version("1.0.11-SNAPSHOT"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.junit.remote").version("1.0.11-SNAPSHOT"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.junit.scriptable").version("1.0.11-SNAPSHOT"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.launchpad.test-services").version("2.0.9-SNAPSHOT"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.launchpad.test-fragment").version("2.0.9-SNAPSHOT"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.testing.tools").version("1.0.10"),
            mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpcore-osgi").version("4.4.1"),
            mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpclient-osgi").version("4.4.1")
        );
    }

}
