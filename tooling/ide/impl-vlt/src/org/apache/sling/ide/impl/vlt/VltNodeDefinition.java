/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.impl.vlt;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

public class VltNodeDefinition implements NodeDefinition {

    private NodeType declaringNodeType;
    private String name;
    private int onParentVersion;
    private boolean autoCreated;
    private boolean mandatory;
    private boolean isProtected;
    private boolean allowsSameNameSiblings;
    private NodeType defaultPrimaryType;
    private String defaultPrimaryTypeName;
    private String[] requiredPrimaryTypeNames;
    private NodeType[] requiredPrimaryTypes;

    @Override
    public NodeType getDeclaringNodeType() {
        return declaringNodeType;
    }

    void setDeclaringNodeType(NodeType declaringNodeType) {
        this.declaringNodeType = declaringNodeType;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    void setName(String name) {
        this.name = name;
    }

    @Override
    public int getOnParentVersion() {
        return onParentVersion;
    }
    
    void setOnParentVersion(int onParentVersion) {
        this.onParentVersion = onParentVersion;
    }

    @Override
    public boolean isAutoCreated() {
        return autoCreated;
    }
    
    void setAutoCreated(boolean autoCreated) {
        this.autoCreated = autoCreated;
    }

    @Override
    public boolean isMandatory() {
        return mandatory;
    }
    
    void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    @Override
    public boolean isProtected() {
        return isProtected;
    }
    
    void setProtected(boolean isProtected) {
        this.isProtected = isProtected;
    }

    @Override
    public boolean allowsSameNameSiblings() {
        return allowsSameNameSiblings;
    }
    
    void setAllowsSameNameSiblings(boolean allowsSameNameSiblings) {
        this.allowsSameNameSiblings = allowsSameNameSiblings;
    }

    @Override
    public NodeType getDefaultPrimaryType() {
        return defaultPrimaryType;
    }
    
    void setDefaultPrimaryType(NodeType defaultPrimaryType) {
        this.defaultPrimaryType = defaultPrimaryType;
    }

    @Override
    public String getDefaultPrimaryTypeName() {
        return defaultPrimaryTypeName;
    }
    
    void setDefaultPrimaryTypeName(String defaultPrimaryTypeName) {
        this.defaultPrimaryTypeName = defaultPrimaryTypeName;
    }

    @Override
    public String[] getRequiredPrimaryTypeNames() {
        return requiredPrimaryTypeNames;
    }
    
    void setRequiredPrimaryTypeNames(String[] requiredPrimaryTypeNames) {
        this.requiredPrimaryTypeNames = requiredPrimaryTypeNames;
    }

    @Override
    public NodeType[] getRequiredPrimaryTypes() {
        return requiredPrimaryTypes;
    }
    
    void setRequiredPrimaryTypes(NodeType[] requiredPrimaryTypes) {
        this.requiredPrimaryTypes = requiredPrimaryTypes;
    }

}
