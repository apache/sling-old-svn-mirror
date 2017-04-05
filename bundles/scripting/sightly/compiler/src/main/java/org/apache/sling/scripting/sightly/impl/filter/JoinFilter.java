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
package org.apache.sling.scripting.sightly.impl.filter;

import org.apache.sling.scripting.sightly.compiler.RuntimeFunction;
import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall;

/**
 * Filter providing support for the {@code join} option applied to arrays.
 */
public class JoinFilter extends AbstractFilter {

    public static final String JOIN_OPTION = "join";

    private static final class JoinFilterLoader {
        private static final JoinFilter INSTANCE = new JoinFilter();
    }

    private JoinFilter() {
        if (JoinFilterLoader.INSTANCE != null) {
            throw new IllegalStateException("INSTANCE was already defined.");
        }
    }

    public static JoinFilter getInstance() {
        return JoinFilterLoader.INSTANCE;
    }

    @Override
    public Expression apply(Expression expression, ExpressionContext expressionContext) {
        if (!expression.containsOption(JOIN_OPTION) || expressionContext == ExpressionContext.PLUGIN_DATA_SLY_USE || expressionContext
                == ExpressionContext.PLUGIN_DATA_SLY_TEMPLATE || expressionContext == ExpressionContext.PLUGIN_DATA_SLY_CALL) {
            return expression;
        }
        ExpressionNode translation =
                new RuntimeCall(RuntimeFunction.JOIN, expression.getRoot(), expression.removeOption(JOIN_OPTION));
        return expression.withNode(translation);
    }
}
