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

import java.io.File;
import java.net.ServerSocket;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

public class SlingLaunchpadConfiguration {

    public static final String KARAF_GROUP_ID = "org.apache.karaf";

    public static final String KARAF_ARTIFACT_ID = "apache-karaf";

    public static final String KARAF_VERSION = "3.0.3";

    public static final String KARAF_NAME = "Apache Karaf";

    protected synchronized int findFreePort() {
        try {
            final ServerSocket serverSocket = new ServerSocket(0);
            final int port = serverSocket.getLocalPort();
            serverSocket.close();
            return port;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String karafGroupId() {
        return KARAF_GROUP_ID;
    }

    protected String karafArtifactId() {
        return KARAF_ARTIFACT_ID;
    }

    protected String karafVersion() {
        return KARAF_VERSION;
    }

    protected String karafName() {
        return KARAF_NAME;
    }

    protected Option addBootFeature(final String feature) {
        return editConfigurationFileExtend("etc/org.apache.karaf.features.cfg", "featuresBoot", "," + feature);
    }

    protected String featureRepository() {
        return "mvn:org.apache.sling/org.apache.sling.launchpad.karaf-features/0.1.1-SNAPSHOT/xml/features";
    }

    @Configuration
    public Option[] configuration() {
        final int httpPort = 8888; // TODO findFreePort();
        return options(
            karafDistributionConfiguration()
                .frameworkUrl(maven().groupId(karafGroupId()).artifactId(karafArtifactId()).version(karafVersion()).type("tar.gz"))
                .karafVersion(karafVersion())
                .useDeployFolder(false)
                .name(karafName())
                .unpackDirectory(new File("target/paxexam/" + getClass().getSimpleName())),
            keepRuntimeFolder(),
            logLevel(LogLevelOption.LogLevel.INFO),
            editConfigurationFileExtend("etc/org.apache.karaf.features.cfg", "featuresRepositories", "," + featureRepository()),
            editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", Integer.toString(httpPort)),
            mavenBundle().groupId("org.ops4j.base").artifactId("ops4j-base-io").version("1.4.0"),
            addBootFeature("sling-launchpad-jackrabbit"),
            addBootFeature("sling-launchpad-content"),
            addBootFeature("sling-jcr-jackrabbit-security"),
            addBootFeature("sling-auth-form"),
            addBootFeature("sling-auth-openid"),
            addBootFeature("sling-auth-selector"),
            addBootFeature("sling-scripting-groovy"),
            addBootFeature("sling-installer-provider-jcr"),
            // misc (legacy, snapshots, ...) stuff
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.servlets.compat").version("1.0.3-SNAPSHOT"),
            // test support
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.junit.core").version("1.0.11-SNAPSHOT"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.junit.remote").version("1.0.11-SNAPSHOT"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.junit.scriptable").version("1.0.11-SNAPSHOT"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.launchpad.test-services").version("2.0.9-SNAPSHOT"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.launchpad.test-fragment").version("2.0.9-SNAPSHOT"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.testing.samples.failingtests").version("1.0.7-SNAPSHOT"),
            mavenBundle().groupId("org.apache.sling").artifactId("org.apache.sling.testing.tools").version("1.0.9-SNAPSHOT"),
            mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpcore-osgi").version("4.4"),
            mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpclient-osgi").version("4.4")
        );
    }

}
