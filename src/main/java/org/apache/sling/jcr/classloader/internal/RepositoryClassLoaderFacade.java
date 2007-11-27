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
package org.apache.sling.jcr.classloader.internal;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.classloader.DynamicRepositoryClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>RepositoryClassLoaderFacade</code> TODO
 */
class RepositoryClassLoaderFacade extends URLClassLoader {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(RepositoryClassLoaderFacade.class);

    private static final URL[] NO_URLS = new URL[0];

    private RepositoryClassLoaderProviderImpl classLoaderProvider;
    private ClassLoader parent;
    private String sessionOwner;
    private Session session;
    private String[] classPath;
    private DynamicRepositoryClassLoader delegate;

    /**
     * The reference counter. If not greater than zero, there are this
     * number of (assumed) life references.
     *
     * @see #ref()
     * @see #deref()
     */
    private int refCtr = 0;

    public RepositoryClassLoaderFacade(
            RepositoryClassLoaderProviderImpl classLoaderProvider,
            ClassLoader parent,
            String sessionOwner,
            String[] classPath) {

        // no parent class loader, we delegate to repository class loaders
        super(NO_URLS, null);

        this.classLoaderProvider = classLoaderProvider;
        this.parent = parent;
        this.classPath = classPath;
        this.sessionOwner = sessionOwner;
    }

    public void addPath(String path) {
        // create new class path
        String[] newClassPath = new String[this.classPath.length+1];
        System.arraycopy(this.classPath, 0, newClassPath, 0, this.classPath.length);
        newClassPath[this.classPath.length] = path;
        this.classPath = newClassPath;

        // destroy the delegate and have a new one created
        if (this.delegate != null) {
            DynamicRepositoryClassLoader oldLoader = this.delegate;
            this.delegate = null;
            oldLoader.destroy();
        }
    }

    public String[] getClassPath() {
        return this.classPath.clone();
    }

    @Override
    public URL[] getURLs() {
        try {
            return getDelegateClassLoader().getURLs();
        } catch (RepositoryException re) {
            log.error("Cannot get repository class loader to get URLs", re);
            return NO_URLS;
        }
    }

    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return this.getDelegateClassLoader().loadClass(name);
        } catch (RepositoryException re) {
            log.error("Cannot get repository class loader to load class " + name, re);
            throw new ClassNotFoundException(name);
        }
    }

    public URL getResource(String name) {
        try {
            return this.getDelegateClassLoader().getResource(name);
        } catch (RepositoryException re) {
            log.error("Cannot get repository class loader to get resource " + name, re);
            return null;
        }
    }

    public Enumeration findResources(String name) throws IOException {
        try {
            return this.getDelegateClassLoader().getResources(name);
        } catch (RepositoryException re) {
            throw (IOException) new IOException("Cannot lookup " + name).initCause(re);
        }
    }

    //---------- Reference counting support -----------------------------------

    /* package */ void destroy() {
        if (this.delegate != null) {
            this.delegate.destroy();
            this.delegate = null;
        }

        if (this.session != null) {
            this.session.logout();
            this.session = null;
        }
    }

    /**
     * Increases the reference counter of this class loader.
     */
    /* package */void ref() {
        this.refCtr++;
    }

    /**
     * Decreases the reference counter of this class loader and calls the
     * base class <code>destroy()</code> method, if this class loader has
     * already been destroyed by calling the {@link #destroy()} method.
     */
    /* package */void deref() {
        this.refCtr--;

        // destroy if the loader should be destroyed and no refs exist
//        if (refCtr <= 0 /* && destroyed */ ) {
//            destroy();
//        }
    }

    //---------- internal -----------------------------------------------------

    private Session getSession() throws RepositoryException {
        // check current session
        if (this.session != null) {
            if (this.session.isLive()) {
                return this.session;
            }

            // drop delegate
            if (this.delegate != null) {
                this.delegate.destroy();
                this.delegate = null;
            }

            // current session is not live anymore, drop
            this.session.logout();
            this.session = null;
        }

        // no session currently, acquire and return
        this.session = this.classLoaderProvider.getSession(this.sessionOwner);
        return this.session;
    }

    private DynamicRepositoryClassLoader getDelegateClassLoader() throws RepositoryException {
        if (this.delegate != null) {
            if (this.delegate.isDirty()) {
                this.delegate = this.delegate.reinstantiate(this.getSession(), this.parent);
            }
        } else {
            this.delegate = new DynamicRepositoryClassLoader(this.getSession(), this.classPath, this.parent);
        }

        return this.delegate;
    }
}
