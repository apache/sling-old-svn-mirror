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
package org.apache.sling.scripting.javascript.internal;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.apache.sling.scripting.api.ScriptCache;
import org.apache.sling.scripting.javascript.RhinoHostObjectProvider;
import org.apache.sling.scripting.javascript.SlingWrapper;
import org.apache.sling.scripting.javascript.helper.SlingContextFactory;
import org.apache.sling.scripting.javascript.helper.SlingWrapFactory;
import org.apache.sling.scripting.javascript.wrapper.ScriptableCalendar;
import org.apache.sling.scripting.javascript.wrapper.ScriptableItemMap;
import org.apache.sling.scripting.javascript.wrapper.ScriptableMap;
import org.apache.sling.scripting.javascript.wrapper.ScriptableNode;
import org.apache.sling.scripting.javascript.wrapper.ScriptablePrintWriter;
import org.apache.sling.scripting.javascript.wrapper.ScriptableProperty;
import org.apache.sling.scripting.javascript.wrapper.ScriptableResource;
import org.apache.sling.scripting.javascript.wrapper.ScriptableVersion;
import org.apache.sling.scripting.javascript.wrapper.ScriptableVersionHistory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.NativeJavaPackage;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.debugger.ScopeProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    service = ScriptEngineFactory.class,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Rhino Javascript Engine Factory",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        "extensions=" + RhinoJavaScriptEngineFactory.ECMA_SCRIPT_EXTENSION,
        "extensions=" + RhinoJavaScriptEngineFactory.ESP_SCRIPT_EXTENSION,
        "mimeTypes=text/javascript",
        "mimeTypes=application/ecmascript",
        "mimeTypes=application/javascript",
        "names=javascript",
        "names=JavaScript",
        "names=ecmascript",
        "names=ECMAScript"
    },
    reference = @Reference(
        name = "HostObjectProvider",
        service = RhinoHostObjectProvider.class,
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        bind = "addHostObjectProvider",
        unbind = "removeHostObjectProvider"
    )
)
@Designate(
    ocd = RhinoJavaScriptEngineFactoryConfiguration.class
)
public class RhinoJavaScriptEngineFactory extends AbstractScriptEngineFactory implements ScopeProvider {

    public final static int DEFAULT_OPTIMIZATION_LEVEL = 9;

    public final static String ECMA_SCRIPT_EXTENSION = "ecma";

    public final static String ESP_SCRIPT_EXTENSION = "esp";

    private static final Class<?>[] HOSTOBJECT_CLASSES = {
            ScriptableResource.class, ScriptableNode.class,
            ScriptableProperty.class, ScriptableItemMap.class,
            ScriptablePrintWriter.class, ScriptableVersionHistory.class,
            ScriptableVersion.class, ScriptableCalendar.class, ScriptableMap.class
    };

    /**
     * default log
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private int optimizationLevel;

    private String languageVersion;

    private SlingWrapFactory wrapFactory;

    private Scriptable rootScope;

    private final Set<RhinoHostObjectProvider> hostObjectProvider = new HashSet<RhinoHostObjectProvider>();

    @Reference
    private DynamicClassLoaderManager dynamicClassLoaderManager = null;

    @Reference
    private ScriptCache scriptCache = null;

    public ScriptEngine getScriptEngine() {
        return new RhinoJavaScriptEngine(this, getRootScope(), scriptCache);
    }

    public String getLanguageName() {
        return "ECMAScript";
    }

    public String getLanguageVersion() {
        return languageVersion;
    }

    /**
     * Get the optimization level that should be used when running JS scripts
     * with Rhino
     *
     * @return an integer from 0-9 with 9 being the most aggressive optimization, or
     * -1 if interpreted mode is to be used
     */
    public int getOptimizationLevel() {
        return optimizationLevel;
    }

    public Object getParameter(String name) {
        if ("THREADING".equals(name)) {
            return "MULTITHREADED";
        }

        return super.getParameter(name);
    }

    public Scriptable getScope() {
        return getRootScope();
    }

    SlingWrapFactory getWrapFactory() {
        return wrapFactory;
    }

    @SuppressWarnings("unchecked")
    private Scriptable getRootScope() {
        if (rootScope == null) {

            final Context rhinoContext = Context.enter();
            try {
                rhinoContext.setOptimizationLevel(optimizationLevel);
                Scriptable tmpScope = rhinoContext.initStandardObjects(new ImporterTopLevel(rhinoContext), false);

                // default classes
                addHostObjects(tmpScope, (Class<? extends ScriptableObject>[]) HOSTOBJECT_CLASSES);

                // provided classes
                for (RhinoHostObjectProvider provider : hostObjectProvider) {
                    addHostObjects(tmpScope, provider.getHostObjectClasses());
                    addImportedClasses(rhinoContext, tmpScope, provider.getImportedClasses());
                    addImportedPackages(rhinoContext, tmpScope, provider.getImportedPackages());
                }

                // only assign the root scope when complete set up
                rootScope = tmpScope;

            } finally {
                // ensure the context is exited after setting up the
                // the new root scope
                Context.exit();
            }
        }

        return rootScope;
    }

    private void dropRootScope() {

        // ensure the debugger is closed if the root scope will
        // be replaced to ensure no references to the old scope
        // and context remain
        ContextFactory contextFactory = ContextFactory.getGlobal();
        if (contextFactory instanceof SlingContextFactory) {
            ((SlingContextFactory) contextFactory).exitDebugger();
        }

        // drop the scope
        rootScope = null;
    }

    // ---------- SCR integration
    @Activate
    protected void activate(final ComponentContext context, final RhinoJavaScriptEngineFactoryConfiguration configuration) {
        Dictionary<?, ?> props = context.getProperties();
        boolean debugging = getProperty("org.apache.sling.scripting.javascript.debug", props, context.getBundleContext(), false);

        optimizationLevel = readOptimizationLevel(configuration);

        // setup the wrap factory
        wrapFactory = new SlingWrapFactory();

        // initialize the Rhino Context Factory
        SlingContextFactory.setup(this);

        Context cx = Context.enter();
        setEngineName(getEngineName() + " (" + cx.getImplementationVersion() + ")");
        languageVersion = String.valueOf(cx.getLanguageVersion());
        Context.exit();

        setExtensions(ECMA_SCRIPT_EXTENSION, ESP_SCRIPT_EXTENSION);
        setMimeTypes("text/javascript", "application/ecmascript", "application/javascript");
        setNames("javascript", ECMA_SCRIPT_EXTENSION, ESP_SCRIPT_EXTENSION);

        final ContextFactory contextFactory = ContextFactory.getGlobal();
        if (contextFactory instanceof SlingContextFactory) {
            ((SlingContextFactory) contextFactory).setDebugging(debugging);
        }
        // set the dynamic class loader as the application class loader
        final DynamicClassLoaderManager dclm = this.dynamicClassLoaderManager;
        if (dclm != null) {
            contextFactory.initApplicationClassLoader(dynamicClassLoaderManager.getDynamicClassLoader());
        }

        log.info("Activated with optimization level {}", optimizationLevel);
    }

    @Deactivate
    @SuppressWarnings("unused")
    protected void deactivate(ComponentContext context) {

        // remove the root scope
        dropRootScope();

        // remove our context factory
        SlingContextFactory.teardown();

        // remove references
        wrapFactory = null;
        hostObjectProvider.clear();
    }

    @SuppressWarnings("unused")
    protected void addHostObjectProvider(RhinoHostObjectProvider provider) {
        hostObjectProvider.add(provider);

        if (rootScope != null) {
            addHostObjects(rootScope, provider.getHostObjectClasses());
        }
    }

    @SuppressWarnings("unused")
    protected void removeHostObjectProvider(RhinoHostObjectProvider provider) {
        // remove the current root scope and have it recreated using the
        // new host object classes
        if (hostObjectProvider.remove(provider)) {
            dropRootScope();
        }
    }

    // ---------- internal

    private void addHostObjects(Scriptable scope, Class<? extends Scriptable>[] classes) {
        if (classes != null) {
            for (Class<? extends Scriptable> clazz : classes) {
                try {

                    // register the host object
                    ScriptableObject.defineClass(scope, clazz);

                    if (SlingWrapper.class.isAssignableFrom(clazz)) {

                        // SlingWrappers can map to several classes if needed
                        final SlingWrapper hostWrapper = (SlingWrapper) clazz.newInstance();
                        for (Class<?> c : hostWrapper.getWrappedClasses()) {
                            getWrapFactory().registerWrapper(c, hostWrapper.getClassName());
                        }
                    } else {
                        // but other Scriptable host objects need to be
                        // registered as well
                        final Scriptable host = clazz.newInstance();
                        getWrapFactory().registerWrapper(host.getClass(), host.getClassName());
                    }
                } catch (Throwable t) {
                    log.warn("addHostObjects: Cannot prepare host object " + clazz, t);
                }
            }
        }
    }

    private void addImportedClasses(Context cx, Scriptable scope,
                                    Class<?>[] classes) {
        if (classes != null && classes.length > 0) {
            NativeJavaClass[] np = new NativeJavaClass[classes.length];
            for (int i = 0; i < classes.length; i++) {
                np[i] = new NativeJavaClass(scope, classes[i]);
            }
            ScriptableObject.callMethod(cx, scope, "importClass", np);
        }
    }

    private void addImportedPackages(Context cx, Scriptable scope,
                                     String[] packages) {
        if (packages != null && packages.length > 0) {
            NativeJavaPackage[] np = new NativeJavaPackage[packages.length];
            for (int i = 0; i < packages.length; i++) {
                np[i] = new NativeJavaPackage(packages[i]);
            }
            ScriptableObject.callMethod(cx, scope, "importPackage", np);
        }
    }

    private boolean getProperty(String name, Dictionary<?, ?> props,
                                BundleContext bundleContext, boolean defaultValue) {
        Object value = props.get(name);
        if (value == null) {
            value = bundleContext.getProperty(name);
        }

        return (value != null)
                ? Boolean.parseBoolean(String.valueOf(value))
                : defaultValue;
    }

    private int readOptimizationLevel(final RhinoJavaScriptEngineFactoryConfiguration configuration) {
        int optLevel = configuration.org_apache_sling_scripting_javascript_rhino_optLevel();
        if (!Context.isValidOptimizationLevel(optLevel)) {
            log.warn("Invalid optimization level {}, using default value", optLevel);
            optLevel = DEFAULT_OPTIMIZATION_LEVEL;
        }
        return optLevel;
    }
}
