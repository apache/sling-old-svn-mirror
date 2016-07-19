/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.auth.requirement.impl;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class UtilsTest {

    @Test(expected = NullPointerException.class)
    public void getValidPathsForNull() throws Exception {
        Utils.getValidPaths(null);
    }

    @Test
    public void getValidPathsForEmptyArray() throws Exception {
        assertArrayEquals(new String[0], Utils.getValidPaths(new String[0]));
    }

    @Test
    public void getValidPathsForAbsolutePaths() throws Exception {
        assertArrayEquals(new String[]{"/"}, Utils.getValidPaths(new String[]{"/"}));
        assertArrayEquals(new String[]{"/content"}, Utils.getValidPaths(new String[]{"/content"}));
        assertArrayEquals(new String[]{"/a", "/a/b"}, Utils.getValidPaths(new String[]{"/a", "/a/b"}));
        assertArrayEquals(new String[]{"/", "/a", "/a/b"}, Utils.getValidPaths(new String[]{"/", "/a", "/a/b"}));
    }

    @Test
    public void getValidPathsForRelativePaths() throws Exception {
        assertArrayEquals(new String[0], Utils.getValidPaths(new String[]{"a"}));
        assertArrayEquals(new String[0], Utils.getValidPaths(new String[]{"a/b"}));
        assertArrayEquals(new String[0], Utils.getValidPaths(new String[]{".", "a", "../a/b", "a/b", "a/./b"}));
    }

    @Test
    public void getValidPathsIncludesNull() throws Exception {
        assertArrayEquals(new String[0], Utils.getValidPaths(new String[]{null}));
        assertArrayEquals(new String[] {"/a", "/"}, Utils.getValidPaths(new String[]{"/a", null, "/"}));
    }


    @Test
    public void getValidPathsIncludesEmpty() throws Exception {
        assertArrayEquals(new String[0], Utils.getValidPaths(new String[]{""}));
        assertArrayEquals(new String[] {"/a", "/"}, Utils.getValidPaths(new String[]{"/a", "", "/"}));
    }

    @Test
    public void getValidPathsIncludesInvalid() throws Exception {
        assertArrayEquals(new String[0], Utils.getValidPaths(new String[]{"/a/"}));
        assertArrayEquals(new String[0], Utils.getValidPaths(new String[]{"//"}));
    }

    @Test
    public void getValidPathsIncludesDuplicate() throws Exception {
        assertArrayEquals(new String[] {"/a"}, Utils.getValidPaths(new String[]{"/a", "/a"}));
    }

    @Test(expected = NullPointerException.class)
    public void getCommonAncestorForNull() throws Exception {
        Utils.getCommonAncestor(null);
    }

    @Test
    public void getCommonAncestorForEmptyArray() throws Exception {
        assertEquals("/", Utils.getCommonAncestor(new String[0]));
    }

    @Test
    public void getCommonAncestorSingleElement() throws Exception {
        assertEquals("/content", Utils.getCommonAncestor(new String[]{"/content"}));
    }

    @Test
    public void getCommonAncestor() throws Exception {
        assertEquals("/a", Utils.getCommonAncestor(new String[]{"/a", "/a/b", "/a/c/d"}));
        assertEquals("/a", Utils.getCommonAncestor(new String[]{"/a/b", "/a/b/c/d", "/a"}));
        assertEquals("/a", Utils.getCommonAncestor(new String[]{"/a/b/c/d", "/a/b", "/a"}));
        assertEquals("/a", Utils.getCommonAncestor(new String[]{"/a/b/c/d", "/a", "/a/b",}));

        assertEquals("/a/b", Utils.getCommonAncestor(new String[]{"/a/b", "/a/b/c", "/a/b/c/d"}));

        assertEquals("/", Utils.getCommonAncestor(new String[]{"/a", "/a/b", "/a/b/c/d", "/content", "/an/other/content"}));
        assertEquals("/", Utils.getCommonAncestor(new String[]{"/a/b/c/d", "/content", "/an/other/content", "/a", "/a/b"}));
    }

    @Test
    public void getCommonAncestorIncludesNull() throws Exception {
        assertEquals("/", Utils.getCommonAncestor(new String[]{null}));
        assertEquals("/a/b", Utils.getCommonAncestor(new String[]{"/a/b", null, "/a/b/c"}));
        assertEquals("/", Utils.getCommonAncestor(new String[]{"/a/b", null, "/content"}));

    }

    @Test
    public void getCommonAncestorIncludesEmptyString() throws Exception {
        assertEquals("/", Utils.getCommonAncestor(new String[]{""}));
        assertEquals("/", Utils.getCommonAncestor(new String[]{"/a/b", "", "/content"}));
        assertEquals("/a/b", Utils.getCommonAncestor(new String[]{"/a/b", "", "/a/b/c"}));
    }

}