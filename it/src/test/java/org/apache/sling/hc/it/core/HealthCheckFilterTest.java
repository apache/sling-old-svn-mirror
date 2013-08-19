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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.HealthCheckFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
public class HealthCheckFilterTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private HealthCheckFilter filter;

    @Inject
    private BundleContext bundleContext;

    private List<TestHealthCheck> testServices = new ArrayList<TestHealthCheck>();
    private static int instanceCounter = 0;

    class TestHealthCheck implements HealthCheck {

        private final int id;
        private final ServiceRegistration<?> reg;
        final String[] tags;

        TestHealthCheck(String... tags) {
            id = instanceCounter++;
            this.tags = tags;
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            if (tags != null) {
                props.put(HealthCheck.TAGS, tags);
            }
            props.put(HealthCheck.TAGS, tags);
            reg = bundleContext.registerService(HealthCheck.class.getName(),
                    this, props);
            log.info("Registered {} with {}={}", new Object[] { this,
                    HealthCheck.TAGS, props.get(HealthCheck.TAGS) });
        }

        @Override
        public String toString() {
            return Arrays.asList(tags).toString();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof TestHealthCheck
                    && ((TestHealthCheck) other).id == id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public Result execute() {
            return null;
        }

        void unregister() {
            reg.unregister();
        }
    }

    @Configuration
    public Option[] config() {
        return U.config();
    }

    @Before
    public void setup() {
        testServices.add(new TestHealthCheck("foo"));
        testServices.add(new TestHealthCheck("bar"));
        testServices.add(new TestHealthCheck("foo", "bar"));
        testServices.add(new TestHealthCheck("other", "thing"));
        testServices.add(new TestHealthCheck());
        filter = new HealthCheckFilter(bundleContext);
    }

    @After
    public void cleanup() {
        for (TestHealthCheck tc : testServices) {
            tc.unregister();
        }
    }

    /**
     * @param included
     *            true or false, in the same order as testServices
     */
    private void assertServices(List<HealthCheck> s, boolean... included) {
        final Iterator<TestHealthCheck> it = testServices.iterator();
        for (boolean inc : included) {
            final TestHealthCheck thc = it.next();
            if (inc) {
                assertTrue("Expecting list of services to include " + thc,
                        s.contains(thc));
            } else {
                assertFalse("Not expecting list of services to include " + thc,
                        s.contains(thc));
            }
        }
    }

    @Test
    public void testSelectorService() {
        assertNotNull("Expecting HealthCheckSelector service to be provided",
                filter);
    }

    @Test
    public void testAllServices() {
        final List<HealthCheck> s = filter.getTaggedHealthChecks();
        assertServices(s, true, true, true, true, true);
    }

    @Test
    public void testEmptyTags() {
        final List<HealthCheck> s = filter.getTaggedHealthChecks("", "", "");
        assertServices(s, true, true, true, true, true);
    }

    @Test
    public void testFooTag() {
        final List<HealthCheck> s = filter.getTaggedHealthChecks("foo");
        assertServices(s, true, false, true, false, false);
    }

    @Test
    public void testBarTag() {
        final List<HealthCheck> s = filter.getTaggedHealthChecks("bar");
        assertServices(s, false, true, true, false, false);
    }

    @Test
    public void testFooAndBar() {
        final List<HealthCheck> s = filter.getTaggedHealthChecks("foo", "bar");
        assertServices(s, false, false, true, false, false);
    }

    @Test
    public void testFooMinusBar() {
        final List<HealthCheck> s = filter
                .getTaggedHealthChecks("foo", "-bar");
        assertServices(s, true, false, false, false, false);
    }

    @Test
    public void testWhitespace() {
        final List<HealthCheck> s = filter.getTaggedHealthChecks(
                "\t \n\r foo  \t", "", " \t-bar\n", "");
        assertServices(s, true, false, false, false, false);
    }

    @Test
    public void testOther() {
        final List<HealthCheck> s = filter.getTaggedHealthChecks("other");
        assertServices(s, false, false, false, true, false);
    }

    @Test
    public void testMinusOther() {
        final List<HealthCheck> s = filter.getTaggedHealthChecks("-other");
        assertServices(s, true, true, true, false, true);
    }

    @Test
    public void testMinusOtherFoo() {
        final List<HealthCheck> s = filter.getTaggedHealthChecks("-other",
                "-foo");
        assertServices(s, false, true, false, false, true);
    }

    @Test
    public void testNoResults() {
        final List<HealthCheck> s = filter.getTaggedHealthChecks("NOT A TAG");
        assertTrue("Expecting no services", s.isEmpty());
    }
}
