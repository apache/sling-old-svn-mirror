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
package org.apache.sling.testing.mock.sling.resource;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.builder.ContentBuilder;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * Implements simple write and read resource and values test. Sling CRUD API is
 * used to create the test data.
 */
public abstract class AbstractSlingCrudResourceResolverTest {
    
    @Rule
    public SlingContext context = new SlingContext(getResourceResolverType());

    private static final String STRING_VALUE = "value1";
    private static final String[] STRING_ARRAY_VALUE = new String[] { "value1", "value2" };
    private static final int INTEGER_VALUE = 25;
    private static final double DOUBLE_VALUE = 3.555d;
    private static final boolean BOOLEAN_VALUE = true;
    private static final Date DATE_VALUE = new Date(10000);
    private static final Calendar CALENDAR_VALUE = Calendar.getInstance();
    private static final byte[] BINARY_VALUE = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };

    protected Resource testRoot;
    protected abstract ResourceResolverType getResourceResolverType();

    @Before
    public final void setUp() throws IOException {

        // prepare some test data using Sling CRUD API
        Resource rootNode = getTestRootResource();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        props.put("stringProp", STRING_VALUE);
        props.put("stringArrayProp", STRING_ARRAY_VALUE);
        props.put("integerProp", INTEGER_VALUE);
        props.put("doubleProp", DOUBLE_VALUE);
        props.put("booleanProp", BOOLEAN_VALUE);
        props.put("dateProp", DATE_VALUE);
        props.put("calendarProp", CALENDAR_VALUE);
        props.put("binaryProp", new ByteArrayInputStream(BINARY_VALUE));
        Resource node1 = context.resourceResolver().create(rootNode, "node1", props);

        context.resourceResolver().create(node1, "node11", ImmutableMap.<String, Object>builder()
                .put("stringProp11", STRING_VALUE)
                .build());
        context.resourceResolver().create(node1, "node12", ValueMap.EMPTY);

        context.resourceResolver().commit();
    }

    /**
     * Return a test root resource, created on demand, with a unique path
     * @throws PersistenceException
     */
    protected Resource getTestRootResource() throws PersistenceException {
        if (this.testRoot == null) {
            this.testRoot = context.resourceResolver().getResource(context.uniqueRoot().content());
        }
        return this.testRoot;
    }

    @Test
    public void testSimpleProperties() throws IOException {
        Resource resource1 = context.resourceResolver().getResource(getTestRootResource().getPath() + "/node1");
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
        Resource resource1 = context.resourceResolver().getResource(testRoot.getPath());
        assertNotNull(resource1);
        assertEquals(testRoot.getName(), resource1.getName());

        ValueMap props = ResourceUtil.getValueMap(resource1);
        assertEquals(STRING_VALUE, props.get("node1/stringProp", String.class));
        assertArrayEquals(STRING_ARRAY_VALUE, props.get("node1/stringArrayProp", String[].class));
        assertEquals((Integer) INTEGER_VALUE, props.get("node1/integerProp", Integer.class));
        assertEquals(DOUBLE_VALUE, props.get("node1/doubleProp", Double.class), 0.0001);
        assertEquals(BOOLEAN_VALUE, props.get("node1/booleanProp", Boolean.class));
        assertEquals(STRING_VALUE, props.get("node1/node11/stringProp11", String.class));
    }
    
    @Test
    public void testDateProperty() throws IOException {
        Resource resource1 = context.resourceResolver().getResource(getTestRootResource().getPath() + "/node1");
        ValueMap props = ResourceUtil.getValueMap(resource1);
        // TODO: enable this test when JCR resource implementation supports
        // writing Date objects (SLING-3846)
        if (getResourceResolverType() != ResourceResolverType.JCR_MOCK
                && getResourceResolverType() != ResourceResolverType.JCR_JACKRABBIT 
                && getResourceResolverType() != ResourceResolverType.JCR_OAK ) {
            assertEquals(DATE_VALUE, props.get("dateProp", Date.class));
        }
    }

    @Test
    public void testDatePropertyToCalendar() throws IOException {
        Resource resource1 = context.resourceResolver().getResource(getTestRootResource().getPath() + "/node1");
        ValueMap props = ResourceUtil.getValueMap(resource1);
        // TODO: enable this test when JCR resource implementation supports
        // writing Date objects (SLING-3846)
        if (getResourceResolverType() != ResourceResolverType.JCR_MOCK
                && getResourceResolverType() != ResourceResolverType.JCR_JACKRABBIT
                && getResourceResolverType() != ResourceResolverType.JCR_OAK ) {
            Calendar calendarValue = props.get("dateProp", Calendar.class);
            assertNotNull(calendarValue);
            assertEquals(DATE_VALUE, calendarValue.getTime());
        }
    }

    @Test
    public void testCalendarProperty() throws IOException {
        Resource resource1 = context.resourceResolver().getResource(getTestRootResource().getPath() + "/node1");
        ValueMap props = ResourceUtil.getValueMap(resource1);
        assertEquals(CALENDAR_VALUE.getTime(), props.get("calendarProp", Calendar.class).getTime());
    }

    @Test
    public void testCalendarPropertyToDate() throws IOException {
        Resource resource1 = context.resourceResolver().getResource(getTestRootResource().getPath() + "/node1");
        ValueMap props = ResourceUtil.getValueMap(resource1);
        Date dateValue = props.get("calendarProp", Date.class);
        assertNotNull(dateValue);
        assertEquals(CALENDAR_VALUE.getTime(), dateValue);
    }

    @Test
    public void testListChildren() throws IOException {
        Resource resource1 = context.resourceResolver().getResource(getTestRootResource().getPath() + "/node1");

        List<Resource> children = ImmutableList.copyOf(resource1.listChildren());
        assertEquals(2, children.size());
        assertEquals("node11", children.get(0).getName());
        assertEquals("node12", children.get(1).getName());
    }

    @Test
    public void testListChildren_RootNode() throws IOException {
        Resource resource1 = context.resourceResolver().getResource("/");

        List<Resource> children = Lists.newArrayList(resource1.listChildren());
        assertFalse(children.isEmpty());
        assertTrue(containsResource(children, getTestRootResource().getParent()));

        children = Lists.newArrayList(resource1.getChildren());
        assertFalse(children.isEmpty());
        assertTrue(containsResource(children, getTestRootResource().getParent()));
    }
    
    private boolean containsResource(List<Resource> children, Resource resource) {
        for (Resource child : children) {
            if (StringUtils.equals(child.getPath(), resource.getPath())) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testBinaryData() throws IOException {
        Resource resource1 = context.resourceResolver().getResource(getTestRootResource().getPath() + "/node1");

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
    public void testPrimaryTypeResourceType() throws PersistenceException {
        Resource resource = context.resourceResolver().getResource(getTestRootResource().getPath() + "/node1");
        assertEquals(JcrConstants.NT_UNSTRUCTURED, resource.getResourceType());
    }

    @Test
    public void testGetRootResourceByNullPath() {
        Resource rootResource = context.resourceResolver().resolve((String)null);
        assertNotNull(rootResource);
        assertEquals("/", rootResource.getPath());
    }

    @Test
    public void testSearchPath() {
        ContentBuilder builder = new ContentBuilder(context.resourceResolver());
        builder.resource("/libs/any/path");

        Resource resource = context.resourceResolver().getResource("any/path");
        assertNotNull(resource);
        assertEquals("/libs/any/path", resource.getPath());

        builder.resource("/apps/any/path");

        resource = context.resourceResolver().getResource("any/path");
        assertNotNull(resource);
        assertEquals("/apps/any/path", resource.getPath());
    }

    @Test
    public void testPendingChangesCommit() throws PersistenceException {
        
        // skip this test for JCR_MOCK because it does not track pending changes
        if (getResourceResolverType()==ResourceResolverType.JCR_MOCK) {
            return;
        }
        
        context.resourceResolver().delete(getTestRootResource());
        assertTrue(context.resourceResolver().hasChanges());
        
        context.resourceResolver().commit();
        assertFalse(context.resourceResolver().hasChanges());
    }

}
