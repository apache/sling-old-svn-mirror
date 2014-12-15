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
package org.apache.sling.scripting.sightly.js.impl.jsapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import javax.servlet.http.HttpServletRequest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.scripting.sightly.js.impl.JsEnvironment;
import org.apache.sling.scripting.sightly.js.impl.Variables;
import org.apache.sling.scripting.sightly.js.impl.async.AsyncContainer;
import org.apache.sling.scripting.sightly.js.impl.async.AsyncExtractor;
import org.apache.sling.scripting.sightly.js.impl.async.TimingBindingsValuesProvider;
import org.apache.sling.scripting.sightly.js.impl.async.TimingFunction;
import org.apache.sling.scripting.sightly.js.impl.cjs.CommonJsModule;
import org.apache.sling.scripting.sightly.js.impl.rhino.HybridObject;
import org.apache.sling.scripting.sightly.js.impl.rhino.JsValueAdapter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the {@code sightly} namespace for usage in Sightly & JS scripts
 * called from Sightly
 */
@Component(metatype = true, label = "Apache Sling Scripting Sightly JavaScript Bindings Provider",
        description = "The Apache Sling Scripting Sightly JavaScript Bindings Provider loads the JS Use-API and makes it available in the" +
                " bindings map.")
@Service(SlyBindingsValuesProvider.class)
@Properties({
        @Property(
                name = SlyBindingsValuesProvider.SCR_PROP_JS_BINDING_IMPLEMENTATIONS,
                value = {
                    "sightly:" + SlyBindingsValuesProvider.SLING_NS_PATH
                },
                unbounded = PropertyUnbounded.ARRAY,
                label = "Script Factories",
                description = "Script factories to load in the bindings map. The entries should be in the form " +
                        "'namespace:/path/from/repository'."
        )
})
@SuppressWarnings("unused")
public class SlyBindingsValuesProvider {

    public static final String SCR_PROP_JS_BINDING_IMPLEMENTATIONS = "org.apache.sling.scripting.sightly.js.bindings";

    public static final String SLING_NS_PATH = "/libs/sling/sightly/js/internal/sly.js";
    public static final String Q_PATH = "/libs/sling/sightly/js/3rd-party/q.js";

    private static final String REQ_NS = SlyBindingsValuesProvider.class.getCanonicalName();

    private static final Logger log = LoggerFactory.getLogger(SlyBindingsValuesProvider.class);

    @Reference
    private ScriptEngineManager scriptEngineManager;

    @Reference
    private ResourceResolverFactory rrf = null;

    private final AsyncExtractor asyncExtractor = new AsyncExtractor();
    private final JsValueAdapter jsValueAdapter = new JsValueAdapter(asyncExtractor);

    private Map<String, String> scriptPaths = new HashMap<String, String>();
    private Map<String, Function> factories = new HashMap<String, Function>();

    private Script qScript;
    private final ScriptableObject qScope = createQScope();

    public void processBindings(Bindings bindings) {
        if (needsInit()) {
            init(bindings);
        }
        Context context = null;
        try {
            context = Context.enter();
            Object qInstance = obtainQInstance(context, bindings);
            if (qInstance == null) {
                return;
            }
            for (Map.Entry<String, Function> entry : factories.entrySet()) {
                addBinding(context, entry.getValue(), bindings, entry.getKey(), qInstance);
            }
        } finally {
            if (context != null) {
                Context.exit();
            }
        }
    }

    @Activate
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties();
        String[] factories = PropertiesUtil.toStringArray(properties.get(SCR_PROP_JS_BINDING_IMPLEMENTATIONS), new String[]{SLING_NS_PATH});
        scriptPaths = new HashMap<String, String>(factories.length);
        for (String f : factories) {
            String[] parts = f.split(":");
            if (parts.length == 2) {
                scriptPaths.put(parts[0], parts[1]);
            }
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {
        if (scriptPaths != null) {
            scriptPaths.clear();
        }
        if (factories != null) {
            factories.clear();
        }
    }


    private void addBinding(Context context, Function factory, Bindings bindings, String globalName, Object qInstance) {
        if (factory == null) {
            return;
        }
        Object result = factory.call(context, factory, factory, new Object[] {bindings, qInstance});
        HybridObject global = new HybridObject((Scriptable) result, jsValueAdapter);
        bindings.put(globalName, global);
    }

    private boolean needsInit() {
        return factories == null || factories.isEmpty() || qScript == null;
    }

    private synchronized void init(Bindings bindings) {
        if (needsInit()) {
            ensureFactoriesLoaded(bindings);
        }
    }

    private void ensureFactoriesLoaded(Bindings bindings) {
        JsEnvironment jsEnvironment = null;
        try {
            ScriptEngine scriptEngine = obtainEngine();
            if (scriptEngine == null) {
                return;
            }
            jsEnvironment = new JsEnvironment(scriptEngine);
            jsEnvironment.initialize();
            factories = new HashMap<String, Function>(scriptPaths.size());
            for (Map.Entry<String, String> entry : scriptPaths.entrySet()) {
                factories.put(entry.getKey(), loadFactory(jsEnvironment, entry.getValue(), bindings));
            }
            qScript = loadQScript();
        } finally {
            if (jsEnvironment != null) {
                jsEnvironment.cleanup();
            }
        }
    }

    private Function loadFactory(JsEnvironment jsEnvironment, String path, Bindings bindings) {
        ResourceResolver resolver = null;
        try {
            resolver = rrf.getAdministrativeResourceResolver(null);
            Resource resource = resolver.getResource(path);
            if (resource == null) {
                log.warn("Sly namespace loader could not find the following script: " + path);
                return null;
            }
            AsyncContainer container = jsEnvironment.runResource(resource, createBindings(bindings), new SimpleBindings());
            Object obj = container.getResult();
            if (!(obj instanceof Function)) {
                log.warn("Script was expected to return a function");
                return null;
            }
            return (Function) obj;
        } catch (LoginException e) {
            log.error("Cannot evaluate script " + path, e);
            return null;
        } finally {
            if (resolver != null) {
                resolver.close();
            }
        }
    }

    private Bindings createBindings(Bindings global) {
        Bindings bindings = new SimpleBindings();
        bindings.putAll(global);
        TimingBindingsValuesProvider.INSTANCE.addBindings(bindings);
        return bindings;
    }

    private ScriptEngine obtainEngine() {
        return scriptEngineManager.getEngineByName("javascript");
    }

    private Object obtainQInstance(Context context, Bindings bindings) {
        if (qScript == null) {
            return null;
        }
        HttpServletRequest request = (HttpServletRequest) bindings.get(SlingBindings.REQUEST);
        Object qInstance = null;
        if (request != null) {
            qInstance = request.getAttribute(REQ_NS);
        }
        if (qInstance == null) {
            qInstance = createQInstance(context, qScript);
            if (request != null) {
                request.setAttribute(REQ_NS, qInstance);
            }
        }
        return qInstance;
    }

    private ScriptableObject createQScope() {
        Context context = Context.enter();
        try {
            ScriptableObject scope = context.initStandardObjects();
            ScriptableObject.putProperty(scope, Variables.SET_IMMEDIATE, TimingFunction.INSTANCE);
            ScriptableObject.putProperty(scope, Variables.SET_TIMEOUT, TimingFunction.INSTANCE);
            return scope;
        } finally {
            Context.exit();
        }
    }

    private Object createQInstance(Context context, Script qScript) {
        CommonJsModule module = new CommonJsModule();
        Scriptable tempScope = context.newObject(qScope);
        ScriptableObject.putProperty(tempScope, Variables.MODULE, module);
        ScriptableObject.putProperty(tempScope, Variables.EXPORTS, module.getExports());
        qScript.exec(context, tempScope);
        return module.getExports();
    }

    private Script loadQScript() {
        ResourceResolver resourceResolver = null;
        Context context = Context.enter();
        context.initStandardObjects();
        context.setOptimizationLevel(9);
        InputStream reader = null;
        try {
            resourceResolver = rrf.getAdministrativeResourceResolver(null);
            Resource resource = resourceResolver.getResource(Q_PATH);
            if (resource == null) {
                log.warn("Could not load Q library at path: " + Q_PATH);
                return null;
            }
            reader = resource.adaptTo(InputStream.class);
            if (reader == null) {
                log.warn("Could not read content of Q library");
                return null;
            }
            return context.compileReader(new InputStreamReader(reader), Q_PATH, 0, null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            Context.exit();
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error("Error while closing reader", e);
                }
            }
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

}
