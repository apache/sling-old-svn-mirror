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

import java.util.Collection;
import java.util.Iterator;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import org.apache.sling.scripting.sightly.api.ExtensionInstance;
import org.apache.sling.scripting.sightly.api.RenderContext;
import org.apache.sling.scripting.sightly.api.RuntimeExtension;
import org.apache.sling.scripting.sightly.api.RuntimeExtensionException;
import org.apache.sling.scripting.sightly.compiler.api.Filter;
import org.apache.sling.scripting.sightly.compiler.api.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.api.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.api.expression.node.RuntimeCall;
import org.apache.sling.scripting.sightly.common.Dynamic;
import org.apache.sling.scripting.sightly.api.ExtensionInstance;
import org.apache.sling.scripting.sightly.api.RenderContext;
import org.apache.sling.scripting.sightly.api.RuntimeExtension;
import org.apache.sling.scripting.sightly.api.RuntimeExtensionException;
import org.apache.sling.scripting.sightly.common.Dynamic;
import org.apache.sling.scripting.sightly.compiler.api.Filter;
import org.apache.sling.scripting.sightly.compiler.api.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.api.expression.ExpressionNode;
import org.apache.sling.scripting.sightly.compiler.api.expression.node.RuntimeCall;

/**
 * Filter providing support for the {@code join} option applied to arrays.
 */
@Component
@Service({Filter.class, RuntimeExtension.class})
public class JoinFilter extends FilterComponent implements RuntimeExtension {

    public static final String JOIN_OPTION = "join";
    public static final String JOIN_FUNCTION = "join";

    @Override
    public Expression apply(Expression expression) {
        if (!expression.containsOption(JOIN_OPTION)) {
            return expression;
        }
        ExpressionNode argumentNode = expression.getOption(JOIN_OPTION);
        ExpressionNode joinResult = new RuntimeCall(JOIN_FUNCTION, expression.getRoot(), argumentNode);
        return expression.withNode(joinResult).removeOptions(JOIN_OPTION);
    }

    @Override
    public String name() {
        return JOIN_FUNCTION;
    }

    @Override
    public ExtensionInstance provide(RenderContext renderContext) {
        final Dynamic dynamic = new Dynamic(renderContext.getObjectModel());

        return new ExtensionInstance() {
            @Override
            public Object call(Object... arguments) {
                if (arguments.length != 2) {
                    throw new RuntimeExtensionException("Join function must be called with two arguments.");
                }
                Collection<?> collection = dynamic.coerceToCollection(arguments[0]);
                String joinString = dynamic.coerceToString(arguments[1]);
                return join(collection, joinString);
            }

            private String join(Collection<?> collection, String joinString) {
                StringBuilder sb = new StringBuilder();
                Iterator<?> iterator = collection.iterator();
                while (iterator.hasNext()) {
                    String element = dynamic.coerceToString(iterator.next());
                    sb.append(element);
                    if (iterator.hasNext()) {
                        sb.append(joinString);
                    }
                }
                return sb.toString();
            }
        };

    }
}
