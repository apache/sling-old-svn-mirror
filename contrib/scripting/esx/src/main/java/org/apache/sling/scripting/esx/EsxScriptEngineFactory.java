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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.apache.felix.scr.annotations.Activate;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.scripting.esx.services.ScriptSandboxService;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

@Component(label = "ESX Scripting Engine Factory", description = "", metatype = true)
@Service
@Properties({
    @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation")
    ,
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Scripting Engine for Ecmas Script using Node JS like module loader")
    ,
        @Property(name = "compatible.javax.script.name", value = "esx")
})
public class EsxScriptEngineFactory extends AbstractScriptEngineFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private ScriptEngine nashornScriptEngine;
    
    @Reference
    ScriptModuleCache moduleCache;

    @Reference
    ScriptSandboxService sandboxService;

    public EsxScriptEngineFactory() {
        setNames("esx", "ESX");
        setExtensions("esx", "ESX");
    }

    @Override
    public String getLanguageName() {
        return "ESX";
    }

    @Override
    public String getLanguageVersion() {
        return "1.0";
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new EsxScriptEngine(this);
    }

    public ScriptSandboxService getSandboxService() {
        return sandboxService;
    }
    /**
     * 
     * @return 
     */
    public ScriptModuleCache getModuleCache() {
        return moduleCache;        
    }
    
    /**
     *
     * @return
     */
    public ScriptEngine getNashornEngine() {
        return nashornScriptEngine;
    }

    @Deactivate
    private void deactivate() {
        log.debug("Deactivating Engine");
    }

    @Activate
    protected void activate(ComponentContext context) {
        log.debug("Starting Engine");
        // create one script engine
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        this.nashornScriptEngine = factory.getScriptEngine();
        log.debug("Engine started");
    }
}
