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
package org.apache.sling.maven.jspc;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.plexus.util.StringUtils;

/**
 * Classloader that tracks which classes are loaded.
 */
public class TrackingClassLoader extends URLClassLoader {

    private final Set<String> classNames = Collections.synchronizedSet(new HashSet<String>());

    private final Set<String> packageNames = Collections.synchronizedSet(new HashSet<String>());

    public TrackingClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * Returns the loaded classes.
     * @return the set of class names.
     */
    public Set<String> getClassNames() {
        return classNames;
    }

    /**
     * Returns the package names of the loaded classes.
     * @return the set of package names.
     */
    public Set<String> getPackageNames() {
        return packageNames;
    }

    /**
     * @see java.lang.ClassLoader#loadClass(java.lang.String)
     */
    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        final Class<?> c = super.loadClass(name);
        this.classNames.add(name);
        this.packageNames.add(c.getPackage().getName());
        return c;
    }

    @Override
    public URL findResource(String name) {
        final URL url = super.findResource(name);
        if (url != null && name.endsWith(".class")) {
            int lastDot = name.lastIndexOf('.');
            int lastSlash = name.lastIndexOf('/');
            String className = name.substring(0, lastDot).replaceAll("/", ".");
            classNames.add(className);
            if (lastSlash > 0) {
                packageNames.add(className.substring(0, lastSlash));
            }
        }
        return url;
    }

}