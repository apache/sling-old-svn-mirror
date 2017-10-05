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
package org.apache.sling.commons.fsclassloader.impl;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sling.commons.classloader.DynamicClassLoader;

public class FSDynamicClassLoader
    extends URLClassLoader
    implements DynamicClassLoader {

    private volatile boolean isDirty = false;

    private final Set<String> loads = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private final DynamicClassLoader parentLoader;

    public FSDynamicClassLoader(final URL[] urls, final ClassLoader parent) {
        super(urls, parent);
        parentLoader = (parent instanceof DynamicClassLoader ? (DynamicClassLoader)parent : null);
    }

    /**
     * @see org.apache.sling.commons.classloader.DynamicClassLoader#isLive()
     */
    public boolean isLive() {
        return !isDirty && (parentLoader == null || parentLoader.isLive());
    }

    /**
     * @see java.lang.ClassLoader#loadClass(java.lang.String)
     */
    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        try {
            return super.loadClass(name);
        } finally {
            this.loads.add(name);
        }
    }

    public void check(final String className) {
        if ( !this.isDirty ) {
            this.isDirty = loads.contains(className);
        }
    }
}
