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

import java.util.EnumMap;
import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.expression.nodes.BinaryOperator;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.UnaryOperator;

/**
 * Provides mappings from operators to generators
 */
public class Operators {
    private static final Map<BinaryOperator, BinaryOpGen> representationMap =
            new EnumMap<BinaryOperator, BinaryOpGen>(BinaryOperator.class);

    private static final Map<UnaryOperator, UnaryOpGen> unaryMapping =
            new EnumMap<UnaryOperator, UnaryOpGen>(UnaryOperator.class);

    static {
        representationMap.put(BinaryOperator.AND, LogicalOpGen.AND);
        representationMap.put(BinaryOperator.OR, LogicalOpGen.OR);
        representationMap.put(BinaryOperator.CONCATENATE, ConcatenateOpGen.INSTANCE);
        representationMap.put(BinaryOperator.ADD, new NumericOpGen("+"));
        representationMap.put(BinaryOperator.SUB, new NumericOpGen("-"));
        representationMap.put(BinaryOperator.MUL, new NumericOpGen("*"));
        representationMap.put(BinaryOperator.I_DIV, new LongOpGen("/"));
        representationMap.put(BinaryOperator.REM, new LongOpGen("%"));
        representationMap.put(BinaryOperator.DIV, new NumericOpGen("/"));
        representationMap.put(BinaryOperator.EQ, new EquivalenceOpGen(false));
        representationMap.put(BinaryOperator.NEQ, new EquivalenceOpGen(true));
        representationMap.put(BinaryOperator.LT, new ComparisonOpGen(BinaryOperator.LT));
        representationMap.put(BinaryOperator.LEQ, new ComparisonOpGen(BinaryOperator.LEQ));
        representationMap.put(BinaryOperator.GT, new ComparisonOpGen(BinaryOperator.GT));
        representationMap.put(BinaryOperator.GEQ, new ComparisonOpGen(BinaryOperator.GEQ));
        representationMap.put(BinaryOperator.STRICT_EQ, new StrictEqGenOp(false));
        representationMap.put(BinaryOperator.STRICT_NEQ, new StrictEqGenOp(true));

        unaryMapping.put(UnaryOperator.LENGTH, LengthOpGen.INSTANCE);
        unaryMapping.put(UnaryOperator.IS_WHITESPACE, IsWhiteSpaceGen.INSTANCE);
        unaryMapping.put(UnaryOperator.NOT, NotOpGen.INSTANCE);
    }


    /**
     * Provide the signature of the given operator
     * @param operator - the operator
     * @return - the signature for the operator
     */
    public static BinaryOpGen generatorFor(BinaryOperator operator) {
        return provide(representationMap, operator);
    }

    /**
     * Provide the signature of the given operator
     * @param operator - the operator
     * @return - the signature for the operator
     */
    public static UnaryOpGen generatorFor(UnaryOperator operator) {
        return provide(unaryMapping, operator);
    }

    private static <K, V> V provide(Map<K, V> map, K key) {
        V v = map.get(key);
        if (v == null) {
            throw new UnsupportedOperationException("Cannot find generator for operator: " + key);
        }
        return v;
    }
}
