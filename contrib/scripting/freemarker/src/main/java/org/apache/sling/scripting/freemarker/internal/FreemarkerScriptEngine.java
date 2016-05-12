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
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import freemarker.ext.beans.BeansWrapper;
import freemarker.log.Logger;
import freemarker.template.TemplateHashModel;
import freemarker.template.Version;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * A ScriptEngine that uses <a href="http://freemarker.org/">FreeMarker</a> templates
 * to render a Resource in HTML.
 */
public class FreemarkerScriptEngine extends AbstractSlingScriptEngine {
    private static final Logger log = Logger.getLogger(FreemarkerScriptEngine.class.getName());

    private final Version version = new Version(2, 3, 24);

    private final Configuration configuration;

    private final BeansWrapper beansWrapper;

    private final TemplateHashModel statics;

    public FreemarkerScriptEngine(ScriptEngineFactory factory) {
        super(factory);
        configuration = new Configuration(version);
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        beansWrapper = new BeansWrapper(version);
        statics = beansWrapper.getStaticModels();
    }

    public Object eval(Reader reader, ScriptContext scriptContext)
            throws ScriptException {
        Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        SlingScriptHelper helper = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
        if (helper == null) {
            throw new ScriptException("SlingScriptHelper missing from bindings");
        }

        // ensure GET request
        if (!"GET".equals(helper.getRequest().getMethod())) {
            throw new ScriptException(
                "FreeMarker templates only support GET requests");
        }

        String scriptName = helper.getScript().getScriptResource().getPath();

        try {
            Template tmpl = new Template(scriptName, reader, configuration);
            bindings.put("statics", statics);
            tmpl.process(bindings, scriptContext.getWriter());
        } catch (Throwable t) {
            log.error("Failure running Freemarker script.", t);
            throw new ScriptException("Failure running FreeMarker script "
                + scriptName);
        }

        return null;
    }

}