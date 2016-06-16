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
package org.apache.sling.scripting.sightly.java.compiler.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.SideEffectVisitor;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.Identifier;
import org.apache.sling.scripting.sightly.java.compiler.impl.operator.TypedNode;

/**
 * Helper for code generation
 */
public class GenHelper {

    public static void generateTernary(JavaSource source, SideEffectVisitor visitor,
                                       TypedNode condition, TypedNode thenBranch, TypedNode elseBranch) {
        source.startExpression();
        typeCoercion(source, visitor, condition, Type.BOOLEAN);
        source.conditional();
        thenBranch.getNode().accept(visitor);
        source.conditionalBranchSep();
        elseBranch.getNode().accept(visitor);
        source.endExpression();
    }

    public static void typeCoercion(JavaSource source, SideEffectVisitor visitor, TypedNode node, Type type) {
        if (type == node.getType() || type == Type.UNKNOWN) {
            node.getNode().accept(visitor);
        } else if (type == Type.LONG && node.getType() == Type.DOUBLE) {
            callLongCoercion(source, visitor, node.getNode());
        } else {
            String coercionMethod = dynamicCoercions.get(type);
            if (coercionMethod == null) {
                throw new UnsupportedOperationException("Cannot generate coercion to type " + type);
            }
            callDynamicCoercion(source, visitor, node.getNode(), dynamicCoercions.get(type));
        }
    }

    public static void listCoercion(JavaSource source, ExpressionTranslator visitor, TypedNode typedNode) {
        ExpressionNode node = typedNode.getNode();
        if (node instanceof Identifier) {
            //using list coercion caching optimization
            VariableDescriptor descriptor = visitor.getAnalyzer().descriptor(((Identifier) node).getName());
            String listCoercionVar = descriptor.requireListCoercion();
            source.startExpression()
                    .append(listCoercionVar)
                    .equality()
                    .nullLiteral()
                    .conditional()
                    .startExpression()
                    .append(listCoercionVar)
                    .assign()
                    .objectModel().startCall(SourceGenConstants.ROM_TO_COLLECTION, true);
            node.accept(visitor);
            source
                    .endCall()
                    .endExpression()
                    .conditionalBranchSep()
                    .append(listCoercionVar)
                    .endExpression();
        } else {
            source.objectModel().startCall(SourceGenConstants.ROM_TO_COLLECTION, true);
            typedNode.getNode().accept(visitor);
            source.endCall();
        }
    }

    private static void callLongCoercion(JavaSource source, SideEffectVisitor visitor, ExpressionNode node) {
        source.cast(Type.LONG.getNativeClass());
        source.startExpression();
        node.accept(visitor);
        source.endExpression();
    }

    private static void callDynamicCoercion(JavaSource source, SideEffectVisitor visitor, ExpressionNode node, String methodName) {
        source.objectModel().startCall(methodName, true);
        node.accept(visitor);
        source.endCall();
    }

    private static final Map<Type, String> dynamicCoercions = new HashMap<>();

    static {
        dynamicCoercions.put(Type.STRING, SourceGenConstants.ROM_TO_STRING);
        dynamicCoercions.put(Type.BOOLEAN, SourceGenConstants.ROM_TO_BOOLEAN);
        dynamicCoercions.put(Type.LONG, SourceGenConstants.ROM_TO_NUMBER);
        dynamicCoercions.put(Type.DOUBLE, SourceGenConstants.ROM_TO_NUMBER);
    }

}
