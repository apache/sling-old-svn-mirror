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
package org.apache.sling.scripting.jsp.taglib.tei;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

/**
 * Abstract TEI that that provides the type for a single
 * variable named in the tag's "var" attribute.
 * <p>
 * The name of the attribute can be overwritten via a custom default
 * constructor or by overwriting {@link #getVariableName(javax.servlet.jsp.tagext.TagData)}.
 * <p>
 * All implementations need to overwrite {@link #getClassName(javax.servlet.jsp.tagext.TagData)}
 * in order to provide the type (class name) of the variable.
 */
public abstract class AbstractVarTEI extends TagExtraInfo {

    protected static final String ATTR_VAR = "var";

    private final String variableNameAttribute;

    public AbstractVarTEI() {
        this(ATTR_VAR);
    }

    /**
     * Constructor that takes the name of the attribute that defines the variable name
     * and the name of the attribute that defines the class name.
     *
     * @param variableNameAttribute Name of the attribute that defines the variable name.
     */
    protected AbstractVarTEI(final String variableNameAttribute) {
        this.variableNameAttribute = variableNameAttribute;
    }

    /**
     * Provides the name of the variable injected into the {@code pageContext}.
     *
     * @param data The TagData.
     * @return The variable name.
     */
    protected String getVariableName(TagData data) {
        return data.getAttributeString(variableNameAttribute);
    }

    /**
     * Provides the fully qualified class name of the variable injected into
     * the {@code pageContext}.
     *
     * @param data The TagData.
     * @return The class name of the variable's type.
     */
    protected abstract String getClassName(TagData data);

    @Override
    public VariableInfo[] getVariableInfo(TagData data) {
        final String variableName = getVariableName(data);
        if (variableName == null) {
            return new VariableInfo[0];
        } else {
            return new VariableInfo[]{
                    new VariableInfo(variableName, getClassName(data), true, VariableInfo.AT_END)
            };
        }
    }
}
