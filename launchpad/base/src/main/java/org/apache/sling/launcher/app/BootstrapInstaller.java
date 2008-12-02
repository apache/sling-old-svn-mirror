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
package org.apache.sling.launcher.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.felix.framework.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;

/**
 * The <code>BootstrapInstaller</code> class is installed into the OSGi
 * framework as an activator to be called when the framework is starting up.
 * Upon startup all bundles from the {@link #PATH_CORE_BUNDLES} and the
 * {@link #PATH_BUNDLES} location are checked whether they are already installed
 * or not. If they are not installed, they are installed, their start level set
 * to 1 and started. Any bundle already installed is not installed again and
 * will also not be started here.
 */
class BootstrapInstaller implements BundleActivator {

    /**
     * The Bundle location scheme (protocol) used for bundles installed by this
     * activator (value is "slinginstall:"). The path part of the Bundle
     * location of Bundles installed by this class is the name (without the
     * path) of the resource from which the Bundle was installed.
     */
    public static final String SCHEME = "slinginstall:";

    /**
     * The location the core Bundles (value is "resources"). These
     * bundles are installed first.
     */
    public static final String PATH_BUNDLE_ROOT = "resources";
    
    /**
     * The location the core Bundles (value is "resources/corebundles"). These
     * bundles are installed first.
     */
    public static final String PATH_CORE_BUNDLES = "corebundles";

    /**
     * The location the additional Bundles (value is "resources/bundles"). These
     * Bundles are installed after the {@link #PATH_CORE_BUNDLES core Bundles}.
     */
    public static final String PATH_BUNDLES = "bundles";

    /**
     * The {@link Logger} use for logging messages during installation and
     * startup.
     */
    private final Logger logger;

    /**
     * The {@link ResourceProvider} used to access the Bundle jar files to
     * install.
     */
    private final ResourceProvider resourceProvider;

    /** The data file which works as a marker to detect the first startup. */
    private static final String DATA_FILE = "bootstrapinstaller.ser";

    BootstrapInstaller(Logger logger, ResourceProvider resourceProvider) {
        this.logger = logger;
        this.resourceProvider = resourceProvider;
    }

    /**
     * Installs any Bundles missing in the current framework instance. The
     * Bundles are verified by the Bundle location string. All missing Bundles
     * are first installed and then started in the order of installation.
     * Also install all deployment packages.
     *
     * This installation stuff is only performed during the first startup!
     */
    public void start(BundleContext context) throws Exception {
        if (!isAlreadyInstalled(context)) {
            // register deployment package support
            final DeploymentPackageInstaller dpi =
                new DeploymentPackageInstaller(context, logger, resourceProvider);
            context.addFrameworkListener(dpi);
            context.addServiceListener(dpi, "("
                    + Constants.OBJECTCLASS + "=" + DeploymentPackageInstaller.DEPLOYMENT_ADMIN + ")");

            // list all existing bundles
            Bundle[] bundles = context.getBundles();
            Map<String, Bundle> byLocation = new HashMap<String, Bundle>();
            for (int i = 0; i < bundles.length; i++) {
                byLocation.put(bundles[i].getLocation(), bundles[i]);
            }

            // the start level service to set the initial start level
            ServiceReference ref = context.getServiceReference(StartLevel.class.getName());
            StartLevel startLevelService = (ref != null)
                    ? (StartLevel) context.getService(ref)
                    : null;

            // install bundles
            List<Bundle> installed = new LinkedList<Bundle>();

            Iterator<String> res = resourceProvider.getChildren(PATH_BUNDLE_ROOT);
            while (res.hasNext()) {
                String path = res.next();
                // only consider folders
                if (path.endsWith("/")) {
                    
                    // cut off trailing slash
                    path = path.substring(0, path.length()-1);

                    // calculate the startlevel of bundles contained
                    int startLevel = getStartLevel(path);
                    if (startLevel >= 0) {
                        installBundles(context, byLocation, path, installed,
                            startLevelService, startLevel);
                    }
                }
            }

            // release the start level service
            if (ref != null) {
                context.ungetService(ref);
            }

            // set start levels on the bundles and start them
            startBundles(installed);
            
            // mark everything installed
            markInstalled(context);
        }
    }

    /** Nothing to be done on stop */
    public void stop(BundleContext context) {
    }

    /**
     * Install the Bundles from JAR files found in the given <code>parent</code>
     * path.
     *
     * @param context The <code>BundleContext</code> used to install the new
     *            Bundles.
     * @param currentBundles The currently installed Bundles indexed by their
     *            Bundle location.
     * @param parent The path to the location in which to look for JAR files to
     *            install. Only resources whose name ends with <em>.jar</em>
     *            are considered for installation.
     * @param installed The list of Bundles installed by this method. Each
     *            Bundle successfully installed is added to this list.
     */
    private void installBundles(BundleContext context,
            Map<String, Bundle> currentBundles, String parent,
            List<Bundle> installed, StartLevel startLevelService, int startLevel) {

        Iterator<String> res = resourceProvider.getChildren(parent);
        while (res.hasNext()) {

            String path = res.next();

            if (path.endsWith(".jar")) {

                // check for an already installed Bundle with the given location
                String location = SCHEME
                    + path.substring(path.lastIndexOf('/') + 1);
                if (currentBundles.containsKey(location)) {
                    continue;
                }

                // try to access the JAR file, ignore if not possible
                InputStream ins = resourceProvider.getResourceAsStream(path);
                if (ins == null) {
                    continue;
                }

                // install the JAR file as a bundle
                Bundle newBundle;
                try {
                    newBundle = context.installBundle(location, ins);
                    logger.log(Logger.LOG_INFO, "Bundle "
                        + newBundle.getSymbolicName() + " installed from "
                        + location);
                } catch (BundleException be) {
                    logger.log(Logger.LOG_ERROR, "Bundle installation from "
                        + location + " failed", be);
                    continue;
                }
                
                // optionally set the start level
                if (startLevel > 0) {
                    startLevelService.setBundleStartLevel(newBundle, startLevel);
                }

                // finally add the bundle to the list for later start
                installed.add(newBundle);
            }
        }
    }

    /**
     * Starts the Bundles in the <code>bundles</code> list. If the framework
     * provides an active <code>StartLevel</code> service, the start levels of
     * the Bundles is first set to <em>1</em>.
     */
    private void startBundles(List<Bundle> bundles) {

        // start all bundles
        for (Bundle bundle : bundles) {
            try {
                bundle.start();
            } catch (BundleException be) {
                logger.log(Logger.LOG_ERROR, "Bundle "
                    + bundle.getSymbolicName() + " could not be started", be);
            }
        }

    }

    private int getStartLevel(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        
        // core bundles are installed at start level 1
        if (PATH_CORE_BUNDLES.equals(name)) {
            return 1;
        }
        
        // bundles in the default location are started at the defualt
        // framework start level
        if (PATH_BUNDLES.equals(name)) {
            return 0;
        }
        
        // otherwise the name is the start level
        try {
            int level = Integer.parseInt(name);
            if (level >= 0) {
                return level;
            }
            
            logger.log(Logger.LOG_ERROR, "Illegal Runlevel for " + path
                + ", ignoring");
        } catch (NumberFormatException nfe) {
            logger.log(Logger.LOG_INFO, "Folder " + path
                + " does not denote start level, ignoring");
        }
        
        // no valid start level, ignore this location
        return -1;
    }
    
    private boolean isAlreadyInstalled(BundleContext context) {
        final File dataFile = context.getDataFile(DATA_FILE);
        if ( dataFile != null && dataFile.exists() ) {
            try {
                final FileInputStream fis = new FileInputStream(dataFile);
                try {
                    // only care for the first few bytes
                    byte[] bytes = new byte[10];
                    int len = fis.read(bytes);
                    return Boolean.parseBoolean(new String(bytes, 0, len));
                } finally {
                    try {
                        fis.close();
                    } catch (IOException ignore) {}
                }
            } catch (IOException ioe) {
                logger.log(Logger.LOG_ERROR, "IOException during reading of installed flag.", ioe);
            }
        }
        
        // fallback assuming not installed yet
        return false;
    }
    
    private void markInstalled(BundleContext context) {
        final File dataFile = context.getDataFile(DATA_FILE);
        try {
            final FileOutputStream fos = new FileOutputStream(dataFile);
            try {
                fos.write("true".getBytes());
            } finally {
                try {
                    fos.close();
                } catch (IOException ignore) {}
            }
        } catch (IOException ioe) {
            logger.log(Logger.LOG_ERROR, "IOException during writing of installed flag.", ioe);
        }
    }
}
