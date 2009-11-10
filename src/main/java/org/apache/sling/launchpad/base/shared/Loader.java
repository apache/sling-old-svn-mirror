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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarFile;

/**
 * The <code>Loader</code> class provides utility methods for the actual
 * launchers to help launching the framework.
 */
public class Loader {

    /**
     * The Sling home folder set by the constructor
     */
    private final File slingHome;

    /**
     * The current launcher JAR file in use. Set on-demand by the
     * {@link #getLauncherJarFile()} method and reset to the installed or
     * updated launcher JAR file by the {@link #installLauncherJar(URL)} method.
     */
    private File launcherJarFile;

    /**
     * Creates a loader instance to load from the given Sling home folder.
     * Besides ensuring the existence of the Sling home folder, the constructor
     * also removes all but the most recent launcher JAR files from the Sling
     * home folder (thus cleaning up from previous upgrades).
     *
     * @param slingHome The Sling home folder. If this is <code>null</code> the
     *            default value {@link SharedConstants#SLING_HOME_DEFAULT} is
     *            assumed.
     * @throws IllegalArgumentException If the Sling home folder exists but is
     *             not a directory or if the Sling home folder cannot be
     *             created.
     */
    public Loader(final String slingHome) throws IllegalArgumentException {
        this.slingHome = getSlingHomeFile(slingHome);
        removeOldLauncherJars();
    }

    /**
     * Creates an URLClassLoader from a _launcher JAR_ file in the given
     * slingHome directory and loads and returns the launcher class identified
     * by the launcherClassName.
     *
     * @param launcherClassName The fully qualified name of a class implementing
     *            the Launcher interface. This class must have a public
     *            constructor taking no arguments.
     * @param slingHome The value to be used as ${slingHome}. This may be null
     *            in which case the sling folder in the current working
     *            directory is assumed. If this name is empty, the current
     *            working directory is assumed to be used as ${slingHome}.
     * @return the Launcher instance loaded from the newly created classloader
     * @throws NullPointerException if launcherClassName is null
     * @throws IllegalArgumentException if the launcherClassName cannot be
     *             instantiated. The cause of the failure is contained as the
     *             cause of the exception.
     */
    public Object loadLauncher(String launcherClassName) {

        final File launcherJarFile = getLauncherJarFile();
        if (!launcherJarFile.canRead()) {
            throw new IllegalArgumentException("Sling Launcher JAR "
                + launcherJarFile + " is not accessible");
        }

        final ClassLoader loader;
        try {
            loader = new LauncherClassLoader(launcherJarFile);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                "Cannot create an URL from the Sling Launcher JAR path name", e);
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
     *
     * @param slingHome The home directory of Sling. This is used to ensure the
     *            launcher jar file is not open anymore (as much as possible).
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
     * sling home directory and sets the last modification time stamp fo the
     * file. If the existing file is not older than the contents of the launcher
     * JAR file, the file is not replaced.
     *
     * @return <code>true</code> if the launcher JAR file has been installed or
     *         updated. If the launcher JAR is already up to date,
     *         <code>false</code> is returned.
     * @throws IOException If an error occurrs transferring the contents
     */
    public boolean installLauncherJar(URL launcherJar) throws IOException {
        final File currentLauncherJarFile = getLauncherJarFile();

        // check whether we have to overwrite
        final URLConnection launcherJarConn = launcherJar.openConnection();
        launcherJarConn.setUseCaches(false);
        final long lastModifTime = launcherJarConn.getLastModified();
        final File newLauncherJarFile;
        if (currentLauncherJarFile.exists()) {

            // nothing to do if there is no update
            if (lastModifTime <= currentLauncherJarFile.lastModified()) {
                return false;
            }

            // use a new timestamped name for the new version
            newLauncherJarFile = new File(slingHome,
                SharedConstants.LAUNCHER_JAR_REL_PATH + "." + lastModifTime);

        } else {

            // create the current file
            newLauncherJarFile = currentLauncherJarFile;

        }

        // store the new launcher JAR and set the last modification time
        spool(launcherJarConn.getInputStream(), newLauncherJarFile);
        newLauncherJarFile.setLastModified(lastModifTime);
        launcherJarFile = newLauncherJarFile;

        return true;
    }

    /**
     * Removes old candidate launcher JAR files leaving the most recent one as
     * the launcher JAR file to use on next Sling startup.
     *
     * @param slingHome The Sling home directory location containing the
     *            candidate launcher JAR files.
     */
    private void removeOldLauncherJars() {
        final File[] launcherJars = getLauncherJarFiles();
        if (launcherJars != null && launcherJars.length > 0) {

            // start with the first entry being the newest
            File mostRecentJarFile = launcherJars[0];
            long mostRecentLastModification = mostRecentJarFile.lastModified();
            for (int i = 1; i < launcherJars.length; i++) {

                if (mostRecentLastModification < launcherJars[i].lastModified()) {
                    // if this entry is newer than the fromer newest, remove
                    // the former file and use this entry as the newest
                    mostRecentJarFile.delete();
                    mostRecentJarFile = launcherJars[i];
                    mostRecentLastModification = mostRecentJarFile.lastModified();

                } else {
                    // otherwise remove this entry and keep on using the current
                    launcherJars[i].delete();
                }

            }
            // fact: mostRecentJarFile is the only remaining and the most recent

            // ensure the most recent file has the common name
            if (!SharedConstants.LAUNCHER_JAR_REL_PATH.equals(mostRecentJarFile.getName())) {
                File launcherFileName = new File(
                    mostRecentJarFile.getParentFile(),
                    SharedConstants.LAUNCHER_JAR_REL_PATH);
                mostRecentJarFile.renameTo(launcherFileName);
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
        if (launcherJarFile == null) {
            final File[] launcherJars = getLauncherJarFiles();
            if (launcherJars == null || launcherJars.length == 0) {

                // return a non-existing file naming the desired primary name
                launcherJarFile = new File(slingHome,
                    SharedConstants.LAUNCHER_JAR_REL_PATH);

            } else if (launcherJars.length == 1) {

                // only a single file existing, that's it
                launcherJarFile = launcherJars[0];

            } else {

                // start with the first entry being the newest
                File mostRecentJarFile = launcherJars[0];
                long mostRecentLastModification = mostRecentJarFile.lastModified();
                for (int i = 1; i < launcherJars.length; i++) {

                    // if this entry is newer than the fromer newest, use this
                    // entry
                    // as the newest
                    if (mostRecentLastModification < launcherJars[i].lastModified()) {
                        mostRecentJarFile = launcherJars[i];
                        mostRecentLastModification = mostRecentJarFile.lastModified();
                    }

                }
                launcherJarFile = mostRecentJarFile;
            }
        }

        return launcherJarFile;
    }

    /**
     * Returns all files in the <code>slingHome</code> directory which may be
     * considered as launcher JAR files. These files all start with the
     * {@link SharedConstants#LAUNCHER_JAR_REL_PATH}. This list may be empty if
     * the launcher JAR file has not been installed yet.
     *
     * @param slingHome The sling home directory where the launcher JAR files
     *            are stored
     * @return The list of candidate launcher JAR files, which may be empty.
     *         <code>null</code> is returned if an IO error occurrs trying to
     *         list the files.
     */
    private File[] getLauncherJarFiles() {
        return slingHome.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile()
                    && pathname.getName().startsWith(
                        SharedConstants.LAUNCHER_JAR_REL_PATH);
            }
        });
    }

    /**
     * Returns the <code>slingHome</code> path as a directory. If the directory
     * does not exist it is created. If creation fails or if
     * <code>slingHome</code> exists but is not a directory a
     * <code>IllegalArgumentException</code> is thrown.
     *
     * @param slingHome The sling home directory where the launcher JAR files
     *            are stored
     * @return The Sling home directory
     * @throws IllegalArgumentException if <code>slingHome</code> exists and is
     *             not a directory or cannot be created as a directory.
     */
    private static File getSlingHomeFile(String slingHome) {
        if (slingHome == null) {
            slingHome = SharedConstants.SLING_HOME_DEFAULT;
        }

        File slingDir = new File(slingHome).getAbsoluteFile();
        if (slingDir.exists()) {
            if (!slingDir.isDirectory()) {
                throw new IllegalArgumentException("Sling Home " + slingDir
                    + " exists but is not a directory");
            }
        } else if (!slingDir.mkdirs()) {
            throw new IllegalArgumentException("Sling Home " + slingDir
                + " cannot be created as a directory");
        }

        return slingDir;
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
}
