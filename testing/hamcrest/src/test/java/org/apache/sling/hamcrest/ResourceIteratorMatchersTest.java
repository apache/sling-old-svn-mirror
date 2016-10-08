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
package org.apache.sling.hamcrest;

import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ResourceIteratorMatchersTest {

    @Rule
    public final SlingContext context = new SlingContext();
    
    private List<Resource> list;
    
    @Before
    public void setUp() {
        list = ImmutableList.of(
                context.create().resource("/content/1"),
                context.create().resource("/content/2"),
                context.create().resource("/content/3")
        );
    }
    
    @Test
    public void testMatch() {
        assertThat(list.iterator(), ResourceIteratorMatchers.paths(
                "/content/1",
                "/content/2",
                "/content/3"
        ));
    }

    @Test
    public void testMisMatch() {
        assertThat(list.iterator(), not(ResourceIteratorMatchers.paths(
                "/content/1",
                "/content/2",
                "/content/3",
                "/content/4"
        )));
        assertThat(list.iterator(), not(ResourceIteratorMatchers.paths(
                "/content/1",
                "/content/2"
        )));
        assertThat(list.iterator(), not(ResourceIteratorMatchers.paths(
                "/content/1",
                "/content/3",
                "/content/2"
        )));
    }

}
