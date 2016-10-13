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

import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.MapLiteral;
import org.apache.sling.scripting.sightly.compiler.expression.nodes.RuntimeCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter providing support for the {@code dateFormat} option applied to format a date.
 */
public class DateFormatFilter extends AbstractFilter {

    public static final String DATE_FORMAT = "dateFormat";
    
    public static final String LOCALE_OPTION = "locale";

    private static final Logger LOG = LoggerFactory.getLogger(DateFormatFilter.class);

    private static final class DateFormatFilterLoader {
        private static final DateFormatFilter INSTANCE = new DateFormatFilter();
    }

    private DateFormatFilter() {
        if (DateFormatFilterLoader.INSTANCE != null) {
            throw new IllegalStateException("INSTANCE was already defined.");
        }
        priority = 90;
    }

    public static DateFormatFilter getInstance() {
        return DateFormatFilterLoader.INSTANCE;
    }


    @Override
    public Expression apply(Expression expression, ExpressionContext expressionContext) {
    	LOG.trace("in dateFormatFilter#apply..");
    	
    	if (!expression.containsOption(DATE_FORMAT)) {
            return expression;
        }
        ExpressionNode translation =
                new RuntimeCall(DATE_FORMAT, expression.getRoot(), new MapLiteral
        (getFilterOptions(expression, DATE_FORMAT, LOCALE_OPTION)));
        return expression.withNode(translation);
    }
}
