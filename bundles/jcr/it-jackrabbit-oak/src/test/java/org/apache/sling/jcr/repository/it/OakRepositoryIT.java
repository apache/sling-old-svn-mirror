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

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OakRepositoryIT extends CommonTests {

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        final String localRepo = System.getProperty("maven.repo.local", "");
        final String oakVersion = System.getProperty("oak.version", "NO_OAK_VERSION??");

        return options(
                when( localRepo.length() > 0 ).useOptions(
                        systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepo)
                ),
                mavenBundle("org.apache.sling", "org.apache.sling.fragment.xml", "1.0.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.fragment.transaction", "1.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.fragment.activation", "1.0.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.fragment.ws", "1.0.2"),

                mavenBundle("org.apache.sling", "org.apache.sling.commons.log", "3.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.logservice", "1.0.2"),

                mavenBundle("org.slf4j", "slf4j-api", "1.6.4"),
                mavenBundle("org.slf4j", "jcl-over-slf4j", "1.6.4"),
                mavenBundle("org.slf4j", "log4j-over-slf4j", "1.6.4"),

                mavenBundle("commons-io", "commons-io", "1.4"),
                mavenBundle("commons-fileupload", "commons-fileupload", "1.2.2"),
                mavenBundle("commons-collections", "commons-collections", "3.2.1"),
                mavenBundle("commons-codec", "commons-codec", "1.6"),
                mavenBundle("commons-lang", "commons-lang", "2.5"),

                mavenBundle("org.apache.geronimo.bundles", "commons-httpclient", "3.1_1"),
                mavenBundle("org.apache.tika", "tika-core", "1.2"),
                mavenBundle("org.apache.tika", "tika-bundle", "1.2"),

                mavenBundle("org.apache.felix", "org.apache.felix.http.jetty", "2.2.0"),
                mavenBundle("org.apache.felix", "org.apache.felix.eventadmin", "1.2.14"),
                mavenBundle("org.apache.felix", "org.apache.felix.scr", "1.8.0"),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.6.0"),
                mavenBundle("org.apache.felix", "org.apache.felix.inventory", "1.0.0"),

                mavenBundle("org.apache.sling", "org.apache.sling.commons.osgi", "2.2.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.json", "2.0.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.mime", "2.1.4"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.classloader", "1.3.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.scheduler", "2.3.4"),
                mavenBundle("org.apache.sling", "org.apache.sling.commons.threads", "3.1.0"),

                mavenBundle("org.apache.sling", "org.apache.sling.launchpad.api", "1.1.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.auth.core", "1.1.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.discovery.api", "1.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.discovery.standalone", "1.0.0"),

                mavenBundle("org.apache.sling", "org.apache.sling.api", "2.4.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.settings", "1.2.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.resourceresolver", "1.0.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.adapter", "2.1.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.resource", "2.2.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.classloader", "3.1.12"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.contentloader", "2.1.2"),
                mavenBundle("org.apache.sling", "org.apache.sling.engine", "2.2.6"),
                mavenBundle("org.apache.sling", "org.apache.sling.serviceusermapper", "1.0.0"),

                mavenBundle("org.apache.sling", "org.apache.sling.jcr.jcr-wrapper", "2.0.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.api", "2.2.0"),
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.base", "2.2.2"),
                
                // Oak
                mavenBundle("org.apache.sling", "org.apache.sling.jcr.oak.server", "0.0.1-SNAPSHOT"),
                mavenBundle("com.google.guava", "guava", "14.0.1"),
                mavenBundle("com.google.code.findbugs", "jsr305", "2.0.0"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-api", "2.7.1"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-commons", "2.7.1"),
                mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-rmi", "2.4.2"),
                mavenBundle("org.apache.jackrabbit", "oak-core", oakVersion),
                // embedded for now mavenBundle("org.apache.jackrabbit", "oak-jcr", oakVersion),
                mavenBundle("org.apache.jackrabbit", "oak-commons", oakVersion),
                mavenBundle("org.apache.jackrabbit", "oak-mk", oakVersion),
                mavenBundle("org.apache.jackrabbit", "oak-mk-api", oakVersion),
                mavenBundle("org.apache.jackrabbit", "oak-mk-remote", oakVersion),
                mavenBundle("org.apache.jackrabbit", "oak-lucene", oakVersion),

                // Testing
                mavenBundle("org.apache.sling", "org.apache.sling.testing.tools", "1.0.6"),
                mavenBundle("org.apache.httpcomponents", "httpcore-osgi", "4.1.2"),
                mavenBundle("org.apache.httpcomponents", "httpclient-osgi", "4.1.2"),

                junitBundles()
           );
    }

    protected void doCheckRepositoryDescriptors() {
        final String propName = "jcr.repository.name";
        final String name = repository.getDescriptor(propName);
        final String expected = "Oak";
        if(!name.contains(expected)) {
            fail("Expected repository descriptor " + propName + " to contain " 
                    + expected + ", failed (descriptor=" + name + ")");
        }
    }
}
