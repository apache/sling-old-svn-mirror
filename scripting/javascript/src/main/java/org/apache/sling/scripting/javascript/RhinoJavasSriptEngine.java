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

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptEngine;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

/**
 * A ScriptEngine that uses the Rhino interpreter to process Sling requests with
 * server-side javascript.
 *
 * @scr.component
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description" value="Sling JavaScript Script Engine"
 * @scr.service interface="org.apache.sling.api.scripting.SlingScriptEngine"
 */
public class RhinoJavasSriptEngine implements SlingScriptEngine {

    public final static String JS_SCRIPT_EXTENSION = "js";

    public final static String ESP_SCRIPT_EXTENSION = "esp";

    private String engineName;

    {
        Context cx = Context.enter();
        engineName = "ECMAScript Engine (" + cx.getImplementationVersion() + ")";
        Context.exit();
    }

    public String[] getExtensions() {
        return new String[] { JS_SCRIPT_EXTENSION, ESP_SCRIPT_EXTENSION };
    }

    public String getEngineName() {
        return engineName;
    }

    public String getEngineVersion() {
        return "0.9";
    }

    public void eval(SlingScript script, Map<String, Object> props)
            throws SlingException, IOException {

        String scriptName = script.getScriptResource().getURI();

        // wrap the reader in an EspReader for ESP scripts
        Reader scriptReader = script.getScriptReader();
        if (scriptName.endsWith(ESP_SCRIPT_EXTENSION)) {
            scriptReader = new EspReader(scriptReader);
        }

        // create a rhino Context and execute the script
        try {
            final Context rhinoContext = Context.enter();
            final ScriptableObject scope = rhinoContext.initStandardObjects();

            // add initial properties to the scope
            for (Entry<String, Object> entry : props.entrySet()) {
                Object wrapped = wrap(scope, entry.getValue(), rhinoContext);
                ScriptableObject.putProperty(scope, entry.getKey(), wrapped);
            }

            final int lineNumber = 1;
            final Object securityDomain = null;

            rhinoContext.evaluateReader(scope, scriptReader, scriptName,
                lineNumber, securityDomain);

        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable t) {
            throw new SlingException("Failure running script " + scriptName, t);
        } finally {
            Context.exit();
        }
    }

    /**
     * Wraps Java objects in Rhino host objects.
     *
     * @param scope the current scope
     * @param entry the object to wrap
     * @param context the current context, if needed for class definitions
     * @return a rhino hostobject
     */
    protected Object wrap(final ScriptableObject scope, Object entry,
            Context context) {
        // wrap the resource to make it friendlier for javascript
        if (entry instanceof Resource) {
            try {
                ScriptableObject.defineClass(scope, ScriptableResource.class);
                return context.newObject(scope, "Resource",  new Object[] { entry });
            } catch (Exception e) {
                return Context.javaToJS(entry, scope);
            }
        }
        return Context.javaToJS(entry, scope);
    }
}
