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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Test;

public class PathUtilTest {

    @Test
    public void testGetChildPathPattern() {
        Pattern pattern = PathUtil.getChildPathPattern("/my/path");
        
        assertFalse(pattern.matcher("/my/path").matches());
        assertTrue(pattern.matcher("/my/path/child1").matches());
        assertTrue(pattern.matcher("/my/path/child2").matches());
        assertFalse(pattern.matcher("/my/path/child1/subchild1").matches());
        assertFalse(pattern.matcher("/my/path/child1/subchild1/subchild2").matches());
        assertFalse(pattern.matcher("/my/sibling").matches());
        assertFalse(pattern.matcher("/other").matches());
    }

    @Test
    public void testGetDescendantPathPattern() {
        Pattern pattern = PathUtil.getSameOrDescendantPathPattern("/my/path");
        
        assertTrue(pattern.matcher("/my/path").matches());
        assertTrue(pattern.matcher("/my/path/child1").matches());
        assertTrue(pattern.matcher("/my/path/child2").matches());
        assertTrue(pattern.matcher("/my/path/child1/subchild1").matches());
        assertTrue(pattern.matcher("/my/path/child1/subchild1/subchild2").matches());
        assertFalse(pattern.matcher("/my/sibling").matches());
        assertFalse(pattern.matcher("/other").matches());
    }

}
