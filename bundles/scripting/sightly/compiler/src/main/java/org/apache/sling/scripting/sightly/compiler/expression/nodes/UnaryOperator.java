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
package org.apache.sling.scripting.sightly.compiler.expression.nodes;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.scripting.sightly.impl.compiler.CompileTimeObjectModel;

/**
 * Unary operators used in expressions.
 */
public enum UnaryOperator {

    /** Evaluates to logical negation of the operand */
    NOT {
        @Override
        public Object eval(Object operand) {
            return !CompileTimeObjectModel.toBoolean(operand);
        }
    },

    /** Evaluates whether the operand is a string of only whitespace characters */
    IS_WHITESPACE  {
        @Override
        public Object eval(Object operand) {
            return StringUtils.isWhitespace(CompileTimeObjectModel.toString(operand));
        }
    },

    /**
     * Evaluates the length of a collection
     */
    LENGTH {
        @Override
        public Object eval(Object operand) {
            return CompileTimeObjectModel.toCollection(operand).size();
        }
    };

    public abstract Object eval(Object operand);

}
