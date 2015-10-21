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

import static org.apache.commons.lang.StringUtils.split;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PathTree<T extends Pathable> {

    private Node<T> root;

    public PathTree(List<T> values) {
        this.root = new Node<T>();
        for (T v : values) {
            addNewValue(v);
        }
    }

    private void addNewValue(T value) {
        Node<T> node = root;
        for (String segment : split(value.getPath(), '/')) {
            if (node.hasChild(segment)) {
                node = node.getChild(segment);
            } else {
                node = node.addChild(segment);
            }
        }
        node.setValue(value);
    }

    public List<T> getMatchingNodes(String path) {
        if (path == null || path.isEmpty() || path.charAt(0) != '/') {
            return Collections.emptyList();
        }
        List<T> values = new ArrayList<T>();

        if (root.getValue() != null) {
            values.add(root.getValue());
        }

        Node<T> node = root;
        Iterator<String> it = new PathSegmentIterator(path, 1);
        while (it.hasNext()) {
            String segment = it.next();
            node = node.getChild(segment);
            if (node == null) {
                break;
            } else {
                if (node.getValue() != null) {
                    values.add(node.getValue());
                }
            }
        }
        return values;
    }

    public Node<T> getNode(String path) {
        if (path == null || path.isEmpty() || path.charAt(0) != '/') {
            return null;
        }
        Node<T> node = root;
        Iterator<String> it = new PathSegmentIterator(path, 1);
        while (it.hasNext()) {
            String segment = it.next();
            node = node.getChild(segment);
            if (node == null) {
                return null;
            }
        }
        return node;
    }
}
