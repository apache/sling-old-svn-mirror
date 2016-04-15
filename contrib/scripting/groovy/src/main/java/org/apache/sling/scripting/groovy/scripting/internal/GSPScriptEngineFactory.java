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

import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.codehaus.groovy.util.ReleaseInfo;

/**
 * Script engine for Groovy Scripting Pages.
 */
@Component
@Service
@Properties({
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="service.description", value="GSP Script Engine"),
    @Property(name="compatible.javax.script.name", value="groovy")
})
public class GSPScriptEngineFactory extends AbstractScriptEngineFactory {
    
    public GSPScriptEngineFactory() {
        setNames("gsp", "GSP");
    }

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    public String getLanguageName() {
        return "Groovy Scripting Pages";
    }
    
    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("gsp");
    }

    public String getLanguageVersion() {
        return ReleaseInfo.getVersion();
    }

    public ScriptEngine getScriptEngine() {
        return new GSPScriptEngine(this, dynamicClassLoaderManager.getDynamicClassLoader());
    }

}
