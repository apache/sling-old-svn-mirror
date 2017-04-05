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
package org.apache.sling.caconfig.resource.impl.util;

import static org.apache.sling.caconfig.resource.impl.util.ContextResourceTestUtil.toResourceIterator;
import static org.junit.Assert.assertThat;

import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.caconfig.resource.spi.ContextResource;
import org.apache.sling.hamcrest.ResourceIteratorMatchers;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ResourceEliminateDuplicatesIteratorTest {
    
    @Rule
    public SlingContext context = new SlingContext();

    @SuppressWarnings("unchecked")
    @Test
    public void testIterator() {
        context.build()
            .resource("/content/a")
            .resource("/content/a/b")
            .resource("/content/a/b/c");
        
        ResourceResolver rr = context.resourceResolver();
        List<ContextResource> list = ImmutableList.of(
                new ContextResource(rr.getResource("/content/a"), "/conf/a"), 
                new ContextResource(rr.getResource("/content/a/b"), "/conf/a/b"),
                new ContextResource(rr.getResource("/content/a"), "/conf/a"),
                new ContextResource(rr.getResource("/content/a"), null),
                new ContextResource(rr.getResource("/content/a/b/c"), "/conf/a/b"));
        
        Iterator<Resource> result = toResourceIterator(new ResourceEliminateDuplicatesIterator(list.iterator()));
        assertThat(result, ResourceIteratorMatchers.paths(
                "/content/a",
                "/content/a/b",
                "/content/a",
                "/content/a/b/c"));
    }

}
