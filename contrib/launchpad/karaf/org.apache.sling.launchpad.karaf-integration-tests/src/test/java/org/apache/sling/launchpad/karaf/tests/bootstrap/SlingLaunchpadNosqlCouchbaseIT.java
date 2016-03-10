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
package org.apache.sling.launchpad.karaf.tests.bootstrap;

import java.io.IOException;

import javax.inject.Inject;

import de.flapdoodle.embed.process.runtime.Network;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.launchpad.karaf.testing.KarafTestSupport;
// import org.couchbase.mock.CouchbaseMock;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SlingLaunchpadNosqlCouchbaseIT extends KarafTestSupport {

    // private static CouchbaseMock couchbase;

    @Inject
    @Filter(timeout = 300000)
    public ResourceProviderFactory resourceProviderFactory;

    protected void startCouchbase(final int port) throws IOException, InterruptedException {
        /*
        couchbase = new CouchbaseMock("localhost", port, 10, 1024);
        couchbase.start();
        couchbase.waitForStartup();
        */
    }

    @AfterClass // TODO does it work? (no - not supported by Pax Exam)
    public static void stopCouchbase() throws Exception {
        /*
        if (couchbase != null) {
            couchbase.stop();
        }
        */
    }

    @Configuration
    public Option[] configuration() throws IOException, InterruptedException {
        final int port = Network.getFreeServerPort();
        startCouchbase(port);
        final String couchbaseHosts = String.format("localhost:%s", port);
        return OptionUtils.combine(baseConfiguration(),
            editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "featuresBoot", "(wrap)"),
            editConfigurationFilePut("etc/org.apache.sling.nosql.couchbase.client.CouchbaseClient.factory.config.cfg", "couchbaseHosts", couchbaseHosts),
            editConfigurationFilePut("etc/org.apache.sling.nosql.couchbase.client.CouchbaseClient.factory.config.cfg", "clientId", "sling-resourceprovider-couchbase"),
            editConfigurationFilePut("etc/org.apache.sling.nosql.couchbase.client.CouchbaseClient.factory.config.cfg", "bucketName", "sling"),
            editConfigurationFilePut("etc/org.apache.sling.nosql.couchbase.client.CouchbaseClient.factory.config.cfg", "enabled", "true"),
            // wrappedBundle(mavenBundle().groupId("org.couchbase.mock").artifactId("CouchbaseMock").versionAsInProject()),
            wrappedBundle(mavenBundle().groupId("com.couchbase.client").artifactId("couchbase-client").versionAsInProject()),
            wrappedBundle(mavenBundle().groupId("com.intellij").artifactId("annotations").versionAsInProject()),
            mavenBundle().groupId("com.google.code.gson").artifactId("gson").versionAsInProject(),
            mavenBundle().groupId("com.googlecode.json-simple").artifactId("json-simple").versionAsInProject(),
            mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.rhino").versionAsInProject(),
            mavenBundle().groupId("org.tukaani").artifactId("xz").versionAsInProject(),
            addSlingFeatures("sling-launchpad-nosql-couchbase")
        );
    }

    @Test
    @Ignore
    public void testResourceProviderFactory() {
        assertNotNull(resourceProviderFactory);
        assertEquals("org.apache.sling.nosql.couchbase.resourceprovider.impl.CouchbaseNoSqlResourceProviderFactory", resourceProviderFactory.getClass().getName());
    }

}
