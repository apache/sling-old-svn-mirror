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
package org.apache.sling.scripting.javascript;

import javax.script.ScriptEngine;

import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.apache.sling.scripting.javascript.helper.SlingContextFactory;
import org.apache.sling.scripting.javascript.helper.SlingWrapFactory;
import org.apache.sling.scripting.javascript.helper.SlingWrapper;
import org.apache.sling.scripting.javascript.wrapper.ScriptableCalendar;
import org.apache.sling.scripting.javascript.wrapper.ScriptableItemMap;
import org.apache.sling.scripting.javascript.wrapper.ScriptableNode;
import org.apache.sling.scripting.javascript.wrapper.ScriptablePrintWriter;
import org.apache.sling.scripting.javascript.wrapper.ScriptableProperty;
import org.apache.sling.scripting.javascript.wrapper.ScriptableResource;
import org.apache.sling.scripting.javascript.wrapper.ScriptableVersion;
import org.apache.sling.scripting.javascript.wrapper.ScriptableVersionHistory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.debugger.ScopeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>RhinoJavaScriptEngineFactory</code> TODO
 */
public class RhinoJavaScriptEngineFactory extends AbstractScriptEngineFactory
        implements ScopeProvider {

    public final static String ECMA_SCRIPT_EXTENSION = "ecma";

    public final static String ESP_SCRIPT_EXTENSION = "esp";

    private static final Class<?>[] HOSTOBJECT_CLASSES = {
        ScriptableResource.class, 
        ScriptableNode.class,
        ScriptableProperty.class, 
        ScriptableItemMap.class,
        ScriptablePrintWriter.class,
        ScriptableVersionHistory.class,
        ScriptableVersion.class,
        ScriptableCalendar.class
    };

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final String languageVersion;

    private Scriptable rootScope;

    public RhinoJavaScriptEngineFactory() {

        // initialize the Rhino Context Factory
        SlingContextFactory.setup(this);

        Context cx = Context.enter();
        setEngineName(getEngineName() + " (" + cx.getImplementationVersion()
            + ")");
        languageVersion = String.valueOf(cx.getLanguageVersion());
        Context.exit();

        setExtensions(ECMA_SCRIPT_EXTENSION, ESP_SCRIPT_EXTENSION);
        setMimeTypes("text/javascript", "application/ecmascript",
            "application/javascript");
        setNames("javascript", ECMA_SCRIPT_EXTENSION, ESP_SCRIPT_EXTENSION);
    }

    public ScriptEngine getScriptEngine() {
        return new RhinoJavaScriptEngine(this, getRootScope());
    }

    public String getLanguageName() {
        return "ECMAScript";
    }

    public String getLanguageVersion() {
        return languageVersion;
    }

    public Scriptable getScope() {
        return getRootScope();
    }

    private Scriptable getRootScope() {
        if (rootScope == null) {
            final Context rhinoContext = Context.enter();
            rootScope = rhinoContext.initStandardObjects();

            for (Class<?> clazz : HOSTOBJECT_CLASSES) {
                try {

                    // register the host object
                    ScriptableObject.defineClass(rootScope, clazz);
                    final ScriptableObject host = (ScriptableObject) clazz.newInstance();

                    if (SlingWrapper.class.isAssignableFrom(clazz)) {
                        // SlingWrappers can map to several classes if needed
                        final SlingWrapper hostWrapper = (SlingWrapper) host;
                        for (Class<?> c : hostWrapper.getWrappedClasses()) {
                            SlingWrapFactory.INSTANCE.registerWrapper(c,
                                hostWrapper.getClassName());
                        }
                    } else {
                        // but other ScriptableObjects need to be registered as
                        // well
                        SlingWrapFactory.INSTANCE.registerWrapper(
                            host.getClass(), host.getClassName());
                    }
                } catch (Throwable t) {
                    log.warn("getRootScope: Cannot prepare host object " + clazz, t);
                }
            }
        }

        return rootScope;
    }
}
