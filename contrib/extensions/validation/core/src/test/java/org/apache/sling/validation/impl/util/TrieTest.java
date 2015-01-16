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

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class TrieTest {

    private Trie<String> dictionary;

    @Before
    public void setUp() {
        dictionary = new Trie<String>();
        dictionary.insert("/apps/example", "/apps/example");
        dictionary.insert("/apps/examples/node/jcr:content", "/apps/examples/node/jcr:content");
        dictionary.insert("/apps/examples/node/jcr:content/nodes", "/apps/examples/node/jcr:content/nodes");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInsertNullValue() {
        dictionary.insert(null, "invalid");
    }
    
    @Test
    public void testOverwriteRootNode() {
        TrieNode<String> node = dictionary.getElement("invalid key");
        assertThat(node.getValue(), Matchers.equalTo(dictionary.ROOT.getValue()));
        dictionary.insert("", "new root value");
        node = dictionary.getElement("invalid key");
        assertThat(node.getValue(), Matchers.equalTo("new root value"));
    }
    
    @Test
    public void testLongestMatchingKey() throws Exception {
        TrieNode<String> node;
        node = dictionary.getElementForLongestMatchingKey("/apps/examples/node/jcr:content/nodes/1");
        assertThat(node.getValue(), Matchers.equalTo("/apps/examples/node/jcr:content/nodes"));

        node = dictionary.getElementForLongestMatchingKey("/apps/example/node/jcr:content/nodes/1");
        assertThat(node.getValue(), Matchers.equalTo("/apps/example"));

        node = dictionary.getElementForLongestMatchingKey("/libs");
        assertThat(node, Matchers.equalTo(dictionary.ROOT));
        
        // test empty key (not allowed!!!)
        dictionary.insert("", "emptyKey");
        node = dictionary.getElementForLongestMatchingKey("/apps/test");
        assertThat(node.getValue(), Matchers.equalTo("emptyKey"));
    }

    @Test
    public void testExactKey() {
        TrieNode<String> node;
        node = dictionary.getElement("/apps/examples/node/jcr:content/nodes");
        assertThat(node.getValue(), Matchers.equalTo("/apps/examples/node/jcr:content/nodes"));

        node = dictionary.getElement("/apps/example");
        assertThat(node.getValue(), Matchers.equalTo("/apps/example"));

        node = dictionary.getElement("/libs");
        assertThat(node, Matchers.equalTo(dictionary.ROOT));
    }

}
