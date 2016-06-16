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
 * Implementation for the format filter &amp; runtime support.
 */
public class FormatFilter extends AbstractFilter {

    public static final String FORMAT_OPTION = "format";

    private static final class FormatFilterLoader {
        private static final FormatFilter INSTANCE = new FormatFilter();
    }

    private FormatFilter() {
        if (FormatFilterLoader.INSTANCE != null) {
            throw new IllegalStateException("INSTANCE was already defined.");
        }
    }

    public static FormatFilter getInstance() {
        return FormatFilterLoader.INSTANCE;
    }

    @Override
    public Expression apply(Expression expression, ExpressionContext expressionContext) {
        //todo: if the expression is a string constant, we can produce the transformation at
        //compile time, with no need of a runtime function
        if (!expression.containsOption(FORMAT_OPTION) || expressionContext == ExpressionContext.PLUGIN_DATA_SLY_USE || expressionContext
                == ExpressionContext.PLUGIN_DATA_SLY_TEMPLATE || expressionContext == ExpressionContext.PLUGIN_DATA_SLY_CALL) {
            return expression;
        }
        ExpressionNode translation =
                new RuntimeCall(RuntimeFunction.FORMAT, expression.getRoot(), expression.removeOption(FORMAT_OPTION));
        return expression.withNode(translation);
    }
}
