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
package org.apache.sling.commons.compiler.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;

import org.apache.sling.commons.classloader.ClassLoaderWriter;


/**
 * The <code>IsolatedClassLoader</code> class loads classes through
 * the class loader writer
 */
public final class IsolatedClassLoader
    extends SecureClassLoader {

    private final ClassLoaderWriter classLoaderWriter;

    public IsolatedClassLoader(final ClassLoader parent,
            final ClassLoaderWriter classLoaderWriter) {
        super(parent);
        this.classLoaderWriter = classLoaderWriter;
    }


    //---------- Class loader overwrites -------------------------------------

    /**
     * Loads the class from this <code>ClassLoader</class>.  If the
     * class does not exist in this one, we check the parent.  Please
     * note that this is the exact opposite of the
     * <code>ClassLoader</code> spec.  We use it to work around
     * inconsistent class loaders from third party vendors.
     *
     * @param     name the name of the class
     * @param     resolve if <code>true</code> then resolve the class
     * @return    the resulting <code>Class</code> object
     * @exception ClassNotFoundException if the class could not be found
     */
    public final Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // First check if it's already loaded
        Class<?> clazz = findLoadedClass(name);

        if (clazz == null) {

            try {
                clazz = findClass(name);
            } catch (final ClassNotFoundException cnfe) {
                final ClassLoader parent = getParent();
                if (parent != null) {
                    // Ask to parent ClassLoader (can also throw a CNFE).
                    clazz = parent.loadClass(name);
                } else {
                    // Propagate exception
                    throw cnfe;
                }
            }
        }

        if (resolve) {
            resolveClass(clazz);
        }

        return clazz;
    }

    /**
     * Finds and loads the class with the specified name from the class path.
     *
     * @param name the name of the class
     * @return the resulting class
     *
     * @throws ClassNotFoundException If the named class could not be found or
     *      if this class loader has already been destroyed.
     */
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        try {
            return AccessController.doPrivileged(
                new PrivilegedExceptionAction<Class<?>>() {

                    public Class<?> run() throws ClassNotFoundException {
                        return findClassPrivileged(name);
                    }
                });
        } catch (final java.security.PrivilegedActionException pae) {
            throw (ClassNotFoundException) pae.getException();
        }
    }

    /**
     * Tries to find the class in the class path from within a
     * <code>PrivilegedAction</code>. Throws <code>ClassNotFoundException</code>
     * if no class can be found for the name.
     *
     * @param name the name of the class
     *
     * @return the resulting class
     *
     * @throws ClassNotFoundException if the class could not be found
     * @throws NullPointerException If this class loader has already been
     *      destroyed.
     */
    private Class<?> findClassPrivileged(final String name) throws ClassNotFoundException {
        // prepare the name of the class
        final String path = "/" + name.replace('.', '/') + ".class";
        InputStream is = null;
        try {
            is = this.classLoaderWriter.getInputStream(path);
            final Class<?> c = defineClass(name, is);
            if (c == null) {
                throw new ClassNotFoundException(name);
            }
            return c;
        } catch ( final ClassNotFoundException cnfe) {
            throw cnfe;
        } catch (final Throwable t) {
            throw new ClassNotFoundException(name, t);
        }
     }

    /**
     * Defines a class getting the bytes for the class from the resource
     *
     * @param name The fully qualified class name
     * @param is The resource to obtain the class bytes from
     *
     * @throws IOException If a problem occurrs reading the class bytes from
     *      the resource.
     * @throws ClassFormatError If the class bytes read from the resource are
     *      not a valid class.
     */
    private Class<?> defineClass(final String name, final InputStream is)
    throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final byte[] buffer = new byte[2048];
        int l;
        while ( ( l = is.read(buffer)) >= 0 ) {
            baos.write(buffer, 0, l);
        }
        final byte[] data = baos.toByteArray();
        return defineClass(name, data, 0, data.length);
    }
}
