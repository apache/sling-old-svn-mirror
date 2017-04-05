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
package org.apache.sling.scripting.sightly.impl.compiler.optimization.reduce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.SightlyCompilerException;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.ArrayLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.BooleanConstant;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.MapLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.NullLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.NumericConstant;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.StringConstant;

/**
 * Data structure used in expression reducing
 */
public final class EvalResult {
    private final ExpressionNode node;
    private final Object value;

    public static EvalResult constant(Object obj) {
        return new EvalResult(null, obj);
    }

    public static EvalResult nonConstant(ExpressionNode node) {
        return new EvalResult(node, null);
    }

    private EvalResult(ExpressionNode node, Object value) {
        this.node = node;
        this.value = value;
    }

    public boolean isConstant() {
        return node == null;
    }

    public Object getValue() {
        if (!isConstant()) {
            throw new SightlyCompilerException("Cannot get constant value from non-constant result.");
        }
        return value;
    }

    public ExpressionNode getNode() {
        return (isConstant()) ? asLiteral(getValue()) : node;
    }

    private static ExpressionNode asLiteral(Object value) {
        if (value instanceof Boolean) {
            return new BooleanConstant((Boolean) value);
        }
        if (value instanceof String) {
            return new StringConstant((String) value);
        }
        if (value instanceof Number) {
            return new NumericConstant((Number) value);
        }
        if (value instanceof Map) {
            //noinspection unchecked
            return asMapLiteral((Map<String, Object>) value);
        }
        if (value instanceof List) {
            //noinspection unchecked
            return asArrayLiteral((List<Object>) value);
        }
        if (value == null) {
            return NullLiteral.INSTANCE;
        }
        throw new SightlyCompilerException("Cannot transform to literal: " + value);
    }

    private static MapLiteral asMapLiteral(Map<String, Object> map) {
        HashMap<String, ExpressionNode> literal = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            literal.put(entry.getKey(), asLiteral(entry.getValue()));
        }
        return new MapLiteral(literal);
    }

    private static ArrayLiteral asArrayLiteral(List<Object> list) {
        ArrayList<ExpressionNode> literal = new ArrayList<>();
        for (Object obj : list) {
            literal.add(asLiteral(obj));
        }
        return new ArrayLiteral(literal);
    }
}
