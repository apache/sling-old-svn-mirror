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
package org.apache.sling.i18n.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Helper class for creating test data in a generic way.
 */
public class Message {
    public String key;
    public String message;
    public boolean useNodeName;
    public String path;

    public Message(String path, String key, String message, boolean useNodeName) {
        this.path = path;
        this.key = key;
        this.message = message;
        this.useNodeName = useNodeName;
    }

    private static int nodeNameCounter = 0;

    public void add(Node languageNode) throws RepositoryException {
        Node node = languageNode;
        String[] pathElements = path.split("/");
        for (String pathStep : pathElements) {
            if (pathStep != null && pathStep.length() > 0) {
                node = node.addNode(pathStep, "nt:folder");
            }
        }
        if (useNodeName) {
            node = node.addNode(key, "sling:MessageEntry");
        } else {
            node = node.addNode("node" + nodeNameCounter, "sling:MessageEntry");
            nodeNameCounter++;
            node.setProperty("sling:key", key);
        }
        node.setProperty("sling:message", message);
    }
}

