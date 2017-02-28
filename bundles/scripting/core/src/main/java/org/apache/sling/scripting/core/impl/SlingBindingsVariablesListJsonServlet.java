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
package org.apache.sling.scripting.core.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.apache.felix.utils.json.JSONWriter;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

/**
 * Return all scripting variables for all registered scripting languages for the default context (=request).
 * This can only be achieved when a real Sling request and Sling response is available.
 * Also the context (i.e. the resource on which the request is acting) is important, 
 * because the actual binding variables might differ depending on the context
 */
@SlingServlet(
        resourceTypes = "sling/servlet/default",
        selectors = "SLING_availablebindings",
        methods = "GET",
        extensions = "json"
)
public class SlingBindingsVariablesListJsonServlet extends SlingSafeMethodsServlet {

    /**
     * 
     */
    private static final long serialVersionUID = -6744726829737263875L;

    /**
     * The script engine manager.
     */
    @Reference
    private ScriptEngineManager scriptEngineManager;
    
    /**
     * The BindingsValuesProviderTracker
     */
    @Reference
    private BindingsValuesProvidersByContext bindingsValuesProviderTracker;

    private BundleContext bundleContext;

    private static final String PARAMETER_EXTENSION = "extension";

    @Activate
    protected void activate(ComponentContext context) {
        bundleContext = context.getBundleContext();
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        JSONWriter jsonWriter = new JSONWriter(response.getWriter());
        jsonWriter.array();
        // get filter by engine selector
        String requestedExtension = request.getParameter(PARAMETER_EXTENSION);
        if (StringUtils.isNotBlank(requestedExtension)) {
            ScriptEngine selectedScriptEngine = scriptEngineManager.getEngineByExtension(requestedExtension);
            if (selectedScriptEngine == null) {
                throw new IllegalArgumentException("Invalid extension requested: "+requestedExtension);
            } else {
                writeBindingsToJsonWriter(jsonWriter, selectedScriptEngine.getFactory(), request, response);
            }
        } else {
            for (ScriptEngineFactory engineFactory : scriptEngineManager.getEngineFactories()) {
                writeBindingsToJsonWriter(jsonWriter, engineFactory, request, response);
            }
        }
        jsonWriter.endArray();
    }

    private void writeBindingsToJsonWriter(JSONWriter jsonWriter, ScriptEngineFactory engineFactory, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        jsonWriter.object();
        jsonWriter.key("engine");
        jsonWriter.value(engineFactory.getEngineName());
        jsonWriter.key("extensions");
        jsonWriter.value(engineFactory.getExtensions());
        Bindings bindings = getBindingsByEngine(engineFactory, request, response);
        jsonWriter.key("bindings");
        jsonWriter.array();
        for (Map.Entry<String, Object> entry : bindings.entrySet()) {
            jsonWriter.object();
            jsonWriter.key("name");
            jsonWriter.value(entry.getKey());
            jsonWriter.key("class");
            jsonWriter.value(entry.getValue() == null ? "&lt;NO VALUE&gt;" : entry.getValue().getClass().getName());
            jsonWriter.endObject();
        }
        jsonWriter.endArray();
        jsonWriter.endObject();
    }

    /**
     * Gets the {@link Bindings} object for the given {@link ScriptEngineFactory}.
     * It only considers the default context "request".
     * 
     * @see <a href="https://issues.apache.org/jira/browse/SLING-3038">binding contexts(SLING-3083)</a>
     * 
     * @param scriptEngineFactory the factory of the script engine, for which to retrieve the bindings
     * @param request the current request (necessary to create the bindings)
     * @param response the current response (necessary to create the bindings)
     * @return the bindings (list of key/value pairs) as defined by {@link Bindings} for the given script engine.
     * @throws IOException
     * @throws JSONException
     */
    private Bindings getBindingsByEngine(ScriptEngineFactory scriptEngineFactory, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String context = SlingScriptAdapterFactory.BINDINGS_CONTEXT; // use default context only
        final Collection<BindingsValuesProvider> bindingsValuesProviders = 
                bindingsValuesProviderTracker.getBindingsValuesProviders(scriptEngineFactory, context);
        
        Resource invalidScriptResource = new NonExistingResource(request.getResourceResolver(), "some/invalid/scriptpath");
        DefaultSlingScript defaultSlingScript = new DefaultSlingScript(bundleContext, invalidScriptResource, scriptEngineFactory.getScriptEngine(), bindingsValuesProviders, null, null);
        
        // prepare the bindings (similar as in DefaultSlingScript#service)
        final SlingBindings initalBindings = new SlingBindings();
        initalBindings.setRequest((SlingHttpServletRequest) request);
        initalBindings.setResponse((SlingHttpServletResponse) response);
        final Bindings bindings = defaultSlingScript.verifySlingBindings(initalBindings);
        
        // only thing being added in {DefaultSlingScript#call(...)} is resource resolver
        bindings.put(SlingScriptConstants.ATTR_SCRIPT_RESOURCE_RESOLVER, request.getResourceResolver());
        
        return bindings;
    }
}
