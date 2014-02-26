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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The <code>LauncherClassLoader</code> extends the standard Java VM
 * <code>URLClassLoader</code> such, that classes and resources which are
 * contained in the launcher JAR File are never looked up in the parent class
 * loader.
 * <p>
 * This class loader shields the Sling OSGi framework completely from the
 * environment because
 * <ul>
 * <li>Classes and resources contained in packages provided by the launcher JAR
 * file are only looked up in the launcher JAR file</li>
 * <li>Classes and resources from the environment are only used from within the
 * framework if the framework is configured to do so by the
 * <code>org.osgi.framework.bootdelegation</code> or
 * <code>org.osgi.framework.systempackages</code> framework properties.</li>
 * </ul>
 * <p>
 * The first point is important if Sling is deployed into any container, which
 * provides some or all of the same packages as the Sling launcher JAR file. One
 * such example is the Glassfish v3 Prelude application service, which itself
 * runs in a Felix OSGi framework and sort of leaks the classes into the web
 * application.
 * <p>
 * In the general case, we cannot prevent any leaking of classes, which may also
 * be the OSGi core or compendium libraries, into Sling. So, this class loader
 * is the barrier for this leaking and shields Sling from the environment unless
 * explicitly configured to use this leaking.
 * <p>
 * Instances of this class loader are setup with the launcher JAR file as the
 * only contents of the <code>URLClassLoaders</code> class path and the class
 * loader of this class itself as the parent class loader.
 */
public class LauncherClassLoader extends URLClassLoader {

    /**
     * Set of packages never to be used from the environment. Each package is
     * contained in this set in two forms: The Java package form where the
     * segments are separated by dots, e.g. org.osgi.framework, and the resource
     * form where the segments are separated by slash, e.g. org/osgi/framework.
     * This makes checking packages for classes and resources equaly simple
     * without requiring name mangling.
     */
    private final Set<String> launcherPackages;

    LauncherClassLoader(File launcherJar, File[] extJars) throws MalformedURLException {
        super(new URL[] { launcherJar.toURI().toURL() },
            LauncherClassLoader.class.getClassLoader());

        Set<String> collectedPackages = new HashSet<String>();

        //process launcher jar
        processJarPackages(launcherJar, collectedPackages);

        //process extension jars
        List<File> extJarFileList = getExtJarFileList(extJars);

        //add external jars to classloader
        for(File extJarFile:extJarFileList){
            addURL(extJarFile.toURI().toURL());
            processJarPackages(extJarFile, collectedPackages);
        }

        launcherPackages = collectedPackages;
    }

    private void processJarPackages(File jarFile, Set<String> packageSet ){
        JarFile jar = null;
        try {
            jar = new JarFile(jarFile, false);
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName();
                if (entryName.endsWith(".class")
                        && !entryName.startsWith("META-INF/")
                        && !entryName.startsWith("javax/")) {
                    String packageName = getPackageName(entryName, '/');
                    if (packageName != null
                            && packageSet.add(packageName)) {
                        packageSet.add(packageName.replace('/', '.'));
                    }
                }
            }
        } catch (IOException ioe) {
            // might log or throw, don't know ??
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException ignore) {
                }
            }
        }
    }


    private List<File> getExtJarFileList(File[] extJars) throws MalformedURLException {
        List<File> jarList = new ArrayList<File>();
        for (File extJarFile : extJars) {
            if (extJarFile != null && extJarFile.exists()) {
                jarList.add(extJarFile);
            }
        }
        return jarList;
    }

    /**
     * Load the name class and optionally resolve it, if found.
     * <p>
     * This method checks whether the package of the class is contained in the
     * launcher JAR file. If so, the launcher JAR file is looked up for the
     * class and class loading fails if not found. Otherwise the standard class
     * loading strategy is applied by calling the base class implementation.
     */
    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            if (containsPackage(name, '.')) {
                // finds the class or throws a ClassNotFoundException if
                // the class cannot be found, which is ok, since we only
                // want the class from our jar file, if it contains the
                // package.
                c = findClass(name);
            } else {
                return super.loadClass(name, resolve);
            }
        }

        if (resolve) {
            resolveClass(c);
        }

        return c;
    }

    /**
     * Return an URL to the requested resource.
     * <p>
     * This method checks whether the package of the resource is contained in
     * the launcher JAR file. If so, the launcher JAR file is looked up for the
     * resource and resource access fails if not found. Otherwise the standard
     * resource access strategy is applied by calling the base class
     * implementation.
     */
    @Override
    public URL getResource(String name) {

        // if the package of the name is contained in our jar file
        // file, return the resource or nothing
        if (containsPackage(name, '/')) {
            return findResource(name);
        }

        // try parent class loader only after having checked our packages
        return super.getResource(name);
    }

    /**
     * Returns the name of the package of the fully qualified name using the
     * given <code>separator</code> as the segment separator. If the
     * <code>name</code> does not contain the separator, the name is contained
     * in the root package and this method returns <code>null</code>.
     * <p>
     * Example: Called for <i>org.osgi.framework.Bundle</i> this method returns
     * <i>org.osgi.framework</i>.
     *
     * @param name The fully qualified name of the class or resource to check
     * @param separator The separator for package segments
     */
    private String getPackageName(String name, int separator) {
        int speIdx = name.lastIndexOf(separator);
        return (speIdx > 0) ? name.substring(0, speIdx) : null;
    }

    /**
     * Returns <code>true</code> if the launcher JAR file provides the package
     * to which the named class or resource belongs.
     *
     * @param name The fully qualified name of the class or resource to check
     * @param separator The separator for package segments
     */
    private boolean containsPackage(String name, int separator) {
        String packageName = getPackageName(name, separator);
        return (packageName == null)
                ? false
                : launcherPackages.contains(packageName);
    }
}
