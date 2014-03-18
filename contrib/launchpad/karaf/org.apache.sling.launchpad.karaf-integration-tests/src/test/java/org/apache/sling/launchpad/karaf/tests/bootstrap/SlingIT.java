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

import javax.inject.Inject;

import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.launchpad.karaf.testing.KarafTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.Bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SlingIT extends KarafTestSupport {

    @Inject
    @Filter(timeout = 300000)
    public SlingRequestProcessor slingRequestProcessor;

    @Configuration
    public Option[] configuration() {
        return OptionUtils.combine(baseConfiguration(),
            addBootFeature("sling")
        );
    }

    @Test
    public void testOrgApacheSlingApi() {
        final Bundle bundle = findBundle("org.apache.sling.api");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheSlingAuthCore() {
        final Bundle bundle = findBundle("org.apache.sling.auth.core");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheSlingEngine() {
        final Bundle bundle = findBundle("org.apache.sling.engine");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheSlingResourceresolver() {
        final Bundle bundle = findBundle("org.apache.sling.resourceresolver");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheSlingServiceusermapper() {
        final Bundle bundle = findBundle("org.apache.sling.serviceusermapper");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheSlingSettings() {
        final Bundle bundle = findBundle("org.apache.sling.settings");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheSlingCommonsClassloader() {
        final Bundle bundle = findBundle("org.apache.sling.commons.classloader");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheSlingCommonsJson() {
        final Bundle bundle = findBundle("org.apache.sling.commons.json");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheSlingCommonsMime() {
        final Bundle bundle = findBundle("org.apache.sling.commons.mime");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheSlingCommonsOsgi() {
        final Bundle bundle = findBundle("org.apache.sling.commons.osgi");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheSlingCommonsScheduler() {
        final Bundle bundle = findBundle("org.apache.sling.commons.scheduler");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheSlingCommonsThreads() {
        final Bundle bundle = findBundle("org.apache.sling.commons.threads");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheSlingLaunchpadApi() {
        final Bundle bundle = findBundle("org.apache.sling.launchpad.api");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheSlingLaunchpadKaraf() {
        final Bundle bundle = findBundle("org.apache.sling.launchpad.karaf");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testJavaxJcr() {
        final Bundle bundle = findBundle("javax.jcr");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheGeronimoBundlesJson() {
        final Bundle bundle = findBundle("org.apache.geronimo.bundles.json");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testSlingRequestProcessor() throws Exception {
        assertNotNull(slingRequestProcessor);
    }

}
