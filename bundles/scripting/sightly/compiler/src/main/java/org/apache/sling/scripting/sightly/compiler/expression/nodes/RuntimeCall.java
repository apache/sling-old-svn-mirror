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
package org.apache.sling.scripting.sightly.compiler.expression.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.NodeVisitor;

/**
 * A {@code RuntimeCall} is a special expression which provides access to utility functions from the runtime.
 */
public final class RuntimeCall implements ExpressionNode {

    private final String functionName;
    private final List<ExpressionNode> arguments;

    /**
     * Creates a {@code RuntimeCall} based on a {@code functionName} and an array of {@code arguments}.
     *
     * @param functionName the name of the function identifying the runtime call
     * @param arguments    the arguments passed to the runtime call
     */
    public RuntimeCall(String functionName, ExpressionNode... arguments) {
        this(functionName, Arrays.asList(arguments));
    }

    /**
     * Creates a {@code RuntimeCall} based on a {@code functionName} and a list of {@code arguments}.
     *
     * @param functionName the name of the function identifying the runtime call
     * @param arguments    the arguments passed to the runtime call
     */
    public RuntimeCall(String functionName, List<ExpressionNode> arguments) {
        this.functionName = functionName;
        this.arguments = new ArrayList<>(arguments);
    }

    /**
     * Get the name of the runtime call.
     *
     * @return the name of the runtime call
     */
    public String getFunctionName() {
        return functionName;
    }

    /**
     * Get the nodes of the argument calls.
     *
     * @return the arguments list
     */
    public List<ExpressionNode> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) {
        return visitor.evaluate(this);
    }
}
