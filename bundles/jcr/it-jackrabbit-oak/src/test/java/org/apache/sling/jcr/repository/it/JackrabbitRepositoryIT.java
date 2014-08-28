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
package org.apache.sling.jcr.repository.it;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JackrabbitRepositoryIT extends CommonTests {

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        final String localRepo = System.getProperty("maven.repo.local", "");

        return options(
                when( localRepo.length() > 0 ).useOptions(
                        systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepo)
                ),
                mavenBundle("org.apache.sling", "org.apache.sling.fragment.xml", "1.0.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.fragment.transaction", "1.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.fragment.activation", "1.0.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.fragment.ws", "1.0.2"),

                mavenBundle("org.apache.sling", "org.apache.sling.commons.log", "4.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.logservice", "1.0.2"),

                mavenBundle("org.slf4j", "slf4j-api", "1.6.4"),
                mavenBundle("org.slf4j", "jcl-over-slf4j", "1.6.4"),
                mavenBundle("org.slf4j", "log4j-over-slf4j", "1.6.4"),

                mavenBundle("commons-io", "commons-io", "1.4"),
                mavenBundle("commons-fileupload", "commons-fileupload", "1.3.1"),
                mavenBundle("commons-collections", "commons-collections", "3.2.1"),
                mavenBundle("commons-codec", "commons-codec", "1.9"),
                mavenBundle("commons-lang", "commons-lang", "2.6"),
                mavenBundle("commons-pool", "commons-pool", "1.6"),

                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.concurrent", "1.3.4_1"),

                mavenBundle("org.apache.geronimo.bundles", "commons-httpclient", "3.1_1"),
                mavenBundle("org.apache.tika", "tika-core", "1.2"),
                mavenBundle("org.apache.tika", "tika-bundle", "1.2"),

                mavenBundle("org.apache.felix", "org.apache.felix.http.jetty", "2.2.2"),
                mavenBundle("org.apache.felix", "org.apache.felix.eventadmin", "1.3.2"),
                mavenBundle("org.apache.felix", "org.apache.felix.scr", "1.8.2"),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.8.0"),
                mavenBundle("org.apache.felix", "org.apache.felix.inventory", "1.0.4"),

                mavenBundle("org.apache.sling", "org.apache.sling.commons.osgi", "2.2.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.json", "2.0.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.mime", "2.1.4"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.classloader", "1.3.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.scheduler", "2.4.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.threads", "3.2.0"),

                mavenBundle("org.apache.sling", "org.apache.sling.launchpad.api", "1.1.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.auth.core", "1.1.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.discovery.api", "1.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.discovery.standalone", "1.0.0"),

                mavenBundle("org.apache.sling", "org.apache.sling.api", "2.7.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.settings", "1.3.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.resourceresolver", "1.1.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.adapter", "2.1.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.resource", "2.3.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.classloader", "3.2.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.contentloader", "2.1.8"),
                mavenBundle("org.apache.sling", "org.apache.sling.engine", "2.3.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.event", "3.2.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.serviceusermapper", "1.0.0"),

                mavenBundle("org.apache.sling", "org.apache.sling.jcr.jcr-wrapper", "2.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.api", "2.2.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.base", "2.2.2"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-api", "2.6.5"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-commons", "2.6.5"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-spi", "2.6.5"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-spi-commons", "2.6.5"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-rmi", "2.6.5"),
                mavenBundle("org.apache.derby", "derby", "10.5.3.0_1"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.jackrabbit.server", "2.1.3-SNAPSHOT"),

                mavenBundle("org.apache.sling", "org.apache.sling.testing.tools", "1.0.6"),
                mavenBundle("org.apache.httpcomponents", "httpcore-osgi", "4.1.2"),
                mavenBundle("org.apache.httpcomponents", "httpclient-osgi", "4.1.2"),

                junitBundles()
           );
    }

    @Override
    protected void doCheckRepositoryDescriptors() {
        assertEquals("Jackrabbit", repository.getDescriptor("jcr.repository.name"));
    }

    @Override
    @Before
    public void setup() throws IOException {
        super.setup();
    }
}
