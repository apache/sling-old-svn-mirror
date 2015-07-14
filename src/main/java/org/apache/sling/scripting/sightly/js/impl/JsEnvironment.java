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
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.core.ScriptNameAwareReader;
import org.apache.sling.scripting.sightly.ResourceResolution;
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

    /**
     * Run a Js script at a given path
     * @param caller the resource of the script that invokes the Js code
     * @param path the path to the JS script
     * @param globalBindings the global bindings for the script
     * @param arguments the arguments from the use-plugin
     * @param callback callback that will receive the result of the script
     */
    public void run(Resource caller, String path, Bindings globalBindings, Bindings arguments, UnaryCallback callback) {
        Resource scriptResource = caller.getChild(path);
        SlingScriptHelper scriptHelper = (SlingScriptHelper) globalBindings.get(SlingBindings.SLING);
        Resource componentCaller = ResourceResolution.getResourceForRequest(caller.getResourceResolver(), scriptHelper.getRequest());
        if (scriptResource == null) {
            if (isResourceOverlay(caller, componentCaller)) {
                scriptResource = ResourceResolution.getResourceFromSearchPath(componentCaller, path);
            } else {
                scriptResource = ResourceResolution.getResourceFromSearchPath(caller, path);
            }
        }
        if (scriptResource == null) {
            throw new SightlyException("Required script resource could not be located: " + path);
        }
        runResource(scriptResource, globalBindings, arguments, callback);
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

    /**
     * Run a script at a given path
     *
     * @param caller         the resource of the script that invokes the Js code
     * @param path           the path to the JS script
     * @param globalBindings bindings for the JS script
     * @param arguments      the arguments for the JS script
     * @return an asynchronous container for the result
     * @throws UnsupportedOperationException if this method is run when the event loop is not empty
     */
    public AsyncContainer run(Resource caller, String path, Bindings globalBindings, Bindings arguments) {
        AsyncContainer asyncContainer = new AsyncContainer();
        run(caller, path, globalBindings, arguments, asyncContainer.createCompletionCallback());
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

    /**
     * Using the inheritance chain created with the help of {@code sling:resourceSuperType} this method checks if {@code resourceB}
     * inherits from {@code resourceA}. In case {@code resourceA} is a {@code nt:file}, its parent will be used for the inheritance check.
     *
     * @param resourceA the base resource
     * @param resourceB the potentially overlaid resource
     * @return {@code true} if {@code resourceB} overlays {@code resourceB}, {@code false} otherwise
     */
    private boolean isResourceOverlay(Resource resourceA, Resource resourceB) {
        String resourceBSuperType = resourceB.getResourceSuperType();
        if (StringUtils.isNotEmpty(resourceBSuperType)) {
            ResourceResolver resolver = resourceA.getResourceResolver();
            String parentResourceType = resourceA.getResourceType();
            if ("nt:file".equals(parentResourceType)) {
                parentResourceType = ResourceUtil.getParent(resourceA.getPath());
            }
            Resource parentB = resolver.getResource(resourceBSuperType);
            while (parentB != null && !"/".equals(parentB.getPath()) && StringUtils.isNotEmpty(resourceBSuperType)) {
                if (parentB.getPath().equals(parentResourceType)) {
                    return true;
                }
                resourceBSuperType = parentB.getResourceSuperType();
                parentB = resolver.getResource(resourceBSuperType);
            }
        }
        return false;
    }
}
