/*
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
 */
package org.apache.sling.scripting.thymeleaf;

import java.io.Reader;
import java.util.Locale;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

public class ThymeleafScriptEngine extends AbstractSlingScriptEngine {

    private final TemplateEngine templateEngine;

    public ThymeleafScriptEngine(final ScriptEngineFactory scriptEngineFactory, final TemplateEngine templateEngine) {
        super(scriptEngineFactory);
        this.templateEngine = templateEngine;
    }

    @Override
    public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
        final Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        final SlingScriptHelper helper = (SlingScriptHelper) bindings.get(SlingBindings.SLING);

        if (helper == null) {
            throw new ScriptException("SlingScriptHelper missing from bindings");
        }

        final Locale locale = helper.getRequest().getLocale();
        final String scriptName = helper.getScript().getScriptResource().getPath();

        try {
            final IContext context = new SlingContext(locale, bindings, reader);
            templateEngine.process(scriptName, context, scriptContext.getWriter());
        } catch (Exception e) {
            final String message = String.format("Failure rendering Thymeleaf template '%s': %s", scriptName, e.getMessage());
            throw new ScriptException(message);
        }

        return null;
    }

}
