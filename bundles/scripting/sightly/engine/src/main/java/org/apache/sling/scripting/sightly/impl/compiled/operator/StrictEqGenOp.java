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
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.BinaryOperator;

/**
 * Generator for strict equality
 */
public class StrictEqGenOp implements BinaryOpGen {

    private final boolean negated;

    public StrictEqGenOp(boolean negated) {
        this.negated = negated;
    }

    @Override
    public Type returnType(Type leftType, Type rightType) {
        return Type.BOOLEAN;
    }

    @Override
    public void generate(JavaSource source, ExpressionTranslator visitor, TypedNode left, TypedNode right) {
        if (negated) {
            source.negation();
        }
        source.startMethodCall(BinaryOperator.OBJECT_NAME, BinaryOperator.METHOD_STRICT_EQ);
        left.getNode().accept(visitor);
        source.separateArgument();
        right.getNode().accept(visitor);
        source.endCall();
    }
}
