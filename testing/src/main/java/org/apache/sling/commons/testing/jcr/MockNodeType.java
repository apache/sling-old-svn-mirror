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
package org.apache.sling.commons.testing.jcr;

import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;

public class MockNodeType implements NodeType {

    private String name;

    public MockNodeType(String name) {
        this.name = (name == null) ? "nt:unstructured" : name;
    }

    public String getName() {
        return name;
    }

    public boolean canAddChildNode(String childNodeName) {
        return false;
    }

    public boolean canAddChildNode(String childNodeName, String nodeTypeName) {
        return false;
    }

    public boolean canRemoveItem(String itemName) {
        return false;
    }

    public boolean canSetProperty(String propertyName, Value value) {
        return false;
    }

    public boolean canSetProperty(String propertyName, Value[] values) {
        return false;
    }

    public NodeDefinition[] getChildNodeDefinitions() {
        return null;
    }

    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        return null;
    }

    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        return null;
    }

    public NodeType[] getDeclaredSupertypes() {
        return null;
    }

    public String getPrimaryItemName() {
        return null;
    }

    public PropertyDefinition[] getPropertyDefinitions() {
        return null;
    }

    public NodeType[] getSupertypes() {
        return null;
    }

    public boolean hasOrderableChildNodes() {
        return false;
    }

    public boolean isMixin() {
        return false;
    }

    public boolean isNodeType(String nodeTypeName) {
        return false;
    }

    // JCR 2.0 methods

    public boolean canRemoveNode(String nodeName) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean canRemoveProperty(String propertyName) {
        // TODO Auto-generated method stub
        return false;
    }

    public NodeTypeIterator getDeclaredSubtypes() {
        // TODO Auto-generated method stub
        return null;
    }

    public NodeTypeIterator getSubtypes() {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getDeclaredSupertypeNames() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isAbstract() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isQueryable() {
        // TODO Auto-generated method stub
        return false;
    }
}
