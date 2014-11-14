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

package org.apache.sling.scripting.sightly.filter;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

import org.apache.sling.scripting.sightly.compiler.api.Filter;
import org.apache.sling.scripting.sightly.compiler.api.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.api.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.api.expression.node.NullLiteral;
import org.apache.sling.scripting.sightly.compiler.api.expression.node.RuntimeCall;
import org.apache.sling.scripting.sightly.compiler.api.Filter;
import org.apache.sling.scripting.sightly.compiler.api.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.api.expression.node.NullLiteral;
import org.apache.sling.scripting.sightly.compiler.api.expression.node.RuntimeCall;

/**
 * Filter for i18n translation
 */
@Component
@Service(Filter.class)
@Property(name = FilterComponent.PRIORITY, intValue = 90)
public class I18nFilter extends FilterComponent {

    public static final String FUNCTION = "i18nTranslation";

    public static final String I18N_OPTION = "i18n";
    public static final String HINT_OPTION = "hint";
    public static final String LOCALE_OPTION = "locale";

    @Override
    public Expression apply(Expression expression) {
        if (!expression.containsOption(I18N_OPTION)) {
            return expression;
        }
        ExpressionNode hint = option(expression, HINT_OPTION);
        ExpressionNode locale = option(expression, LOCALE_OPTION);
        ExpressionNode translation = new RuntimeCall(FUNCTION, expression.getRoot(), locale, hint);
        return expression.withNode(translation).removeOptions(HINT_OPTION, LOCALE_OPTION);
    }

    private ExpressionNode option(Expression expression, String optionName) {
        ExpressionNode node = expression.getOption(optionName);
        if (node == null) {
            return NullLiteral.INSTANCE;
        }
        return node;
    }
}
