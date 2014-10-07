/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.validation.impl.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TrieTest {

    private Trie<Object> dictionary;

    @Before
    public void setUp() {
        dictionary = new Trie<Object>();
        dictionary.insert("/apps/example", "/apps/example");
        dictionary.insert("/apps/examples/node/jcr:content", "/apps/examples/node/jcr:content");
        dictionary.insert("/apps/examples/node/jcr:content/nodes", "/apps/examples/node/jcr:content/nodes");
    }

    @Test
    public void testLongestMatchingKey() throws Exception {
        TrieNode<Object> node;
        node = dictionary.getElementForLongestMatchingKey("/apps/examples/node/jcr:content/nodes/1");
        assertTrue("/apps/examples/node/jcr:content/nodes".equals(node.getValue()));

        node = dictionary.getElementForLongestMatchingKey("/apps/example/node/jcr:content/nodes/1");
        assertTrue("/apps/example".equals(node.getValue()));

        node = dictionary.getElementForLongestMatchingKey("/libs");
        assertTrue(node.getValue() == null);
    }

    @Test
    public void testExactKey() {
        TrieNode<Object> node;

        node = dictionary.getElement("/apps/examples/node/jcr:content/nodes");
        assertTrue("/apps/examples/node/jcr:content/nodes".equals(node.getValue()));

        node = dictionary.getElement("/apps/example");
        assertTrue("/apps/example".equals(node.getValue()));

        node = dictionary.getElement("/libs");
        assertTrue(dictionary.ROOT.equals(node));
    }

}
