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
package org.apache.sling.launchpad.base.shared;

import java.beans.Introspector;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.jar.JarFile;

import org.apache.sling.commons.osgi.bundleversion.BundleVersionInfo;
import org.apache.sling.commons.osgi.bundleversion.FileBundleVersionInfo;

/**
 * The <code>Loader</code> class provides utility methods for the actual
 * launchers to help launching the framework.
 */
public class Loader {

    /**
     * The launchpad home folder set by the constructor
     */
    private final File launchpadHome;

    private final File extLibHome;

    /**
     * Default External Library Home
     */
    private static final String EXTENSION_LIB_PATH="ext";

    /**
     * Creates a loader instance to load from the given launchpad home folder.
     * Besides ensuring the existence of the launchpad home folder, the
     * constructor also removes all but the most recent launcher JAR files from
     * the Sling home folder (thus cleaning up from previous upgrades).
     *
     * @param launchpadHome The launchpad home folder. This must not be
     *            <code>null</code> or an empty string.
     * @throws IllegalArgumentException If the <code>launchpadHome</code>
     *             argument is <code>null</code> or an empty string or if the
     *             launchpad home folder exists but is not a directory or if the
     *             Sling home folder cannot be created.
     */
    public Loader(final File launchpadHome) {
        if (launchpadHome == null) {
            throw new IllegalArgumentException(
                "Launchpad Home folder must not be null or empty");
        }

        this.launchpadHome = getLaunchpadHomeFile(launchpadHome);
        extLibHome = getExtensionLibHome();
        removeOldLauncherJars();
    }

    /**
     * Creates an URLClassLoader from a _launcher JAR_ file in the given
     * launchpadHome directory and loads and returns the launcher class
     * identified by the launcherClassName.
     *
     * @param launcherClassName The fully qualified name of a class implementing
     *            the Launcher interface. This class must have a public
     *            constructor taking no arguments.
     * @return the Launcher instance loaded from the newly created classloader
     * @throws NullPointerException if launcherClassName is null
     * @throws IllegalArgumentException if the launcherClassName cannot be
     *             instantiated. The cause of the failure is contained as the
     *             cause of the exception.
     */
    public Object loadLauncher(String launcherClassName) {

        final File launcherJarFile = getLauncherJarFile();
        info("Loading launcher class " + launcherClassName + " from " + launcherJarFile.getName());
        if (!launcherJarFile.canRead()) {
            throw new IllegalArgumentException("Sling Launcher JAR "
                + launcherJarFile + " is not accessible");
        }

        final ClassLoader loader;
        try {
            loader = new LauncherClassLoader(launcherJarFile, getExtLibs());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                "Cannot create an URL from the  JAR path name", e);
        }

        try {
            final Class<?> launcherClass = loader.loadClass(launcherClassName);
            return launcherClass.newInstance();
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalArgumentException("Cannot find class "
                + launcherClassName + " in " + launcherJarFile, cnfe);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(
                "Cannot instantiate launcher class " + launcherClassName, e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(
                "Cannot access constructor of class " + launcherClassName, e);
        }
    }

    /**
     * Tries to remove as many traces of class loaded by the framework from the
     * Java VM as possible. Most notably the following traces are removed:
     * <ul>
     * <li>JavaBeans property caches
     * <li>Close the Launcher Jar File (if opened by the platform)
     * </ul>
     * <p>
     * This method must be called when the notifier is called.
     */
    public void cleanupVM() {

        // ensure the JavaBeans introspector lets go of any classes it
        // may haved cached after introspection
        Introspector.flushCaches();

        // if sling home is set, check whether we have to close the
        // launcher JAR JarFile, which might be cached in the platform
        closeLauncherJarFile(getLauncherJarFile());
    }

    /**
     * Copies the contents of the launcher JAR as indicated by the URL to the
     * sling home directory. If the existing file is is a more recent bundle
     * version than the supplied launcher JAR file, it is is not replaced.
     *
     * @return <code>true</code> if the launcher JAR file has been installed or
     *         updated, <code>false</code> otherwise.
     * @throws IOException If an error occurrs transferring the contents
     */
    public boolean installLauncherJar(URL launcherJar) throws IOException {
        info("Checking launcher JAR in folder " + launchpadHome);
        final File currentLauncherJarFile = getLauncherJarFile();

        // Copy the new launcher jar to a temporary file, and
        // extract bundle version info
        final URLConnection launcherJarConn = launcherJar.openConnection();
        launcherJarConn.setUseCaches(false);
        final File tmp = new File(launchpadHome, "Loader_tmp_" + System.currentTimeMillis() + SharedConstants.LAUNCHER_JAR_REL_PATH);
        spool(launcherJarConn.getInputStream(), tmp);
        final FileBundleVersionInfo newVi = new FileBundleVersionInfo(tmp);
        boolean installNewLauncher = true;

        try {
            if(!newVi.isBundle()) {
                throw new IOException("New launcher jar is not a bundle, cannot get version info:" + launcherJar);
            }

            // Compare versions to decide whether to use the existing or new launcher jar
            if (currentLauncherJarFile.exists()) {
                final FileBundleVersionInfo currentVi = new FileBundleVersionInfo(currentLauncherJarFile);
                if(!currentVi.isBundle()) {
                    throw new IOException("Existing launcher jar is not a bundle, cannot get version info:"
                            + currentLauncherJarFile.getAbsolutePath());
                }

                String info = null;
                if(currentVi.compareTo(newVi) == 0) {
                    info = "up to date";
                    installNewLauncher = false;
                } else if(currentVi.compareTo(newVi) > 0) {
                    info = "more recent than ours";
                    installNewLauncher = false;
                }

                if(info != null) {
                    info("Existing launcher is " + info + ", using it: "
                            + getBundleInfo(currentVi) + " (" + currentLauncherJarFile.getName() + ")");
                }
            }

            if(installNewLauncher) {
                final File f = new File(tmp.getParentFile(), SharedConstants.LAUNCHER_JAR_REL_PATH + "." + System.currentTimeMillis());
                if(!tmp.renameTo(f)) {
                    throw new IOException("Failed to rename " + tmp.getName() + " to " + f.getName());
                }
                info("Installing new launcher: " + launcherJar  + ", " + getBundleInfo(newVi) + " (" + f.getName() + ")");
            }
        } finally {
            if(tmp.exists()) {
                tmp.delete();
            }
        }

        return installNewLauncher;
    }

    /** Return relevant bundle version info for logging */
    static String getBundleInfo(BundleVersionInfo<?> v) {
        final StringBuilder sb = new StringBuilder();
        sb.append(v.getVersion());
        if(v.isSnapshot()) {
            sb.append(", Last-Modified:");
            sb.append(new Date(v.getBundleLastModified()));
        }
        return sb.toString();
    }

    /**
     * Removes old candidate launcher JAR files leaving the most recent one as
     * the launcher JAR file to use on next Sling startup.
     */
    private void removeOldLauncherJars() {
        final File[] launcherJars = getLauncherJarFiles();
        if (launcherJars != null && launcherJars.length > 0) {

            // Remove all files except current one
            final File current = getLauncherJarFile();
            for(File f : launcherJars) {
                if(f.getAbsolutePath().equals(current.getAbsolutePath())) {
                    continue;
                }
                String versionInfo = null;
                try {
                    FileBundleVersionInfo vi = new FileBundleVersionInfo(f);
                    versionInfo = getBundleInfo(vi);
                } catch(IOException ignored) {
                }
                info("Deleting obsolete launcher jar: " + f.getName() + ", " + versionInfo);
                f.delete();
            }

            // And ensure the current file has the standard launcher name
            if (!SharedConstants.LAUNCHER_JAR_REL_PATH.equals(current.getName())) {
                info("Renaming current launcher jar " + current.getName()
                        + " to " + SharedConstants.LAUNCHER_JAR_REL_PATH);
                File launcherFileName = new File(
                        current.getParentFile(),
                    SharedConstants.LAUNCHER_JAR_REL_PATH);
                current.renameTo(launcherFileName);
            }
        }
    }

    /**
     * Spools the contents of the input stream to the given file replacing the
     * contents of the file with the contents of the input stream. When this
     * method returns, the input stream is guaranteed to be closed.
     *
     * @throws IOException If an error occurrs reading or writing the input
     *             stream contents.
     */
    public static void spool(InputStream ins, File destFile) throws IOException {
        OutputStream out = null;
        try {
            out = new FileOutputStream(destFile);
            byte[] buf = new byte[8192];
            int rd;
            while ((rd = ins.read(buf)) >= 0) {
                out.write(buf, 0, rd);
            }
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    // ---------- internal helper

    /**
     * Returns a <code>File</code> object representing the Launcher JAR file
     * found in the sling home folder.
     */
    private File getLauncherJarFile() {
        File result = null;
        final File[] launcherJars = getLauncherJarFiles();
        if (launcherJars == null || launcherJars.length == 0) {

            // return a non-existing file naming the desired primary name
            result = new File(launchpadHome,
                SharedConstants.LAUNCHER_JAR_REL_PATH);

        } else {
            // last file is the most recent one, use it
            result = launcherJars[launcherJars.length - 1];
        }

        return result;
    }

    /**
     * Returns all files in the <code>launchpadHome</code> directory which may
     * be considered as launcher JAR files, sorted based on their bundle version
     * information, most recent last. These files all start with the
     * {@link SharedConstants#LAUNCHER_JAR_REL_PATH}. This list may be empty if
     * the launcher JAR file has not been installed yet.
     *
     * @return The list of candidate launcher JAR files, which may be empty.
     *         <code>null</code> is returned if an IO error occurs trying to
     *         list the files.
     */
    private File[] getLauncherJarFiles() {
        // Get list of files with names starting with our prefix
        final File[] rawList = launchpadHome.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile()
                    && pathname.getName().startsWith(
                        SharedConstants.LAUNCHER_JAR_REL_PATH);
            }
        });

        // Keep only those which have valid Bundle headers, and
        // sort them according to the bundle version numbers
        final List<FileBundleVersionInfo> list = new ArrayList<FileBundleVersionInfo>();
        for(File f : rawList) {
            FileBundleVersionInfo fvi = null;
            try {
                fvi = new FileBundleVersionInfo(f);
            } catch(IOException ioe) {
                // Cannot read bundle info from jar file - should never happen??
                throw new IllegalStateException("Cannot read bundle information from loader file " + f.getAbsolutePath());
            }
            if(fvi.isBundle()) {
                list.add(fvi);
            }
        }
        Collections.sort(list);
        final File [] result = new File[list.size()];
        int i = 0;
        for(FileBundleVersionInfo fvi : list) {
            result[i++] = fvi.getSource();
        }
        return result;
    }

    /**
     * Returns the <code>launchpadHome</code> path as a directory. If the
     * directory does not exist it is created. If creation fails or if
     * <code>launchpadHome</code> exists but is not a directory a
     * <code>IllegalArgumentException</code> is thrown.
     *
     * @param launchpadHome The sling home directory where the launcher JAR
     *            files are stored
     * @return The Sling home directory
     * @throws IllegalArgumentException if <code>launchpadHome</code> exists and
     *             is not a directory or cannot be created as a directory.
     */
    private static File getLaunchpadHomeFile(File launchpadHome) {
        if (launchpadHome.exists()) {
            if (!launchpadHome.isDirectory()) {
                throw new IllegalArgumentException("Sling Home " + launchpadHome
                    + " exists but is not a directory");
            }
        } else if (!launchpadHome.mkdirs()) {
            throw new IllegalArgumentException("Sling Home " + launchpadHome
                + " cannot be created as a directory");
        }

        return launchpadHome;
    }

    private static void closeLauncherJarFile(final File launcherJar) {
        try {
            final URI launcherJarUri = launcherJar.toURI();
            final URL launcherJarRoot = new URL("jar:" + launcherJarUri + "!/");
            final URLConnection conn = launcherJarRoot.openConnection();
            if (conn instanceof JarURLConnection) {
                final JarFile jarFile = ((JarURLConnection) conn).getJarFile();
                jarFile.close();
            }
        } catch (Exception e) {
            // better logging here
        }
    }

    /** Meant to be overridden to display or log info */
    protected void info(String msg) {
    }

    private File getExtensionLibHome(){
        //check if sling home is initialized
        if(launchpadHome == null || !launchpadHome.exists()){
            throw new IllegalArgumentException("Sling Home  has not been initialized" );
        }
        //assumes launchpadHome is initialized
        File extLibFile=new File(launchpadHome, EXTENSION_LIB_PATH);
        if (extLibFile.exists()) {
            if (!extLibFile.isDirectory()) {
                throw new IllegalArgumentException("Sling  Extension Lib Home " + extLibFile
                        + " exists but is not a directory");
            }
        }

        info("Sling  Extension Lib Home : " + extLibFile);
        return extLibFile;
    }

    private File[] getExtLibs(){
        if (extLibHome == null || !extLibHome.exists()) {
            info("External Libs Home (ext) is null or does not exists.");
            return new File[]{};
        }
        File[] libs = extLibHome.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith(".jar"));
            }
        });

        if (libs == null) {
            libs = new File[]{};
        }
        StringBuilder logStringBldr = new StringBuilder("Sling Extension jars found = [ ");

        for(File lib:libs){
            logStringBldr.append(lib);
            logStringBldr.append(",");
        }

        logStringBldr.append(" ] ");
        info(logStringBldr.toString());
        return libs;
    }
}
