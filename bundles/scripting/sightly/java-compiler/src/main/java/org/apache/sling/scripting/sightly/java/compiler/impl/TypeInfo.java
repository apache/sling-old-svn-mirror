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

import java.util.Map;

import org.apache.sling.scripting.sightly.java.compiler.impl.operator.TypedNode;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;

/**
 * Provide type information for expressions
 */
public class TypeInfo {

    private final Map<ExpressionNode, Type> typeMap;

    public TypeInfo(Map<ExpressionNode, Type> typeMap) {
        this.typeMap = typeMap;
    }

    public Type typeOf(ExpressionNode node) {
        Type type = typeMap.get(node);
        if (type == null) {
            return Type.UNKNOWN;
        }
        return type;
    }

    public TypedNode getTyped(ExpressionNode expressionNode) {
        return new TypedNode(expressionNode, typeOf(expressionNode));
    }
}
