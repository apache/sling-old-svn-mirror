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
import org.apache.sling.scripting.javascript.wrapper.ScriptableNode;
import org.apache.sling.scripting.javascript.wrapper.ScriptableResource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * The <code>RhinoJavaScriptEngineFactory</code> TODO
 */
public class RhinoJavaScriptEngineFactory extends AbstractScriptEngineFactory {

    public final static String JS_SCRIPT_EXTENSION = "js";

    public final static String ESP_SCRIPT_EXTENSION = "esp";

    private final String languageVersion;

    private Scriptable rootScope;

    public RhinoJavaScriptEngineFactory() {
        Context cx = Context.enter();
        setEngineName(getEngineName() + " (" + cx.getImplementationVersion()
            + ")");
        languageVersion = String.valueOf(cx.getLanguageVersion());
        Context.exit();

        setExtensions(JS_SCRIPT_EXTENSION, ESP_SCRIPT_EXTENSION);
        setMimeTypes("text/javascript", "application/ecmascript",
            "application/javascript");
        setNames("ecma", "javascript", JS_SCRIPT_EXTENSION,
            ESP_SCRIPT_EXTENSION);
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

    private Scriptable getRootScope() {
        if (rootScope == null) {
            final Context rhinoContext = Context.enter();
            rootScope = rhinoContext.initStandardObjects();

            try {
                ScriptableObject.defineClass(rootScope, ScriptableResource.class);
                ScriptableObject.defineClass(rootScope, ScriptableNode.class);
            } catch (Throwable t) {
                // TODO: log
            }
        }

        return rootScope;
    }
}
