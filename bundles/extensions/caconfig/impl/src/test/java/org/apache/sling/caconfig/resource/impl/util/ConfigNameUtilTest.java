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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ConfigNameUtilTest {

    @Test
    public void testIsValid() {
        assertTrue(ConfigNameUtil.isValid("a"));
        assertTrue(ConfigNameUtil.isValid("a/b"));
        assertTrue(ConfigNameUtil.isValid("a/b/c"));
        assertTrue(ConfigNameUtil.isValid("a/jcr:content/b/c"));

        assertTrue(ConfigNameUtil.isValid(ImmutableList.<String>of()));
        assertTrue(ConfigNameUtil.isValid(ImmutableList.of("a")));
        assertTrue(ConfigNameUtil.isValid(ImmutableList.of("a", "a/b", "a/b/c")));
        
        assertFalse(ConfigNameUtil.isValid((String)null));
        assertFalse(ConfigNameUtil.isValid(""));
        assertFalse(ConfigNameUtil.isValid("/a"));
        assertFalse(ConfigNameUtil.isValid("/a/b/c"));
        assertFalse(ConfigNameUtil.isValid("a/b/../c"));

        assertFalse(ConfigNameUtil.isValid((Collection<String>)null));
        assertFalse(ConfigNameUtil.isValid(ImmutableList.of("a", "/a")));
    }

    @Test
    public void testGetAllPartialConfigNameVariations() {
        assertArrayEquals(new String[0], ConfigNameUtil.getAllPartialConfigNameVariations(""));
        assertArrayEquals(new String[0], ConfigNameUtil.getAllPartialConfigNameVariations("a"));
        assertArrayEquals(new String[] {"a"}, ConfigNameUtil.getAllPartialConfigNameVariations("a/b"));
        assertArrayEquals(new String[] {"a","a/b"}, ConfigNameUtil.getAllPartialConfigNameVariations("a/b/c"));
        assertArrayEquals(new String[] {"a","a/b","a/b/c"}, ConfigNameUtil.getAllPartialConfigNameVariations("a/b/c/d"));
    }

}
