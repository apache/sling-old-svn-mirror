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
package org.apache.sling.jcr.resource.internal.loader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Node {

    private String name;
    private String primaryNodeType;
    private Set<String> mixinNodeTypes;
    private List<Property> properties;
    private List<Node> children;

    /**
     * @return the children
     */
    List<Node> getChildren() {
        return children;
    }

    /**
     * @param children the children to set
     */
    void addChild(Node child) {
        if (child != null) {
            if (children == null) {
                children = new ArrayList<Node>();
            }

            children.add(child);
        }
    }

    /**
     * @return the mixinNodeTypes
     */
    Set<String> getMixinNodeTypes() {
        return mixinNodeTypes;
    }

    /**
     * @param mixinNodeTypes the mixinNodeTypes to set
     */
    void addMixinNodeType(String mixinNodeType) {
        if (mixinNodeType != null && mixinNodeType.length() > 0) {
            if (mixinNodeTypes == null) {
                mixinNodeTypes = new HashSet<String>();
            }

            mixinNodeTypes.add(mixinNodeType);
        }
    }

    /**
     * @return the name
     */
    String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    void setName(String name) {
        this.name = name;
    }

    /**
     * @return the primaryNodeType
     */
    String getPrimaryNodeType() {
        return primaryNodeType;
    }

    /**
     * @param primaryNodeType the primaryNodeType to set
     */
    void setPrimaryNodeType(String primaryNodeType) {
        this.primaryNodeType = primaryNodeType;
    }

    /**
     * @return the properties
     */
    List<Property> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    void addProperty(Property property) {
        if (property != null) {
            if (properties == null) {
                properties = new ArrayList<Property>();
            }

            properties.add(property);
        }
    }

    public int hashCode() {
        int code = getName().hashCode() * 17;
        if (getPrimaryNodeType() != null) {
            code += getPrimaryNodeType().hashCode();
        }
        return code;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Node)) {
            return false;
        }

        Node other = (Node) obj;
        return getName().equals(other.getName())
            && equals(getPrimaryNodeType(), other.getPrimaryNodeType())
            && equals(getMixinNodeTypes(), other.getMixinNodeTypes())
            && equals(getProperties(), other.getProperties())
            && equals(getChildren(), other.getChildren());
    }

    public String toString() {
        return "Node " + getName() + ", primary=" + getPrimaryNodeType()
            + ", mixins=" + getMixinNodeTypes();
    }

    private boolean equals(Object o1, Object o2) {
        return (o1 == null) ? o2 == null : o1.equals(o2);
    }
}
