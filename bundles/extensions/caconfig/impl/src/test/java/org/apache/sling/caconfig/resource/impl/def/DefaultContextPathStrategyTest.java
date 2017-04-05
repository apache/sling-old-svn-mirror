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
package org.apache.sling.caconfig.resource.impl.def;

import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_REF;
import static org.apache.sling.caconfig.resource.impl.util.ContextResourceTestUtil.toConfigRefIterator;
import static org.apache.sling.caconfig.resource.impl.util.ContextResourceTestUtil.toResourceIterator;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.resource.spi.ContextPathStrategy;
import org.apache.sling.hamcrest.ResourceIteratorMatchers;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class DefaultContextPathStrategyTest {
    
    @Rule
    public SlingContext context = new SlingContext();
    
    private Resource site1Page1;
    private Resource site2Page1;

    @Before
    public void setUp() {
        // content resources that form a deeper hierarchy
        context.build()
            .resource("/content/tenant1", PROPERTY_CONFIG_REF, "/conf/tenant1")
            .resource("/content/tenant1/region1", PROPERTY_CONFIG_REF, "/conf/tenant1/region1")
            .resource("/content/tenant1/region1/site1", PROPERTY_CONFIG_REF, "/conf/tenant1/region1/site1")
            .resource("/content/tenant1/region1/site2", PROPERTY_CONFIG_REF, "/conf/tenant1/region1/site2");
        site1Page1 = context.create().resource("/content/tenant1/region1/site1/page1");
        site2Page1 = context.create().resource("/content/tenant1/region1/site2/page1");
    }

    @Test
    public void testFindContextPaths() {
        ContextPathStrategy underTest = context.registerInjectActivateService(new DefaultContextPathStrategy());

        assertThat(toResourceIterator(underTest.findContextResources(site1Page1)), ResourceIteratorMatchers.paths( 
                "/content/tenant1/region1/site1",
                "/content/tenant1/region1",
                "/content/tenant1"));
        assertThat(ImmutableList.copyOf(toConfigRefIterator(underTest.findContextResources(site1Page1))), Matchers.contains( 
                "/conf/tenant1/region1/site1",
                "/conf/tenant1/region1",
                "/conf/tenant1"));

        assertThat(toResourceIterator(underTest.findContextResources(site2Page1)), ResourceIteratorMatchers.paths(
                "/content/tenant1/region1/site2",
                "/content/tenant1/region1",
                "/content/tenant1"));
        assertThat(ImmutableList.copyOf(toConfigRefIterator(underTest.findContextResources(site2Page1))), Matchers.contains( 
                "/conf/tenant1/region1/site2",
                "/conf/tenant1/region1",
                "/conf/tenant1"));
    }

    @Test
    public void testDisabled() {
        ContextPathStrategy underTest = context.registerInjectActivateService(new DefaultContextPathStrategy(),
                "enabled", false);

        assertFalse(underTest.findContextResources(site1Page1).hasNext());
        assertFalse(underTest.findContextResources(site2Page1).hasNext());
    }

    @Test
    public void testConfigRefResourceNames() {
        ContextPathStrategy underTest = context.registerInjectActivateService(new DefaultContextPathStrategy(),
                "configRefResourceNames", new String[] { "jcr:content" });

        context.build()
            .resource("/content/tenant1/region1/jcr:content", PROPERTY_CONFIG_REF, "/conf/tenant1/region1");
        
        assertThat(toResourceIterator(underTest.findContextResources(site1Page1)), ResourceIteratorMatchers.paths( 
                "/content/tenant1/region1"));
        assertThat(ImmutableList.copyOf(toConfigRefIterator(underTest.findContextResources(site1Page1))), Matchers.contains( 
                "/conf/tenant1/region1"));

        assertThat(toResourceIterator(underTest.findContextResources(site2Page1)), ResourceIteratorMatchers.paths(
                "/content/tenant1/region1"));
        assertThat(ImmutableList.copyOf(toConfigRefIterator(underTest.findContextResources(site2Page1))), Matchers.contains( 
                "/conf/tenant1/region1"));
    }

}
