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
import org.apache.sling.scripting.sightly.impl.compiled.GenHelper;
import org.apache.sling.scripting.sightly.impl.compiled.JavaSource;
import org.apache.sling.scripting.sightly.impl.compiled.Type;
import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.util.expression.SideEffectVisitor;

/**
 * Generator for logical operators
 */
public abstract class LogicalOpGen implements BinaryOpGen {

    private final String javaOperator;

    protected LogicalOpGen(String javaOperator) {
        this.javaOperator = javaOperator;
    }

    public static final LogicalOpGen AND = new LogicalOpGen("&&") {
        @Override
        protected void generateGeneric(JavaSource source, SideEffectVisitor visitor, TypedNode left, TypedNode right) {
            GenHelper.generateTernary(source, visitor, left, right, left);
        }
    };

    public static final LogicalOpGen OR = new LogicalOpGen("||") {
        @Override
        protected void generateGeneric(JavaSource source, SideEffectVisitor visitor, TypedNode left, TypedNode right) {
            GenHelper.generateTernary(source, visitor, left, left, right);
        }
    };

    @Override
    public Type returnType(Type leftType, Type rightType) {
        if (leftType == rightType) {
            return leftType;
        }
        return Type.UNKNOWN;
    }

    @Override
    public void generate(JavaSource source, ExpressionTranslator visitor, TypedNode left, TypedNode right) {
        if (OpHelper.sameType(left, right) == Type.BOOLEAN) {
            generateWithOperator(source, visitor, left.getNode(), right.getNode());
        } else {
            generateGeneric(source, visitor, left, right);
        }
    }

    protected abstract void generateGeneric(JavaSource source, SideEffectVisitor visitor,
                                            TypedNode left, TypedNode right);

    private void generateWithOperator(JavaSource source, SideEffectVisitor visitor,
                                      ExpressionNode leftNode, ExpressionNode rightNode) {
        leftNode.accept(visitor);
        source.append(javaOperator);
        rightNode.accept(visitor);
    }
}
