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

import org.apache.sling.scripting.sightly.compiler.expression.nodes.ArrayLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperation;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BooleanConstant;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.Identifier;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.MapLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.NullLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.NumericConstant;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.PropertyAccess;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.StringConstant;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.TernaryOperator;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.UnaryOperation;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A {@code NodeVisitor} represents the mechanism through which {@link ExpressionNode} entities are processed.
 *
 * @param <T> the type of the object that's returned after an {@link ExpressionNode} evaluation
 */
@ProviderType
public interface NodeVisitor<T> {

    /**
     * Evaluate a {@link PropertyAccess} node.
     *
     * @param propertyAccess the node to evaluate
     * @return the evaluation result
     */
    T evaluate(PropertyAccess propertyAccess);

    /**
     * Evaluate an {@link Identifier} node.
     *
     * @param identifier the node to evaluate
     * @return the evaluation result
     */
    T evaluate(Identifier identifier);

    /**
     * Evaluate a {@link StringConstant} node.
     *
     * @param text the node to evaluate
     * @return the evaluation result
     */
    T evaluate(StringConstant text);

    /**
     * Evaluate a {@link BinaryOperation} node.
     *
     * @param binaryOperation the node to evaluate
     * @return the evaluation result
     */
    T evaluate(BinaryOperation binaryOperation);

    /**
     * Evaluate a {@link BooleanConstant} node.
     *
     * @param booleanConstant the node to evaluate
     * @return the evaluation result
     */
    T evaluate(BooleanConstant booleanConstant);

    /**
     * Evaluate a {@link NumericConstant} node.
     *
     * @param numericConstant the node to evaluate
     * @return the evaluation result
     */
    T evaluate(NumericConstant numericConstant);

    /**
     * Evaluate a {@link UnaryOperation} node.
     *
     * @param unaryOperation the node to evaluate
     * @return the evaluation result
     */
    T evaluate(UnaryOperation unaryOperation);

    /**
     * Evaluate a {@link TernaryOperator} node.
     *
     * @param ternaryOperator the node to evaluate
     * @return the evaluation result
     */
    T evaluate(TernaryOperator ternaryOperator);

    /**
     * Evaluate a {@link RuntimeCall} node.
     *
     * @param runtimeCall the node to evaluate
     * @return the evaluation result
     */
    T evaluate(RuntimeCall runtimeCall);

    /**
     * Evaluate a {@link MapLiteral} node.
     *
     * @param mapLiteral the node to evaluate
     * @return the evaluation result
     */
    T evaluate(MapLiteral mapLiteral);

    /**
     * Evaluate a {@link ArrayLiteral} node.
     *
     * @param arrayLiteral the node to evaluate
     * @return the evaluation result
     */
    T evaluate(ArrayLiteral arrayLiteral);

    /**
     * Evaluate a {@link NullLiteral} node.
     *
     * @param nullLiteral the node to evaluate
     * @return the evaluation result
     */
    T evaluate(NullLiteral nullLiteral);

}
