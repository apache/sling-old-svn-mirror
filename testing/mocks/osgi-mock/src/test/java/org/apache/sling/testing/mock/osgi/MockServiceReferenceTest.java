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
package org.apache.sling.testing.mock.osgi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.testing.mock.osgi.OsgiMetadataUtilTest.ServiceWithMetadata;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class MockServiceReferenceTest {

    private BundleContext bundleContext;
    private ServiceReference serviceReference;
    private Object service;

    @Before
    public void setUp() {
        this.bundleContext = MockOsgi.newBundleContext();

        this.service = new Object();
        String clazz = String.class.getName();
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("customProp1", "value1");

        this.bundleContext.registerService(clazz, this.service, props);
        this.serviceReference = this.bundleContext.getServiceReference(clazz);
    }

    @Test
    public void testBundle() {
        assertSame(this.bundleContext.getBundle(), this.serviceReference.getBundle());
    }

    @Test
    public void testServiceId() {
        assertNotNull(this.serviceReference.getProperty(Constants.SERVICE_ID));
    }

    @Test
    public void testProperties() {
        assertEquals(3, this.serviceReference.getPropertyKeys().length);
        assertEquals("value1", this.serviceReference.getProperty("customProp1"));
        // mandatory properties set by the container
        assertNotNull(this.serviceReference.getProperty(Constants.SERVICE_ID));
        assertArrayEquals((String[]) this.serviceReference.getProperty(Constants.OBJECTCLASS), new String[] { String.class.getName() });
    }

    @Test
    public void testWithOsgiMetadata() {
        ServiceWithMetadata serviceWithMetadata = new OsgiMetadataUtilTest.ServiceWithMetadata();
        bundleContext.registerService((String) null, serviceWithMetadata, null);
        ServiceReference reference = this.bundleContext.getServiceReference(Comparable.class.getName());

        assertEquals(5000, reference.getProperty(Constants.SERVICE_RANKING));
        assertEquals("The Apache Software Foundation", reference.getProperty(Constants.SERVICE_VENDOR));
        assertEquals("org.apache.sling.testing.mock.osgi.OsgiMetadataUtilTest$ServiceWithMetadata", reference.getProperty(Constants.SERVICE_PID));
    }

}
