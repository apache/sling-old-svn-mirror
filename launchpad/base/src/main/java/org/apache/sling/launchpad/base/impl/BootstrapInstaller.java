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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.apache.sling.launchpad.base.shared.SharedConstants;
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
     * The path of startup bundles in the sling home
     */
    static final String PATH_STARTUP = "startup/";

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

    //---------- BundleActivator interface

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
    public void start(BundleContext context) throws Exception {
        // get the startup location in sling home
        String slingHome = context.getProperty(SharedConstants.SLING_HOME);
        File slingStartupDir = getSlingStartupDir(slingHome);

        if (!isAlreadyInstalled(context, slingStartupDir)) {
            // only run the deployment package stuff and war/jar copies when this war/jar is new/changed

            // register deployment package support
            try {
                final DeploymentPackageInstaller dpi = new DeploymentPackageInstaller(
                    context, logger, resourceProvider);
                context.addFrameworkListener(dpi);
                context.addServiceListener(dpi, "(" + Constants.OBJECTCLASS
                    + "=" + DeploymentPackageInstaller.DEPLOYMENT_ADMIN + ")");
            } catch (Throwable t) {
                logger.log(
                    Logger.LOG_WARNING,
                    "Cannot register Deployment Admin support, continuing without",
                    t);
            }

            // see if the loading of bundles from the package is disabled
            String dpblString = context.getProperty(SharedConstants.DISABLE_PACKAGE_BUNDLE_LOADING);
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
            Bundle[] bundles = context.getBundles();
            Map<String, Bundle> bySymbolicName = new HashMap<String, Bundle>();
            for (int i = 0; i < bundles.length; i++) {
                bySymbolicName.put(bundles[i].getSymbolicName(), bundles[i]);
            }

            // holds the bundles we install during this processing
            List<Bundle> installed = new LinkedList<Bundle>();

            // get all bundles from the startup location and install them
            installBundles(slingStartupDir, context, bySymbolicName, installed);

            // start all the newly installed bundles (existing bundles are not started if they are stopped)
            startBundles(installed);

            // mark everything installed
            markInstalled(context, slingStartupDir);
        }
    }

    /** Nothing to be done on stop */
    public void stop(BundleContext context) {
    }

    //---------- Startup folder maintenance

    /**
     * Get the sling startup directory (or create it) in the sling home if possible
     * @param slingHome the path to the sling home
     * @return the sling startup directory
     * @throws IllegalStateException if the sling home or startup directories cannot be created/accessed
     */
    private File getSlingStartupDir(String slingHome) {
        if (isBlank(slingHome)) {
            throw new IllegalStateException("Fatal error in bootstrap: Cannot get the "+SharedConstants.SLING_HOME+" value: " + slingHome);
        }
        File slingHomeDir = new File(slingHome).getAbsoluteFile();
        if (! slingHomeDir.exists()
                || ! slingHomeDir.canRead()
                || ! slingHomeDir.canWrite()
                || ! slingHomeDir.isDirectory()) {
            throw new IllegalStateException("Fatal error in bootstrap: Cannot find accessible existing "
                    +SharedConstants.SLING_HOME+" directory: " + slingHomeDir);
        }
        File slingHomeStartupDir = getOrCreateDirectory(slingHomeDir, PATH_STARTUP);
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
                        +SharedConstants.SLING_HOME+PATH_STARTUP+" directory: " + slingHomeStartupDir);
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
            // we only deal with jars
            if (path.endsWith(".jar")) {
                // try to access the JAR file, ignore if not possible
                InputStream ins = resourceProvider.getResourceAsStream(path);
                if (ins == null) {
                    continue;
                }

                // ensure we have a directory for the startlevel only when
                // needed
                if (startUpLevelDir == null) {
                    startUpLevelDir = getOrCreateDirectory(slingStartupDir,
                        String.valueOf(startLevel));
                }

                // copy over the bundle based on the startlevel
                String bundleFileName = extractFileName(path);
                File bundleJar = new File(startUpLevelDir, bundleFileName);
                try {
                    copyStreamToFile(ins, bundleJar);
                } catch (IOException e) {
                    // should this fail here or just log a warning?
                    throw new RuntimeException("Failure copying file from "
                        + path + " to startup dir (" + startUpLevelDir
                        + ") and name (" + bundleFileName + "): " + e, e);
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
     * Install the Bundles from JAR files found in startup directory under the
     * level directories, this will only install bundles which are new or updated
     * and will skip over them otherwise
     *
     * @param context The <code>BundleContext</code> used to install the new Bundles.
     * @param currentBundles The currently installed Bundles indexed by their
     *            Bundle location.
     * @param parent The path to the location in which to look for JAR files to
     *            install. Only resources whose name ends with <em>.jar</em> are
     *            considered for installation.
     * @param installed The list of Bundles installed by this method. Each
     *            Bundle successfully installed is added to this list.
     */
    private void installBundles(File slingStartupDir,
            BundleContext context, Map<String, Bundle> currentBundles,
            List<Bundle> installed) {

        // get the start level service (if possible) so we can set the initial start level
        ServiceReference ref = context.getServiceReference(StartLevel.class.getName());
        StartLevel startLevelService = (ref != null)
                ? (StartLevel) context.getService(ref)
                : null;

        try {
            File[] directories = slingStartupDir.listFiles(DIRECTORY_FILTER);
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
                File[] jarFiles = levelDir.listFiles(JAR_FILE_FILTER);
                for (File bundleJar : jarFiles) {
                    installBundle(bundleJar, startLevel, context, currentBundles, installed, startLevelService);
                }
            }

        } finally {
            // release the start level service
            if (ref != null) {
                context.ungetService(ref);
            }
        }
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
     */
    private void installBundle(File bundleJar, int startLevel,
            BundleContext context, Map<String, Bundle> currentBundles,
            List<Bundle> installed, StartLevel startLevelService) {
        // get the manifest for the bundle information
        Manifest manifest = getManifest(bundleJar);
        if (manifest == null) {
            logger.log(Logger.LOG_ERROR, "Ignoring " + bundleJar
                + ": Cannot read manifest");
            return; // SHORT CIRCUIT
        }

        // ensure a symbolic name in the jar file
        String symbolicName = getBundleSymbolicName(manifest);
        if (symbolicName == null) {
            logger.log(Logger.LOG_ERROR, "Ignoring " + bundleJar
                + ": Missing " + Constants.BUNDLE_SYMBOLICNAME
                + " in manifest");
            return; // SHORT CIRCUIT
        }

        // check for an installed Bundle with the symbolic name
        Bundle installedBundle = currentBundles.get(symbolicName);
        if (ignore(installedBundle, manifest)) {
            logger.log(Logger.LOG_INFO, "Ignoring " + bundleJar
                + ": More recent version already installed");
            return; // SHORT CIRCUIT
        }

        // try to access the JAR file, ignore if not possible
        InputStream ins;
        try {
            ins = new FileInputStream(bundleJar);
        } catch (FileNotFoundException e) {
            return; // SHORT CIRCUIT
        }

        if (installedBundle != null) {
            try {
                installedBundle.update(ins);
                logger.log(Logger.LOG_INFO, "Bundle "
                    + installedBundle.getSymbolicName()
                    + " updated from " + bundleJar);
            } catch (BundleException be) {
                logger.log(Logger.LOG_ERROR, "Bundle update from "
                    + bundleJar + " failed", be);
            }

        } else {
            // install the JAR file as a bundle
            String path = bundleJar.getPath();
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
                    "Bundle installation from " + location + " failed", be);
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
     * Returns the Manifest from the JAR file in the given resource provided by
     * the resource provider or <code>null</code> if the resource does not
     * exists or is not a JAR file or has no Manifest.
     *
     * @param jarPath The path to the JAR file provided by the resource provider
     *            of this instance.
     */
    private Manifest getManifest(File jar) {
        try {
            InputStream ins = new FileInputStream(jar);
            return getManifest(ins);
        } catch (FileNotFoundException e) {
            logger.log(Logger.LOG_WARNING, "Could not get inputstream from file ("+jar+"):"+e);
            //throw new IllegalArgumentException("Could not get inputstream from file ("+jar+"):"+e, e);
        }
        return null;
    }

    /**
     * Return the manifest from a jar if it is possible to get it,
     * this will also handle closing out the stream
     *
     * @param ins the inputstream for the jar
     * @return the manifest OR null if it cannot be obtained
     */
    Manifest getManifest(InputStream ins) {
        try {
            JarInputStream jis = new JarInputStream(ins);
            return jis.getManifest();
        } catch (IOException ioe) {
            logger.log(Logger.LOG_ERROR, "Failed to read manifest from stream: "
                    + ins, ioe);
        } finally {
            try {
                ins.close();
            } catch (IOException ignore) {
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
    String getBundleSymbolicName(Manifest manifest) {
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

        // if the new version and the current version are the same, reinstall if
        // the version is a snapshot
        if (newVersion.equals(installedVersion)
            && installedVersionProp.endsWith("SNAPSHOT")) {
            logger.log(Logger.LOG_INFO, "Forcing upgrade of SNAPSHOT bundle: "
                + installedBundle.getSymbolicName());
            return false;
        }

        return newVersion.compareTo(installedVersion) <= 0;
    }

    // ---------- Bundle Installation marker file

    private boolean isAlreadyInstalled(BundleContext context,
            File slingStartupDir) {
        final File dataFile = context.getDataFile(DATA_FILE);
        if (dataFile != null && dataFile.exists()) {

            FileInputStream fis = null;
            try {

                long selfStamp = getSelfTimestamp(slingStartupDir);
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

    private void markInstalled(BundleContext context, File slingStartupDir) {
        final File dataFile = context.getDataFile(DATA_FILE);
        try {
            final FileOutputStream fos = new FileOutputStream(dataFile);
            try {
                fos.write(String.valueOf(getSelfTimestamp(slingStartupDir)).getBytes());
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
    private long getSelfTimestamp(File slingStartupDir) throws IOException {

        // the timestamp of the launcher jar
        long selfStamp = -1;
        ClassLoader loader = getClass().getClassLoader();
        if (loader instanceof URLClassLoader) {
            URLClassLoader urlLoader = (URLClassLoader) loader;
            URL[] urls = urlLoader.getURLs();
            if (urls.length > 0) {
                selfStamp = urls[0].openConnection().getLastModified();
            }
        }

        // check whether any bundle is younger than the launcher jar
        File[] directories = slingStartupDir.listFiles(DIRECTORY_FILTER);
        for (File levelDir : directories) {

            // iterate through all files in the startlevel dir
            File[] jarFiles = levelDir.listFiles(JAR_FILE_FILTER);
            for (File bundleJar : jarFiles) {
                if (bundleJar.lastModified() > selfStamp) {
                    selfStamp = bundleJar.lastModified();
                }
            }
        }

        // return the final stamp (may be -1 if launcher jar cannot be checked
        // and there are no bundle jar files)
        return selfStamp;
    }

    //---------- FileFilter implementations to scan startup folders

    /**
     * Simple directory filter
     */
    private static final FileFilter DIRECTORY_FILTER = new FileFilter() {
        public boolean accept(File f) {
            return f.isDirectory();
        }
    };

    /**
     * Simple jar file filter
     */
    private static final FileFilter JAR_FILE_FILTER = new FileFilter() {
        public boolean accept(File f) {
            return f.isFile() && f.getName().endsWith(".jar");
        }
    };

    //---------- helper

    /**
     * Simple check to see if a string is blank since
     * StringUtils is not available here, maybe fix this later
     * @param str the string to check
     * @return true if the string is null or empty OR false otherwise
     */
    static boolean isBlank(String str) {
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
