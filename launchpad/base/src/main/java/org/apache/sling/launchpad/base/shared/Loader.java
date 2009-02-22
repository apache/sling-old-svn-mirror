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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;

/**
 * The <code>Loader</code> class provides utility methods for the actual
 * launchers to help launching the framework.
 */
public class Loader {

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
    public static Object loadLauncher(String launcherClassName, String slingHome) {

        File launcherJarFile = getLauncherJarFile(slingHome);
        if (!launcherJarFile.canRead()) {
            throw new IllegalArgumentException("Sling Launcher JAR "
                + launcherJarFile + " is not accessible");
        }

        ClassLoader loader;
        try {
            loader = new LauncherClassLoader(launcherJarFile);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                "Cannot create an URL from the Sling Launcher JAR path name", e);
        }

        try {
            Class<?> launcherClass = loader.loadClass(launcherClassName);
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
     * </ul>
     * <p>
     * This method must be called when the notifier is called.
     */
    public static void cleanupVM() {

        // ensure the JavaBeans introspector lets go of any classes it
        // may haved cached after introspection
        Introspector.flushCaches();

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
    public static boolean installLauncherJar(URL launcherJar, String slingHome)
            throws IOException {
        File launcherJarDestFile = getLauncherJarFile(slingHome);

        // check whether we have to overwrite
        URLConnection launcherJarConn = launcherJar.openConnection();
        long lastModifTime = launcherJarConn.getLastModified();
        if (launcherJarDestFile.exists()) {
            if (lastModifTime <= launcherJarDestFile.lastModified()) {
                return false;
            }
            launcherJarDestFile.delete();
        }

        spool(launcherJarConn.getInputStream(), launcherJarDestFile);

        launcherJarDestFile.setLastModified(lastModifTime);
        return true;
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

    /**
     * Returns a <code>File</code> object representing the Launcher JAR file
     * found in the sling home folder.
     * 
     * @throws IllegalArgumentException if the sling home folder cannot be
     *             created or exists as a non-directory filesystem entry.
     */
    private static File getLauncherJarFile(String slingHome) {
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

        return new File(slingDir, SharedConstants.LAUNCHER_JAR_REL_PATH);
    }
}
