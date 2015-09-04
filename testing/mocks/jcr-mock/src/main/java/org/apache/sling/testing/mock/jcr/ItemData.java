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
package org.apache.sling.testing.mock.jcr;

import java.util.UUID;

import javax.jcr.Item;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

/**
 * Holds node and property item data independently from session.
 */
class ItemData {
    
    private final String path;
    private final String name;
    private final boolean isNode;
    private final String uuid;
    private final NodeType nodeType;
    private Value[] values;
    private boolean isMultiple;
    private boolean isNew;
    
    private ItemData(String path, boolean isNode, String uuid, NodeType nodeType) {
        this.path = path;
        this.name = ResourceUtil.getName(path);
        this.uuid = uuid;
        this.isNode = isNode;
        this.nodeType = nodeType;
        this.isNew = true;
    }
    
    public String getPath() {
        return path;
    }
    
    public String getName() {
        return name;
    }

    public boolean isNode() {
        return isNode;
    }
    
    public boolean isProperty() {
        return !isNode;
    }
    
    public String getUuid() {
        if (!isNode()) {
            throw new UnsupportedOperationException();
        }
        return uuid;
    }

    public NodeType getNodeType() {
        if (!isNode()) {
            throw new UnsupportedOperationException();
        }
        return nodeType;
    }

    public Value[] getValues() {
        if (!isProperty()) {
            throw new UnsupportedOperationException();
        }
        return values;
    }

    public void setValues(Value[] values) {
        if (!isProperty()) {
            throw new UnsupportedOperationException();
        }
        this.values = values;
    }

    public boolean isMultiple() {
        if (!isProperty()) {
            throw new UnsupportedOperationException();
        }
        return isMultiple;
    }

    public void setMultiple(boolean isMultiple) {
        if (!isProperty()) {
            throw new UnsupportedOperationException();
        }
        this.isMultiple = isMultiple;
    }
    
    public Item getItem(Session session) {
        if (isNode) {
            return new MockNode(this, session);
        }
        else {
            return new MockProperty(this, session);
        }
    }
    
    public boolean isNew() {
        return isNew;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ItemData) {
            return path.equals(((ItemData)obj).path);
        }
        return false;
    }

    public static ItemData newNode(String path, NodeType nodeType) {
        return new ItemData(path, true, UUID.randomUUID().toString(), nodeType);
    }
    
    public static ItemData newProperty(String path) {
        return new ItemData(path, false, null, null);
    }
    
}
