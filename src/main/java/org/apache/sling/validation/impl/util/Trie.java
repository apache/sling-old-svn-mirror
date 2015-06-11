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

import java.util.Map;

/**
 * Trie data structure used for storing objects using {@link String} keys that allows object retrieval using a longest matching key
 * mechanism.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Trie">Trie</a>
 */
public class Trie<T> {

    /**
     * The {@code ROOT} node of the Trie, initialised with the "null" character.
     */
    public final TrieNode<T> ROOT = new TrieNode<T>('\0');

    /**
     * Inserts an object {@link T} under the specified {@code key}.
     *
     * @param key   the key under which the object will be stored. If empty String, the value of the root node will be overwritten!
     * @param value the object to be stored
     */
    public void insert(String key, T value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null!");
        }
        int length = key.length();
        TrieNode<T> node = ROOT;
        for (int index = 0; index < length; index++) {
                Map<Character, TrieNode<T>> children = node.getChildren();
                char character = key.charAt(index);
                node = children.get(character);
                if (node == null) {
                    node = new TrieNode<T>(character);
                    children.put(character, node);
                }
        }
        node.setLeaf(true);
        node.setValue(value);
    }

    /**
     * Retrieves the {@link TrieNode} stored under the best matching key.
     *
     * @param key the key; if the key doesn't match with an existing key, the best matching key will be used for retrieval; if no match is
     *            found the {@link Trie#ROOT} node will be returned.
     * @return the {@link TrieNode} stored under the best matching key or the {@link Trie#ROOT} node if no match was found
     */
    public TrieNode<T> getElementForLongestMatchingKey(String key) {
        TrieNode<T> result = ROOT;
        if (key != null && !"".equals(key)) {
            int length = key.length();
            TrieNode<T> node = ROOT;
            for (int index = 0; index < length; index++) {
                char character = key.charAt(index);
                Map<Character, TrieNode<T>> children = node.getChildren();
                node = children.get(character);
                if (node != null) {
                    if (node.isLeaf()) {
                        result = node;
                    }
                } else {
                    break;
                }
            }

        }
        return result;
    }

    /**
     * Returns the {@link TrieNode} stored under the given {@code key}. If no element is stored under that key, the {@link Trie#ROOT} node
     * will be returned.
     * @param key the key
     * @return the {@link TrieNode} stored under the given key or the {@link Trie#ROOT} if no node is found under that {@code key}
     */
    public TrieNode<T> getElement(String key) {
        TrieNode<T> result = ROOT;
        TrieNode<T> node = null;
        boolean nodeExists = true;
        if (key != null && !"".equals(key)) {
            int length = key.length();
            node = ROOT;
            for (int index = 0; index < length; index++) {
                char character = key.charAt(index);
                Map<Character, TrieNode<T>> children = node.getChildren();
                node = children.get(character);
                if (node == null) {
                    nodeExists = false;
                    break;
                }
            }
        }
        if (nodeExists) {
            result = node;
        }
        return result;
    }
    
    /**
     * 
     * @return {@code true} if the trie is still empty, otherwise {@code false}
     */
    public boolean isEmpty() {
        return ROOT.getChildren().isEmpty();
    }
}
