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
import java.security.SecureClassLoader;
import java.util.Enumeration;

import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>RepositoryClassLoaderFacade</code> TODO
 */
class RepositoryClassLoaderFacade extends SecureClassLoader {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(RepositoryClassLoaderFacade.class);

    private DynamicClassLoaderProviderImpl classLoaderProvider;
    private ClassLoader parent;
    private String[] classPath;
    private DynamicRepositoryClassLoader delegate;

    public RepositoryClassLoaderFacade(
            DynamicClassLoaderProviderImpl classLoaderProvider,
            ClassLoader parent,
            String[] classPath) {

        // no parent class loader, we delegate to repository class loaders
        super(null);

        this.classLoaderProvider = classLoaderProvider;
        this.parent = parent;
        this.classPath = classPath;
    }

    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
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

    public Enumeration<URL> findResources(String name) throws IOException {
        try {
            return this.getDelegateClassLoader().getResources(name);
        } catch (RepositoryException re) {
            throw (IOException) new IOException("Cannot lookup " + name).initCause(re);
        }
    }

    void destroy() {
        if (this.delegate != null) {
            this.delegate.destroy();
            this.delegate = null;
        }
    }

    //---------- internal -----------------------------------------------------

    private synchronized DynamicRepositoryClassLoader getDelegateClassLoader() throws RepositoryException {
        if ( this.delegate == null ) {
            this.delegate = new DynamicRepositoryClassLoader( this.classLoaderProvider.getReadSession(), this.classPath, this.parent);

        } else {
            if (this.delegate.isDirty()) {
                this.delegate = this.delegate.reinstantiate(this.classLoaderProvider.getReadSession(), this.parent);
            }

        }
        return this.delegate;
    }

}
