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
package org.apache.sling.scripting.javascript.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.javascript.RhinoJavaScriptEngineFactory;
import org.apache.sling.scripting.javascript.io.EspReader;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>SlingGlobal</code> class provides two interesting new global
 * functions which are not part of the ECMAScript standard but which are
 * available in the Rhino Shell and which may be of use by JavaScripts:
 * <p>
 * <dl>
 * <dt><code>print(args, ...)</code></dt>
 * <dd>Prints the arguments <code>args</code> in a single message to the
 * scripts logger available as the global <em>log</em> variable.</dd>
 * <dt><code>load(args, ...)</code></dt>
 * <dd>Loads the scripts named as parameters into the current scope one, after
 * the other. Usually the script files are read as plain JavaScript files. If
 * the file extension happens to be <em>.esp</em> to indicate an ECMAScript
 * Server Page, the file is read through an
 * {@link org.apache.sling.scripting.javascript.io.EspReader}. Failure to read
 * one of the files throws an error.</dd>
 * </dl>
 */
public class SlingGlobal implements Serializable, IdFunctionCall {
    static final long serialVersionUID = 6080442165748707530L;

    private static final Object FTAG = new Object();

    private static final int Id_load = 1;

    private static final int Id_print = 2;

    private static final int LAST_SCOPE_FUNCTION_ID = 2;

    /** default log */
    private final Logger defaultLog = LoggerFactory.getLogger(getClass());

    public static void init(Scriptable scope, boolean sealed) {
        SlingGlobal obj = new SlingGlobal();

        for (int id = 1; id <= LAST_SCOPE_FUNCTION_ID; ++id) {
            String name;
            int arity = 1;
            switch (id) {
                case Id_load:
                    name = "load";
                    break;
                case Id_print:
                    name = "print";
                    break;
                default:
                    throw Kit.codeBug();
            }
            IdFunctionObject f = new IdFunctionObject(obj, FTAG, id, name,
                arity, scope);
            if (sealed) {
                f.sealObject();
            }
            f.exportAsScopeProperty();
        }

    }

    public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope,
            Scriptable thisObj, Object[] args) {
        if (f.hasTag(FTAG)) {
            int methodId = f.methodId();
            switch (methodId) {
                case Id_load: {
                    load(cx, thisObj, args);
                    return Context.getUndefinedValue();
                }

                case Id_print: {
                    print(cx, thisObj, args);
                    return Context.getUndefinedValue();
                }
            }
        }
        throw f.unknown();
    }

    private void print(Context cx, Scriptable thisObj, Object[] args) {
        StringBuffer message = new StringBuffer();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                message.append(" ");
            }
            // Convert the arbitrary JavaScript value into a string form.
            String s = ScriptRuntime.toString(args[i]);

            message.append(s);
        }

        getLogger(cx, thisObj).info(message.toString());
    }

    private void load(Context cx, Scriptable thisObj, Object[] args) {

        SlingScriptHelper sling = getProperty(cx, thisObj, SlingBindings.SLING,
            SlingScriptHelper.class);
        if (sling == null) {
            throw new NullPointerException(SlingBindings.SLING);
        }

        Scriptable globalScope = ScriptableObject.getTopLevelScope(thisObj);

        Resource scriptResource = sling.getScript().getScriptResource();
        ResourceResolver resolver = scriptResource.getResourceResolver();

        // the path of the current script to resolve realtive paths
        String currentScript = sling.getScript().getScriptResource().getPath();
        String scriptParent = ResourceUtil.getParent(currentScript);

        for (Object arg : args) {
            String scriptName = ScriptRuntime.toString(arg);

            Resource loadScript = null;
            if (!scriptName.startsWith("/")) {
                String absScriptName = scriptParent + "/" + scriptName;
                loadScript = resolver.resolve(absScriptName);
            }

            // not resolved relative to the current script
            if (loadScript == null) {
                loadScript = resolver.resolve(scriptName);
            }

            if (loadScript == null) {
                throw Context.reportRuntimeError("Script file " + scriptName
                    + " not found");
            }

            InputStream scriptStream = loadScript.adaptTo(InputStream.class);
            if (scriptStream == null) {
                throw Context.reportRuntimeError("Script file " + scriptName
                    + " cannot be read from");
            }

            try {
                // reader for the stream
                Reader scriptReader = new InputStreamReader(scriptStream);

                // check whether we have to wrap the basic reader
                if (scriptName.endsWith(RhinoJavaScriptEngineFactory.ESP_SCRIPT_EXTENSION)) {
                    scriptReader = new EspReader(scriptReader);
                }

                // read the suff buffered for better performance
                scriptReader = new BufferedReader(scriptReader);

                // now, let's go
                cx.evaluateReader(globalScope, scriptReader, scriptName, 1,
                    null);

            } catch (IOException ioe) {

                throw Context.reportRuntimeError("Failure reading file "
                    + scriptName + ": " + ioe);

            } finally {
                // ensure the script input stream is closed
                try {
                    scriptStream.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    /**
     * Returns the script logger or the logger of this class as a fallback
     * default if the global log variable is not accessible.
     */
    private Logger getLogger(Context cx, Scriptable scope) {
        Logger log = getProperty(cx, scope, SlingBindings.LOG, Logger.class);
        if (log == null) {
            log = this.defaultLog;
        }
        return log;
    }

    /**
     * Returns the named toplevel property converted to the requested
     * <code>type</code> or <code>null</code> if no such property exists or
     * the property is of the wrong type.
     */
    @SuppressWarnings("unchecked")
    private <Type> Type getProperty(Context cx, Scriptable scope, String name,
            Class<Type> type) {
        Object prop = ScriptRuntime.name(cx, scope, name);

        if (prop instanceof Wrapper) {
            prop = ((Wrapper) prop).unwrap();
        }

        if (type.isInstance(prop)) {
            return (Type) prop; // unchecked case
        }

        return null;
    }
}
