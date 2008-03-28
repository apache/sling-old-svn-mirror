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
package org.apache.sling.jcr.resource.internal.helper.starresource;

import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

/** Fake NodeType when only some methods work, as needed to support
 *  the StarResource.
 */
class FakeNodeType implements NodeType {

    private final String name;
    
    FakeNodeType(String name) {
        this.name = name;
    }
    
    public boolean canAddChildNode(String arg0) {
        return false;
    }

    public boolean canAddChildNode(String arg0, String arg1) {
        return false;
    }

    public boolean canRemoveItem(String arg0) {
        return false;
    }

    public boolean canSetProperty(String arg0, Value arg1) {
        return false;
    }

    public boolean canSetProperty(String arg0, Value[] arg1) {
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

    public String getName() {
        return name;
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

    public boolean isNodeType(String arg0) {
        return false;
    }
}
