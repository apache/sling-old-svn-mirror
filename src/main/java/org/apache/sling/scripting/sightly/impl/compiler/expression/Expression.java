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
package org.apache.sling.scripting.sightly.impl.compiler.expression;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represent a Sightly Expression.
 */
public class Expression {

    private final Map<String, ExpressionNode> options;
    private final ExpressionNode root;

    public Expression(ExpressionNode root, Map<String, ExpressionNode> options) {
        this.root = root;
        this.options = new HashMap<String, ExpressionNode>(options);
    }

    public Expression(ExpressionNode root) {
        this(root, Collections.<String, ExpressionNode>emptyMap());
    }

    /**
     * Get the options for this expression
     * @return - the expression options
     */
    public Map<String, ExpressionNode> getOptions() {
        return options;
    }

    /**
     * Get the root node of this expression
     * @return - the root expression node
     */
    public ExpressionNode getRoot() {
        return root;
    }

    /**
     * Get the option with the specified name
     * @param name the name of the option
     * @return the expression node for the option value, or null if the
     * option is not in the expression
     */
    public ExpressionNode getOption(String name) {
        return options.get(name);
    }

    /**
     * Return an expression where the given options are no longer present
     * @param removedOptions the options to be removed
     * @return a copy where the mention options are no longer present
     */
    public Expression withRemovedOptions(String... removedOptions) {
        HashMap<String, ExpressionNode> newOptions = new HashMap<String, ExpressionNode>(options);
        for (String option : removedOptions) {
            newOptions.remove(option);
        }
        return new Expression(root, newOptions);
    }

    /**
     * Removes the given options from this expression.
     *
     * @param removedOptions the options to be removed
     */
    public void removeOptions(String... removedOptions) {
        for (String option : removedOptions) {
            options.remove(option);
        }
    }

    /**
     * Removes the given option from this expression.
     *
     * @param option the option to be removed
     * @return the option, or {@code null} if the option doesn't exist
     */
    public ExpressionNode removeOption(String option) {
        return options.remove(option);
    }

    /**
     * Return a copy, but with the specified node as root
     * @param node the new root
     * @return a copy with a new root
     */
    public Expression withNode(ExpressionNode node) {
        return new Expression(node, options);
    }

    /**
     * Checks whether the expression has the specified option
     * @param name the name of the option
     * @return true if the option is present, false otherwise
     */
    public boolean containsOption(String name) {
        return options.containsKey(name);
    }

    @Override
    public String toString() {
        return "Expression{" +
                "options=" + getOptions() +
                ", root=" + root +
                '}';
    }
}
