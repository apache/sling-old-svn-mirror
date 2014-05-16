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

import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

public class VltPropertyDefinition implements PropertyDefinition {

    private NodeType declaringNodeType = null;
    private String name;
    private int onParentVersion;
    private boolean autoCreated;
    private boolean isMandatory;
    private boolean isProtected;
    private String[] availableQueryOperators;
    private Value[] defaultValues;
    private int requiredType;
    private String[] valueConstraints;
    private boolean fullTextSearchable;
    private boolean multiple;
    private boolean queryOrderable;
    
    //TODO: Current merge mechanism (or propertydefinition-inheritance) is not
    // accurate - it should probably do that taking the input directly and
    // apply the inheritance rules, rather than taking derived values and overwrite here..
    void mergeFrom(VltPropertyDefinition pd) {
        if (pd.declaringNodeType!=null) {
            declaringNodeType = pd.declaringNodeType;
        }
        if (pd.onParentVersion!=0) {
            onParentVersion = pd.onParentVersion;
        }
        if (pd.autoCreated) {
            autoCreated = pd.autoCreated;
        }
        if (pd.isMandatory) {
            isMandatory = pd.isMandatory;
        }
        if (pd.isProtected) {
            isProtected = pd.isProtected;
        }
        if (pd.availableQueryOperators!=null) {
            availableQueryOperators = pd.availableQueryOperators;
        }
        if (pd.defaultValues!=null) {
            defaultValues = pd.defaultValues;
        }
        if (pd.requiredType!=0) {
            requiredType = pd.requiredType;
        }
        if (pd.valueConstraints!=null) {
            valueConstraints = pd.valueConstraints;
        }
        if (pd.fullTextSearchable) {
            fullTextSearchable = pd.fullTextSearchable;
        }
        if (pd.multiple) {
            multiple = pd.multiple;
        }
        if (pd.queryOrderable) {
            queryOrderable = pd.queryOrderable;
        }
    }

    @Override
    public NodeType getDeclaringNodeType() {
        return declaringNodeType;
    }
    
    void setDeclaringNodeType(NodeType nt) {
        this.declaringNodeType = nt;
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
        return isMandatory;
    }
    
    void setMandatory(boolean mandatory) {
        this.isMandatory = mandatory;
    }

    @Override
    public boolean isProtected() {
        return isProtected;
    }
    
    void setProtected(boolean isProtected) {
        this.isProtected = isProtected;
    }

    @Override
    public String[] getAvailableQueryOperators() {
        return availableQueryOperators;
    }
    
    void setAvailableQueryOperators(String[] availableQueryOperators) {
        this.availableQueryOperators = availableQueryOperators;
    }

    @Override
    public Value[] getDefaultValues() {
        return defaultValues;
    }
    
    void setDefaultValues(Value[] values) {
        this.defaultValues = values;
    }

    @Override
    public int getRequiredType() {
        return requiredType;
    }
    
    void setRequiredType(int requiredType) {
        this.requiredType = requiredType;
    }

    @Override
    public String[] getValueConstraints() {
        return valueConstraints;
    }
    
    void setValueConstraints(String[] valueConstraints) {
        this.valueConstraints = valueConstraints;
    }

    @Override
    public boolean isFullTextSearchable() {
        return fullTextSearchable;
    }
    
    void setFullTextSearchable(boolean fullTextSearchable) {
        this.fullTextSearchable = fullTextSearchable;
    }

    @Override
    public boolean isMultiple() {
        return multiple;
    }
    
    void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    @Override
    public boolean isQueryOrderable() {
        return queryOrderable;
    }
    
    void setQueryOrderable(boolean queryOrderable) {
        this.queryOrderable = queryOrderable;
    }

}
