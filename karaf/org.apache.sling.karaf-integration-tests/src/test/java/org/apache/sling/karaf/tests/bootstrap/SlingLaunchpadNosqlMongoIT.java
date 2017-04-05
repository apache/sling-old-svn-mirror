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
package org.apache.sling.karaf.tests.bootstrap;

import java.io.IOException;

import javax.inject.Inject;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.karaf.testing.KarafTestSupport;
import org.junit.AfterClass;
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
public class SlingLaunchpadNosqlMongoIT extends KarafTestSupport {

    private static MongodExecutable executable;

    private static MongodProcess process;

    @Inject
    @Filter(timeout = 300000)
    public ResourceProviderFactory resourceProviderFactory;

    protected void startMongo(final int port) throws IOException {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        final Net net = new Net(port, Network.localhostIsIPv6());
        final IMongodConfig mongodConfig = new MongodConfigBuilder().version(Version.Main.PRODUCTION).net(net).build();
        executable = starter.prepare(mongodConfig);
        process = executable.start();
    }

    @AfterClass // TODO does it work? (no - not supported by Pax Exam)
    public static void stopMongo() throws Exception {
        if (executable != null) {
            executable.stop();
        }
    }

    @Configuration
    public Option[] configuration() throws IOException {
        final int port = Network.getFreeServerPort();
        startMongo(port);
        final String connectionString = String.format("localhost:%s", port);
        return OptionUtils.combine(baseConfiguration(),
            editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "featuresBoot", "(wrap)"),
            editConfigurationFilePut("etc/org.apache.sling.nosql.mongodb.resourceprovider.MongoDBNoSqlResourceProviderFactory.factory.config.config", "connectionString", connectionString),
            wrappedBundle(mavenBundle().groupId("de.flapdoodle.embed").artifactId("de.flapdoodle.embed.mongo").versionAsInProject()),
            wrappedBundle(mavenBundle().groupId("de.flapdoodle.embed").artifactId("de.flapdoodle.embed.process").versionAsInProject()),
            wrappedBundle(mavenBundle().groupId("net.java.dev.jna").artifactId("jna").versionAsInProject()),
            wrappedBundle(mavenBundle().groupId("net.java.dev.jna").artifactId("jna-platform").versionAsInProject()),
            mavenBundle().groupId("org.apache.commons").artifactId("commons-compress").versionAsInProject(),
            addSlingFeatures("sling-launchpad-nosql-mongodb")
        );
    }

    @Test
    public void testResourceProviderFactory() {
        assertNotNull(resourceProviderFactory);
        assertEquals("org.apache.sling.nosql.mongodb.resourceprovider.impl.MongoDBNoSqlResourceProviderFactory", resourceProviderFactory.getClass().getName());
    }

}
