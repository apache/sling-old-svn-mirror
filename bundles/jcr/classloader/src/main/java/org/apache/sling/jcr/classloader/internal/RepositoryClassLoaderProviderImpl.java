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

import java.util.Dictionary;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.classloader.RepositoryClassLoaderProvider;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;

/**
 * The <code>RepositoryClassLoaderProviderImpl</code> TODO
 *
 * @scr.component immediate="false" label="%loader.name"
 *      description="%loader.description"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description"
 *      value="Provides Repository ClassLoaders"
 * @scr.service servicefactory="true"
 */
public class RepositoryClassLoaderProviderImpl
        implements RepositoryClassLoaderProvider {

    /**
     * @scr.property values0="/var/classes"
     */
    public static final String CLASS_PATH_PROP = "classpath";

    /**
     * @scr.property valueRef="OWNER_DEFAULT"
     */
    public static final String OWNER_PROP = "owner";

    // JSP Class Loader class path will be injected to the class loader !
    private static final String[] CLASS_PATH_DEFAULT = { };

    private static final String OWNER_DEFAULT = "admin";

    private BidiMap loaders = new DualHashBidiMap();

    /**
     * @scr.reference
     */
    private SlingRepository repository;

    private String[] classPath;

    private String classLoaderOwner;

    private BundleProxyClassLoader parent;

    public ClassLoader getClassLoader(String owner) {
        String classLoaderOwner = this.getClassLoaderOwner(owner);
        RepositoryClassLoaderFacade loader =
            (RepositoryClassLoaderFacade) this.loaders.get(classLoaderOwner);
        if (loader == null) {
            loader = new RepositoryClassLoaderFacade(this, this.parent,
                classLoaderOwner, this.classPath);
            this.loaders.put(classLoaderOwner, loader);
        }

        // extend reference counter
        loader.ref();

        return loader;
    }

    public void ungetClassLoader(ClassLoader classLoader) {
        if (classLoader instanceof RepositoryClassLoaderFacade) {
            RepositoryClassLoaderFacade cl = (RepositoryClassLoaderFacade) classLoader;
            cl.deref();
        }
    }

    //---------- Support for RepositoryClassLoaderFacade ----------------------

    /* package */ Session getSession(String owner) throws RepositoryException {
        // get an administrative session for potentiall impersonation
        Session admin = this.repository.loginAdministrative(null);

        // do use the admin session, if the admin's user id is the same as owner
        if (admin.getUserID().equals(owner)) {
            return admin;
        }

        // else impersonate as the owner and logout the admin session again
        try {
            return admin.impersonate(new SimpleCredentials(owner, new char[0]));
        } finally {
            admin.logout();
        }
    }

    //---------- SCR Integration ----------------------------------------------

    protected void activate(ComponentContext componentContext) {
        @SuppressWarnings("unchecked")
        Dictionary properties = componentContext.getProperties();

        Object prop = properties.get(CLASS_PATH_PROP);
        this.classPath = (prop instanceof String[]) ? (String[]) prop : CLASS_PATH_DEFAULT;

        prop = properties.get(OWNER_PROP);
        this.classLoaderOwner = (prop instanceof String)? (String) prop : OWNER_DEFAULT;

        Bundle owner = componentContext.getUsingBundle();

        // if there is no using bundle, we have an error !!
        if (owner == null) {
            throw new IllegalStateException("Using Bundle expected. Is this a servicefactory component ?");
        }

        this.parent = new BundleProxyClassLoader(owner, null);
    }

    @SuppressWarnings("unchecked")
    protected void deactivate(ComponentContext componentContext) {
        for (Iterator ci=this.loaders.values().iterator(); ci.hasNext(); ) {
            RepositoryClassLoaderFacade cl = (RepositoryClassLoaderFacade) ci.next();
            cl.destroy();
            ci.remove();
        }

        this.parent = null;
    }

    //---------- internal -----------------------------------------------------

    private String getClassLoaderOwner(String userId) {
        return this.classLoaderOwner;
    }
}
