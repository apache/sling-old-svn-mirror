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
import org.apache.sling.scripting.sightly.impl.compiler.Syntax;
import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall;

/**
 * XSS filter implementation.
 */
public class XSSFilter extends AbstractFilter {

    private static final class XSSFilterLoader {
        private static final XSSFilter INSTANCE = new XSSFilter();
    }

    private XSSFilter() {
        if (XSSFilterLoader.INSTANCE != null) {
            throw new IllegalStateException("INSTANCE was already defined.");
        }
        priority = 110;
    }

    public static XSSFilter getInstance() {
        return XSSFilterLoader.INSTANCE;
    }

    @Override
    public Expression apply(Expression expression, ExpressionContext expressionContext) {
        if (expressionContext == ExpressionContext.PLUGIN_DATA_SLY_USE || expressionContext == ExpressionContext.PLUGIN_DATA_SLY_TEMPLATE
                || expressionContext == ExpressionContext.PLUGIN_DATA_SLY_CALL) {
            return expression;
        }
        ExpressionNode context = expression.removeOption(Syntax.CONTEXT_OPTION);
        if (context != null) {
            return expression.withNode(new RuntimeCall(RuntimeFunction.XSS, expression.getRoot(), context));
        }
        return expression;
    }

}
