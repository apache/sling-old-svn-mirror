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
package org.apache.sling.scripting.core.it;

import org.apache.sling.testing.paxexam.TestSupport;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.apache.sling.testing.paxexam.SlingOptions.sling;
import static org.apache.sling.testing.paxexam.SlingOptions.versionResolver;
import static org.apache.sling.testing.paxexam.SlingOptions.webconsole;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

public class ScriptingCoreTestSupport extends TestSupport {

    @Configuration
    public Option[] configuration() {
        return new Option[]{
            baseConfiguration(),
            launchpad(),
            // Sling Scripting Core
            testBundle("bundle.filename"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.scripting.api").versionAsInProject(),
            factoryConfiguration("org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended")
                .put("user.mapping", new String[]{
                    "org.apache.sling.scripting.core=sling-scripting"
                })
                .asOption(),
            // debugging
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.inventory").version(versionResolver),
            mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.webconsole.plugins.ds").version(versionResolver),
            // testing
            junitBundles()
        };
    }

    protected Option launchpad() {
        versionResolver.setVersion("org.apache.felix", "org.apache.felix.http.jetty", "3.1.6"); // Java 7
        versionResolver.setVersion("org.apache.felix", "org.apache.felix.http.whiteboard", "2.3.2"); // Java 7
        final int httpPort = findFreePort();
        System.out.println("http port " + httpPort);
        return composite(
            sling(),
            webconsole(),
            newConfiguration("org.apache.felix.http")
                .put("org.osgi.service.http.port", httpPort)
                .asOption()
        );
    }

}
