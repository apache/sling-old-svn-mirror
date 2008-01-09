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
package org.apache.sling.scripting.resolver.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptResolver;
import org.apache.sling.scripting.resolver.ScriptPathSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Find scripts in the repository, based on the current Resource type. The
 * script filename is built using the current HTTP request method name, followed
 * by the extension of the current request and the desired script extension. For
 * example, a "js" script for a GET request on a Resource of type some/type with
 * request extension "html" should be stored as
 *
 * <pre>
 *      /sling/scripts/some/type/get.html.js
 * </pre>
 *
 * in the repository.
 *
 * @scr.component metatype="no"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description" value="Default SlingScriptResolver"
 * @scr.service
 * @scr.reference name="ScriptEngineFactory"
 *                interface="javax.script.ScriptEngineFactory"
 *                cardinality="0..n" policy="dynamic"
 */
public class DefaultSlingScriptResolver implements SlingScriptResolver,
        BundleListener {

    private static final Logger log = LoggerFactory.getLogger(DefaultSlingScriptResolver.class);

    /**
     * jcr:encoding
     */
    public static final String JCR_ENCODING = "jcr:encoding";

    /** @scr.property values.1="/apps" values.2="/libs" */
    public static final String PROP_SCRIPT_PATH = "path";

    private static final String ENGINE_FACTORY_SERVICE = "META-INF/services/"
        + ScriptEngineFactory.class.getName();

    private ScriptEngineManager scriptEngineManager;

    private String[] scriptPath;

    private List<Bundle> engineSpiBundles = new LinkedList<Bundle>();

    private List<ScriptEngineFactory> engineSpiServices = new LinkedList<ScriptEngineFactory>();

    /**
     * Try to find a script Node that can process the given request, based on
     * the rules defined above.
     *
     * @return null if not found.
     */
    public SlingScript resolveScript(final SlingHttpServletRequest request)
            throws SlingException {

        ResourceResolver resolver = request.getResourceResolver();

        String baseName = ScriptPathSupport.getScriptBaseName(request) + ".";

        SlingScript result = null;
        Iterator<String> pathIterator = ScriptPathSupport.getPathIterator(
            request, scriptPath);
        while (result == null && pathIterator.hasNext()) {

            Resource scriptRoot = resolver.getResource(pathIterator.next());
            if (scriptRoot != null) {

                log.debug("Looking for script with filename={} under {}",
                    baseName, scriptRoot.getPath());

                // get the item and ensure it is a node
                Iterator<Resource> children = resolver.listChildren(scriptRoot);
                while (result == null && children.hasNext()) {
                    Resource resource = children.next();
                    result = getScript(resource, baseName);
                }
            }
        }

        if (result != null) {
            log.info("Script {} found for Resource={}",
                result.getScriptResource().getPath(), request.getResource());
        } else {
            log.debug("No script found for Resource={}", request.getResource());
        }

        return result;
    }

    public SlingScript findScript(ResourceResolver resourceResolver, String path)
            throws SlingException {
        Resource scriptResource;
        if (path.startsWith("/")) {
            scriptResource = resourceResolver.getResource(path);
        } else {
            scriptResource = null;
            for (int i = 0; scriptResource == null && i < scriptPath.length; i++) {
                String testPath = scriptPath[i] + "/" + path;
                scriptResource = resourceResolver.getResource(testPath);
            }
        }

        if (scriptResource != null) {
            SlingScript script = getScript(scriptResource, null);
            if (script == null) {
                log.error("Cannot handle script {} for path {}",
                    scriptResource.getPath(), path);
            } else {
                log.debug("Returning script {} for path {}",
                    scriptResource.getPath(), path);
            }
        } else {
            log.error("No resource found at " + path);
        }

        return null;
    }

    private ScriptEngineManager getScriptEngineManager() {
        if (scriptEngineManager == null) {

            // create (empty) script engine manager
            ClassLoader loader = getClass().getClassLoader();
            ScriptEngineManager tmp = new ScriptEngineManager(loader);

            // register script engines from bundles
            for (Bundle bundle : engineSpiBundles) {
                registerFactories(tmp, bundle);
            }

            // register script engines from registered services
            for (ScriptEngineFactory factory : engineSpiServices) {
                registerFactory(tmp, factory);
            }

            scriptEngineManager = tmp;
        }
        return scriptEngineManager;
    }

    private void registerFactories(ScriptEngineManager mgr, Bundle bundle) {
        URL url = bundle.getEntry(ENGINE_FACTORY_SERVICE);
        InputStream ins = null;
        try {
            ins = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                ins));
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<ScriptEngineFactory> clazz = bundle.loadClass(line);
                    ScriptEngineFactory spi = clazz.newInstance();
                    registerFactory(mgr, spi);
                } catch (Throwable t) {
                    log.error("Cannot register ScriptEngineFactory " + line, t);
                }
            }
        } catch (IOException ioe) {
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    private void registerFactory(ScriptEngineManager mgr,
            ScriptEngineFactory factory) {
        log.info("Adding ScriptEngine {}, {} for language {}, {}",
            new Object[] { factory.getEngineName(), factory.getEngineVersion(),
                factory.getLanguageName(), factory.getLanguageVersion() });

        for (Object ext : factory.getExtensions()) {
            mgr.registerEngineExtension((String) ext, factory);
        }

        for (Object mime : factory.getMimeTypes()) {
            mgr.registerEngineMimeType((String) mime, factory);
        }

        for (Object name : factory.getNames()) {
            mgr.registerEngineName((String) name, factory);
        }
    }

    // ---------- BundleListener interface -------------------------------------

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.STARTED
            && event.getBundle().getEntry(ENGINE_FACTORY_SERVICE) != null) {

            engineSpiBundles.add(event.getBundle());
            scriptEngineManager = null;

        } else if (event.getType() == BundleEvent.STOPPED
            && engineSpiBundles.remove(event.getBundle())) {

            scriptEngineManager = null;

        }
    }

    // ---------- SCR integration ----------------------------------------------

    protected void activate(ComponentContext context) {

        Object pathProp = context.getProperties().get(PROP_SCRIPT_PATH);
        if (pathProp instanceof String[]) {
            scriptPath = (String[]) pathProp;
        } else if (pathProp instanceof Vector<?>) {
            Vector<?> pathVector = (Vector<?>) pathProp;
            List<String> pathList = new ArrayList<String>();
            for (Object entry : pathVector) {
                if (entry != null) {
                    pathList.add(entry.toString());
                }
            }
            scriptPath = pathList.toArray(new String[pathList.size()]);
        } else {
            scriptPath = new String[] { "/" };
        }

        context.getBundleContext().addBundleListener(this);

        Bundle[] bundles = context.getBundleContext().getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getState() == Bundle.ACTIVE
                && bundle.getEntry(ENGINE_FACTORY_SERVICE) != null) {
                engineSpiBundles.add(bundle);
            }
        }
    }

    protected void deactivate(ComponentContext context) {
        context.getBundleContext().removeBundleListener(this);

        engineSpiBundles.clear();
        engineSpiServices.clear();
        scriptEngineManager = null;
    }

    protected void bindScriptEngineFactory(
            ScriptEngineFactory scriptEngineFactory) {
        engineSpiServices.add(scriptEngineFactory);
        scriptEngineManager = null;
    }

    protected void unbindScriptEngineFactory(
            ScriptEngineFactory scriptEngineFactory) {
        engineSpiServices.remove(scriptEngineFactory);
        scriptEngineManager = null;
    }

    // ---------- inner class --------------------------------------------------

    private SlingScript getScript(Resource resource, String baseName) {
        String path = resource.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1);

        if (baseName == null || name.startsWith(baseName)) {
            String ext = name.substring(baseName.length());
            ScriptEngine engine = getScriptEngineManager().getEngineByExtension(
                ext);
            if (engine != null) {
                return new DefaultSlingScript(resource, engine);
            }
        }

        return null;
    }
}
