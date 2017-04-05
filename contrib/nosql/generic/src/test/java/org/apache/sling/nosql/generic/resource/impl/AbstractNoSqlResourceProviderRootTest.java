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
package org.apache.sling.nosql.generic.resource.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * Test monting NoSqlResourceProvider as root resource provider.
 */
public abstract class AbstractNoSqlResourceProviderRootTest {
    
    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.NONE);
    
    protected abstract void registerResourceProviderFactoryAsRoot();

    @Before
    public void setUp() throws Exception {
        registerResourceProviderFactoryAsRoot();
    }
    
    @After
    public void tearDown() {
        context.resourceResolver().revert();
    }
    
    @Test
    public void testRoot() {
        Resource root = context.resourceResolver().getResource("/");
        assertNotNull(root);
        assertTrue(root instanceof NoSqlResource);
    }

    @Test
    public void testCreatePath() throws PersistenceException {
        ResourceUtil.getOrCreateResource(context.resourceResolver(), "/test/test1",
                ImmutableMap.<String, Object>of(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED),
                JcrConstants.NT_UNSTRUCTURED, true);
        
        Resource test = context.resourceResolver().getResource("/test");
        assertNotNull(test);
        
        Resource test1 = context.resourceResolver().getResource("/test/test1");
        assertNotNull(test1);
        
        context.resourceResolver().delete(test);
    }
    
    @Test
    public void testListChildren_RootNode() throws IOException {
        Resource testResource = ResourceUtil.getOrCreateResource(context.resourceResolver(), "/test",
                ImmutableMap.<String, Object>of(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED),
                JcrConstants.NT_UNSTRUCTURED, true);

        Resource root = context.resourceResolver().getResource("/");

        List<Resource> children = Lists.newArrayList(root.listChildren());
        assertFalse(children.isEmpty());
        assertTrue(containsResource(children, testResource));

        children = Lists.newArrayList(root.getChildren());
        assertFalse(children.isEmpty());
        assertTrue(containsResource(children, testResource));

        context.resourceResolver().delete(testResource);
    }

    private boolean containsResource(List<Resource> children, Resource resource) {
        for (Resource child : children) {
            if (StringUtils.equals(child.getPath(), resource.getPath())) {
                return true;
            }
        }
        return false;
    }
    
    @Test(expected = PersistenceException.class)
    public void testDeleteRootPath() throws PersistenceException {
        Resource root = context.resourceResolver().getResource("/");
        context.resourceResolver().delete(root);
    }

    @Test
    public void testUpdateRootPath() throws PersistenceException {
        Resource root = context.resourceResolver().getResource("/");
        ModifiableValueMap props = root.adaptTo(ModifiableValueMap.class);
        props.put("prop1", "value1");
        context.resourceResolver().commit();
        
        root = context.resourceResolver().getResource("/");
        assertThat(root.getValueMap().get("prop1", String.class), equalTo("value1"));
    }

}
