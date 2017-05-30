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
package org.apache.sling.fsprovider.internal.mapper.jcr;

import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang3.StringUtils;

class FsNodeType implements NodeType {
    
    private final String name;
    private final boolean mixin;
    
    public FsNodeType(String name, boolean mixin) {
        this.name = name;
        this.mixin = mixin;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String[] getDeclaredSupertypeNames() {
        return new String[0];
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isMixin() {
        return mixin;
    }

    @Override
    public boolean hasOrderableChildNodes() {
        return false;
    }

    @Override
    public boolean isQueryable() {
        return false;
    }

    @Override
    public String getPrimaryItemName() {
        return null;
    }

    @Override
    public NodeType[] getSupertypes() {
        return new NodeType[0];
    }

    @Override
    public NodeType[] getDeclaredSupertypes() {
        return new NodeType[0];
    }

    @Override
    public boolean isNodeType(String nodeTypeName) {
        return StringUtils.equals(name, nodeTypeName);
    }


    // --- unsupported methods ---    
    
    @Override
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeTypeIterator getSubtypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeTypeIterator getDeclaredSubtypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyDefinition[] getPropertyDefinitions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeDefinition[] getChildNodeDefinitions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canSetProperty(String propertyName, Value value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canSetProperty(String propertyName, Value[] values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canAddChildNode(String childNodeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canAddChildNode(String childNodeName, String nodeTypeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canRemoveItem(String itemName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canRemoveNode(String nodeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canRemoveProperty(String propertyName) {
        throw new UnsupportedOperationException();
    }

}
