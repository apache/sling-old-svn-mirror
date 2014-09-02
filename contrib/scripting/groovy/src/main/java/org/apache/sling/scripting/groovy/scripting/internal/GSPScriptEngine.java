/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.scripting.groovy.scripting.internal;

import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;
import groovy.text.TemplateEngine;

import java.io.IOException;
import java.io.Reader;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.sling.scripting.api.AbstractSlingScriptEngine;

/**
 * The actual GSP Script Engine, which simply wraps Groovy's 
 */
public class GSPScriptEngine extends AbstractSlingScriptEngine {

    private TemplateEngine templateEngine;

    public GSPScriptEngine(ScriptEngineFactory scriptEngineFactory, ClassLoader classLoader) {
        super(scriptEngineFactory);
        this.templateEngine = new GStringTemplateEngine(classLoader);
    }
    
    public Object eval(Reader reader, ScriptContext ctx) throws ScriptException {
        Template template = null;
        try {
            template = templateEngine.createTemplate(reader);
        } catch (IOException e) {
            throw new ScriptException("Unable to compile GSP script: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new ScriptException("Unable to compile GSP script: " + e.getMessage());
        }

        Bindings bindings = ctx.getBindings(ScriptContext.ENGINE_SCOPE);

        Writable result = template.make(bindings);

        try {
            result.writeTo(ctx.getWriter());
        } catch (IOException e) {
            throw new ScriptException("Unable to write result of script execution: " + e.getMessage());
        }

        return null;
    }

}
