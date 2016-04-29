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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * Implements simple write and read resource and values test.
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
    private Resource testRoot;

    @Before
    public final void setUp() throws IOException, LoginException {
        resourceResolver = new MockResourceResolverFactory().getResourceResolver(null);

        Resource root = resourceResolver.getResource("/");
        testRoot = resourceResolver.create(root, "test", ValueMap.EMPTY);

        Resource node1 = resourceResolver.create(testRoot, "node1",
            ImmutableMap.<String, Object>builder()
                .put(MockResource.JCR_PRIMARYTYPE, NT_UNSTRUCTURED)
                .put("stringProp", STRING_VALUE)
                .put("stringArrayProp", STRING_ARRAY_VALUE)
                .put("integerProp", INTEGER_VALUE)
                .put("doubleProp", DOUBLE_VALUE)
                .put("booleanProp", BOOLEAN_VALUE)
                .put("dateProp", DATE_VALUE)
                .put("calendarProp", CALENDAR_VALUE)
                .put("binaryProp", new ByteArrayInputStream(BINARY_VALUE))
                .build());

        resourceResolver.create(node1, "node11", ImmutableMap.<String, Object>builder()
                .put("stringProp11", STRING_VALUE)
                .build());
        resourceResolver.create(node1, "node12", ValueMap.EMPTY);

        resourceResolver.commit();
    }

    @Test
    public void testSimpleProperties() throws IOException {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");
        assertNotNull(resource1);
        assertEquals("node1", resource1.getName());

        ValueMap props = ResourceUtil.getValueMap(resource1);
        assertEquals(STRING_VALUE, props.get("stringProp", String.class));
        assertArrayEquals(STRING_ARRAY_VALUE, props.get("stringArrayProp", String[].class));
        assertEquals((Integer) INTEGER_VALUE, props.get("integerProp", Integer.class));
        assertEquals(DOUBLE_VALUE, props.get("doubleProp", Double.class), 0.0001);
        assertEquals(BOOLEAN_VALUE, props.get("booleanProp", Boolean.class));
    }

    @Test
    public void testSimpleProperties_DeepPathAccess() throws IOException {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath());
        assertNotNull(resource1);
        assertEquals(testRoot.getName(), resource1.getName());

        ValueMap props = ResourceUtil.getValueMap(resource1);
        assertEquals(STRING_VALUE, props.get("node1/stringProp", String.class));
        assertArrayEquals(STRING_ARRAY_VALUE, props.get("node1/stringArrayProp", String[].class));
        assertEquals((Integer) INTEGER_VALUE, props.get("node1/integerProp", Integer.class));
        assertEquals(DOUBLE_VALUE, props.get("node1/doubleProp", Double.class), 0.0001);
        assertEquals(BOOLEAN_VALUE, props.get("node1/booleanProp", Boolean.class));
        assertEquals(STRING_VALUE, props.get("node1/node11/stringProp11", String.class));

        assertTrue(STRING_VALUE, props.containsKey("node1/stringProp"));
        assertFalse(STRING_VALUE, props.containsKey("node1/unknownProp"));
    }

    @Test
    public void testDateProperty() throws IOException {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");
        ValueMap props = ResourceUtil.getValueMap(resource1);
        assertEquals(DATE_VALUE, props.get("dateProp", Date.class));
    }

    @Test
    public void testDatePropertyToCalendar() throws IOException {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");
        ValueMap props = ResourceUtil.getValueMap(resource1);
        Calendar calendarValue = props.get("dateProp", Calendar.class);
        assertNotNull(calendarValue);
        assertEquals(DATE_VALUE, calendarValue.getTime());
    }
    
    @Test
    public void testCalendarProperty() throws IOException {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");
        ValueMap props = ResourceUtil.getValueMap(resource1);
        assertEquals(CALENDAR_VALUE.getTime(), props.get("calendarProp", Calendar.class).getTime());
    }

    @Test
    public void testCalendarPropertyToDate() throws IOException {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");
        ValueMap props = ResourceUtil.getValueMap(resource1);
        Date dateValue = props.get("calendarProp", Date.class);
        assertNotNull(dateValue);
        assertEquals(CALENDAR_VALUE.getTime(), dateValue);
    }
    
    @Test
    public void testStringToCalendarConversion() throws IOException {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");
        ModifiableValueMap modProps = resource1.adaptTo(ModifiableValueMap.class);
        modProps.put("dateISO8601String", ISO8601.format(CALENDAR_VALUE));
        resourceResolver.commit();
        
        resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");
        ValueMap props = ResourceUtil.getValueMap(resource1);
        assertEquals(CALENDAR_VALUE.getTime(), props.get("calendarProp", Calendar.class).getTime());
    }
    
    @Test
    public void testListChildren() throws IOException {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");

        List<Resource> children = Lists.newArrayList(resource1.listChildren());
        assertEquals(2, children.size());
        assertEquals("node11", children.get(0).getName());
        assertEquals("node12", children.get(1).getName());
    }

    @Test
    public void testListChildren_RootNode() throws IOException {
        Resource resource1 = resourceResolver.getResource("/");

        List<Resource> children = Lists.newArrayList(resource1.listChildren());
        assertEquals(1, children.size());
        assertEquals("test", children.get(0).getName());

        children = Lists.newArrayList(resource1.getChildren());
        assertEquals(1, children.size());
        assertEquals("test", children.get(0).getName());
    }

    @Test
    public void testBinaryData() throws IOException {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");

        Resource binaryPropResource = resource1.getChild("binaryProp");
        InputStream is = binaryPropResource.adaptTo(InputStream.class);
        byte[] dataFromResource = IOUtils.toByteArray(is);
        is.close();
        assertArrayEquals(BINARY_VALUE, dataFromResource);

        // read second time to ensure not the original input stream was returned
        // and this time using another syntax
        InputStream is2 = ResourceUtil.getValueMap(resource1).get("binaryProp", InputStream.class);
        byte[] dataFromResource2 = IOUtils.toByteArray(is2);
        is2.close();
        assertArrayEquals(BINARY_VALUE, dataFromResource2);
    }

    @Test
    public void testPrimaryTypeResourceType() {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");
        assertEquals(NT_UNSTRUCTURED, resource1.getResourceType());
    }

    @Test
    public void testNormalizePath() {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/./node1");
        assertEquals("node1", resource1.getName());

        Resource resource11 = resourceResolver.getResource(testRoot.getPath() + "/node1/../node1/node11");
        assertEquals("node11", resource11.getName());
    }

    @Test
    public void testGetResourceNullPath() {
        Resource resource = resourceResolver.getResource((String)null);
        assertNull(resource);
    }

    @Test
    public void testGetConvertedToNullType() {
        Resource resource1 = resourceResolver.getResource(testRoot.getPath() + "/node1");
        Object propValue = ResourceUtil.getValueMap(resource1).get("stringProp", null);

        assertEquals(STRING_VALUE, propValue);
    }

    @Test
    public void testGetRootResourceByNullPath() {
        Resource rootResource = this.resourceResolver.resolve((String)null);
        assertNotNull(rootResource);
        assertEquals("/", rootResource.getPath());
    }

    @Test
    public void testResolveExistingResource() {
        Resource resource = resourceResolver.resolve(testRoot.getPath() + "/node1");
        assertNotNull(resource);
        assertEquals(testRoot.getPath() + "/node1", resource.getPath());
    }
    
    @Test
    public void testResolveNonexistingResource() {
        Resource resource = resourceResolver.resolve("/non/existing/path");
        assertTrue(resource instanceof NonExistingResource);
        assertEquals("/non/existing/path", resource.getPath());
    }
    
}
