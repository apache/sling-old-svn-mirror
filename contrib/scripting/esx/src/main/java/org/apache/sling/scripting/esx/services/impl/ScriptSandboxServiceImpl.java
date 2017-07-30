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
package org.apache.sling.scripting.esx.services.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.scripting.esx.ScriptModuleCache;
import org.apache.sling.scripting.esx.services.ScriptSandboxService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventHandler;
import org.slf4j.LoggerFactory;

@Component
@Service(value = {ScriptSandboxService.class})
@Property(name = org.osgi.service.event.EventConstants.EVENT_TOPIC,
        value = {SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_REMOVED})
public class ScriptSandboxServiceImpl implements ScriptSandboxService {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(getClass());
    private final String SLING_BABEL_SOURCE_CODE = "/resources/internal/sling-babel.js";

    private String slingBabelSource = null;
    private ScriptEngine scriptEngine;
    private Object babelOptions = null;
    private Object babel = null;

    @Override
    public String compileSource(String source) throws ScriptException {      
        Invocable inv = (Invocable) scriptEngine;
        JSObject rs;
        try {       
            rs = (JSObject) inv.invokeMethod(babel, "transform", source, babelOptions);
            return rs.getMember("code").toString(); 
        } catch (ScriptException ex) {
            throw new ScriptException(ex);
        } catch (NoSuchMethodException ex) {
            throw new ScriptException(ex);
        }
    }

    /**
     *
     * @param context
     */
    @Activate
    protected void activate(ComponentContext context) {
        try {
            this.slingBabelSource = "//@sourceURL=" + getClass().getCanonicalName() + "\n load( { name : \"" + getClass().getCanonicalName() + "\", script: \""
                    + StringEscapeUtils.escapeEcmaScript(new String(IOUtils.toByteArray(
                                            context.getBundleContext().getBundle()
                                            .getEntry(this.SLING_BABEL_SOURCE_CODE).openStream())))
                    + "\"} )";
        } catch (Exception ex) {
           log.error("failed to load babel source", ex);
        }
        
        String options = "var config = { "
                + "    presets: [\"es2015\"], "
                + "    compact: 'true',"
                + "    plugins: [[\"transform-react-jsx\", { pragma : \"SlingEsx.createElement\"}]] "
                //+ // disabled sandbox plugin for 
                //"    \"plugins\": [\"SlingSandbox, [\"transform-react-jsx\", { \"pragma\" : \"SlingEsx.createElement\"}]] \n" +        
                + "    }"; 
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        this.scriptEngine = factory.getScriptEngine();
        try {
            log.info("trying to load babel into the system");
            this.scriptEngine.eval(options);
            this.babelOptions = this.scriptEngine.get("config");
            scriptEngine.eval(slingBabelSource);
            this.babel = this.scriptEngine.get("SlingBabel");
            log.info("Babel loaded");
        } catch (ScriptException ex) {
            log.error("coudlnt load babel options", ex);
        }
    }
}
