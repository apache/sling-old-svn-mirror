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
package org.apache.sling.scripting.javascript;

import java.io.Reader;
import java.util.Map.Entry;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.scripting.javascript.helper.SlingWrapFactory;
import org.apache.sling.scripting.javascript.io.EspReader;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;

/**
 * A ScriptEngine that uses the Rhino interpreter to process Sling requests with
 * server-side javascript.
 */
public class RhinoJavaScriptEngine extends AbstractSlingScriptEngine {

    private Scriptable rootScope;

    public RhinoJavaScriptEngine(ScriptEngineFactory factory, Scriptable rootScope) {
        super(factory);
        this.rootScope = rootScope;
    }

    public Object eval(Reader scriptReader, ScriptContext scriptContext)
            throws ScriptException {
        Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        String scriptName = "NO_SCRIPT_NAME";
        {
            SlingScriptHelper helper = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
            if(helper != null) {
                scriptName = helper.getScript().getScriptResource().getPath();
            }
        }

        // wrap the reader in an EspReader for ESP scripts
        if (scriptName.endsWith(RhinoJavaScriptEngineFactory.ESP_SCRIPT_EXTENSION)) {
            scriptReader = new EspReader(scriptReader);
        }

        // create a rhino Context and execute the script
        try {
            
            final Context rhinoContext = Context.enter();
            final ScriptableObject scope = new NativeObject();

            // Set the global scope to be our prototype
            scope.setPrototype(rootScope);

            // We want "scope" to be a new top-level scope, so set its parent
            // scope to null. This means that any variables created by assignments
            // will be properties of "scope".
            scope.setParentScope(null);

            // setup the context for use
            rhinoContext.setWrapFactory(SlingWrapFactory.INSTANCE);

            // add initial properties to the scope
            for (Object entryObject : bindings.entrySet()) {
                Entry<?, ?> entry = (Entry<?, ?>) entryObject;
                Object wrapped = ScriptRuntime.toObject(scope, entry.getValue());
                ScriptableObject.putProperty(scope, (String) entry.getKey(),
                    wrapped);
            }

            final int lineNumber = 1;
            final Object securityDomain = null;

            return rhinoContext.evaluateReader(scope, scriptReader, scriptName,
                lineNumber, securityDomain);

        } catch (JavaScriptException t) {
            
            final ScriptException se = new ScriptException(t.details(),
                t.sourceName(), t.lineNumber());

            ((Logger) bindings.get(SlingBindings.LOG)).error(t.getScriptStackTrace());
            se.setStackTrace(t.getStackTrace());
            throw se;
            
        } catch (Throwable t) {
            
            final ScriptException se = new ScriptException("Failure running script " + scriptName
                + ": " + t.getMessage());
            se.initCause(t);
            throw se;
            
        } finally {
            
            Context.exit();
            
        }
    }

}
