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
package org.apache.sling.jcr.ocm.impl.classloader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.Bundle;

/**
 * The <code>MapperClassLoader</code> is a very limited class loader which is
 * able to load registered classes only. Classes are registered in or two
 * ways:
 * <ol>
 * <li>Using the <code>java.lang.Class</code> object. In this case, the
 *  {@link #loadClass(String, boolean)} method just returns that class object.
 * <li>Using a <code>java.lang.ClassLoader</code> and the fully qualified className
 *  of the class. In this case, the {@link #loadClass(String, boolean)}
 *  method will call the <code>ClassLoader.loadClass(String)</code> method of
 *  the given class loader with the supplied class className.
 * </ol>
 * <p>
 * Instances of this class loader have no parent class loader.
 */
public class MapperClassLoader extends ClassLoader {

    // Map of class loaders indexed by fully qualified class className
    private Map<String, LoaderDelegate> delegateeLoaders;

    private Loader[] delegatees;

    /**
     * Creates an instance of this class loader without a parent class loader.
     */
    public MapperClassLoader() {
        super(null);

        this.delegateeLoaders = new HashMap<String, LoaderDelegate>();
        this.delegatees = new Loader[0];
    }

    public void dispose() {
        this.delegateeLoaders.clear();
        this.delegatees = new Loader[0];
    }

    public void registerClass(String className, ClassLoader classLoader) {
        this.registerClassInternal(className, new ClassLoaderLoader(classLoader));
    }

    public void registerClass(String className, Bundle bundle) {
        this.registerClassInternal(className, new BundleLoader(bundle));
    }

    public void unregisterClass(String className) {
        this.delegateeLoaders.remove(className);
    }

    public void registerClassLoader(ClassLoader classLoader) {
        this.registerLoaderInternal(new ClassLoaderLoader(classLoader));
    }

    public void unregisterClassLoader(ClassLoader classLoader) {
        this.unregisterLoaderInternal(classLoader);
    }

    public void registerBundle(Bundle bundle) {
        this.registerLoaderInternal(new BundleLoader(bundle));
    }

    public void unregisterBundle(Bundle bundle) {
        this.unregisterLoaderInternal(bundle);
    }

    /**
     * Returns the <code>Class</code> object for the given fully qualified
     * class className. If no class, with the given className has been registered with
     * the {@link #registerClass(Class)} or
     * {@link #registerClass(String, ClassLoader)} method, the boot class loader
     * is asked for the class, which will most certainly fail.
     *
     * @param className The fully qualified className of the class.
     * @param resolve Whether or not to resolve the class. This parameter is
     *      not used by this implementation
     *
     * @return The class for the className
     *
     * @throws ClassNotFoundException If the given class cannot be returned.
     */
    @SuppressWarnings("unchecked")
    protected synchronized Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        // 1. check whether we already know the class
        LoaderDelegate dele = this.delegateeLoaders.get(name);
        if (dele != null) {
            return dele.loadClass();
        }

        // 2. check whether one of the class loaders knows the class
        Loader[] delegatees = this.delegatees;
        for (int i=0; i < delegatees.length; i++) {
            try {
                // try to load the class
                Class clazz = delegatees[i].loadClass(name);

                // register for faster access next time
                this.registerClassInternal(clazz, delegatees[i]);

                // and return
                return clazz;
            } catch (ClassNotFoundException cnfe) {
                // ok, just try the next
            }
        }

        // 3. finally use the base class
        return super.loadClass(name, resolve);
    }

    //---------- internal -----------------------------------------------------

    private void registerClassInternal(String className, Loader loader) {
        this.delegateeLoaders.put(className, LoaderDelegate.create(loader, className));
    }

    private void registerClassInternal(Class<?> clazz, Loader loader) {
        this.delegateeLoaders.put(clazz.getName(), LoaderDelegate.create(loader, clazz));
    }

    private void registerLoaderInternal(Loader loader) {
        // check for duplicate
        if (this.findLoaderIndex(loader.getLoader()) >= 0) {
            return;
        }

        // append the class loader
        Loader[] newList = new Loader[this.delegatees.length+1];
        System.arraycopy(this.delegatees, 0, newList, 0, this.delegatees.length);
        newList[this.delegatees.length] = loader;
        this.delegatees = newList;
    }

    private void unregisterLoaderInternal(Object loader) {
        // remove classes registered with the class loader
        for (Iterator<LoaderDelegate> di = this.delegateeLoaders.values().iterator(); di.hasNext();) {
            LoaderDelegate dele = di.next();

            // remove the entry if the class loaders are the same
            if (dele.getLoader() == loader) {
                di.remove();
            }
        }

        // remove if separately registered
        int idx = this.findLoaderIndex(loader);
        if (idx >= 0) {
            Loader[] newList = new Loader[this.delegatees.length-1];
            if (idx > 0) System.arraycopy(this.delegatees, 0, newList, 0, idx);
            if (idx < newList.length)
                System.arraycopy(this.delegatees, idx, newList, idx+1, this.delegatees.length-idx);
            this.delegatees = newList;
        }
    }

    /**
     * Finds the <code>classLoader</code> in the list of registered delegate
     * class loaders. The class loaders are compared by reference equality.
     * If the class loader is not in the list -1 is returned.
     * <p>
     * This method must be called in a thread safe context, e.g. inside a
     * synchronized block.
     *
     * @param classLoader The <code>ClassLoader</code> whose index is to be
     *      returned.
     *
     * @return The index in the delegate class loader list of the class or
     *      -1 if not found.
     */
    private int findLoaderIndex(Object loader) {
        for (int i=0; i < this.delegatees.length; i++) {
            if (this.delegatees[i].getLoader() == loader) {
                return i;
            }
        }

        // exhausted, not found
        return -1;
    }
}
