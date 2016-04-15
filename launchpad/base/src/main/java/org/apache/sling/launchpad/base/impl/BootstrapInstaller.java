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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.framework.Logger;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.apache.sling.launchpad.api.StartupMode;
import org.apache.sling.launchpad.base.impl.bootstrapcommands.BootstrapCommandFile;
import org.apache.sling.launchpad.base.shared.SharedConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;

/**
 * The <code>BootstrapInstaller</code> class is installed into the OSGi
 * framework as an activator to be called when the framework is starting up.
 * Upon startup all bundles from the {@link #PATH_CORE_BUNDLES} and the
 * {@link #PATH_BUNDLES} location are checked whether they are already installed
 * or not. If they are not installed, they are installed, their start level set
 * to 1 and started. Any bundle already installed is not installed again and
 * will also not be started here.
 */
class BootstrapInstaller {

    /**
     * The Bundle location scheme (protocol) used for bundles installed by this
     * activator (value is "slinginstall:"). The path part of the Bundle
     * location of Bundles installed by this class is the name (without the
     * path) of the resource from which the Bundle was installed.
     */
    private static final String SCHEME = "slinginstall:";

    /**
     * The root location in which the bundles are looked up for installation
     * (value is "resources/").
     */
    private static final String PATH_RESOURCES = "resources/";

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
     * The header which contains the bundle's last modified date.
     */
    static final String BND_LAST_MODIFIED_HEADER = "Bnd-LastModified";

    /**
     * The start level to be assigned to bundles found in the (old style)
     * {@link #PATH_CORE_BUNDLES resources/corebundles} location (value is 1).
     */
    private static final int STARTLEVEL_CORE_BUNDLES = 1;

    /**
     * The start level to be assigned to bundles found in the (old style)
     * {@link #PATH_BUNDLES resources/bundles} location (value is 0).
     */
    private static final int STARTLEVEL_BUNDLES = 0;

    /**
     * The marker start level indicating the location of the bundle cannot be
     * resolved to a valid start level (value is -1).
     */
    private static final int STARTLEVEL_NONE = -1;

    /**
     * The name of the bootstrap commands file
     */
    public static final String BOOTSTRAP_CMD_FILENAME = "sling_bootstrap.txt";

    /**
     * The {@link Logger} use for logging messages during installation and
     * startup.
     */
    private final Logger logger;

    /**
     * The {@link LaunchpadContentProvider} used to access the Bundle files to
     * install.
     */
    private final LaunchpadContentProvider resourceProvider;

    /** The bundle context. */
    private final BundleContext bundleContext;

    /** The startup mode. */
    private final StartupMode startupMode;

    BootstrapInstaller(final BundleContext bundleContext,
            final Logger logger,
            final LaunchpadContentProvider resourceProvider,
            final StartupMode startupMode) {
        this.startupMode = startupMode;
        this.logger = logger;
        this.resourceProvider = resourceProvider;
        this.bundleContext = bundleContext;
    }

    /**
     * https://issues.apache.org/jira/browse/SLING-922
     * Handles the initial detection and installation of bundles into
     * the Felix OSGi running in Sling
     *
     * Process:
     * 1) Copy all bundles from enclosed resources (jar/war) to
     *   ${sling.home}/startup. This gives something like
     *   ${sling.home}/startup/0, /1, /10, /15, ...
     *   Existing files are only replaced if the files
     *   enclosed in the Sling launchpad jar/war file are newer.
     * 2) Scan ${sling.home}/startup for bundles to install
     *   in the same way as today the enclosed resources
     *   are scanned directly.
     *   So you could place your bundles in that structure and get them installed
     *   at the requested start level (0 being "default bundle start level").
     */
    boolean install() throws IOException {

        String launchpadHome = bundleContext.getProperty(SharedConstants.SLING_LAUNCHPAD);
        if (launchpadHome == null) {
            launchpadHome = bundleContext.getProperty(SharedConstants.SLING_HOME);
        }
        final File slingStartupDir = getSlingStartupDir(launchpadHome);

        // execute bootstrap commands, if needed
        final BootstrapCommandFile cmd = new BootstrapCommandFile(logger,
            new File(launchpadHome, BOOTSTRAP_CMD_FILENAME));
        boolean requireRestart = cmd.execute(bundleContext);

        boolean shouldInstall = false;

        // see if the loading of bundles from the package is forced
        final String fpblString = bundleContext.getProperty(SharedConstants.FORCE_PACKAGE_BUNDLE_LOADING);
        if (Boolean.valueOf(fpblString)) {
            shouldInstall = true;
        } else {
            shouldInstall = this.startupMode != StartupMode.RESTART;
        }

        if (shouldInstall) {
            // only run the war/jar copies when this war/jar is new/changed

            // see if the loading of bundles from the package is disabled
            String dpblString = bundleContext.getProperty(SharedConstants.DISABLE_PACKAGE_BUNDLE_LOADING);
            Boolean disablePackageBundleLoading = Boolean.valueOf(dpblString);

            if (disablePackageBundleLoading) {
                logger.log(Logger.LOG_INFO, "Package bundle loading is disabled so no bundles will be installed from the resources location in the sling jar/war");
            } else {
                // get the bundles out of the jar/war and copy them to the startup location
                Iterator<String> resources = resourceProvider.getChildren(PATH_BUNDLES);
                while (resources.hasNext()) {
                    String path = resources.next();
                    // only consider folders
                    if (path.endsWith("/")) {

                        // cut off trailing slash
                        path = path.substring(0, path.length() - 1);

                        // calculate the startlevel of bundles contained
                        int startLevel = getStartLevel(path);
                        if (startLevel != STARTLEVEL_NONE) {
                            copyBundles(slingStartupDir, path, startLevel);
                        }
                    }
                }

                // copy old-style core bundles
                copyBundles(slingStartupDir, PATH_CORE_BUNDLES, STARTLEVEL_CORE_BUNDLES);

                // copy old-style bundles
                copyBundles(slingStartupDir, PATH_BUNDLES, STARTLEVEL_BUNDLES);

                // done with copying at this point
            }

            // get the set of all existing (installed) bundles by symbolic name
            Bundle[] bundles = bundleContext.getBundles();
            Map<String, Bundle> bySymbolicName = new HashMap<String, Bundle>();
            for (int i = 0; i < bundles.length; i++) {
                bySymbolicName.put(bundles[i].getSymbolicName(), bundles[i]);
            }

            // holds the bundles we install during this processing
            List<Bundle> installed = new LinkedList<Bundle>();

            // get all bundles from the startup location and install them
            requireRestart |= installBundles(slingStartupDir, bySymbolicName, installed);

            // start all the newly installed bundles (existing bundles are not started if they are stopped)
            startBundles(installed);
        }

        // due to the upgrade of a framework extension bundle, the framework
        // has to be restarted. For this reason, set the target start level
        // to a negative value.
        if (requireRestart) {
            logger.log(
                Logger.LOG_INFO,
                "Framework extension(s) have been updated, restarting framework after startup has completed");
        }

        return requireRestart;
    }

    //---------- Startup folder maintenance

    /**
     * Get the sling startup directory (or create it) in the sling home if possible
     * @param slingHome the path to the sling home
     * @return the sling startup directory
     * @throws IllegalStateException if the sling home or startup directories cannot be created/accessed
     */
    private File getSlingStartupDir(String slingHome) {
        final File slingHomeDir = new File(slingHome);
        final File slingHomeStartupDir = getOrCreateDirectory(slingHomeDir, DirectoryUtil.PATH_STARTUP);
        return slingHomeStartupDir;
    }

    /**
     * Get or create a sub-directory from an existing parent
     * @param parentDir the parent directory
     * @param subDirName the name of the sub-directory
     * @return the sub-directory
     * @throws IllegalStateException if directory cannot be created/accessed
     */
    private File getOrCreateDirectory(File parentDir, String subDirName) {
        File slingHomeStartupDir = new File(parentDir, subDirName).getAbsoluteFile();
        if ( slingHomeStartupDir.exists() ) {
            if (! slingHomeStartupDir.isDirectory()
                    || ! parentDir.canRead()
                    || ! parentDir.canWrite() ) {
                throw new IllegalStateException("Fatal error in bootstrap: Cannot find accessible existing "
                        +SharedConstants.SLING_HOME+DirectoryUtil.PATH_STARTUP+" directory: " + slingHomeStartupDir);
            }
        } else if (! slingHomeStartupDir.mkdirs() ) {
            throw new IllegalStateException("Sling Home " + slingHomeStartupDir + " cannot be created as a directory");
        }
        return slingHomeStartupDir;
    }

    /**
     * Copies the bundles from the given parent location in the jar/war
     * to the startup directory in the sling.home based on the startlevel
     * e.g. {sling.home}/startup/{startLevel}
     */
    private void copyBundles(File slingStartupDir, String parent, int startLevel) {

        // set default start level
        if (startLevel < 0) {
            startLevel = 0;
        }
        // this will be set and created on demand
        File startUpLevelDir = null;

        Iterator<String> res = resourceProvider.getChildren(parent);
        while (res.hasNext()) {
            // path to the next resource
            String path = res.next();

            if (DirectoryUtil.isBundle(path)) {
                // try to access the bundle file, ignore if not possible
                InputStream ins = resourceProvider.getResourceAsStream(path);
                if (ins == null) {
                    continue;
                }

                try {
                    // ensure we have a directory for the startlevel only when
                    // needed
                    if (startUpLevelDir == null) {
                        startUpLevelDir = getOrCreateDirectory(slingStartupDir,
                            String.valueOf(startLevel));
                    }

                    // copy over the bundle based on the startlevel
                    String bundleFileName = extractFileName(path);
                    File bundleFile = new File(startUpLevelDir, bundleFileName);
                    try {
                        copyStreamToFile(ins, bundleFile);
                    } catch (IOException e) {
                        // should this fail here or just log a warning?
                        throw new RuntimeException("Failure copying file from "
                            + path + " to startup dir (" + startUpLevelDir
                            + ") and name (" + bundleFileName + "): " + e, e);
                    }
                } finally {
                    try {
                        ins.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
    }

    /**
     * Copies a stream from the resource (jar/war) to a file
     * @param fromStream
     * @param toFile
     */
    static void copyStreamToFile(InputStream fromStream, File toFile) throws IOException {
        if (fromStream == null || toFile == null) {
            throw new IllegalArgumentException("fromStream and toFile must not be null");
        }
        if (! toFile.exists()) {
            toFile.createNewFile();
        }
        // overwrite
        OutputStream out = new FileOutputStream(toFile);
        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = fromStream.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            out.close();
        }
    }

    /**
     * Install the Bundles from files found in startup directory under the
     * level directories, this will only install bundles which are new or updated
     * and will skip over them otherwise
     *
     * @param context The <code>BundleContext</code> used to install the new Bundles.
     * @param currentBundles The currently installed Bundles indexed by their
     *            Bundle location.
     * @param parent The path to the location in which to look for bundle files to
     *            install. Only resources whose name ends with one of the known bundle extensions are
     *            considered for installation.
     * @param installed The list of Bundles installed by this method. Each
     *            Bundle successfully installed is added to this list.
     *
     * @return <code>true</code> if a system bundle fragment was updated which
     *      requires the framework to restart.
     */
    private boolean installBundles(final File slingStartupDir,
            final Map<String, Bundle> currentBundles,
            final List<Bundle> installed) {

        boolean requireRestart = false;
        File[] directories = slingStartupDir.listFiles(DirectoryUtil.DIRECTORY_FILTER);
        for (File levelDir : directories) {
            // get startlevel from dir name
            String dirName = levelDir.getName();
            int startLevel;
            try {
                startLevel = Integer.decode(dirName);
            } catch (NumberFormatException e) {
                startLevel = 0;
            }

            // iterate through all files in the startlevel dir
            File[] bundleFiles = levelDir.listFiles(DirectoryUtil.BUNDLE_FILE_FILTER);
            for (File bundleFile : bundleFiles) {
                requireRestart |= installBundle(bundleFile, startLevel,
                    currentBundles, installed);
            }
        }

        return requireRestart;
    }

    /**
     * @param bundleJar the jar file for the bundle to install
     * @param startLevel the start level to use for this bundle
     * @param context The <code>BundleContext</code> used to install the new Bundles.
     * @param currentBundles The currently installed Bundles indexed by their
     *            Bundle location.
     * @param installed The list of Bundles installed by this method. Each
     *            Bundle successfully installed is added to this list.
     * @param startLevelService the service which sets the start level
     *
     * @return <code>true</code> if a system bundle fragment was updated which
     *      requires the framework to restart.
     */
    private boolean installBundle(final File bundleJar,
            final int startLevel,
            final Map<String, Bundle> currentBundles,
            final List<Bundle> installed) {
        // get the manifest for the bundle information
        Manifest manifest = getManifest(bundleJar);
        if (manifest == null) {
            logger.log(Logger.LOG_ERROR, "Ignoring " + bundleJar
                + ": Cannot read manifest");
            return false; // SHORT CIRCUIT
        }

        // ensure a symbolic name in the jar file
        String symbolicName = getBundleSymbolicName(manifest);
        if (symbolicName == null) {
            logger.log(Logger.LOG_ERROR, "Ignoring " + bundleJar
                + ": Missing " + Constants.BUNDLE_SYMBOLICNAME
                + " in manifest");
            return false; // SHORT CIRCUIT
        }

        // check for an installed Bundle with the symbolic name
        Bundle installedBundle = currentBundles.get(symbolicName);
        if (ignore(installedBundle, manifest)) {
            logger.log(Logger.LOG_INFO, "Ignoring " + bundleJar
                + ": More recent version already installed");
            return false; // SHORT CIRCUIT
        }

        // try to access the JAR file, ignore if not possible
        InputStream ins;
        try {
            ins = new FileInputStream(bundleJar);
        } catch (FileNotFoundException e) {
            return false; // SHORT CIRCUIT
        }

        final boolean requireRestart;
        if (installedBundle != null) {

            // if the installed bundle is an extension fragment we have to
            // restart the framework after completing the installation
            // or upgrade of all new bundles
            requireRestart = isSystemBundleFragment(installedBundle);

            try {
                installedBundle.update(ins);
                logger.log(Logger.LOG_INFO, "Bundle "
                    + installedBundle.getSymbolicName()
                    + " updated from " + bundleJar);

                // optionally set the start level
                if (startLevel > 0) {
                    installedBundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
                }
            } catch (BundleException be) {
                logger.log(Logger.LOG_ERROR, "Bundle update from "
                    + bundleJar + " failed", be);
            }

        } else {

            // restart is not required for any bundle installation at this
            // stage
            requireRestart = false;

            // install the JAR file as a bundle
            String path = bundleJar.getPath();
            String location = SCHEME
                + path.substring(path.lastIndexOf('/') + 1);
            try {
                Bundle theBundle = bundleContext.installBundle(location, ins);
                logger.log(Logger.LOG_INFO, "Bundle "
                    + theBundle.getSymbolicName() + " installed from "
                    + location);

                // finally add the bundle to the list for later start
                installed.add(theBundle);

                // optionally set the start level
                if (startLevel > 0) {
                    theBundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
                }

            } catch (BundleException be) {
                logger.log(Logger.LOG_ERROR,
                    "Bundle installation from " + location + " failed", be);
            }
        }

        return requireRestart;
    }

    /**
     * Starts the Bundles in the <code>bundles</code> list. If the framework
     * provides an active <code>StartLevel</code> service, the start levels of
     * the Bundles is first set to <em>1</em>.
     */
    private void startBundles(final List<Bundle> bundles) {

        // start all bundles
        for (final Bundle bundle : bundles) {
            try {
                if (!isFragment(bundle)) {
                    bundle.start();
                }
            } catch (final BundleException be) {
                logger.log(Logger.LOG_ERROR, "Bundle "
                    + bundle.getSymbolicName() + " could not be started", be);
            }
        }

    }

    private int getStartLevel(final String path) {
        final String name = path.substring(path.lastIndexOf('/') + 1);
        try {
            int level = Integer.parseInt(name);
            if (level >= 0) {
                return level;
            }

            logger.log(Logger.LOG_ERROR, "Illegal Runlevel for " + path
                + ", ignoring");
        } catch (final NumberFormatException nfe) {
            logger.log(Logger.LOG_INFO, "Folder " + path
                + " does not denote start level, ignoring");
        }

        // no valid start level, ignore this location
        return STARTLEVEL_NONE;
    }

    private boolean isSystemBundleFragment(final Bundle installedBundle) {
        final String fragmentHeader = installedBundle.getHeaders().get(
            Constants.FRAGMENT_HOST);
        return fragmentHeader != null
            && fragmentHeader.indexOf(Constants.EXTENSION_DIRECTIVE) > 0;
    }

    // ---------- Bundle JAR file information

    /**
     * Returns the Manifest from the JAR file in the given resource provided by
     * the resource provider or <code>null</code> if the resource does not
     * exists or is not a JAR file or has no Manifest.
     *
     * @param jarPath The path to the JAR file provided by the resource provider
     *            of this instance.
     */
    private Manifest getManifest(final File jar) {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jar, false);
            return jarFile.getManifest();
        } catch (IOException e) {
            logger.log(Logger.LOG_WARNING,
                "Could not get inputstream from file (" + jar + "):" + e);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
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
    static String getBundleSymbolicName(final Manifest manifest) {
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
    private boolean ignore(final Bundle installedBundle, final Manifest manifest) {

        // the bundle is not installed yet, so we have to install it
        if (installedBundle == null) {
            return false;
        }

        String versionProp = manifest.getMainAttributes().getValue(
            Constants.BUNDLE_VERSION);
        Version newVersion = Version.parseVersion(versionProp);

        String installedVersionProp = installedBundle.getHeaders().get(
            Constants.BUNDLE_VERSION);
        Version installedVersion = Version.parseVersion(installedVersionProp);

        // if the new version and the current version are the same, reinstall if
        // the version is a snapshot
        if (newVersion.equals(installedVersion)
            && installedVersionProp.endsWith("SNAPSHOT")
            && isNewerSnapshot(installedBundle, manifest)) {
            logger.log(Logger.LOG_INFO, "Forcing upgrade of SNAPSHOT bundle: "
                + installedBundle.getSymbolicName());
            return false;
        }

        return newVersion.compareTo(installedVersion) <= 0;
    }

    /**
     * Returns <code>true</code> if the bundle must be assumed to be a fragment
     * according to its <code>Fragment-Host</code> header.
     */
    private static boolean isFragment(final Bundle bundle) {
        Dictionary<?, ?> headerMap = bundle.getHeaders();
        return headerMap.get(Constants.FRAGMENT_HOST) != null;
    }

    /**
     * Determine if the bundle containing the passed manfiest is a newer
     * SNAPSHOT than the already-installed bundle.
     *
     * @param installedBundle the already-installed bundle
     * @param manifest the manifest of the to-be-installed bundle
     * @return true if the to-be-installed bundle is newer or if the comparison
     *         fails for some reason
     */
    private boolean isNewerSnapshot(final Bundle installedBundle, final Manifest manifest) {
        String installedDate = installedBundle.getHeaders().get(
            BND_LAST_MODIFIED_HEADER);
        String toBeInstalledDate = manifest.getMainAttributes().getValue(
            BND_LAST_MODIFIED_HEADER);
        if (installedDate == null) {
            logger.log(Logger.LOG_DEBUG, String.format(
                "Currently installed bundle %s doesn't have a %s header",
                installedBundle.getSymbolicName(), BND_LAST_MODIFIED_HEADER));
            return true;
        }
        if (toBeInstalledDate == null) {
            logger.log(Logger.LOG_DEBUG, String.format(
                "Candidate bundle %s doesn't have a %s header",
                installedBundle.getSymbolicName(), BND_LAST_MODIFIED_HEADER));
            return true;
        }

        long installedTime, toBeInstalledTime = 0;
        try {
            installedTime = Long.valueOf(installedDate);
        } catch (NumberFormatException e) {
            logger.log(Logger.LOG_DEBUG, String.format(
                "%s header of currently installed bundle %s isn't parseable.",
                BND_LAST_MODIFIED_HEADER, installedBundle.getSymbolicName()));
            return true;
        }
        try {
            toBeInstalledTime = Long.valueOf(toBeInstalledDate);
        } catch (NumberFormatException e) {
            logger.log(Logger.LOG_DEBUG, String.format(
                "%s header of candidate bundle %s isn't parseable.",
                BND_LAST_MODIFIED_HEADER, installedBundle.getSymbolicName()));
            return true;
        }

        return toBeInstalledTime > installedTime;

    }

    //---------- helper
    /**
     * Simple check to see if a string is blank since
     * StringUtils is not available here, maybe fix this later
     * @param str the string to check
     * @return true if the string is null or empty OR false otherwise
     */
    static boolean isBlank(final String str) {
        return str == null || str.length() == 0 || str.trim().length() == 0;
    }

    /**
     * @param path any path (cannot be blank)
     * @return the filename from the end of the path
     * @throws IllegalArgumentException if there is no filename available
     */
    static String extractFileName(String path) {
        if (isBlank(path)) {
            throw new IllegalArgumentException("Invalid blank path specified, cannot extract filename: " + path);
        }

        // ensure forward slashes in the path
        path = path.replace(File.separatorChar, '/');

        String name = "";
        int slashPos = path.lastIndexOf('/');
        if (slashPos == -1) {
            // this is only a filename (no directory path included)
            name = path;
        } else if (path.length() > slashPos+1) {
            // split off the ending of the path
            name = path.substring(slashPos+1);
        }
        if (isBlank(name)) {
            throw new IllegalArgumentException("Invalid path, no filename found: " + path);
        }
        return name;
    }

}
