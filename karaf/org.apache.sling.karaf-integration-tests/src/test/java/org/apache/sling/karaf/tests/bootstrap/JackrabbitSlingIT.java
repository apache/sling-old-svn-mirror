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

import org.apache.sling.karaf.testing.KarafTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JackrabbitSlingIT extends KarafTestSupport {

    @Configuration
    public Option[] configuration() {
        return OptionUtils.combine(baseConfiguration(),
            addSlingFeatures("jackrabbit-sling")
        );
    }

    @Test
    public void testOrgApacheJackrabbitJackrabbitApi() {
        final Bundle bundle = findBundle("org.apache.jackrabbit.jackrabbit-api");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheJackrabbitJackrabbitJcrCommons() {
        final Bundle bundle = findBundle("org.apache.jackrabbit.jackrabbit-jcr-commons");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheJackrabbitJackrabbitJcrRmi() {
        final Bundle bundle = findBundle("org.apache.jackrabbit.jackrabbit-jcr-rmi");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheJackrabbitJackrabbitSpi() {
        final Bundle bundle = findBundle("org.apache.jackrabbit.jackrabbit-spi");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheJackrabbitJackrabbitSpiCommons() {
        final Bundle bundle = findBundle("org.apache.jackrabbit.jackrabbit-spi-commons");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

    @Test
    public void testOrgApacheJackrabbitJackrabbitWebdav() {
        final Bundle bundle = findBundle("org.apache.jackrabbit.jackrabbit-webdav");
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
    public void testComGoogleGuava() {
        final Bundle bundle = findBundle("com.google.guava");
        assertNotNull(bundle);
        assertEquals(Bundle.ACTIVE, bundle.getState());
    }

}
