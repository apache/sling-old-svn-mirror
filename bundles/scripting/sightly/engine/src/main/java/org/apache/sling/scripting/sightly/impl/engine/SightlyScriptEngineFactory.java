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

package org.apache.sling.scripting.sightly.impl.engine;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;

/**
 * Sightly template engine factory
 */
@Component()
@Service(ScriptEngineFactory.class)
@Properties({
        @Property(name = "service.description", value = "Sightly Templating Engine"),
        @Property(name = "compatible.javax.script.name", value = "sly")
})
public class SightlyScriptEngineFactory extends AbstractScriptEngineFactory {

    @Reference
    private UnitLoader unitLoader;

    @Reference
    private ExtensionRegistryService extensionRegistryService;

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    public final static String SHORT_NAME = "sightly";

    public final static String LANGUAGE_NAME = "The Sightly Templating Language";

    public final static String LANGUAGE_VERSION = "1.0";

    public final static String EXTENSION = "html";

    public SightlyScriptEngineFactory() {
        setNames(SHORT_NAME);
        setExtensions(EXTENSION);
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    @Override
    public String getLanguageVersion() {
        return LANGUAGE_VERSION;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new SightlyScriptEngine(this, unitLoader, extensionRegistryService);
    }

    protected ClassLoader getClassLoader() {
        return dynamicClassLoaderManager.getDynamicClassLoader();
    }
}
