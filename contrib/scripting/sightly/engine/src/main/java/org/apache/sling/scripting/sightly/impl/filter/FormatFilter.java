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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.extension.RuntimeExtension;
import org.apache.sling.scripting.sightly.impl.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.impl.compiler.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.impl.compiler.expression.node.RuntimeCall;
import org.apache.sling.scripting.sightly.render.RenderContext;

/**
 * Implementation for the format filter & runtime support.
 */
@Component
@Service({Filter.class, RuntimeExtension.class})
@Properties({
        @Property(name = RuntimeExtension.NAME, value = FormatFilter.FORMAT_FUNCTION)
})
public class FormatFilter extends FilterComponent implements RuntimeExtension {

    public static final String FORMAT_OPTION = "format";
    public static final String FORMAT_FUNCTION = "format";

    private static final Pattern PLACEHOLDER_REGEX = Pattern.compile("\\{\\d}");

    @Override
    public Expression apply(Expression expression) {
        //todo: if the expression is a string constant, we can produce the transformation at
        //compile time, with no need of a runtime function
        if (!expression.containsOption(FORMAT_OPTION)) {
            return expression;
        }
        ExpressionNode argNode = expression.getOption(FORMAT_OPTION);
        ExpressionNode formattedNode = new RuntimeCall(FORMAT_FUNCTION, expression.getRoot(), argNode);
        return expression.withNode(formattedNode).removeOptions(FORMAT_OPTION);
    }

    @Override
    public Object call(final RenderContext renderContext, Object... arguments) {
        if (arguments.length != 2) {
            throw new SightlyException("Format function must be called with two arguments");
        }
        String source = renderContext.toString(arguments[0]);
        Object[] params = decodeParams(renderContext, arguments[1]);
        return replace(renderContext, source, params);
    }

    private Object[] decodeParams(final RenderContext renderContext, Object paramObj) {
        if (renderContext.isCollection(paramObj)) {
            return renderContext.toCollection(paramObj).toArray();
        }
        return new Object[] {paramObj};
    }

    private String replace(final RenderContext renderContext, String source, Object[] params) {
        Matcher matcher = PLACEHOLDER_REGEX.matcher(source);
        StringBuilder builder = new StringBuilder();
        int lastPos = 0;
        boolean matched = true;
        while (matched) {
            matched = matcher.find();
            if (matched) {
                int paramIndex = placeholderIndex(matcher.group());
                String replacement = param(renderContext, params, paramIndex);
                int matchStart = matcher.start();
                int matchEnd = matcher.end();
                builder.append(source, lastPos, matchStart).append(replacement);
                lastPos = matchEnd;
            }
        }
        builder.append(source, lastPos, source.length());
        return builder.toString();
    }

    private String param(final RenderContext renderContext, Object[] params, int index) {
        if (index >= 0 && index < params.length) {
            return renderContext.toString(params[index]);
        }
        return "";
    }

    private int placeholderIndex(String placeholder) {
        return Integer.parseInt(placeholder.substring(1, placeholder.length() - 1));
    }
}
