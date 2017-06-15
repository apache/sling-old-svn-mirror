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

import java.util.Map;

import org.apache.sling.scripting.sightly.compiler.RuntimeFunction;
import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.MapLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall;

/**
 * The {@code URIManipulationFilter} provides support for Sightly's URI Manipulation options according to the
 * <a href="https://github.com/Adobe-Marketing-Cloud/htl-spec/blob/1.2/SPECIFICATION.md">language specification</a>
 */
public class URIManipulationFilter extends AbstractFilter {

    public static final String SCHEME = "scheme";
    public static final String DOMAIN = "domain";
    public static final String PATH = "path";
    public static final String APPEND_PATH = "appendPath";
    public static final String PREPEND_PATH = "prependPath";
    public static final String SELECTORS = "selectors";
    public static final String ADD_SELECTORS = "addSelectors";
    public static final String REMOVE_SELECTORS = "removeSelectors";
    public static final String EXTENSION = "extension";
    public static final String SUFFIX = "suffix";
    public static final String PREPEND_SUFFIX = "prependSuffix";
    public static final String APPEND_SUFFIX = "appendSuffix";
    public static final String FRAGMENT = "fragment";
    public static final String QUERY = "query";
    public static final String ADD_QUERY = "addQuery";
    public static final String REMOVE_QUERY = "removeQuery";


    private static final class URIManipulationFilterLoader {
        private static final URIManipulationFilter INSTANCE = new URIManipulationFilter();
    }

    private URIManipulationFilter() {
        if (URIManipulationFilterLoader.INSTANCE != null) {
            throw new IllegalStateException("INSTANCE was already defined.");
        }
    }

    public static URIManipulationFilter getInstance() {
        return URIManipulationFilterLoader.INSTANCE;
    }

    @Override
    public Expression apply(Expression expression, ExpressionContext expressionContext) {
        if ((expression.containsOption(SCHEME) || expression.containsOption(DOMAIN) || expression.containsOption(PATH) || expression
                .containsOption(APPEND_PATH) || expression.containsOption(PREPEND_PATH) || expression.containsOption(SELECTORS) ||
                expression.containsOption(ADD_SELECTORS) || expression.containsOption(REMOVE_SELECTORS) || expression.containsOption
                (EXTENSION) || expression.containsOption(SUFFIX) || expression.containsOption(PREPEND_SUFFIX) || expression
                .containsOption(APPEND_SUFFIX) || expression.containsOption(FRAGMENT) || expression.containsOption(QUERY) || expression
                .containsOption(ADD_QUERY) || expression.containsOption(REMOVE_QUERY)) && expressionContext != ExpressionContext
                .PLUGIN_DATA_SLY_USE && expressionContext
                != ExpressionContext.PLUGIN_DATA_SLY_TEMPLATE && expressionContext != ExpressionContext.PLUGIN_DATA_SLY_CALL &&
                expressionContext != ExpressionContext.PLUGIN_DATA_SLY_RESOURCE) {
            Map<String, ExpressionNode> uriOptions = getFilterOptions(expression, SCHEME, DOMAIN, PATH, APPEND_PATH, PREPEND_PATH,
                    SELECTORS, ADD_SELECTORS, REMOVE_SELECTORS, EXTENSION, SUFFIX, PREPEND_SUFFIX, APPEND_SUFFIX, FRAGMENT, QUERY,
                    ADD_QUERY, REMOVE_QUERY);
            if (uriOptions.size() > 0) {
                ExpressionNode translation =
                        new RuntimeCall(RuntimeFunction.URI_MANIPULATION, expression.getRoot(), new MapLiteral(uriOptions));
                return expression.withNode(translation);
            }
        }
        return expression;
    }


}
