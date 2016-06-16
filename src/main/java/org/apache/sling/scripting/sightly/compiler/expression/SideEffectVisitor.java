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

/**
 * Expression visitor which does not return anything for nodes; instead it performs specific side-effects depending on the expression
 * nodes types.
 */
public abstract class SideEffectVisitor implements NodeVisitor<Object> {

    public abstract void visit(PropertyAccess propertyAccess);

    public abstract void visit(Identifier identifier);

    public abstract void visit(StringConstant text);

    public abstract void visit(BinaryOperation binaryOperation);

    public abstract void visit(BooleanConstant booleanConstant);

    public abstract void visit(NumericConstant numericConstant);

    public abstract void visit(UnaryOperation unaryOperation);

    public abstract void visit(TernaryOperator ternaryOperator);

    public abstract void visit(RuntimeCall runtimeCall);

    public abstract void visit(MapLiteral mapLiteral);

    public abstract void visit(ArrayLiteral arrayLiteral);

    public abstract void visit(NullLiteral nullLiteral);

    @Override
    public Object evaluate(PropertyAccess propertyAccess) {
        visit(propertyAccess);
        return null;
    }

    @Override
    public Object evaluate(Identifier identifier) {
        visit(identifier);
        return null;
    }

    @Override
    public Object evaluate(StringConstant text) {
        visit(text);
        return null;
    }

    @Override
    public Object evaluate(BinaryOperation binaryOperation) {
        visit(binaryOperation);
        return null;
    }

    @Override
    public Object evaluate(BooleanConstant booleanConstant) {
        visit(booleanConstant);
        return null;
    }

    @Override
    public Object evaluate(NumericConstant numericConstant) {
        visit(numericConstant);
        return null;
    }

    @Override
    public Object evaluate(UnaryOperation unaryOperation) {
        visit(unaryOperation);
        return null;
    }

    @Override
    public Object evaluate(TernaryOperator ternaryOperator) {
        visit(ternaryOperator);
        return null;
    }

    @Override
    public Object evaluate(RuntimeCall runtimeCall) {
        visit(runtimeCall);
        return null;
    }

    @Override
    public Object evaluate(MapLiteral mapLiteral) {
        visit(mapLiteral);
        return null;
    }

    @Override
    public Object evaluate(ArrayLiteral arrayLiteral) {
        visit(arrayLiteral);
        return null;
    }

    @Override
    public Object evaluate(NullLiteral nullLiteral) {
        visit(nullLiteral);
        return null;
    }
}

