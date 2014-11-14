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
package org.apache.sling.scripting.sightly.compiler.frontend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.Syntax;
import org.apache.sling.scripting.sightly.compiler.api.Filter;
import org.apache.sling.scripting.sightly.compiler.api.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.api.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.api.expression.node.BinaryOperation;
import org.apache.sling.scripting.sightly.compiler.api.expression.node.BinaryOperator;
import org.apache.sling.scripting.sightly.compiler.api.expression.node.StringConstant;
import org.apache.sling.scripting.sightly.compiler.api.plugin.MarkupContext;
import org.apache.sling.scripting.sightly.compiler.api.Filter;
import org.apache.sling.scripting.sightly.compiler.api.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.api.expression.node.BinaryOperation;
import org.apache.sling.scripting.sightly.compiler.api.expression.node.StringConstant;

/**
 * This object wraps expressions in filter applications depending
 * on options
 */
class ExpressionWrapper {

    private final List<Filter> filters;

    ExpressionWrapper(Collection<Filter> filters) {
        this.filters = new ArrayList<Filter>();
        this.filters.addAll(filters);
        Collections.sort(this.filters);
    }

    public Expression transform(Interpolation interpolation, MarkupContext markupContext) {
        ArrayList<ExpressionNode> nodes = new ArrayList<ExpressionNode>();
        HashMap<String, ExpressionNode> options = new HashMap<String, ExpressionNode>();
        for (Fragment fragment : interpolation.getFragments()) {
            if (fragment.isString()) {
                nodes.add(new StringConstant(fragment.getText()));
            } else {
                Expression expression = fragment.getExpression();
                options.putAll(expression.getOptions());
                nodes.add(transformExpr(expression, markupContext).getRoot());
            }
        }
        ExpressionNode root = join(nodes);
        if (interpolation.size() > 1 && options.containsKey(Syntax.CONTEXT_OPTION)) {
            //context must not be calculated by merging
            options.remove(Syntax.CONTEXT_OPTION);
        }
        return new Expression(root, options);
    }

    private Expression applyFilters(Expression expression) {
        Expression result = expression;
        for (Filter filter : filters) {
            result = filter.apply(result);
        }
        return result;
    }

    public Expression adjustToContext(Expression expression, MarkupContext markupContext) {
        if (expression.containsOption(Syntax.CONTEXT_OPTION)) {
            return expression;
        }
        Map<String, ExpressionNode> opt = addDefaultContext(Collections.<String, ExpressionNode>emptyMap(), markupContext);
        Expression result = applyFilters(new Expression(expression.getRoot(), opt));
        return expression.withNode(result.getRoot());
    }

    private ExpressionNode join(List<ExpressionNode> nodes) {
        if (nodes.isEmpty()) {
            return StringConstant.EMPTY;
        }
        ExpressionNode root = nodes.get(0);
        for (int i = 1; i < nodes.size(); i++) {
            ExpressionNode node = nodes.get(i);
            root = new BinaryOperation(BinaryOperator.CONCATENATE, root, node);
        }
        return root;
    }

    private Expression transformExpr(Expression expression, MarkupContext markupContext) {
        expression = addDefaultContext(expression, markupContext);
        return applyFilters(expression);
    }

    private Expression addDefaultContext(Expression expression, MarkupContext context) {
        return new Expression(expression.getRoot(), addDefaultContext(expression.getOptions(), context));
    }

    private Map<String, ExpressionNode> addDefaultContext(Map<String, ExpressionNode> options, MarkupContext context) {
        if (context == null || options.containsKey(Syntax.CONTEXT_OPTION)) {
            return options;
        }
        HashMap<String, ExpressionNode> newOptions = new HashMap<String, ExpressionNode>(options);
        newOptions.put(Syntax.CONTEXT_OPTION, new StringConstant(context.getName()));
        return newOptions;
    }

}
