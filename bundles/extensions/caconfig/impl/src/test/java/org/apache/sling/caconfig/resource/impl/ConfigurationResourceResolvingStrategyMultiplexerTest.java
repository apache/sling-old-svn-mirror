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
package org.apache.sling.caconfig.resource.impl;

import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_COLLECTION_INHERIT;
import static org.apache.sling.caconfig.resource.impl.def.ConfigurationResourceNameConstants.PROPERTY_CONFIG_REF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.management.impl.ContextPathStrategyMultiplexerImpl;
import org.apache.sling.caconfig.resource.impl.def.DefaultConfigurationResourceResolvingStrategy;
import org.apache.sling.caconfig.resource.impl.def.DefaultContextPathStrategy;
import org.apache.sling.caconfig.resource.spi.ConfigurationResourceResolvingStrategy;
import org.apache.sling.hamcrest.ResourceCollectionMatchers;
import org.apache.sling.hamcrest.ResourceMatchers;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Constants;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

public class ConfigurationResourceResolvingStrategyMultiplexerTest {

    private static final String BUCKET = "sling:test";
    private static final Collection<String> BUCKETS = Collections.singleton(BUCKET);
    
    @Rule
    public SlingContext context = new SlingContext();
    
    private ConfigurationResourceResolvingStrategyMultiplexer underTest;
    
    private Resource site1Page1;

    @Before
    public void setUp() {
        context.registerInjectActivateService(new DefaultContextPathStrategy());
        context.registerInjectActivateService(new ContextPathStrategyMultiplexerImpl());
        underTest = context.registerInjectActivateService(new ConfigurationResourceResolvingStrategyMultiplexer());

        // content resources
        context.build()
            .resource("/content/site1", PROPERTY_CONFIG_REF, "/conf/site1")
            .resource("/content/site2", PROPERTY_CONFIG_REF, "/conf/site2");
        site1Page1 = context.create().resource("/content/site1/page1");
        
        // configuration
        context.build()
            .resource("/conf/site1/sling:test/test")
            .resource("/conf/site1/sling:test/feature", PROPERTY_CONFIG_COLLECTION_INHERIT, true)
                .resource("c")
            .resource("/conf/site2/sling:test/feature", PROPERTY_CONFIG_COLLECTION_INHERIT, true)
                .siblingsMode()
                .resource("c")
                .resource("d")
            .resource("/apps/conf/sling:test/feature", PROPERTY_CONFIG_COLLECTION_INHERIT, true)
                .resource("a")
            .resource("/libs/conf/sling:test/test")
            .resource("/libs/conf/sling:test/feature")
                .resource("b");
    }
    
    @Test
    public void testWithNoStrategies() {
        assertNull(underTest.getResource(site1Page1, BUCKETS, "test"));
        assertNull(underTest.getResourceCollection(site1Page1, BUCKETS, "feature"));

        assertNull(underTest.getResourceInheritanceChain(site1Page1, BUCKETS, "test"));
        assertNull(underTest.getResourceCollectionInheritanceChain(site1Page1, BUCKETS, "feature"));

        assertNull(underTest.getResourcePath(site1Page1, BUCKET, "test"));
        assertNull(underTest.getResourceCollectionParentPath(site1Page1, BUCKET, "feature"));
    }

    @Test
    public void testWithDefaultStrategy() {
        context.registerInjectActivateService(new DefaultConfigurationResourceResolvingStrategy());

        assertThat(underTest.getResource(site1Page1, BUCKETS, "test"), ResourceMatchers.path("/conf/site1/sling:test/test"));
        assertThat(underTest.getResourceCollection(site1Page1, BUCKETS, "feature"), ResourceCollectionMatchers.paths( 
                "/conf/site1/sling:test/feature/c",
                "/apps/conf/sling:test/feature/a", 
                "/libs/conf/sling:test/feature/b"));

        assertThat(first(underTest.getResourceInheritanceChain(site1Page1, BUCKETS, "test")), ResourceMatchers.path("/conf/site1/sling:test/test"));
        assertThat(first(underTest.getResourceCollectionInheritanceChain(site1Page1, BUCKETS, "feature")), ResourceCollectionMatchers.paths( 
                "/conf/site1/sling:test/feature/c",
                "/apps/conf/sling:test/feature/a", 
                "/libs/conf/sling:test/feature/b"));

        assertEquals("/conf/site1/sling:test/test", underTest.getResourcePath(site1Page1, BUCKET, "test"));
        assertEquals("/conf/site1/sling:test/feature", underTest.getResourceCollectionParentPath(site1Page1, BUCKET, "feature"));
    }
    
    @Test
    public void testMultipleStrategies() {
        
        // strategy 1
        context.registerService(ConfigurationResourceResolvingStrategy.class, new ConfigurationResourceResolvingStrategy() {
            @Override
            public Resource getResource(Resource resource, Collection<String> bucketNames, String configName) {
                return context.resourceResolver().getResource("/conf/site1/sling:test/test");
            }
            @Override
            public Collection<Resource> getResourceCollection(Resource resource, Collection<String> bucketNames, String configName) {
                return ImmutableList.copyOf(context.resourceResolver().getResource("/conf/site1/sling:test/feature").listChildren());
            }
            @Override
            public Iterator<Resource> getResourceInheritanceChain(Resource resource, Collection<String> bucketNames, String configName) {
                return Iterators.singletonIterator(getResource(resource, bucketNames, configName));
            }
            @Override
            public Collection<Iterator<Resource>> getResourceCollectionInheritanceChain(Resource resource,
                    Collection<String> bucketNames, String configName) {
                return Collections2.transform(getResourceCollection(resource, bucketNames, configName), new Function<Resource, Iterator<Resource>>() {
                    @Override
                    public Iterator<Resource> apply(Resource input) {
                        return Iterators.singletonIterator(input);
                    }
                });
            }
            @Override
            public String getResourcePath(Resource resource, String bucketName, String configName) {
                return "/conf/site1/sling:test/test";
            }
            @Override
            public String getResourceCollectionParentPath(Resource resource, String bucketName, String configName) {
                return "/conf/site1/sling:test/feature";
            }
        }, Constants.SERVICE_RANKING, 2000);
        
        // strategy 2
        context.registerService(ConfigurationResourceResolvingStrategy.class, new ConfigurationResourceResolvingStrategy() {
            @Override
            public Resource getResource(Resource resource, Collection<String> bucketNames, String configName) {
                return context.resourceResolver().getResource("/libs/conf/sling:test/test");
            }
            @Override
            public Collection<Resource> getResourceCollection(Resource resource, Collection<String> bucketNames, String configName) {
                return ImmutableList.copyOf(context.resourceResolver().getResource("/libs/conf/sling:test/feature").listChildren());
            }
            @Override
            public Iterator<Resource> getResourceInheritanceChain(Resource resource, Collection<String> bucketNames, String configName) {
                return Iterators.singletonIterator(getResource(resource, bucketNames, configName));
            }
            @Override
            public Collection<Iterator<Resource>> getResourceCollectionInheritanceChain(Resource resource,
                    Collection<String> bucketNames, String configName) {
                return Collections2.transform(getResourceCollection(resource, bucketNames, configName), new Function<Resource, Iterator<Resource>>() {
                    @Override
                    public Iterator<Resource> apply(Resource input) {
                        return Iterators.singletonIterator(input);
                    }
                });
            }
            @Override
            public String getResourcePath(Resource resource, String bucketName, String configName) {
                return null;
            }
            @Override
            public String getResourceCollectionParentPath(Resource resource, String bucketName, String configName) {
                return null;
            }
        }, Constants.SERVICE_RANKING, 1000);
        
        assertThat(underTest.getResource(site1Page1, BUCKETS, "test"), ResourceMatchers.path("/conf/site1/sling:test/test"));
        assertThat(underTest.getResourceCollection(site1Page1, BUCKETS, "feature"), ResourceCollectionMatchers.paths( 
                "/conf/site1/sling:test/feature/c"));
        
        assertThat(first(underTest.getResourceInheritanceChain(site1Page1, BUCKETS, "test")), ResourceMatchers.path("/conf/site1/sling:test/test"));
        assertThat(first(underTest.getResourceCollectionInheritanceChain(site1Page1, BUCKETS, "feature")), ResourceCollectionMatchers.paths( 
                "/conf/site1/sling:test/feature/c"));
        
        assertEquals("/conf/site1/sling:test/test", underTest.getResourcePath(site1Page1, BUCKET, "test"));
        assertEquals("/conf/site1/sling:test/feature", underTest.getResourceCollectionParentPath(site1Page1, BUCKET, "feature"));
    }

    private Resource first(Iterator<Resource> resources) {
        if (resources.hasNext()) {
            return resources.next();
        }
        else {
            return null;
        }
    }
    
    private Collection<Resource> first(Collection<Iterator<Resource>> resources) {
        return Collections2.transform(underTest.getResourceCollectionInheritanceChain(site1Page1, BUCKETS, "feature"),
                new Function<Iterator<Resource>, Resource>() {
                @Override
                public Resource apply(Iterator<Resource> input) {
                    return input.next();
                }
            });
    }
    
}
