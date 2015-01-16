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

package org.apache.sling.scripting.sightly.impl.engine;

import java.io.Reader;
import java.util.Collections;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptConstants;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderContextImpl;
import org.apache.sling.scripting.sightly.impl.engine.runtime.RenderUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Sightly Script engine
 */
public class SightlyScriptEngine extends AbstractSlingScriptEngine {

    private static final Logger LOG = LoggerFactory.getLogger(SightlyScriptEngine.class);
    private static final Bindings EMPTY_BINDINGS = new SimpleBindings(Collections.<String, Object>emptyMap());
    private static final int MAX_CLASSLOADER_RETRIES = 5;

    private final UnitLoader unitLoader;
    private final ExtensionRegistryService extensionRegistryService;

    public SightlyScriptEngine(ScriptEngineFactory scriptEngineFactory,
                               UnitLoader unitLoader,
                               ExtensionRegistryService extensionRegistryService) {
        super(scriptEngineFactory);
        this.unitLoader = unitLoader;
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
        final ResourceResolver scriptResourceResolver = (ResourceResolver) scriptContext.getAttribute(
            SlingScriptConstants.ATTR_SCRIPT_RESOURCE_RESOLVER, SlingScriptConstants.SLING_SCOPE);

        try {
            request.setAttribute(SlingBindings.class.getName(), slingBindings);
            evaluateScript(scriptResource, globalBindings, scriptResourceResolver);
        } finally {
            request.setAttribute(SlingBindings.class.getName(), oldValue);
            Thread.currentThread().setContextClassLoader(old);
        }

        return null;
    }

    private void evaluateScript(Resource scriptResource, Bindings bindings, ResourceResolver scriptResourceResolver) {
        RenderContextImpl renderContext = new RenderContextImpl(bindings, extensionRegistryService.extensions(), scriptResourceResolver);
        RenderUnit renderUnit = unitLoader.createUnit(scriptResource, bindings, renderContext);
        renderUnit.render(renderContext, EMPTY_BINDINGS);
    }

    private void checkArguments(Reader reader, ScriptContext scriptContext) {
        if (reader == null) {
            throw new NullPointerException("Reader cannot be null");
        }
        if (scriptContext == null) {
            throw new NullPointerException("ScriptContext cannot be null");
        }
    }
}
