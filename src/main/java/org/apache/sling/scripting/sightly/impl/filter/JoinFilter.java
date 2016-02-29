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

import java.util.Collection;
import java.util.Iterator;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.MapLiteral;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.RuntimeCall;
import org.apache.sling.scripting.sightly.impl.utils.RenderUtils;
import org.apache.sling.scripting.sightly.render.RenderContext;

/**
 * Filter providing support for the {@code join} option applied to arrays.
 */
@Component
@Service({Filter.class, RuntimeExtension.class})
@Properties({
        @Property(name = RuntimeExtension.NAME, value = JoinFilter.JOIN_FUNCTION)
})
public class JoinFilter extends FilterComponent implements RuntimeExtension {

    public static final String JOIN_OPTION = "join";
    public static final String JOIN_FUNCTION = "join";

    @Override
    public Expression apply(Expression expression, ExpressionContext expressionContext) {
        if (!expression.containsOption(JOIN_OPTION) || expressionContext == ExpressionContext.PLUGIN_DATA_SLY_USE || expressionContext
                == ExpressionContext.PLUGIN_DATA_SLY_TEMPLATE || expressionContext == ExpressionContext.PLUGIN_DATA_SLY_CALL) {
            return expression;
        }
        ExpressionNode translation = new RuntimeCall(JOIN_FUNCTION, expression.getRoot(), expression.removeOption(JOIN_OPTION));
        return expression.withNode(translation);
    }

    @Override
    public Object call(final RenderContext renderContext, Object... arguments) {
        if (arguments.length != 2) {
            throw new SightlyException("Join function must be called with two arguments.");
        }
        Object joinArgument = arguments[0];
        Collection<?> collection = RenderUtils.toCollection(joinArgument);
        String joinString = RenderUtils.toString(arguments[1]);
        return join(collection, joinString);
    }

    private String join(Collection<?> collection, String joinString) {
        StringBuilder sb = new StringBuilder();
        Iterator<?> iterator = collection.iterator();
        while (iterator.hasNext()) {
            String element = RenderUtils.toString(iterator.next());
            sb.append(element);
            if (iterator.hasNext()) {
                sb.append(joinString);
            }
        }
        return sb.toString();
    }
}
