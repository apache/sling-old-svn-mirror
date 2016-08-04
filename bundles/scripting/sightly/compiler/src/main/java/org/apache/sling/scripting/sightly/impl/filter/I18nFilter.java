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
import org.apache.sling.scripting.sightly.compiler.expression.nodes.MapLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall;

/**
 * Filter for i18n translation
 */
public final class I18nFilter extends AbstractFilter {

    public static final String I18N_OPTION = "i18n";
    public static final String HINT_OPTION = "hint";
    public static final String LOCALE_OPTION = "locale";
    public static final String BASENAME_OPTION = "basename";

    private static final class I18nFilterLoader {
        private static final I18nFilter INSTANCE = new I18nFilter();
    }

    private I18nFilter() {
        if (I18nFilterLoader.INSTANCE != null) {
            throw new IllegalStateException("INSTANCE was already defined.");
        }
        priority = 90;
    }

    public static I18nFilter getInstance() {
        return I18nFilterLoader.INSTANCE;
    }

    @Override
    public Expression apply(Expression expression, ExpressionContext expressionContext) {
        if (!expression.containsOption(I18N_OPTION) || expressionContext == ExpressionContext.PLUGIN_DATA_SLY_USE || expressionContext
                == ExpressionContext.PLUGIN_DATA_SLY_TEMPLATE || expressionContext == ExpressionContext.PLUGIN_DATA_SLY_CALL) {
            return expression;
        }
        ExpressionNode translation = new RuntimeCall(RuntimeFunction.I18N, expression.getRoot(), new MapLiteral
                (getFilterOptions(expression, HINT_OPTION, LOCALE_OPTION, BASENAME_OPTION)));
        expression.removeOption(I18N_OPTION);
        return expression.withNode(translation);
    }
}
