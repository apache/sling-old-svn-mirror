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
package org.apache.sling.scripting.sightly.compiler.expression;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperation;

/**
 * This class represents a Sightly Expression.
 */
public final class Expression {

    private final Map<String, ExpressionNode> options;
    private final ExpressionNode root;
    private final String rawText;

    /**
     * Create an expression with just a root node.
     *
     * @param root the root node
     */
    public Expression(ExpressionNode root) {
        this(root, Collections.<String, ExpressionNode>emptyMap());
    }

    /**
     * Create an expression with a root node and options.
     *
     * @param root    the root node
     * @param options the expression's options
     */
    public Expression(ExpressionNode root, Map<String, ExpressionNode> options) {
        this(root, options, null);
    }

    /**
     * Create an expression with a root node and options.
     *
     * @param root    the root node
     * @param options the expression's options
     * @param rawText the expression's raw text representation
     */
    public Expression(ExpressionNode root, Map<String, ExpressionNode> options, String rawText) {
        this.options = new HashMap<>(options);
        if (root instanceof BinaryOperation) {
            BinaryOperation binaryOperation = (BinaryOperation) root;
            this.root = binaryOperation.withParentExpression(this);
        } else {
            this.root = root;
        }
        this.rawText = rawText;
    }

    /**
     * Get the options for this expression.
     *
     * @return the expression options
     */
    public Map<String, ExpressionNode> getOptions() {
        return options;
    }

    /**
     * Get the root node of this expression.
     *
     * @return the root node of this expression
     */
    public ExpressionNode getRoot() {
        return root;
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
     * Return a copy, but with the specified node as root.
     *
     * @param node the new root
     * @return a copy with a new root
     */
    public Expression withNode(ExpressionNode node) {
        return new Expression(node, options);
    }

    /**
     * Return a copy that provides information about the expression's raw text.
     *
     * @param rawText the raw text representing the expression
     * @return a copy with information about the expression's raw text
     */
    public Expression withRawText(String rawText) {
        return new Expression(root, options, rawText);
    }

    /**
     * Returns the raw text representation of this expression.
     *
     * @return the raw text representation of this expression
     */
    public String getRawText() {
        return rawText;
    }

    /**
     * Checks whether the expression has the specified option.
     *
     * @param name the name of the option
     * @return {@code true} if the option is present, {@code false} otherwise
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
