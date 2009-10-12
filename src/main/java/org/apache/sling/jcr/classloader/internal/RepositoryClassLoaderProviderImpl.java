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

import org.apache.sling.jcr.classloader.RepositoryClassLoaderProvider;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;

/**
 * The <code>RepositoryClassLoaderProviderImpl</code> TODO
 *
 * @scr.component inherit="false" label="%deprecatedloader.name"
 *      description="%deprecatedloader.description"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description"
 *      value="Provides Repository ClassLoaders"
 * @scr.service servicefactory="true" interface="RepositoryClassLoaderProvider"
 * @scr.property nameRef="DynamicClassLoaderProviderImpl.CLASS_PATH_PROP" valueRefs0="DynamicClassLoaderProviderImpl.CLASS_PATH_DEFAULT"
 * @scr.property nameRef="DynamicClassLoaderProviderImpl.OWNER_PROP" valueRef="DynamicClassLoaderProviderImpl.OWNER_DEFAULT"
 * @scr.reference name="repository" interface="org.apache.sling.jcr.api.SlingRepository"
 * @scr.reference name="mimeTypeService" policy="dynamic" interface="org.apache.sling.commons.mime.MimeTypeService"
 */
public class RepositoryClassLoaderProviderImpl
        extends DynamicClassLoaderProviderImpl
        implements RepositoryClassLoaderProvider {

    private BundleProxyClassLoader parent;

    private RepositoryClassLoaderFacade classLoaderFacade;

    /**
     * @see org.apache.sling.jcr.classloader.RepositoryClassLoaderProvider#getClassLoader(java.lang.String)
     */
    public ClassLoader getClassLoader(String owner) {
        if (this.classLoaderFacade == null) {
            this.classLoaderFacade = new RepositoryClassLoaderFacade(this, this.parent,
                this.getClassPaths());
        }

        return this.classLoaderFacade;
    }

    /**
     * @see org.apache.sling.jcr.classloader.RepositoryClassLoaderProvider#ungetClassLoader(java.lang.ClassLoader)
     */
    public void ungetClassLoader(ClassLoader classLoader) {
        // nothing to do
    }

    protected void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        final Bundle owner = componentContext.getUsingBundle();

        // if there is no using bundle, we have an error !!
        if (owner == null) {
            throw new IllegalStateException("Using Bundle expected. Is this a servicefactory component ?");
        }
        this.parent = new BundleProxyClassLoader(owner, null);
    }

    @SuppressWarnings("unchecked")
    protected void deactivate(ComponentContext componentContext) {
        if ( this.classLoaderFacade != null ) {
            this.classLoaderFacade.destroy();
            this.classLoaderFacade = null;
        }
        this.parent = null;
        super.deactivate(componentContext);
    }
}
