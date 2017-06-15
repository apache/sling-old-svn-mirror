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

import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_COLLECTION_INHERIT;
import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_REF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.management.impl.ContextPathStrategyMultiplexerImpl;
import org.apache.sling.caconfig.resource.spi.ConfigurationResourceResolvingStrategy;
import org.apache.sling.hamcrest.ResourceCollectionMatchers;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DefaultConfigurationResourceResolvingStrategyTest {

    private static final String BUCKET = "sling:test";
    private static final Collection<String> BUCKETS = Collections.singleton(BUCKET);

    @Rule
    public SlingContext context = new SlingContext();

    private Resource site1Page1;
    private Resource site2Page1;

    private Resource subPage;
    private Resource deepPage;

    @Before
    public void setUp() {
        context.registerInjectActivateService(new DefaultContextPathStrategy());
        context.registerInjectActivateService(new ContextPathStrategyMultiplexerImpl());

        // content resources
        context.build()
            .resource("/content/site1", PROPERTY_CONFIG_REF, "/conf/site1")
            .resource("/content/site2", PROPERTY_CONFIG_REF, "/conf/site2")
            .resource("/content/mainsite", PROPERTY_CONFIG_REF, "/conf/main")
            .resource("/content/mainsite/sub", PROPERTY_CONFIG_REF, "sub")
            .resource("/content/mainsite/sub/some/where/deep", PROPERTY_CONFIG_REF, "sub/deep");
        site1Page1 = context.create().resource("/content/site1/page1");
        site2Page1 = context.create().resource("/content/site2/page1");
        subPage = context.create().resource("/content/mainsite/sub/page1");
        deepPage = context.create().resource("/content/mainsite/sub/some/where/deep/page1");
    }

    @Test
    public void testGetResource() {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());

        // build config resources
        context.build()
            .resource("/conf/site1/sling:test/test")
            .resource("/libs/conf/sling:test/test");

        assertEquals("/conf/site1/sling:test/test", underTest.getResource(site1Page1, BUCKETS, "test").getPath());
        assertEquals("/libs/conf/sling:test/test", underTest.getResource(site2Page1, BUCKETS, "test").getPath());
    }

    @Test
    public void testRelativeConfPropery() {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());

        // build config resources
        context.build()
            .resource("/conf/main/sub/sling:test/test")
            .resource("/conf/main/sub/deep/sling:test/test");

        assertEquals("/conf/main/sub/sling:test/test", underTest.getResource(subPage, BUCKETS, "test").getPath());
        assertEquals("/conf/main/sub/deep/sling:test/test", underTest.getResource(deepPage, BUCKETS, "test").getPath());
    }

    /**
     * Default resource inheritance without customizing inheritance.
     * => no resource list merging.
     */
    @Test
    public void testGetResourceCollection_NoInheritProps() {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());

        // build config resources
        context.build()
            .resource("/conf/site1/sling:test/feature/c")
            .resource("/conf/site2/sling:test/feature/c")
            .resource("/conf/site2/sling:test/feature/d")
            .resource("/apps/conf/sling:test/feature/a")
            .resource("/libs/conf/sling:test/feature/b");

        assertThat(underTest.getResourceCollection(site1Page1, BUCKETS, "feature"), ResourceCollectionMatchers.paths(
                "/conf/site1/sling:test/feature/c"));

        assertThat(underTest.getResourceCollection(site2Page1, BUCKETS, "feature"), ResourceCollectionMatchers.paths(
                "/conf/site2/sling:test/feature/c",
                "/conf/site2/sling:test/feature/d"));
    }

    /**
     * Default resource inheritance without customizing inheritance, but with now resources inner-most context.
     * => inherit from next context level.
     */
    @Test
    public void testGetResourceCollection_NoInheritProps_InheritParent() {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());

        // build config resources
        context.build()
            .resource("/apps/conf/sling:test/feature/a")
            .resource("/libs/conf/sling:test/feature/a")
            .resource("/libs/conf/sling:test/feature/b");

        assertThat(underTest.getResourceCollection(site1Page1, BUCKETS, "feature"), ResourceCollectionMatchers.paths(
                "/apps/conf/sling:test/feature/a"));

        assertThat(underTest.getResourceCollection(site2Page1, BUCKETS, "feature"), ResourceCollectionMatchers.paths(
                "/apps/conf/sling:test/feature/a"));
    }

    /**
     * Resource inheritance with enabling list merging on inner-most context level.
     * => merge resource lists from next level
     */
    @Test
    public void testGetResourceCollection_Inherit1Level() {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());

        // build config resources
        context.build()
            .resource("/conf/site1/sling:test/feature", PROPERTY_CONFIG_COLLECTION_INHERIT, true)
            .resource("/conf/site1/sling:test/feature/c")
            .resource("/conf/site2/sling:test/feature", PROPERTY_CONFIG_COLLECTION_INHERIT, true)
            .resource("/conf/site2/sling:test/feature/c")
            .resource("/conf/site2/sling:test/feature/d")
            .resource("/apps/conf/sling:test/feature/a")
            .resource("/libs/conf/sling:test/feature/b");

        assertThat(underTest.getResourceCollection(site1Page1, BUCKETS, "feature"), ResourceCollectionMatchers.paths(
                "/conf/site1/sling:test/feature/c",
                "/apps/conf/sling:test/feature/a"));

        assertThat(underTest.getResourceCollection(site2Page1, BUCKETS, "feature"), ResourceCollectionMatchers.paths(
                "/conf/site2/sling:test/feature/c",
                "/conf/site2/sling:test/feature/d",
                "/apps/conf/sling:test/feature/a"));
    }

    /**
     * Resource inheritance with enabling list merging on all levels.
     * => merge resource lists from all levels
     */
    @Test
    public void testGetResourceCollection_InheritMultipleLevels() {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());

        // build config resources
        context.build()
            .resource("/conf/site1/sling:test/feature", PROPERTY_CONFIG_COLLECTION_INHERIT, true)
                .resource("c")
            .resource("/conf/site2/sling:test/feature", PROPERTY_CONFIG_COLLECTION_INHERIT, true)
                .siblingsMode()
                .resource("c")
                .resource("d")
            .resource("/apps/conf/sling:test/feature", PROPERTY_CONFIG_COLLECTION_INHERIT, true)
                .resource("a")
            .resource("/libs/conf/sling:test/feature")
                .resource("b");

        assertThat(underTest.getResourceCollection(site1Page1, BUCKETS, "feature"), ResourceCollectionMatchers.paths(
                "/conf/site1/sling:test/feature/c",
                "/apps/conf/sling:test/feature/a",
                "/libs/conf/sling:test/feature/b"));

        assertThat(underTest.getResourceCollection(site2Page1, BUCKETS, "feature"), ResourceCollectionMatchers.paths(
                "/conf/site2/sling:test/feature/c",
                "/conf/site2/sling:test/feature/d",
                "/apps/conf/sling:test/feature/a",
                "/libs/conf/sling:test/feature/b"));
    }

    /**
     * Resource inheritance with enabling list merging on a parent context level.
     * => no inheritance takes place
     */
    @Test
    public void testGetResourceCollection_PropsParent() {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());

        // build config resources
        context.build()
            .resource("/conf/site1/sling:test/feature/c")
            .resource("/conf/site2/sling:test/feature/c")
            .resource("/conf/site2/sling:test/feature/d")
            .resource("/apps/conf/sling:test/feature/a")
            .resource("/libs/conf/sling:test/feature", PROPERTY_CONFIG_COLLECTION_INHERIT, true).resource("b");

        assertThat(underTest.getResourceCollection(site1Page1, BUCKETS, "feature"), ResourceCollectionMatchers.paths(
                "/conf/site1/sling:test/feature/c"));

        assertThat(underTest.getResourceCollection(site2Page1, BUCKETS, "feature"), ResourceCollectionMatchers.paths(
                "/conf/site2/sling:test/feature/c",
                "/conf/site2/sling:test/feature/d"));
    }

    /**
     * Ensure jcr:content nodes are not included in resource collection.
     */
    @Test
    public void testGetResourceCollection_SkipJcrContent() {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());

        // build config resources
        context.build()
            .resource("/conf/site1/sling:test/feature/a")
            .resource("/conf/site1/sling:test/feature/b")
            .resource("/conf/site2/sling:test/feature/jcr:content");

        assertThat(underTest.getResourceCollection(site1Page1, BUCKETS, "feature"), ResourceCollectionMatchers.paths(
                "/conf/site1/sling:test/feature/a",
                "/conf/site1/sling:test/feature/b"));
    }

    @Test
    public void testGetResourcePath() throws Exception {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());
        assertEquals("/conf/site1/sling:test/test", underTest.getResourcePath(site1Page1, BUCKET, "test"));
    }

    @Test
    public void testGetResourceCollectionParentPath() throws Exception {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());
        assertEquals("/conf/site1/sling:test/feature", underTest.getResourceCollectionParentPath(site1Page1, BUCKET, "feature"));
    }

    @Test
    public void testDisabled() {
        ConfigurationResourceResolvingStrategy underTest = context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy(),
                "enabled", false);

        assertNull(underTest.getResource(site1Page1, BUCKETS, "test"));
        assertNull(underTest.getResourceCollection(site1Page1, BUCKETS, "feature"));
        assertNull(underTest.getResourcePath(site1Page1, BUCKET, "test"));
        assertNull(underTest.getResourceCollectionParentPath(site1Page1, BUCKET, "feature"));
    }

}
