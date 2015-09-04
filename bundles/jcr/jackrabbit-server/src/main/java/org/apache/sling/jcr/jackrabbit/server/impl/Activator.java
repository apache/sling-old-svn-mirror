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
package org.apache.sling.jcr.jackrabbit.server.impl;

import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The <code>Activator</code> TODO
 */
public class Activator implements BundleActivator {

    public static final String SERVER_REPOSITORY_FACTORY_PID = "org.apache.sling.jcr.jackrabbit.server.SlingServerRepository";

    /**
     * The name of the configuration property naming the Sling Context for which
     * a factory configuration has been created.
     */
    public static final String SLING_CONTEXT = "sling.context";

    /**
     * The name of the framework property containing the default sling context
     * name.
     */
    public static final String SLING_CONTEXT_DEFAULT = "sling.context.default";

    // this bundle's context, used by verifyConfiguration
    private static BundleContext bundleContext;

    // the service tracker used by the PluggableDefaultLoginModule
    // this field is only set on the first call to getLoginModules()
    private static ServiceTracker loginModuleTracker;

    // the tracking count when the moduleCache has been filled
    private static int lastTrackingCount = -1;

    // the cache of login module services
    private static LoginModulePlugin[] moduleCache;

    // empty list of login modules if there are none registered
    private static LoginModulePlugin[] EMPTY = new LoginModulePlugin[0];

    // the name of the default sling context
    private String slingContext;
    private static AccessManagerFactoryTracker accessManagerFactoryTracker;

    public void start(BundleContext context) {

        bundleContext = context;

        // ensure the module cache is not set right now, this may
        // (theoretically) be non-null after the last bundle stop
        moduleCache = null;

        // check the name of the default context, nothing to do if none
        slingContext = context.getProperty(SLING_CONTEXT_DEFAULT);
        if (slingContext == null) {
            slingContext = "default";
        }
        
        if (accessManagerFactoryTracker == null) {
            accessManagerFactoryTracker = new AccessManagerFactoryTracker(bundleContext);
        }
        accessManagerFactoryTracker.open();
    }

    public void stop(BundleContext arg0) {

        // drop module cache
        moduleCache = null;

        // close the loginModuleTracker
        if (loginModuleTracker != null) {
            loginModuleTracker.close();
            loginModuleTracker = null;
        }

        if (accessManagerFactoryTracker != null) {
            accessManagerFactoryTracker.close();
            accessManagerFactoryTracker = null;
        }

        // clear the bundle context field
        bundleContext = null;
    }

    // ---------- LoginModule tracker for PluggableDefaultLoginModule

    private static BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * Returns the registered {@link LoginModulePlugin} services. If there are
     * no {@link LoginModulePlugin} services registered, this method returns an
     * empty array. <code>null</code> is never returned from this method.
     */
    public static LoginModulePlugin[] getLoginModules() {
        // fast track cache (cache first, since loginModuleTracker is only
        // non-null if moduleCache is non-null)
        if (moduleCache != null
            && lastTrackingCount == loginModuleTracker.getTrackingCount()) {
            return moduleCache;
        }
        // invariant: moduleCache is null or modules have changed

        // tracker may be null if moduleCache is null
        if (loginModuleTracker == null) {
            loginModuleTracker = new ServiceTracker(getBundleContext(),
                LoginModulePlugin.class.getName(), null);
            loginModuleTracker.open();
        }

        if (moduleCache == null || lastTrackingCount < loginModuleTracker.getTrackingCount()) {
            Object[] services = loginModuleTracker.getServices();
            if (services == null || services.length == 0) {
                moduleCache = EMPTY;
            } else {
                moduleCache = new LoginModulePlugin[services.length];
                System.arraycopy(services, 0, moduleCache, 0, services.length);
            }
            lastTrackingCount = loginModuleTracker.getTrackingCount();
        }

        // the module cache is now up to date
        return moduleCache;
    }

    public static AccessManagerFactoryTracker getAccessManagerFactoryTracker() {
        return accessManagerFactoryTracker;
    }
}
