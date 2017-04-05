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
package org.apache.sling.scripting.esx;

import java.io.IOException;
import java.io.Reader;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EsxScriptEngine extends AbstractSlingScriptEngine {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public EsxScriptEngine(EsxScriptEngineFactory scriptEngineFactory) {
        super(scriptEngineFactory);
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        log.debug("starting to eval ESX Script");
        Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        SlingScriptHelper scriptHelper = (SlingScriptHelper) bindings.get("sling");
        Resource scriptResource = scriptHelper.getScript().getScriptResource();
        Resource resource = scriptHelper.getRequest().getResource();

        ModuleScript moduleScript = new ModuleScript(ModuleScript.JS_FILE, scriptResource);
        //public Module (EsxScriptEngineFactory factory, Resource resource, ModuleScript moduleScript, String id, Module parent, SlingScriptHelper scriptHelper) throws ScriptException {

        Module module = new Module(
                (EsxScriptEngineFactory) getFactory(),
                resource, moduleScript, scriptResource.getPath(), null,
                scriptHelper,
                Module.LOADER_JS
        );
        
        try {
            Object moduleResults = module.require(scriptResource.getPath());
            
            String result = ((EsxScriptEngineFactory) getFactory()).getNashornEngine().eval(
                    "if(exports.render && typeof exports.render === 'function') { exports.render('test'); }"
                  + " else if(typeof exports === 'class') { new exports().render('function') } else {"
                            + "'You need to define either a render function or export an object with a render method'; }", module).toString();
            
            context.getWriter().write(result);
            
        } catch (IOException ex) {
            throw new ScriptException(ex);
        }        
               
        return null;
    }
}
