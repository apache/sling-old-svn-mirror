/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.freemarker.internal;

import java.io.Reader;
import java.nio.charset.StandardCharsets;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;

public class FreemarkerScriptEngine extends AbstractSlingScriptEngine {

    private final Configuration configuration;

    private final FreemarkerScriptEngineFactory freemarkerScriptEngineFactory;

    private final Logger logger = Logger.getLogger(FreemarkerScriptEngine.class.getName());

    public FreemarkerScriptEngine(final FreemarkerScriptEngineFactory freemarkerScriptEngineFactory) {
        super(freemarkerScriptEngineFactory);
        this.freemarkerScriptEngineFactory = freemarkerScriptEngineFactory;
        configuration = new Configuration(Configuration.getVersion());
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
    }

    public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
        final Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        final SlingScriptHelper helper = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
        if (helper == null) {
            throw new ScriptException("SlingScriptHelper missing from bindings");
        }

        // ensure GET request
        if (!"GET".equals(helper.getRequest().getMethod())) {
            throw new ScriptException(
                "FreeMarker templates only support GET requests");
        }

        freemarkerScriptEngineFactory.getTemplateModels().forEach(bindings::put);

        final String scriptName = helper.getScript().getScriptResource().getPath();

        try {
            final Template template = new Template(scriptName, reader, configuration);
            template.process(bindings, scriptContext.getWriter());
        } catch (Throwable t) {
            final String message = String.format("Failure processing FreeMarker template %s.", scriptName);
            logger.error(message, t);
            throw new ScriptException(message);
        }

        return null;
    }

}
