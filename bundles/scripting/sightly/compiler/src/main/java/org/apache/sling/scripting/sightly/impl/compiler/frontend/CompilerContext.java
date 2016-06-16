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
package org.apache.sling.scripting.sightly.impl.compiler.frontend;

import org.apache.sling.scripting.sightly.impl.compiler.PushStream;
import org.apache.sling.scripting.sightly.impl.compiler.Syntax;
import org.apache.sling.scripting.sightly.compiler.expression.Expression;
import org.apache.sling.scripting.sightly.impl.filter.ExpressionContext;
import org.apache.sling.scripting.sightly.compiler.expression.MarkupContext;
import org.apache.sling.scripting.sightly.impl.compiler.util.SymbolGenerator;


/**
 * Default implementation for the compiler context
 * @see org.apache.sling.scripting.sightly.impl.plugin.Plugin
 */
public class CompilerContext {

    private SymbolGenerator symbolGenerator;
    private ExpressionWrapper expressionWrapper;
    private PushStream pushStream;

    public CompilerContext(SymbolGenerator symbolGenerator, ExpressionWrapper wrapper, PushStream pushStream) {
        this.symbolGenerator = symbolGenerator;
        this.expressionWrapper = wrapper;
        this.pushStream = pushStream;
    }

    public String generateVariable(String hint) {
        return symbolGenerator.next(hint);
    }

    public Expression adjustToContext(Expression expression, MarkupContext context, ExpressionContext expressionContext) {
        if (!expression.getOptions().containsKey(Syntax.CONTEXT_OPTION)) {
            return expressionWrapper.adjustToContext(expression, context, expressionContext);
        }
        return expression;
    }

    public PushStream getPushStream() {
        return pushStream;
    }
}
