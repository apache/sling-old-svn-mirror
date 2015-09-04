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
package org.apache.sling.scripting.sightly.js.impl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.scripting.core.ScriptNameAwareReader;
import org.apache.sling.scripting.sightly.SightlyException;
import org.apache.sling.scripting.sightly.js.impl.async.AsyncContainer;
import org.apache.sling.scripting.sightly.js.impl.async.TimingBindingsValuesProvider;
import org.apache.sling.scripting.sightly.js.impl.async.UnaryCallback;
import org.apache.sling.scripting.sightly.js.impl.cjs.CommonJsModule;
import org.apache.sling.scripting.sightly.js.impl.loop.EventLoop;
import org.apache.sling.scripting.sightly.js.impl.loop.EventLoopInterop;
import org.apache.sling.scripting.sightly.js.impl.loop.Task;
import org.apache.sling.scripting.sightly.js.impl.use.DependencyResolver;
import org.apache.sling.scripting.sightly.js.impl.use.UseFunction;
import org.mozilla.javascript.Context;
import org.slf4j.LoggerFactory;

/**
 * Environment for running JS scripts
 */
public class JsEnvironment {

    private final ScriptEngine jsEngine;
    private final Bindings engineBindings;
    private EventLoop eventLoop;

    public JsEnvironment(ScriptEngine jsEngine) {
        this.jsEngine = jsEngine;
        engineBindings = new SimpleBindings();
        TimingBindingsValuesProvider.INSTANCE.addBindings(engineBindings);
    }

    public void initialize() {
        Context context = Context.enter();
        eventLoop = EventLoopInterop.obtainEventLoop(context);
    }

    public void cleanup() {
        Context context = Context.getCurrentContext();
        if (context == null) {
            throw new IllegalStateException("No current context");
        }
        EventLoopInterop.cleanupEventLoop(context);
        Context.exit();
    }

    public void runResource(Resource scriptResource, Bindings globalBindings, Bindings arguments, UnaryCallback callback) {
        ScriptContext scriptContext = new SimpleScriptContext();
        CommonJsModule module = new CommonJsModule();
        Bindings scriptBindings = buildBindings(scriptResource, globalBindings, arguments, module);
        scriptContext.setBindings(scriptBindings, ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute(ScriptEngine.FILENAME, scriptResource.getPath(), ScriptContext.ENGINE_SCOPE);
        runScript(scriptResource, scriptContext, callback, module);
    }

    public AsyncContainer runResource(Resource scriptResource, Bindings globalBindings, Bindings arguments) {
        AsyncContainer asyncContainer = new AsyncContainer();
        runResource(scriptResource, globalBindings, arguments, asyncContainer.createCompletionCallback());
        return asyncContainer;
    }

    private Bindings buildBindings(Resource scriptResource, Bindings local, Bindings arguments, CommonJsModule commonJsModule) {
        Bindings bindings = new SimpleBindings();
        bindings.putAll(engineBindings);
        DependencyResolver dependencyResolver = new DependencyResolver(scriptResource, this, local);
        UseFunction useFunction = new UseFunction(dependencyResolver, arguments);
        bindings.put(Variables.JS_USE, useFunction);
        bindings.put(Variables.MODULE, commonJsModule);
        bindings.put(Variables.EXPORTS, commonJsModule.getExports());
        bindings.put(Variables.CONSOLE, new Console(LoggerFactory.getLogger(scriptResource.getName())));
        bindings.putAll(local);
        return bindings;
    }

    private void runScript(Resource scriptResource, ScriptContext scriptContext, UnaryCallback callback, CommonJsModule commonJsModule) {
        eventLoop.schedule(scriptTask(scriptResource, scriptContext, callback, commonJsModule));
    }

    private Task scriptTask(final Resource scriptResource, final ScriptContext scriptContext,
                            final UnaryCallback callback, final CommonJsModule commonJsModule) {
        return new Task(new Runnable() {
            @Override
            public void run() {
                Reader reader = null;
                try {
                    Object result;
                    if (jsEngine instanceof Compilable) {
                        reader = new ScriptNameAwareReader(new InputStreamReader(scriptResource.adaptTo(InputStream.class)),
                                scriptResource.getPath());
                        result = ((Compilable) jsEngine).compile(reader).eval(scriptContext);
                    } else {
                        reader = new InputStreamReader(scriptResource.adaptTo(InputStream.class));
                        result = jsEngine.eval(reader, scriptContext);
                    }
                    if (commonJsModule.isModified()) {
                        result = commonJsModule.getExports();
                    }
                    if (result instanceof AsyncContainer) {
                        ((AsyncContainer) result).addListener(callback);
                    } else {
                        callback.invoke(result);
                    }
                } catch (ScriptException e) {
                    throw new SightlyException(e);
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            }
        });
    }


}
