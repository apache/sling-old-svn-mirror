/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.apache.sling.scripting.sightly.java.compiler.impl;

/**
 * Information about a variable during the compilation process
 */
public class VariableDescriptor {

    private final String originalName;
    private final String assignedName;
    private VariableScope scope;
    private String listCoercion;
    private final Type type;
    private boolean templateVariable;

    public VariableDescriptor(String originalName, String assignedName, Type type, VariableScope scope) {
        this.originalName = originalName;
        this.assignedName = assignedName;
        this.scope = scope;
        this.type = type;
    }

    /**
     * The name of the list coercion variable
     * @return - the variable that will hold the list coercion
     */
    public String requireListCoercion() {
        if (listCoercion == null) {
            listCoercion = assignedName + "_list_coerced$";
        }
        return listCoercion;
    }

    public String getListCoercion() {
        return listCoercion;
    }

    /**
     * Get the original name of the variable, as it was provided
     * by the command that introduced the variable
     * @return - the original variable name
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * Get the assigned name for the variable - the name that will
     * actually be used during compilation
     * @return - the assigned variable name
     */
    public String getAssignedName() {
        return assignedName;
    }

    /**
     * Get the scope of this variable
     * @return the variable scope
     */
    public VariableScope getScope() {
        return scope;
    }

    /**
     * Get the inferred type fot this variable
     * @return - the class of the variable
     */
    public Type getType() {
        return type;
    }

    /**
     * Check whether this is a template variable
     * @return true if it is a template variable
     */
    public boolean isTemplateVariable() {
        return templateVariable;
    }

    /**
     * Signal that this variable stands for a template
     */
    public void markAsTemplate() {
        if (scope != VariableScope.DYNAMIC) {
            throw new UnsupportedOperationException("Only dynamic variables can be marked as templates");
        }
        templateVariable = true;
    }
}
