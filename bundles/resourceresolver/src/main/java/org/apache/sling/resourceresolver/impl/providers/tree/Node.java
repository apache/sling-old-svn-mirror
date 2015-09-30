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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Node<T> {

    private T value;

    private Map<String, Node<T>> children;

    public boolean hasChild(String name) {
        return children != null && children.containsKey(name);
    }

    public Node<T> getChild(String name) {
        if (children != null) {
            return children.get(name);
        } else {
            return null;
        }
    }

    Node<T> addChild(String name) {
        if (children == null) {
            children = new HashMap<String, Node<T>>();
        }
        Node<T> newNode = new Node<T>();
        children.put(name, newNode);
        return newNode;
    }

    void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public Map<String, Node<T>> getChildren() {
        if (children == null) {
            return Collections.emptyMap();
        } else {
            return children;
        }
    }
}