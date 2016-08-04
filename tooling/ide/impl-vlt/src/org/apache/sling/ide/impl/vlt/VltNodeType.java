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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.sling.ide.transport.ResourceProxy;

public class VltNodeType implements NodeType {

    private NodeDefinition[] declaredChildNodeDefinitions;
    private PropertyDefinition[] declaredPropertyDefinitions;
    private String[] declaredSupertypeNames;
    private String name;
    private String primaryItemName;
    private boolean hasOrderableChildNodes;
    private boolean isAbstract;
    private boolean isMixin;
    private boolean isQueryable;
    private NodeDefinition[] childNodeDefinitions;
    private NodeType[] declaredSupertypes;
    private PropertyDefinition[] propertyDefinitions;
    private NodeType[] superTypes;
    
    private Set<VltNodeType> directChildTypes = new HashSet<>();
    private Set<String> allowedChildNodeTypes;
    private ResourceProxy resourceProxy;

    VltNodeType(ResourceProxy resourceProxy) {
        this.resourceProxy = resourceProxy;
    }
    
    ResourceProxy getResourceProxy() {
        return resourceProxy;
    }
    
    void addSuperType(VltNodeType aSuperType) {
        aSuperType.directChildTypes.add(this);
    }
    
    Set<VltNodeType> getDirectChildTypes() {
        return directChildTypes;
    }

    Set<VltNodeType> getAllKnownChildTypes() {
        Set<VltNodeType> allKnownCTs = new HashSet<>();
        allKnownCTs.add(this);
        for (VltNodeType simpleVltNodeType : directChildTypes) {
            allKnownCTs.addAll(simpleVltNodeType.getAllKnownChildTypes());
        }
        return allKnownCTs;
    }

    void setAllowedPrimaryChildNodeTypes(Set<String> allowedChildNodeTypes) {
        this.allowedChildNodeTypes = allowedChildNodeTypes;
    }
    
    Set<String> getAllowedPrimaryChildNodeTypes() {
        return Collections.unmodifiableSet(allowedChildNodeTypes);
    }
    
    @Override
    public String toString() {
        return "[" + name + "]";
    }
    
    @Override
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        return declaredChildNodeDefinitions;
    }
    
    void setDeclaredChildNodeDefinitions(NodeDefinition[] nodeDefinitions) {
        this.declaredChildNodeDefinitions = nodeDefinitions;
    }

    @Override
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        return declaredPropertyDefinitions;
    }
    
    void setDeclaredPropertyDefinitions(PropertyDefinition[] pds) {
        this.propertyDefinitions = pds;
    }

    @Override
    public String[] getDeclaredSupertypeNames() {
        return declaredSupertypeNames;
    }
    
    void setDeclaredSupertypeNames(String[] supertypeNames) {
        this.declaredSupertypeNames = supertypeNames;
    }

    @Override
    public String getName() {
        return name;
    }
    
    void setName(String name) {
        this.name = name;
    }

    @Override
    public String getPrimaryItemName() {
        return primaryItemName;
    }
    
    void setPrimaryItemName(String primaryItemName) {
        this.primaryItemName = primaryItemName;
    }

    @Override
    public boolean hasOrderableChildNodes() {
        return hasOrderableChildNodes;
    }
    
    void setHasOrderableChildNodes(boolean hasOrderableChildNodes) {
        this.hasOrderableChildNodes = hasOrderableChildNodes;
    }

    @Override
    public boolean isAbstract() {
        return isAbstract;
    }
    
    void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    @Override
    public boolean isMixin() {
        return isMixin;
    }
    
    void setMixin(boolean isMixin) {
        this.isMixin = isMixin;
    }

    @Override
    public boolean isQueryable() {
        return isQueryable;
    }
    
    void setQueryable(boolean isQueryable) {
        this.isQueryable = isQueryable;
    }

    @Override
    public boolean canAddChildNode(String arg0) {
        throw new IllegalStateException("not yet implemented");
    }

    @Override
    public boolean canAddChildNode(String arg0, String arg1) {
        throw new IllegalStateException("not yet implemented");
    }

    @Override
    public boolean canRemoveItem(String arg0) {
        throw new IllegalStateException("not yet implemented");
    }

    @Override
    public boolean canRemoveNode(String arg0) {
        throw new IllegalStateException("not yet implemented");
    }

    @Override
    public boolean canRemoveProperty(String arg0) {
        throw new IllegalStateException("not yet implemented");
    }

    @Override
    public boolean canSetProperty(String arg0, Value arg1) {
        throw new IllegalStateException("not yet implemented");
    }

    @Override
    public boolean canSetProperty(String arg0, Value[] arg1) {
        throw new IllegalStateException("not yet implemented");
    }

    @Override
    public NodeDefinition[] getChildNodeDefinitions() {
        List<NodeDefinition> childNodeDefs = new LinkedList<>();
        childNodeDefs.addAll(Arrays.asList(getDeclaredChildNodeDefinitions()));
        NodeType[] supers = getSupertypes();
        if (supers!=null) {
            for (int i = 0; i < supers.length; i++) {
                NodeType aSuperNodeType = supers[i];
                NodeDefinition[] superChildNodeDefs = aSuperNodeType.getChildNodeDefinitions();
                if (superChildNodeDefs!=null) {
                    childNodeDefs.addAll(Arrays.asList(superChildNodeDefs));
                }
            }
        }
        return childNodeDefs.toArray(new NodeDefinition[0]);
    }
    
    void setChildNodeDefinitions(NodeDefinition[] childNodeDefinitions) {
        this.childNodeDefinitions = childNodeDefinitions;
    }

    @Override
    public NodeTypeIterator getDeclaredSubtypes() {
        throw new IllegalStateException("not yet implemented");
    }

    @Override
    public NodeType[] getDeclaredSupertypes() {
        return declaredSupertypes;
    }
    
    void setDeclaredSupertypes(NodeType[] declaredSupertypes) {
        this.declaredSupertypes = declaredSupertypes;
    }

    @Override
    public PropertyDefinition[] getPropertyDefinitions() {
        return propertyDefinitions;
    }
    
    PropertyDefinition getPropertyDefinition(String name) {
        for (int i = 0; i < propertyDefinitions.length; i++) {
            PropertyDefinition pd = propertyDefinitions[i];
            if (pd.getName().equals(name)) {
                return pd;
            }
        }
        return null;
    }
    
    void setPropertyDefinitions(PropertyDefinition[] pds) {
        propertyDefinitions = pds;
    }

    @Override
    public NodeTypeIterator getSubtypes() {
        throw new IllegalStateException("not yet implemented");
    }

    @Override
    public NodeType[] getSupertypes() {
        return superTypes;
    }
    
    void setSupertypes(NodeType[] superTypes) {
        this.superTypes = superTypes;
    }

    @Override
    public boolean isNodeType(String arg0) {
        throw new IllegalStateException("not yet implemented");
    }

}
