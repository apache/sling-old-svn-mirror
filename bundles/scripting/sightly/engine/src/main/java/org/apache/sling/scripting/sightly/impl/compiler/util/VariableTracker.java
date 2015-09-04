/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.compiler.util;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Track information related to Sightly variables
 */
public class VariableTracker<T> {

    private final Map<String, Stack<T>> variableData = new HashMap<String, Stack<T>>();
    private final Stack<String> declarationStack = new Stack<String>();

    public boolean isDeclared(String name) {
        name = name.toLowerCase();
        Stack<T> dataStack = variableData.get(name);
        return dataStack != null;
    }

    /**
     * Push a variable. Use this at a variable declaration
     * @param name - the name of the variable
     * @param data - data associated with the variable
     */
    public void pushVariable(String name, T data) {
        name = name.toLowerCase();
        Stack<T> dataStack = variableData.get(name);
        if (dataStack == null) {
            dataStack = new Stack<T>();
            variableData.put(name, dataStack);
        }
        dataStack.push(data);
        declarationStack.push(name);
    }

    /**
     * Pop a variable. Use this when a variable goes out of scope
     * @return - the name of the popped variable
     * @throws java.util.NoSuchElementException - if there are no declared variables in the scope
     */
    public String popVariable() {
        String variable = declarationStack.pop();
        Stack<T> dataStack = variableData.get(variable);
        assert dataStack != null;
        dataStack.pop();
        if (dataStack.isEmpty()) {
            variableData.remove(variable);
        }
        return variable;
    }

    /**
     * Peek at the top of the declaration stack
     * @return - the most recently declared variable and it's associated data
     * @throws java.util.NoSuchElementException - if there are no variables in scope
     */
    public Map.Entry<String, T> peek() {
        String variable = declarationStack.peek();
        Stack<T> dataStack = variableData.get(variable);
        assert dataStack != null;
        T data = dataStack.peek();
        return new AbstractMap.SimpleImmutableEntry<String, T>(variable, data);
    }

    public boolean isEmpty() {
        return declarationStack.isEmpty();
    }

    /**
     * Get the data associated with the given variable
     * @param name - the name of the variable
     * @return the associated data or null if that variable is not in scope
     */
    public T get(String name) {
        name = name.toLowerCase();
        Stack<T> dataStack = variableData.get(name);
        if (dataStack == null) {
            return null;
        }
        assert !dataStack.isEmpty();
        return dataStack.peek();
    }

    /**
     * Check whether a variable was declared and is visible in the current scope
     * @param name - the name of the variable
     * @return - the visibility of the variable in the current scope
     */
    public boolean isInScope(String name) {
        name = name.toLowerCase();
        return variableData.get(name) != null;
    }

    /**
     * Get an immutable view of all the data items associated with the specified
     * variable.
     * @param name - the name of the variable
     * @return - a list of all the data associated with this variable name. If the
     * variable is not declared in the current scope then that list will be empty. Otherwise
     * it will contain all the data associated for the current scope starting with the data
     * associated at the topmost scope and ending with the most recently associated data
     */
    public List<T> getAll(String name) {
        name = name.toLowerCase();
        Stack<T> dataStack = variableData.get(name);
        if (dataStack == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(dataStack);
    }

    /**
     * Get how many times a variable was declared in the current scope
     * @param name - the name of the variable
     * @return - the number of declarations for a variable in the current scope
     */
    public int getOccurrenceCount(String name) {
        name = name.toLowerCase();
        Stack<T> dataStack = variableData.get(name);
        if (dataStack == null) {
            return 0;
        }
        return dataStack.size();
    }

}
