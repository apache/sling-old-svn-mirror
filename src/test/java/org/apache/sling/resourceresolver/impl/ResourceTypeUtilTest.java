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
package org.apache.sling.resourceresolver.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ResourceTypeUtilTest {

    @Test public void testAreResourceTypesEqual() {
        assertTrue(ResourceTypeUtil.areResourceTypesEqual("some/type", "/apps/some/type", new String[]{"/apps/", "/libs/"}));
        assertTrue(ResourceTypeUtil.areResourceTypesEqual("/apps/some/type", "some/type", new String[]{"/apps/", "/libs/"}));
        assertTrue(ResourceTypeUtil.areResourceTypesEqual("/apps/some/type", "/apps/some/type", new String[]{"/apps/", "/libs/"}));
        assertTrue(ResourceTypeUtil.areResourceTypesEqual("some/type", "some/type", new String[]{"/apps/", "/libs/"}));
        assertTrue(ResourceTypeUtil.areResourceTypesEqual("/apps/some/type", "/libs/some/type", new String[]{"/apps/", "/libs/"}));
        assertFalse(ResourceTypeUtil.areResourceTypesEqual("/apps/some/type", "/libs/some/type", new String[]{}));
    }

    @Test public void testRelativizeResourceType() {
        assertEquals("relative/type", ResourceTypeUtil.relativizeResourceType("relative/type", new String[]{"/apps/", "/libs/"}));
        assertEquals("relative/type", ResourceTypeUtil.relativizeResourceType("/apps/relative/type", new String[]{"/apps/", "/libs/"}));
        assertEquals("relative/type", ResourceTypeUtil.relativizeResourceType("/libs/relative/type", new String[]{"/apps/", "/libs/"}));
        assertEquals("", ResourceTypeUtil.relativizeResourceType("/apps/", new String[]{"/apps/", "/libs/"}));
        assertEquals("/some/prefix/type", ResourceTypeUtil.relativizeResourceType("/some/prefix/type", new String[]{"/apps/", "/libs/"}));
    }
}
