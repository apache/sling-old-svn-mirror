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

import org.apache.sling.scripting.sightly.compiler.Syntax;
import org.apache.sling.scripting.sightly.compiler.api.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.api.plugin.CompilerContext;
import org.apache.sling.scripting.sightly.compiler.api.plugin.MarkupContext;
import org.apache.sling.scripting.sightly.compiler.util.SymbolGenerator;
import org.apache.sling.scripting.sightly.compiler.Syntax;
import org.apache.sling.scripting.sightly.compiler.api.expression.Expression;
import org.apache.sling.scripting.sightly.compiler.api.plugin.CompilerContext;
import org.apache.sling.scripting.sightly.compiler.util.SymbolGenerator;


/**
 * Default implementation for the compiler context
 * @see org.apache.sling.scripting.sightly.compiler.api.plugin.Plugin
 */
public class DefaultCompilerContext implements CompilerContext {

    private SymbolGenerator symbolGenerator;
    private ExpressionWrapper expressionWrapper;

    DefaultCompilerContext(SymbolGenerator symbolGenerator, ExpressionWrapper wrapper) {
        this.symbolGenerator = symbolGenerator;
        this.expressionWrapper = wrapper;
    }

    @Override
    public String generateVariable(String hint) {
        return symbolGenerator.next(hint);
    }

    @Override
    public Expression adjustToContext(Expression expression, MarkupContext context) {
        if (!expression.getOptions().containsKey(Syntax.CONTEXT_OPTION)) {
            return expressionWrapper.adjustToContext(expression, context);
        }
        return expression;
    }
}
