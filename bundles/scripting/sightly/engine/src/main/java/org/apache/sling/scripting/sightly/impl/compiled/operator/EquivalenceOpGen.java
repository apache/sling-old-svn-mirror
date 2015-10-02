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

package org.apache.sling.scripting.sightly.impl.compiled.operator;

import org.apache.sling.scripting.sightly.impl.compiled.ExpressionTranslator;
import org.apache.sling.scripting.sightly.impl.compiled.JavaSource;
import org.apache.sling.scripting.sightly.impl.compiled.Type;
import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.util.expression.SideEffectVisitor;

/**
 * Generator for logical operators
 */
public class EquivalenceOpGen implements BinaryOpGen {

    private final boolean negated;

    public EquivalenceOpGen(boolean negated) {
        this.negated = negated;
    }

    @Override
    public Type returnType(Type left, Type right) {
        return Type.BOOLEAN;
    }

    @Override
    public void generate(JavaSource source, ExpressionTranslator visitor, TypedNode left, TypedNode right) {
        Type type = OpHelper.sameType(left, right);
        if (type != null && OpHelper.isNumericType(type) || type == Type.BOOLEAN || type == Type.UNKNOWN) {
            generateEqualsOperator(source, visitor, left.getNode(), right.getNode());
        } else if (type != Type.UNKNOWN) {
            generateEqualsMethod(source, visitor, left, right);
        }
    }

    private void generateCheckedEquals(JavaSource source, SideEffectVisitor visitor, TypedNode leftNode, TypedNode rightNode) {
        source.startExpression();
        leftNode.getNode().accept(visitor);
        source.equality().nullLiteral().conditional();
        rightNode.getNode().accept(visitor);
        source.equality().nullLiteral();
        source.conditionalBranchSep();
        generateEqualsMethod(source, visitor, leftNode, rightNode);
        source.endExpression();
    }

    private void generateEqualsMethod(JavaSource source, SideEffectVisitor visitor, TypedNode leftNode, TypedNode rightNode) {
        boolean performCast = leftNode.getType().isPrimitive();
        if (performCast) {
            source.startExpression();
            source.cast(Type.UNKNOWN.getNativeClass());
        }
        leftNode.getNode().accept(visitor);
        if (performCast) {
            source.endExpression();
        }
        source.startCall("equals", true);
        rightNode.getNode().accept(visitor);
        source.endCall();
    }

    private void generateEqualsOperator(JavaSource source, SideEffectVisitor visitor, ExpressionNode leftNode, ExpressionNode rightNode) {
        leftNode.accept(visitor);
        source.append(operator());
        rightNode.accept(visitor);
    }

    private String operator() {
        return (negated) ? "!=" : "==";
    }

}
