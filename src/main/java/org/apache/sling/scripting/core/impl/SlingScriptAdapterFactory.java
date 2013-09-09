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

import java.util.Collection;
import java.util.List;

import javax.script.ScriptEngine;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.mime.MimeTypeProvider;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.apache.sling.scripting.core.impl.helper.SlingScriptEngineManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

/**
 * AdapterFactory that adapts Resources to the DefaultSlingScript servlet, which
 * executes the Resources as scripts.
 */
@Component(metatype=false, immediate=true)
@Service({AdapterFactory.class, MimeTypeProvider.class})
@Properties({
    @Property(name="service.vendor", value="The Apache Software Foundation"),
    @Property(name="service.description", value="Default SlingScriptResolver"),
    @Property(name="adaptables", value="org.apache.sling.api.resource.Resource"),
    @Property(name="adapters", value={"org.apache.sling.api.scripting.SlingScript",
                                      "javax.servlet.Servlet"}),
    @Property(name="adapter.condition", value="If the resource's path ends in an extension registered by a script engine.")
})
public class SlingScriptAdapterFactory implements AdapterFactory, MimeTypeProvider {

    private BundleContext bundleContext;

    /** The context string to use to select BindingsValuesProviders */
    public static final String BINDINGS_CONTEXT = BindingsValuesProvider.DEFAULT_CONTEXT;

    /**
     * The service cache for script execution.
     */
    private ServiceCache serviceCache;

    /**
     * The script engine manager.
     */
    @Reference
    private SlingScriptEngineManager scriptEngineManager;
    
    /**
     * The BindingsValuesProviderTracker
     */
    @Reference
    private BindingsValuesProvidersByContext bindingsValuesProviderTracker;

    // ---------- AdapterFactory -----------------------------------------------

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {

        Resource resource = (Resource) adaptable;
        String path = resource.getPath();
        String ext = path.substring(path.lastIndexOf('.') + 1);

        ScriptEngine engine = scriptEngineManager.getEngineByExtension(ext);
        if (engine != null) {
            final Collection<BindingsValuesProvider> bindingsValuesProviders = 
                    bindingsValuesProviderTracker.getBindingsValuesProviders(engine.getFactory(), BINDINGS_CONTEXT);
            // unchecked cast
            return (AdapterType) new DefaultSlingScript(this.bundleContext,
                    resource, engine, bindingsValuesProviders, this.serviceCache);
        }

        return null;
    }

    // ---------- MimeTypeProvider

    /**
     * Returns the first MIME type entry of the supported MIME types of a
     * ScriptEngineFactory which is registered for the extension of the given
     * name. If no ScriptEngineFactory is registered for the given extension or
     * the registered ScriptEngineFactory is not registered for a MIME type,
     * this method returns <code>null</code>.
     *
     * @param name The name whose extension is to be mapped to a MIME type. The
     *            extension is the string after the last dot in the name. If the
     *            name contains no dot, the entire name is considered the
     *            extension.
     */
    public String getMimeType(String name) {
        name = name.substring(name.lastIndexOf('.') + 1);
        ScriptEngine se = scriptEngineManager.getEngineByExtension(name);
        if (se != null) {
            List<?> mimeTypes = se.getFactory().getMimeTypes();
            if (mimeTypes != null && mimeTypes.size() > 0) {
                return String.valueOf(mimeTypes.get(0));
            }
        }

        return null;
    }

    /**
     * Returns the first extension entry of the supported extensions of a
     * ScriptEngineFactory which is registered for the given MIME type. If no
     * ScriptEngineFactory is registered for the given MIME type or the
     * registered ScriptEngineFactory is not registered for an extensions, this
     * method returns <code>null</code>.
     *
     * @param mimeType The MIME type to be mapped to an extension.
     */
    public String getExtension(String mimeType) {
        ScriptEngine se = scriptEngineManager.getEngineByMimeType(mimeType);
        if (se != null) {
            List<?> extensions = se.getFactory().getExtensions();
            if (extensions != null && extensions.size() > 0) {
                return String.valueOf(extensions.get(0));
            }
        }

        return null;
    }

    // ---------- SCR integration ----------------------------------------------

    protected void activate(ComponentContext context) {
        bundleContext = context.getBundleContext();
        this.serviceCache = new ServiceCache(this.bundleContext);
    }

    protected void deactivate(ComponentContext context) {
        this.serviceCache.dispose();
        this.serviceCache = null;
        this.bundleContext = null;
    }
}
