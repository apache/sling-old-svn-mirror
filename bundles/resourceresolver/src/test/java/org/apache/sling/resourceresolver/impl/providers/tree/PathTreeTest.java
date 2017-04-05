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
package org.apache.sling.resourceresolver.impl.providers.tree;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class PathTreeTest {

    private PathTree<Pathable> tree;

    @Before
    public void createTree() {

        Pathable root = new StringPath("/");
        Pathable libs = new StringPath("/libs");
        Pathable libsSling = new StringPath("/libs/sling");
        Pathable apps = new StringPath("/apps");

        tree = new PathTree<Pathable>(asList(root, libs, libsSling, apps));
    }

    @Test
    public void bestMatchForChildNode() {

        assertPathHasBestMatch("/libs", "/libs");
        assertPathHasExactMatch("/libs");
    }
    
    private void assertPathHasBestMatch(String path, String expectedNode) {
        
        assertThat(tree.getBestMatchingNode(path).getValue().getPath(), equalTo(expectedNode));
    }

    private void assertPathHasExactMatch(String path) {
        
        assertThat(tree.getNode(path).getValue().getPath(), equalTo(path));
    }

    private void assertPathDoesNotHaveExactMatch(String path) {
        
        assertThat(tree.getNode(path), nullValue());
    }

    @Test
    public void bestMatchForChildNodeNested() {

        assertPathHasBestMatch("/apps/sling", "/apps");
        assertPathDoesNotHaveExactMatch("/apps/sling");
    }

    @Test
    public void bestMatchForChildNodeDeeplyNested() {
        
        assertPathHasBestMatch("/libs/sling/base/install", "/libs/sling");
        assertPathDoesNotHaveExactMatch("/libs/sling/base/install");
    }

    @Test
    public void bestMatchRootNodeFallback() {

        assertPathHasBestMatch("/system", "/");
        assertPathDoesNotHaveExactMatch("/system");
    }
    
    @Test
    public void bestMatchForInvalidPaths() {
        
        for ( String invalid : new String[] { null, "", "not/absolute/path"} ) {
            assertThat("getBestMatchingNode(" + invalid + ")", tree.getBestMatchingNode(invalid), Matchers.nullValue());
            assertThat("getNode(" + invalid + ")", tree.getNode(invalid), Matchers.nullValue());
        }
    }
    
    static class StringPath implements Pathable {

        private final String path;

        public StringPath(String path) {
            this.path = path;
        }

        @Override
        public String getPath() {
            return path;
        }
    }
}