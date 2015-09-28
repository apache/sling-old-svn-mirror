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
package org.apache.sling.testing.mock.sling.loader;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Calendar;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.NodeTypeDefinitionScanner;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public abstract class AbstractContentLoaderJsonTest {

    private ResourceResolver resourceResolver;

    protected abstract ResourceResolverType getResourceResolverType();

    protected ResourceResolver newResourceResolver() {
        ResourceResolver resolver = MockSling.newResourceResolver(getResourceResolverType());

        try {
            NodeTypeDefinitionScanner.get().register(resolver.adaptTo(Session.class), 
                    ImmutableList.of("SLING-INF/nodetypes/app.cnd"),
                    getResourceResolverType().getNodeTypeMode());
        }
        catch (RepositoryException ex) {
            throw new RuntimeException("Unable to register namespaces.", ex);
        }

        return resolver;
    }

    @Before
    public final void setUp() {
        this.resourceResolver = newResourceResolver();
        ContentLoader contentLoader = new ContentLoader(this.resourceResolver);
        contentLoader.json("/json-import-samples/content.json", "/content/sample/en");
    }

    @After
    public final void tearDown() throws Exception {
        // make sure all changes from ContentLoader are committed
        assertFalse(resourceResolver.hasChanges());
        // remove everything below /content
        Resource content = resourceResolver.getResource("/content");
        if (content != null) {
            resourceResolver.delete(content);
            resourceResolver.commit();
        }
        this.resourceResolver.close();
    }
            
    @Test
    public void testPageResourceType() {
        Resource resource = this.resourceResolver.getResource("/content/sample/en");
        assertEquals("app:Page", resource.getResourceType());
    }

    @Test
    public void testPageJcrPrimaryType() throws RepositoryException {
        Resource resource = this.resourceResolver.getResource("/content/sample/en");
        assertPrimaryNodeType(resource, "app:Page");
    }

    @Test
    public void testPageContentResourceType() {
        Resource resource = this.resourceResolver.getResource("/content/sample/en/toolbar/profiles/jcr:content");
        assertEquals("sample/components/contentpage", resource.getResourceType());
    }

    @Test
    public void testPageContentJcrPrimaryType() throws RepositoryException {
        Resource resource = this.resourceResolver.getResource("/content/sample/en/toolbar/profiles/jcr:content");
        assertPrimaryNodeType(resource, "app:PageContent");
    }

    @Test
    public void testPageContentProperties() {
        Resource resource = this.resourceResolver.getResource("/content/sample/en/toolbar/profiles/jcr:content");
        ValueMap props = ResourceUtil.getValueMap(resource);
        assertEquals(true, props.get("hideInNav", Boolean.class));

        assertEquals((Long) 1234567890123L, props.get("longProp", Long.class));
        assertEquals(1.2345d, props.get("decimalProp", Double.class), 0.00001d);
        assertEquals(true, props.get("booleanProp", Boolean.class));

        assertArrayEquals(new Long[] { 1234567890123L, 55L }, props.get("longPropMulti", Long[].class));
        assertArrayEquals(new Double[] { 1.2345d, 1.1d }, props.get("decimalPropMulti", Double[].class));
        assertArrayEquals(new Boolean[] { true, false }, props.get("booleanPropMulti", Boolean[].class));
    }

    @Test
    public void testContentResourceType() {
        Resource resource = this.resourceResolver.getResource("/content/sample/en/jcr:content/header");
        assertEquals("sample/components/header", resource.getResourceType());
    }

    @Test
    public void testContentJcrPrimaryType() throws RepositoryException {
        Resource resource = this.resourceResolver.getResource("/content/sample/en/jcr:content/header");
        assertPrimaryNodeType(resource, JcrConstants.NT_UNSTRUCTURED);
    }

    @Test
    public void testContentProperties() {
        Resource resource = this.resourceResolver.getResource("/content/sample/en/jcr:content/header");
        ValueMap props = ResourceUtil.getValueMap(resource);
        assertEquals("/content/dam/sample/header.png", props.get("imageReference", String.class));
    }

    private void assertPrimaryNodeType(final Resource resource, final String nodeType) throws RepositoryException {
        Node node = resource.adaptTo(Node.class);
        if (node != null) {
            assertEquals(nodeType, node.getPrimaryNodeType().getName());
        } else {
            ValueMap props = ResourceUtil.getValueMap(resource);
            assertEquals(nodeType, props.get(JcrConstants.JCR_PRIMARYTYPE));
        }
    }

    @Test
    public void testCalendarEcmaFormat() {
        Resource resource = this.resourceResolver.getResource("/content/sample/en/jcr:content");
        ValueMap props = ResourceUtil.getValueMap(resource);

        Calendar calendar = props.get("app:lastModified", Calendar.class);
        assertNotNull(calendar);

        calendar.setTimeZone(TimeZone.getTimeZone("GMT+2"));

        assertEquals(2014, calendar.get(Calendar.YEAR));
        assertEquals(4, calendar.get(Calendar.MONTH) + 1);
        assertEquals(22, calendar.get(Calendar.DAY_OF_MONTH));

        assertEquals(15, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(11, calendar.get(Calendar.MINUTE));
        assertEquals(24, calendar.get(Calendar.SECOND));
    }
    
    @Test
    public void testCalendarISO8601Format() {
        Resource resource = this.resourceResolver.getResource("/content/sample/en/jcr:content");
        ValueMap props = ResourceUtil.getValueMap(resource);

        Calendar calendar = props.get("dateISO8601String", Calendar.class);
        assertNotNull(calendar);

        calendar.setTimeZone(TimeZone.getTimeZone("GMT+2"));
        
        assertEquals(2014, calendar.get(Calendar.YEAR));
        assertEquals(4, calendar.get(Calendar.MONTH) + 1);
        assertEquals(22, calendar.get(Calendar.DAY_OF_MONTH));

        assertEquals(15, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(11, calendar.get(Calendar.MINUTE));
        assertEquals(24, calendar.get(Calendar.SECOND));
    }
    
    @Test
    public void testUTF8Chars() {
        Resource resource = this.resourceResolver.getResource("/content/sample/en/jcr:content");
        ValueMap props = ResourceUtil.getValueMap(resource);
        
        assertEquals("äöüß€", props.get("utf8Property"));
    }
    
}
