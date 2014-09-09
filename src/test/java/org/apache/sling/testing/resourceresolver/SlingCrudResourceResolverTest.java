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
package org.apache.sling.testing.resourceresolver;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Implements simple write and read resource and values test
 * Sling CRUD API is used to create the test data.
 */
public class SlingCrudResourceResolverTest {

    private static final String STRING_VALUE = "value1";
    private static final String[] STRING_ARRAY_VALUE = new String[] { "value1", "value2" };
    private static final int INTEGER_VALUE = 25;
    private static final double DOUBLE_VALUE = 3.555d;
    private static final boolean BOOLEAN_VALUE = true;
    private static final Date DATE_VALUE = new Date(10000);
    private static final Calendar CALENDAR_VALUE = Calendar.getInstance();
    private static final byte[] BINARY_VALUE = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };

    private static final String NT_UNSTRUCTURED = "nt:unstructured";

    private ResourceResolver resourceResolver;
    protected Resource testRoot;

    @Before
    public final void setUp() throws IOException, LoginException {
        this.resourceResolver = new MockResourceResolverFactory().getResourceResolver(null);

        // prepare some test data using Sling CRUD API
        Resource rootNode = getTestRootResource();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(MockResource.JCR_PRIMARYTYPE, NT_UNSTRUCTURED);
        props.put("stringProp", STRING_VALUE);
        props.put("stringArrayProp", STRING_ARRAY_VALUE);
        props.put("integerProp", INTEGER_VALUE);
        props.put("doubleProp", DOUBLE_VALUE);
        props.put("booleanProp", BOOLEAN_VALUE);
        props.put("dateProp", DATE_VALUE);
        props.put("calendarProp", CALENDAR_VALUE);
        props.put("binaryProp", new ByteArrayInputStream(BINARY_VALUE));
        Resource node1 = this.resourceResolver.create(rootNode, "node1", props);

        this.resourceResolver.create(node1, "node11", ValueMap.EMPTY);
        this.resourceResolver.create(node1, "node12", ValueMap.EMPTY);

        this.resourceResolver.commit();
    }

    @After
    public final void tearDown() {
        this.testRoot = null;
    }

    /**
     * Return a test root resource, created on demand, with a unique path
     * @throws PersistenceException
     */
    private Resource getTestRootResource() throws PersistenceException {
        if (this.testRoot == null) {
            final Resource root = this.resourceResolver.getResource("/");
            this.testRoot = this.resourceResolver.create(root, "test", ValueMap.EMPTY);
        }
        return this.testRoot;
    }

    @Test
    public void testSimpleProperties() throws IOException {
        Resource resource1 = this.resourceResolver.getResource(getTestRootResource().getPath() + "/node1");
        assertNotNull(resource1);
        assertEquals("node1", resource1.getName());

        ValueMap props = resource1.getValueMap();
        assertEquals(STRING_VALUE, props.get("stringProp", String.class));
        assertArrayEquals(STRING_ARRAY_VALUE, props.get("stringArrayProp", String[].class));
        assertEquals((Integer) INTEGER_VALUE, props.get("integerProp", Integer.class));
        assertEquals(DOUBLE_VALUE, props.get("doubleProp", Double.class), 0.0001);
        assertEquals(BOOLEAN_VALUE, props.get("booleanProp", Boolean.class));
    }

    @Test
    public void testDateProperty() throws IOException {
        Resource resource1 = this.resourceResolver.getResource(getTestRootResource().getPath() + "/node1");
        ValueMap props = resource1.getValueMap();
        assertEquals(DATE_VALUE, props.get("dateProp", Date.class));
    }

    @Test
    public void testDatePropertyToCalendar() throws IOException {
        Resource resource1 = this.resourceResolver.getResource(getTestRootResource().getPath() + "/node1");
        ValueMap props = resource1.getValueMap();
        Calendar calendarValue = props.get("dateProp", Calendar.class);
        assertNotNull(calendarValue);
        assertEquals(DATE_VALUE, calendarValue.getTime());
    }
    
    @Test
    public void testCalendarProperty() throws IOException {
        Resource resource1 = this.resourceResolver.getResource(getTestRootResource().getPath() + "/node1");
        ValueMap props = resource1.getValueMap();
        assertEquals(CALENDAR_VALUE.getTime(), props.get("calendarProp", Calendar.class).getTime());
    }

    @Test
    public void testCalendarPropertyToDate() throws IOException {
        Resource resource1 = this.resourceResolver.getResource(getTestRootResource().getPath() + "/node1");
        ValueMap props = resource1.getValueMap();
        Date dateValue = props.get("calendarProp", Date.class);
        assertNotNull(dateValue);
        assertEquals(CALENDAR_VALUE.getTime(), dateValue);
    }
    
    @Test
    public void testListChildren() throws IOException {
        Resource resource1 = this.resourceResolver.getResource(getTestRootResource().getPath() + "/node1");

        Iterator<Resource> children = resource1.listChildren();
        assertEquals("node11", children.next().getName());
        assertEquals("node12", children.next().getName());
        assertFalse(children.hasNext());
    }

    @Test
    public void testBinaryData() throws IOException {
        Resource resource1 = this.resourceResolver.getResource(getTestRootResource().getPath() + "/node1");

        Resource binaryPropResource = resource1.getChild("binaryProp");
        InputStream is = binaryPropResource.adaptTo(InputStream.class);
        byte[] dataFromResource = IOUtils.toByteArray(is);
        is.close();
        assertArrayEquals(BINARY_VALUE, dataFromResource);

        // read second time to ensure not the original input stream was returned
        // and this time using another syntax
        InputStream is2 = resource1.getValueMap().get("binaryProp", InputStream.class);
        byte[] dataFromResource2 = IOUtils.toByteArray(is2);
        is2.close();
        assertArrayEquals(BINARY_VALUE, dataFromResource2);
    }

    @Test
    public void testPrimaryTypeResourceType() throws PersistenceException {
        Resource resource1 = this.resourceResolver.getResource(getTestRootResource().getPath() + "/node1");
        assertEquals(NT_UNSTRUCTURED, resource1.getResourceType());
    }

}
