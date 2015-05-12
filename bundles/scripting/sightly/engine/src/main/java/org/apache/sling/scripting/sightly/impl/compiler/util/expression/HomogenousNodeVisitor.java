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

package org.apache.sling.scripting.sightly.impl.compiler.util.expression;

import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.expression.NodeVisitor;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.ArrayLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BinaryOperation;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BooleanConstant;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.Identifier;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.MapLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.NullLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.NumericConstant;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.PropertyAccess;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.RuntimeCall;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.StringConstant;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.TernaryOperator;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.UnaryOperation;

/**
 * Apply the same evaluation for all expression nodes
 */
public abstract class HomogenousNodeVisitor<T> implements NodeVisitor<T> {

    protected abstract T evaluateDefault(ExpressionNode node);

    @Override
    public T evaluate(PropertyAccess propertyAccess) {
        return evaluateDefault(propertyAccess);
    }

    @Override
    public T evaluate(Identifier identifier) {
        return evaluateDefault(identifier);
    }

    @Override
    public T evaluate(StringConstant text) {
        return evaluateDefault(text);
    }

    @Override
    public T evaluate(BinaryOperation binaryOperation) {
        return evaluateDefault(binaryOperation);
    }

    @Override
    public T evaluate(BooleanConstant booleanConstant) {
        return evaluateDefault(booleanConstant);
    }

    @Override
    public T evaluate(NumericConstant numericConstant) {
        return evaluateDefault(numericConstant);
    }

    @Override
    public T evaluate(UnaryOperation unaryOperation) {
        return evaluateDefault(unaryOperation);
    }

    @Override
    public T evaluate(TernaryOperator ternaryOperator) {
        return evaluateDefault(ternaryOperator);
    }

    @Override
    public T evaluate(RuntimeCall runtimeCall) {
        return evaluateDefault(runtimeCall);
    }

    @Override
    public T evaluate(MapLiteral mapLiteral) {
        return evaluateDefault(mapLiteral);
    }

    @Override
    public T evaluate(ArrayLiteral arrayLiteral) {
        return evaluateDefault(arrayLiteral);
    }

    @Override
    public T evaluate(NullLiteral nullLiteral) {
        return evaluateDefault(nullLiteral);
    }
}
