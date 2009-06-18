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
package org.apache.sling.launchpad.base.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.felix.framework.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
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
    static final String SCHEME = "slinginstall:";

    /**
     * The root location in which the bundles are looked up for installation
     * (value is "resources/").
     */
    static final String PATH_RESOURCES = "resources/";

    /**
     * The location of the core Bundles (value is "resources/corebundles").
     * These bundles are installed at startlevel
     * {@link #STARTLEVEL_CORE_BUNDLES}.
     * <p>
     * This location is deprecated, instead these core bundles should be located
     * in <code>resources/bundles/1</code>.
     */
    static final String PATH_CORE_BUNDLES = PATH_RESOURCES + "corebundles";

    /**
     * The location the additional Bundles (value is "resources/bundles"). These
     * Bundles are installed after the {@link #PATH_CORE_BUNDLES core Bundles}.
     */
    static final String PATH_BUNDLES = PATH_RESOURCES + "bundles";

    /**
     * The start level to be assigned to bundles found in the (old style)
     * {@link #PATH_CORE_BUNDLES resources/corebundles} location (value is 1).
     */
    static final int STARTLEVEL_CORE_BUNDLES = 1;

    /**
     * The start level to be assigned to bundles found in the (old style)
     * {@link #PATH_BUNDLES resources/bundles} location (value is 0).
     */
    static final int STARTLEVEL_BUNDLES = 0;

    /**
     * The marker start level indicating the location of the bundle cannot be
     * resolved to a valid start level (value is -1).
     */
    static final int STARTLEVEL_NONE = -1;

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
     * are first installed and then started in the order of installation. Also
     * install all deployment packages. This installation stuff is only
     * performed during the first startup!
     */
    public void start(BundleContext context) throws Exception {
        if (!isAlreadyInstalled(context)) {
            // register deployment package support
            final DeploymentPackageInstaller dpi = new DeploymentPackageInstaller(
                context, logger, resourceProvider);
            context.addFrameworkListener(dpi);
            context.addServiceListener(dpi, "(" + Constants.OBJECTCLASS + "="
                + DeploymentPackageInstaller.DEPLOYMENT_ADMIN + ")");

            // list all existing bundles
            Bundle[] bundles = context.getBundles();
            Map<String, Bundle> bySymbolicName = new HashMap<String, Bundle>();
            for (int i = 0; i < bundles.length; i++) {
                bySymbolicName.put(bundles[i].getSymbolicName(), bundles[i]);
            }

            // the start level service to set the initial start level
            ServiceReference ref = context.getServiceReference(StartLevel.class.getName());
            StartLevel startLevelService = (ref != null)
                    ? (StartLevel) context.getService(ref)
                    : null;

            // install bundles
            List<Bundle> installed = new LinkedList<Bundle>();

            Iterator<String> res = resourceProvider.getChildren(PATH_BUNDLES);
            while (res.hasNext()) {
                String path = res.next();
                // only consider folders
                if (path.endsWith("/")) {

                    // cut off trailing slash
                    path = path.substring(0, path.length() - 1);

                    // calculate the startlevel of bundles contained
                    int startLevel = getStartLevel(path);
                    if (startLevel != STARTLEVEL_NONE) {
                        installBundles(context, bySymbolicName, path,
                            installed, startLevelService, startLevel);
                    }
                }
            }

            // install old-style core bundles
            installBundles(context, bySymbolicName, PATH_CORE_BUNDLES,
                installed, startLevelService, STARTLEVEL_CORE_BUNDLES);

            // install old-style bundles
            installBundles(context, bySymbolicName, PATH_BUNDLES, installed,
                startLevelService, STARTLEVEL_BUNDLES);

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
     *            install. Only resources whose name ends with <em>.jar</em> are
     *            considered for installation.
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

                // get the manifest for the bundle information
                Manifest manifest = getManifest(path);
                if (manifest == null) {
                    logger.log(Logger.LOG_ERROR, "Ignoring " + path
                        + ": Cannot read manifest");
                    continue;
                }

                // ensure a symbolic name in the jar file
                String symbolicName = getBundleSymbolicName(manifest);
                if (symbolicName == null) {
                    logger.log(Logger.LOG_ERROR, "Ignoring " + path
                        + ": Missing " + Constants.BUNDLE_SYMBOLICNAME
                        + " in manifest");
                    continue;
                }

                // check for an nstalled Bundle with the symbolic name
                Bundle installedBundle = currentBundles.get(symbolicName);
                if (ignore(installedBundle, manifest)) {
                    logger.log(Logger.LOG_INFO, "Ignoring " + path
                        + ": More recent version already installed");
                    continue;
                }

                // try to access the JAR file, ignore if not possible
                InputStream ins = resourceProvider.getResourceAsStream(path);
                if (ins == null) {
                    continue;
                }

                if (installedBundle != null) {

                    try {
                        installedBundle.update(ins);
                        logger.log(Logger.LOG_INFO, "Bundle "
                            + installedBundle.getSymbolicName()
                            + " updated from " + path);
                    } catch (BundleException be) {
                        logger.log(Logger.LOG_ERROR, "Bundle update from "
                            + path + " failed", be);
                    }

                } else {

                    // install the JAR file as a bundle
                    String location = SCHEME
                        + path.substring(path.lastIndexOf('/') + 1);
                    try {
                        Bundle theBundle = context.installBundle(location, ins);
                        logger.log(Logger.LOG_INFO, "Bundle "
                            + theBundle.getSymbolicName() + " installed from "
                            + location);

                        // finally add the bundle to the list for later start
                        installed.add(theBundle);

                        // optionally set the start level
                        if (startLevel > 0) {
                            startLevelService.setBundleStartLevel(theBundle,
                                startLevel);
                        }

                    } catch (BundleException be) {
                        logger.log(Logger.LOG_ERROR,
                            "Bundle installation from " + location + " failed",
                            be);
                    }
                }
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
        return STARTLEVEL_NONE;
    }

    // ---------- Bundle JAR file information

    /**
     * Returns the Manifrest from the JAR file in the given resource provided by
     * the resource provider or <code>null</code> if the resource does not
     * exists or is not a JAR file or has no Manifest.
     * 
     * @param jarPath The path to the JAR file provided by the resource provider
     *            of this instance.
     */
    private Manifest getManifest(String jarPath) {
        InputStream ins = resourceProvider.getResourceAsStream(jarPath);
        if (ins != null) {
            try {
                JarInputStream jar = new JarInputStream(ins);
                return jar.getManifest();
            } catch (IOException ioe) {
                logger.log(Logger.LOG_ERROR, "Failed to read manifest from "
                    + jarPath, ioe);
            } finally {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
        }

        return null;
    }

    /**
     * Returns the <i>Bundle-SymbolicName</i> header from the given manifest or
     * <code>null</code> if no such header exists.
     * <p>
     * Note that bundles are not allowed to have no symbolic name any more.
     * Therefore a bundle without a symbolic name header should not be
     * installed.
     * 
     * @param manifest The Manifest from which to extract the header.
     */
    private String getBundleSymbolicName(Manifest manifest) {
        return manifest.getMainAttributes().getValue(
            Constants.BUNDLE_SYMBOLICNAME);
    }

    /**
     * Checks whether the installed bundle is at the same version (or more
     * recent) than the bundle described by the given manifest.
     * 
     * @param installedBundle The bundle currently installed in the framework
     * @param manifest The Manifest describing the bundle version potentially
     *            updating the installed bundle
     * @return <code>true</code> if the manifest does not describe a bundle with
     *         a higher version number.
     */
    private boolean ignore(Bundle installedBundle, Manifest manifest) {

        // the bundle is not installed yet, so we have to install it
        if (installedBundle == null) {
            return false;
        }

        String versionProp = manifest.getMainAttributes().getValue(
            Constants.BUNDLE_VERSION);
        Version newVersion = Version.parseVersion(versionProp);

        String installedVersionProp = (String) installedBundle.getHeaders().get(
            Constants.BUNDLE_VERSION);
        Version installedVersion = Version.parseVersion(installedVersionProp);

        return newVersion.compareTo(installedVersion) <= 0;
    }

    // ---------- Bundle Installation marker file

    private boolean isAlreadyInstalled(BundleContext context) {
        final File dataFile = context.getDataFile(DATA_FILE);
        if (dataFile != null && dataFile.exists()) {

            FileInputStream fis = null;
            try {

                long selfStamp = getSelfTimestamp();
                if (selfStamp > 0) {

                    fis = new FileInputStream(dataFile);
                    byte[] bytes = new byte[20];
                    int len = fis.read(bytes);
                    String value = new String(bytes, 0, len);

                    long storedStamp = Long.parseLong(value);

                    return storedStamp >= selfStamp;
                }

            } catch (NumberFormatException nfe) {
                // probably still the old value, fallback to assume not
                // installed

            } catch (IOException ioe) {
                logger.log(Logger.LOG_ERROR,
                    "IOException during reading of installed flag.", ioe);

            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ignore) {
                    }
                }
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
                fos.write(String.valueOf(getSelfTimestamp()).getBytes());
            } finally {
                try {
                    fos.close();
                } catch (IOException ignore) {
                }
            }
        } catch (IOException ioe) {
            logger.log(Logger.LOG_ERROR,
                "IOException during writing of installed flag.", ioe);
        }
    }

    /**
     * Returns the time stamp of JAR file from which this class has been loaded
     * or -1 if the timestamp cannot be resolved.
     * <p>
     * This method assumes that the ClassLoader of this class is an
     * URLClassLoader and that the first URL entry of this class loader is the
     * JAR providing this class. This is in fact true as the URLClassLoader has
     * been created by the launcher from the launcher JAR file.
     * 
     * @return The last modification time stamp of the launcher JAR file or -1
     *         if the class loader of this class is not an URLClassLoader or the
     *         class loader has no URL entries. Both situations are not really
     *         expected.
     * @throws IOException If an error occurrs reading accessing the last
     *             modification time stampe.
     */
    private long getSelfTimestamp() throws IOException {

        ClassLoader loader = getClass().getClassLoader();
        if (loader instanceof URLClassLoader) {
            URLClassLoader urlLoader = (URLClassLoader) loader;
            URL[] urls = urlLoader.getURLs();
            if (urls.length > 0) {
                return urls[0].openConnection().getLastModified();
            }
        }

        return -1;
    }
}
