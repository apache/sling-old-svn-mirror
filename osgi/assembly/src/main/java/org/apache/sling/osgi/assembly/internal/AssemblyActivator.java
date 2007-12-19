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
package org.apache.sling.osgi.assembly.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.osgi.assembly.installer.InstallerService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;


/**
 * The <code>AssemblyActivator</code> is the bundle activator for the Project
 * Sling Assembly Manager. As such this activator instantiates the Assembly
 * Manager when started and stops the Assembly Manager when the bundle is
 * stopped.
 * <p>
 * This activator also listens for bundle events and forwards any event for an
 * Assembly Bundle to the Assembly Manager. As this class is an asynchronous
 * listener the <code>BundleEvent.STARTING</code> and
 * <code>BundleEvent.STOPPING</code> events are not received by the
 * {@link #bundleChanged(BundleEvent)} method.
 */
public class AssemblyActivator implements BundleActivator, BundleListener {

    /**
     * The {@link AssemblyManager} managing assemblies and their contained or
     * referred bundles.
     */
    private AssemblyManager assemblyManager;

    // ---------- BundleActivator ----------------------------------------------

    /**
     * Starts this bundle by creating the Assembly Manager and starting to
     * listen for bundle changes. The Assembly Manager is also started, which
     * means that all bundles in the framework are checked to see whether any is
     * an Assembly Bundle. Each Assembly Bundle is taken under control by the
     * Assembly Manager.
     *
     * @param context The <code>BundleContext</code> of the starting bundle.
     */
    public void start(BundleContext context) {
        // prepare the InstallerService
        InstallerService installerService = new InstallerServiceImpl(context);

        // register myself as the factory for the installer service
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_DESCRIPTION,
            "Project Sling Bundle Installler Service");
        context.registerService(InstallerService.class.getName(),
            installerService, props);

        // set up the assembly manager
        this.assemblyManager = new AssemblyManager(context, installerService);
        context.addBundleListener(this);
        this.assemblyManager.start();
    }

    /**
     * Stops this bundle by removing as a bundle change listener and stopping
     * the Assembly Manager for handling Assembly Bundles. When the Assembly
     * Manager is stopped, all Assembly Bundles remain simple OSGi bundles with
     * no special treatment.
     *
     * @param context The <code>BundleContext</code> of the stopping bundle.
     */
    public void stop(BundleContext context) {
        context.removeBundleListener(this);

        this.assemblyManager.stop();
        this.assemblyManager = null;
    }

    // ---------- BundleListener -----------------------------------------------

    /**
     * Called whenver a bundle changes its state. If the bundle changing its
     * state is not an Assembly Bundle, that is the bundle has no
     * <code>Assembly-Bundles</code> header, the event is ignored. Otherwise
     * the event is forwarded to the Assembly Manager.
     *
     * @param event The <code>BundleEvent</code> representing the bundle state
     *            change.
     */
    public void bundleChanged(BundleEvent event) {
        // ignore non-assembly bundles
        Bundle bundle = event.getBundle();
        if (bundle.getHeaders().get(Assembly.ASSEMBLY_BUNDLES) == null) {
            return;
        }

        // handle the bundle
        this.assemblyManager.put(event);
    }
}
