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
package org.apache.sling.bundleresource.impl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator, BundleListener {

    /**
     * The name of the bundle manifest header listing the resource provider root
     * paths provided by the bundle (value is "Sling-Bundle-Resources").
     */
    public static final String BUNDLE_RESOURCE_ROOTS = "Sling-Bundle-Resources";

    /**
     * Fully qualified name of the Web Console Plugin class. This class will be
     * loaded dynamically to prevent issues if the Felix Web Console is not
     * installed in the system (value is
     * "org.apache.sling.bundleresource.impl.BundleResourceWebConsolePlugin").
     */
    private static final String CONSOLE_PLUGIN_CLASS = "org.apache.sling.bundleresource.impl.BundleResourceWebConsolePlugin";

    /**
     * Name of the initialization method to call on the Web Console Plugin class
     * (value is "initPlugin").
     */
    private static final String METHOD_INIT = "initPlugin";

    /**
     * Name of the shutdown method to call on the Web Console Plugin class
     * (value is "destroyPlugin").
     */
    private static final String METHOD_DESTROY = "destroyPlugin";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<Long, BundleResourceProvider> bundleResourceProviderMap = new HashMap<Long, BundleResourceProvider>();

    private BundleContext bundleContext;

    public void start(BundleContext context) throws Exception {

        this.bundleContext = context;

        context.addBundleListener(this);

        try {
            Bundle[] bundles = context.getBundles();
            for (Bundle bundle : bundles) {
                if (bundle.getState() == Bundle.ACTIVE) {
                    // add bundle resource provider for active bundles
                    addBundleResourceProvider(bundle);
                }
            }
        } catch (Throwable t) {
            log.error(
                "activate: Problem while registering bundle resources for existing bundles",
                t);
        }

        // hackery thing to prevent problems if the web console is not present
        callMethod(CONSOLE_PLUGIN_CLASS, METHOD_INIT,
            new Class<?>[] { BundleContext.class }, new Object[] { context });

    }

    public void stop(BundleContext context) throws Exception {
        // hackery thing to prevent problems if the web console is not present
        callMethod(CONSOLE_PLUGIN_CLASS, METHOD_DESTROY, null, null);

        context.removeBundleListener(this);
        this.bundleContext = null;
    }

    /**
     * Loads and unloads any components provided by the bundle whose state
     * changed. If the bundle has been started, the components are loaded. If
     * the bundle is about to stop, the components are unloaded.
     * 
     * @param event The <code>BundleEvent</code> representing the bundle state
     *            change.
     */
    public void bundleChanged(BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTED:
                // register resource provider for the started bundle
                addBundleResourceProvider(event.getBundle());
                break;

            case BundleEvent.STOPPED:
                // remove resource provider after the bundle has stopped
                removeBundleResourceProvider(event.getBundle());
                break;
        }
    }

    // ---------- Bundle provided resources -----------------------------------

    private void addBundleResourceProvider(Bundle bundle) {
        String prefixes = (String) bundle.getHeaders().get(
            BUNDLE_RESOURCE_ROOTS);
        if (prefixes != null) {
            BundleResourceProvider brp = new BundleResourceProvider(bundle,
                prefixes);
            brp.registerService(bundleContext);
            bundleResourceProviderMap.put(bundle.getBundleId(), brp);
        }
    }

    private void removeBundleResourceProvider(Bundle bundle) {
        BundleResourceProvider brp = bundleResourceProviderMap.remove(bundle.getBundleId());
        if (brp != null) {
            brp.unregisterService();
        }
    }

    /**
     * Helper method to call the static method <code>methodName</code> on the
     * class <code>clazzName</code> with the given <code>args</code>. This
     * method operates exclusively using reflection to prevent any issues if the
     * class cannot be loaded.
     * <p>
     * The goal is to enable running the bundle resource provider without a hard
     * dependency on the Felix Web Console.
     * 
     * @param clazzName The fully qualified name of the class whose static
     *            method is to be called.
     * @param methodName The name of the method to call. This method must be
     *            declared in the given class.
     * @param argTypes The types of arguments of the methods to be able to find
     *            the method. This may be <code>null</code> if the method has
     *            no arguments.
     * @param args The actual arguments to the method. This may be
     *            <code>null</code> if the method has no arguments.
     */
    private void callMethod(String clazzName, String methodName,
            Class<?>[] argTypes, Object[] args) {
        try {
            Class<?> clazz = getClass().getClassLoader().loadClass(clazzName);
            Method method = clazz.getDeclaredMethod(methodName, argTypes);
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            method.invoke(null, args);
        } catch (Throwable t) {
            // ignore anything
        }
    }
}
