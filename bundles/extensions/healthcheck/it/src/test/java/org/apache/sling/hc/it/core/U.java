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

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.repository;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.util.Arrays;
import java.util.List;

import org.apache.sling.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.sling.hc.api.execution.HealthCheckExecutionResult;
import org.apache.sling.hc.api.execution.HealthCheckExecutor;
import org.apache.sling.hc.api.execution.HealthCheckSelector;
import org.ops4j.pax.exam.Option;

/** Test utilities */
public class U {

    /** Wait until the specified number of health checks are seen by supplied executor */
    static void expectHealthChecks(int howMany, HealthCheckExecutor executor, String ... tags) {
        expectHealthChecks(howMany, executor, new HealthCheckExecutionOptions(), tags);
    }

    /** Wait until the specified number of health checks are seen by supplied executor */
    static void expectHealthChecks(int howMany, HealthCheckExecutor executor, HealthCheckExecutionOptions options, String ... tags) {
        final long timeout = System.currentTimeMillis() + 10000L;
        int count = 0;
        while(System.currentTimeMillis() < timeout) {
            final List<HealthCheckExecutionResult> results = executor.execute(HealthCheckSelector.tags(tags), options);
            count = results.size();
            if(count== howMany) {
                return;
            }
            try {
                Thread.sleep(100L);
            } catch(InterruptedException iex) {
                throw new RuntimeException("Unexpected InterruptedException");
            }
        }
        fail("Did not get " + howMany + " health checks with tags " + Arrays.asList(tags) + " after " + timeout + " msec (last count=" + count + ")");
    }

    static Option[] config() {
        final String apiVersion = System.getProperty("sling.hc.api.version");
        final String coreVersion = System.getProperty("sling.hc.core.version");
        final String samplesVersion = System.getProperty("sling.hc.samples.version");
        final String johnzonVersion = System.getProperty("sling.johnzon.version");
        final String localRepo = System.getProperty("maven.repo.local", "");
        final boolean felixShell = "true".equals(System.getProperty("felix.shell", "false"));

        return options(
            repository("https://repo.maven.apache.org/maven2/").id("central"),
            repository("https://repository.apache.org/snapshots/").id("apache-snapshots").allowSnapshots(),
            when(localRepo.length() > 0).useOptions(
                    systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepo)
            ),
            junitBundles(),
            when(felixShell).useOptions(
                    provision(
                            mavenBundle("org.apache.felix", "org.apache.felix.gogo.shell", "0.10.0"),
                            mavenBundle("org.apache.felix", "org.apache.felix.gogo.runtime", "0.10.0"),
                            mavenBundle("org.apache.felix", "org.apache.felix.gogo.command", "0.12.0"),
                            mavenBundle("org.apache.felix", "org.apache.felix.shell.remote", "1.1.2")
                    )
            ),
            provision(
                    mavenBundle("org.apache.sling", "org.apache.sling.commons.log", "4.0.6"),
                    mavenBundle("org.apache.sling", "org.apache.sling.commons.logservice", "1.0.6"),
                    mavenBundle("org.slf4j", "slf4j-api", "1.7.13"),
                    mavenBundle("org.slf4j", "jcl-over-slf4j", "1.7.13"),
                    mavenBundle("org.slf4j", "log4j-over-slf4j", "1.7.13"),
                    mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.0.8"),
                    mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.8.10"),
                    mavenBundle("org.apache.felix", "org.apache.felix.http.servlet-api", "1.1.0"),
                    mavenBundle("org.apache.sling", "org.apache.sling.commons.johnzon", johnzonVersion),
                    mavenBundle("org.apache.sling", "org.apache.sling.hc.api", apiVersion),
                    mavenBundle("org.apache.sling", "org.apache.sling.hc.core", coreVersion),
                    mavenBundle("org.apache.sling", "org.apache.sling.hc.samples", samplesVersion),
                    mavenBundle("org.apache.sling", "org.apache.sling.commons.osgi", "2.2.0"),
                    mavenBundle("org.apache.sling", "org.apache.sling.jcr.jcr-wrapper", "2.0.0"),
                    mavenBundle("org.apache.sling", "org.apache.sling.api", "2.4.2"),
                    mavenBundle("org.apache.sling", "org.apache.sling.jcr.api", "2.1.0"),
                    mavenBundle("org.apache.sling", "org.apache.sling.engine", "2.2.8"),
                    mavenBundle("org.apache.sling", "org.apache.sling.auth.core", "1.1.2"),
                    mavenBundle("org.apache.sling", "org.apache.sling.commons.mime", "2.1.4"),
                    mavenBundle("org.apache.sling", "org.apache.sling.settings", "1.2.2"),
                    mavenBundle("org.apache.sling", "org.apache.sling.launchpad.api", "1.1.0"),
                    mavenBundle("org.apache.sling", "org.apache.sling.scripting.api", "2.1.0"),
                    mavenBundle("org.apache.sling", "org.apache.sling.commons.threads", "3.1.0"),
                    mavenBundle("org.apache.sling", "org.apache.sling.commons.scheduler", "2.4.2"),
                    mavenBundle("commons-collections", "commons-collections", "3.2.1"),
                    mavenBundle("commons-io", "commons-io", "1.4"),
                    mavenBundle("commons-fileupload", "commons-fileupload", "1.2.2"),
                    mavenBundle("commons-lang", "commons-lang", "2.5")            )
        );
    }
}
