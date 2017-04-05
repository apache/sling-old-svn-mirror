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

package org.apache.sling.scripting.sightly.java.compiler.impl.operator;

import org.apache.sling.scripting.sightly.compiler.expression.SideEffectVisitor;
import org.apache.sling.scripting.sightly.java.compiler.impl.ExpressionTranslator;
import org.apache.sling.scripting.sightly.java.compiler.impl.GenHelper;
import org.apache.sling.scripting.sightly.java.compiler.impl.JavaSource;
import org.apache.sling.scripting.sightly.java.compiler.impl.Type;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;

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
