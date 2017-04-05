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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class PathParentExpandIteratorTest {

    @Test
    public void testIterator() {
        List<String> list = ImmutableList.of(
                "/conf/a/b/c", 
                "/conf/a/b",
                "/conf/x/y/z");
        
        List<String> result = ImmutableList.copyOf(new PathParentExpandIterator("/conf", list.iterator()));
        assertEquals(ImmutableList.of(
                "/conf/a/b/c",
                "/conf/a/b",
                "/conf/a",
                "/conf/a/b",
                "/conf/a",
                "/conf/x/y/z",
                "/conf/x/y",
                "/conf/x"), result);
    }

}
