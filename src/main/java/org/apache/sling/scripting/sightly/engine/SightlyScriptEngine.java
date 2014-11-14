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

package org.apache.sling.scripting.sightly.engine;

import java.io.PrintWriter;
import java.io.Reader;
import java.util.Collections;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.classloader.DynamicClassLoader;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.scripting.sightly.api.*;
import org.apache.sling.scripting.sightly.common.Dynamic;
import org.apache.sling.scripting.sightly.common.SightlyRuntimeImpl;
import org.apache.sling.scripting.sightly.engine.runtime.UnitLocatorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.sling.scripting.sightly.api.SightlyEngineException;
import org.apache.sling.scripting.sightly.engine.runtime.UnitLocatorImpl;
import org.apache.sling.scripting.sightly.api.RenderContext;
import org.apache.sling.scripting.sightly.api.RenderUnit;
import org.apache.sling.scripting.sightly.common.Dynamic;
import org.apache.sling.scripting.sightly.common.SightlyRuntimeImpl;
import org.apache.sling.scripting.sightly.api.StackedWriter;
import org.apache.sling.scripting.sightly.api.UnitLocator;

/**
 * The Sightly Script engine
 */
public class SightlyScriptEngine extends AbstractSlingScriptEngine {

    private static final Logger LOG = LoggerFactory.getLogger(SightlyScriptEngine.class);
    private static final Bindings EMPTY_BINDINGS = new SimpleBindings(Collections.<String, Object>emptyMap());
    private static final int MAX_CLASSLOADER_RETRIES = 5;

    private final UnitLoader unitLoader;
    private final Dynamic dynamic;
    private final ExtensionRegistryService extensionRegistryService;

    public SightlyScriptEngine(ScriptEngineFactory scriptEngineFactory,
                               UnitLoader unitLoader,
                               Dynamic dynamic,
                               ExtensionRegistryService extensionRegistryService) {
        super(scriptEngineFactory);
        this.unitLoader = unitLoader;
        this.dynamic = dynamic;
        this.extensionRegistryService = extensionRegistryService;
    }

    @Override
    public Object eval(Reader reader, ScriptContext scriptContext) throws ScriptException {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(((SightlyScriptEngineFactory) getFactory()).getClassLoader());
        checkArguments(reader, scriptContext);
        Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        SlingScriptHelper slingScriptHelper = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
        Resource scriptResource = slingScriptHelper.getScript().getScriptResource();

        final SlingBindings slingBindings = new SlingBindings();
        slingBindings.putAll(bindings);

        Bindings globalBindings = new SimpleBindings(slingBindings);

        final SlingHttpServletRequest request = slingBindings.getRequest();
        final Object oldValue = request.getAttribute(SlingBindings.class.getName());
        try {
            request.setAttribute(SlingBindings.class.getName(), slingBindings);
            evaluateScript(scriptResource, globalBindings);
        } finally {
            request.setAttribute(SlingBindings.class.getName(), oldValue);
            Thread.currentThread().setContextClassLoader(old);
        }

        return null;
    }

    private void evaluateScript(Resource scriptResource, Bindings bindings) {
        RenderUnit renderUnit = unitLoader.createUnit(scriptResource, bindings);
        ResourceResolver resourceResolver = null;
        RenderContext context = null;
        try {
            resourceResolver = getAdminResourceResolver(bindings);
            context = provideContext(scriptResource, bindings, resourceResolver);
            renderUnit.render(context, EMPTY_BINDINGS);
        } catch (NoClassDefFoundError defFoundError) {
            if (context != null) {
                ClassLoader dcl = renderUnit.getClass().getClassLoader().getParent();
                if (dcl instanceof DynamicClassLoader && !((DynamicClassLoader) dcl).isLive()) {
                    boolean defError = true;
                    int retries = 0;
                    while (defError) {
                        try {
                            renderUnit = unitLoader.createUnit(scriptResource, bindings);
                            renderUnit.render(context, EMPTY_BINDINGS);
                            defError = false;
                        } catch (Throwable t) {
                            if (!(t instanceof NoClassDefFoundError)) {
                                // break immediately if there's a different error than a classloader one
                                if (t instanceof Error) {
                                    throw (Error) t;
                                }
                                throw (RuntimeException) t;
                            }
                            retries++;
                            if (retries > MAX_CLASSLOADER_RETRIES) {
                                LOG.error("Max number of retries (" + MAX_CLASSLOADER_RETRIES +
                                        ") for obtaining a valid RenderUnit was exceeded.");
                                throw defFoundError;
                            }
                        }
                    }
                } else {
                    // if we can't recover from this just throw the original exception
                    throw defFoundError;
                }
            }
        }
        finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    private RenderContext provideContext(Resource scriptResource, Bindings bindings, ResourceResolver resourceResolver) {
        PrintWriter printWriter = (PrintWriter) bindings.get(SlingBindings.OUT);
        StackedWriter writer = new StackedWriter(printWriter);
        UnitLocator unitLocator = new UnitLocatorImpl(unitLoader, resourceResolver, bindings, scriptResource);
        SightlyRuntimeImpl runtime = new SightlyRuntimeImpl(extensionRegistryService.extensions());
        RenderContext renderContext = new RenderContext(writer, bindings, dynamic, runtime, unitLocator);
        runtime.setRenderContext(renderContext);
        return renderContext;
    }

    private void checkArguments(Reader reader, ScriptContext scriptContext) {
        if (reader == null) {
            throw new NullPointerException("Reader cannot be null");
        }
        if (scriptContext == null) {
            throw new NullPointerException("ScriptContext cannot be null");
        }
    }

    private ResourceResolver getAdminResourceResolver(Bindings bindings) {
        SlingScriptHelper sling = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
        ResourceResolverFactory rrf = sling.getService(ResourceResolverFactory.class);
        try {
            return rrf.getAdministrativeResourceResolver(null);
        } catch (LoginException e) {
            throw new SightlyEngineException(e);
        }
    }
}
