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
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

public class MockPropertyDefinition implements PropertyDefinition {

    private boolean multiple;

    public MockPropertyDefinition(boolean multiple) {
        this.multiple = multiple;

    }
    public Value[] getDefaultValues() {
        return null;
    }

    public int getRequiredType() {
        return 0;
    }

    public String[] getValueConstraints() {
        return null;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public NodeType getDeclaringNodeType() {
        return null;
    }

    public String getName() {
        return null;
    }

    public int getOnParentVersion() {
        return 0;
    }

    public boolean isAutoCreated() {
        return false;
    }

    public boolean isMandatory() {
        return false;
    }

    public boolean isProtected() {
        return false;
    }

    // JCR 2.0 methods

    public String[] getAvailableQueryOperators() {
        // TODO Auto-generated method stub
        return null;
    }
    public boolean isFullTextSearchable() {
        // TODO Auto-generated method stub
        return false;
    }
    public boolean isQueryOrderable() {
        // TODO Auto-generated method stub
        return false;
    }
}
