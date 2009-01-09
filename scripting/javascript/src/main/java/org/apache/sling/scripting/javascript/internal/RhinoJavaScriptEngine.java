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
package org.apache.sling.scripting.javascript.internal;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.scripting.javascript.io.EspReader;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;
import org.slf4j.Logger;

/**
 * A ScriptEngine that uses the Rhino interpreter to process Sling requests with
 * server-side javascript.
 */
public class RhinoJavaScriptEngine extends AbstractSlingScriptEngine {

    private Scriptable rootScope;

    public RhinoJavaScriptEngine(ScriptEngineFactory factory,
            Scriptable rootScope) {
        super(factory);
        this.rootScope = rootScope;
    }

    public Object eval(Reader scriptReader, ScriptContext scriptContext)
            throws ScriptException {
        Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        String scriptName = "NO_SCRIPT_NAME";
        {
            SlingScriptHelper helper = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
            if (helper != null) {
                scriptName = helper.getScript().getScriptResource().getPath();
            }
        }

        // wrap the reader in an EspReader for ESP scripts
        if (scriptName.endsWith(RhinoJavaScriptEngineFactory.ESP_SCRIPT_EXTENSION)) {
            scriptReader = new EspReader(scriptReader);
        }

        // container for replaced properties
        Map<String, Object> replacedProperties = null;
        Scriptable scope = null;

        // create a rhino Context and execute the script
        try {

            final Context rhinoContext = Context.enter();

            if (ScriptRuntime.hasTopCall(rhinoContext)) {

                // reuse the top scope if we are included
                scope = ScriptRuntime.getTopCallScope(rhinoContext);

            } else {

                // create the request top scope, use the ImporterToplevel here
                // to support the importPackage and importClasses functions
                scope = new ImporterTopLevel(); // new NativeObject();

                // Set the global scope to be our prototype
                scope.setPrototype(rootScope);

                // We want "scope" to be a new top-level scope, so set its
                // parent scope to null. This means that any variables created
                // by assignments will be properties of "scope".
                scope.setParentScope(null);

                // setup the context for use
                WrapFactory wrapFactory = ((RhinoJavaScriptEngineFactory) getFactory()).getWrapFactory();
                rhinoContext.setWrapFactory(wrapFactory);
            }

            // add initial properties to the scope
            replacedProperties = setBoundProperties(scope, bindings);

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

            final ScriptException se = new ScriptException(
                "Failure running script " + scriptName + ": " + t.getMessage());
            se.initCause(t);
            throw se;

        } finally {

            // if properties have been replaced, reset them
            resetBoundProperties(scope, replacedProperties);

            Context.exit();

        }
    }

    private Map<String, Object> setBoundProperties(Scriptable scope,
            Bindings bindings) {
        Map<String, Object> replacedProperties = new HashMap<String, Object>();

        for (Object entryObject : bindings.entrySet()) {
            Entry<?, ?> entry = (Entry<?, ?>) entryObject;
            String name = (String) entry.getKey();

            // get the current property value, if set
            if (ScriptableObject.hasProperty(scope, name)) {
                replacedProperties.put(name, ScriptableObject.getProperty(
                    scope, name));
            }

            // wrap the new value and set it
            Object wrapped = ScriptRuntime.toObject(scope, entry.getValue());
            ScriptableObject.putProperty(scope, (String) entry.getKey(),
                wrapped);
        }

        return replacedProperties;
    }

    private void resetBoundProperties(Scriptable scope,
            Map<String, Object> properties) {
        if (scope != null && properties != null && properties.size() > 0) {
            for (Entry<String, Object> entry : properties.entrySet()) {
                ScriptableObject.putProperty(scope, entry.getKey(),
                    entry.getValue());
            }
        }
    }
}
