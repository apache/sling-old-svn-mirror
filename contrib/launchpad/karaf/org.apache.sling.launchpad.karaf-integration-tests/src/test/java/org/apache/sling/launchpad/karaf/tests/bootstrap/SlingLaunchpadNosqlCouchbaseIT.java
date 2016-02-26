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
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SlingLaunchpadNosqlCouchbaseIT extends KarafTestSupport {

    @Inject
    @Filter(timeout = 300000)
    public ResourceProviderFactory resourceProviderFactory;

    @Configuration
    public Option[] configuration() throws IOException {
        final int port = Network.getFreeServerPort();
        // TODO find a way to start CouchDB with this port and do real tests
        final String couchbaseHosts = String.format("localhost:%s", port);
        return OptionUtils.combine(baseConfiguration(),
            editConfigurationFilePut("etc/org.apache.sling.nosql.couchbase.client.CouchbaseClient.factory.config.cfg", "couchbaseHosts", couchbaseHosts),
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
