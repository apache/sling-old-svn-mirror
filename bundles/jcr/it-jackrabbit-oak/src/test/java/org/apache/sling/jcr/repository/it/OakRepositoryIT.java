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
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OakRepositoryIT extends CommonTests {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Inject
    protected ConfigurationAdmin configAdmin;

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        final String jackrabbitVersion = System.getProperty("jackrabbit.version", "NO_JACKRABBIT_VERSION??");
        final String oakVersion = System.getProperty("oak.version", "NO_OAK_VERSION??");
        final String slingOakServerVersion = System.getProperty("sling.oak.server.version", "NO_OAK_SERVER_VERSION??");

        final List<Option> opt = new LinkedList<Option>();
        opt.addAll(commonOptions());

        // Oak
        opt.add(mavenBundle("org.apache.sling", "org.apache.sling.jcr.oak.server", slingOakServerVersion));
        opt.add(mavenBundle("org.apache.jackrabbit", "jackrabbit-api", jackrabbitVersion));
        opt.add(mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-commons", jackrabbitVersion));
        opt.add(mavenBundle("org.apache.jackrabbit", "jackrabbit-jcr-rmi", jackrabbitVersion));
        opt.add(mavenBundle("org.apache.jackrabbit", "oak-core", oakVersion));
        // embedded for now opt.add(mavenBundle("org.apache.jackrabbit", "oak-jcr", oakVersion));
        opt.add(mavenBundle("org.apache.jackrabbit", "oak-commons", oakVersion));

        opt.add(mavenBundle("org.apache.jackrabbit", "oak-lucene", oakVersion));
        opt.add(mavenBundle("org.apache.jackrabbit", "oak-blob", oakVersion));
        opt.add(mavenBundle("org.apache.jackrabbit", "oak-segment", oakVersion));
        opt.add(mavenBundle("org.apache.felix", "org.apache.felix.jaas", "0.0.2"));

        return opt.toArray(new Option[]{});
    }

    @Test
    public void doCheckRepositoryDescriptors() {
        final String propName = "jcr.repository.name";
        final String name = repository.getDescriptor(propName);
        final String expected = "Oak";
        if(!name.contains(expected)) {
            fail("Expected repository descriptor " + propName + " to contain "
                    + expected + ", failed (descriptor=" + name + ")");
        }
        
        log.info("Running on Oak version {}", repository.getDescriptor("jcr.repository.version"));
    }

    @Override
    @Before
    public void setup() throws Exception {
        final org.osgi.service.cm.Configuration cf = this.configAdmin.getConfiguration("org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStoreService", null);
        final Dictionary<String, Object> p = new Hashtable<String, Object>();
        p.put("name", "Default NodeStore");
        p.put("repository.home", "sling/oak/repository");
        cf.update(p);

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
        }
        super.setup();
    }
}
