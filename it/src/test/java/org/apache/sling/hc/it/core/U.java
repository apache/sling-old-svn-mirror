/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.it.core;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import org.ops4j.pax.exam.Option;

/** Test utilities */
public class U {
    
    static Option[] config() {
        final String coreVersion = System.getProperty("sling.hc.core.version");
        final String localRepo = System.getProperty("maven.repo.local", "");
        final boolean felixShell = "true".equals(System.getProperty("felix.shell", "false"));

        return options(
            when(localRepo.length() > 0).useOptions(
                    systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepo)
            ),                    
            junitBundles(),
            when(felixShell).useOptions(
                    provision(
                            mavenBundle("org.apache.felix", "org.apache.felix.gogo.shell", "0.10.0"),
                            mavenBundle("org.apache.felix", "org.apache.felix.gogo.runtime", "0.10.0"),
                            mavenBundle("org.apache.felix", "org.apache.felix.gogo.command", "0.12.0")
                    )
            ),
            provision(
                    mavenBundle("org.apache.felix", "org.apache.felix.scr", "1.6.2"),
                    mavenBundle("org.apache.sling", "org.apache.sling.hc.core", coreVersion),
                    mavenBundle("org.apache.sling", "org.apache.sling.commons.osgi", "2.2.0")
            )
        );
    }
}
