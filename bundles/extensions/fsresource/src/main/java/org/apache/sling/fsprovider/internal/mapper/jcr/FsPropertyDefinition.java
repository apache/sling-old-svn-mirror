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

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;

class FsPropertyDefinition implements PropertyDefinition {
    
    private final String name;
    
    public FsPropertyDefinition(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getDeclaringNodeType() {
        return null;
    }

    @Override
    public boolean isAutoCreated() {
        return false;
    }

    @Override
    public boolean isMandatory() {
        return false;
    }

    @Override
    public int getOnParentVersion() {
        return OnParentVersionAction.COPY;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Override
    public int getRequiredType() {
        return PropertyType.UNDEFINED;
    }

    @Override
    public String[] getValueConstraints() {
        return new String[0];
    }

    @Override
    public Value[] getDefaultValues() {
        return new Value[0];
    }

    @Override
    public boolean isMultiple() {
        return false;
    }

    @Override
    public String[] getAvailableQueryOperators() {
        return new String[0];
    }

    @Override
    public boolean isFullTextSearchable() {
        return false;
    }

    @Override
    public boolean isQueryOrderable() {
        return false;
    }    

}
