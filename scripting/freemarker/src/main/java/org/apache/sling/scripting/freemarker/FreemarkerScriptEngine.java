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
package org.apache.sling.scripting.freemarker;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * A ScriptEngine that uses {@link http://freemarker.org/ FreeMarker} templates
 * to render a Resource in HTML.
 */
public class FreemarkerScriptEngine extends AbstractSlingScriptEngine {
    private static final Logger log = LoggerFactory.getLogger(FreemarkerScriptEngine.class);

    private final Configuration configuration;

    public FreemarkerScriptEngine(ScriptEngineFactory factory) {
        super(factory);
        configuration = new Configuration();
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

        String scriptName = helper.getScript().getScriptResource().getURI();
        
        try {
            Template tmpl = new Template(scriptName, reader, configuration);
            tmpl.process(bindings, scriptContext.getWriter());
        } catch (Throwable t) {
            log.error("Failure running Freemarker script.", t);
            throw new ScriptException("Failure running FreeMarker script "
                + scriptName);
        }

        return null;
    }

}