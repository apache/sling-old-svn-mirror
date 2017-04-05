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
package org.apache.sling.jcr.resource.internal;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.LoggerFactory;

/**
 * The <code>JcrResourceResolverFactoryImpl</code> is the
 * {@link JcrResourceResolverFactory} service providing the following
 * functionality:
 * <ul>
 * <li><code>JcrResourceResolverFactory</code> service
 * </ul>
 */
@Component(name = "org.apache.sling.jcr.resource.internal.LegacyJcrResourceResolverFactoryImpl",
           service = JcrResourceResolverFactory.class,
           property = {
                   Constants.SERVICE_DESCRIPTION + "=Apache Sling JcrResourceResolverFactory Implementation",
                   Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
           })
public class JcrResourceResolverFactoryImpl implements
        JcrResourceResolverFactory {

    @Reference
    private ResourceResolverFactory delegatee;

    /** The dynamic class loader */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile DynamicClassLoaderManager dynamicClassLoaderManager;

    @Activate
    protected void activate() {
        LoggerFactory.getLogger(this.getClass()).warn("DEPRECATION WARNING: JcrResourceResolverFactory is deprecated. Please use ResourceResolverFactory instead.");
    }

    /**
     * Get the dynamic class loader if available.
     *
     * @return the classloader
     */
    public ClassLoader getDynamicClassLoader() {
        final DynamicClassLoaderManager dclm = this.dynamicClassLoaderManager;
        if (dclm != null) {
            return dclm.getDynamicClassLoader();
        }
        return null;
    }

    /**
     * @see org.apache.sling.jcr.resource.JcrResourceResolverFactory#getResourceResolver(javax.jcr.Session)
     */
    @Override
    public ResourceResolver getResourceResolver(final Session session) {
        final Map<String, Object> authInfo = new HashMap<String, Object>(1);
        authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session);
        try {
            return getResourceResolver(authInfo);
        } catch (LoginException le) {
            // we don't expect a LoginException here because just a
            // ResourceResolver wrapping the given session is to be created.
            throw new InternalError("Unexpected LoginException");
        }
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getServiceResourceResolver(Map)
     */
    @Override
    public ResourceResolver getServiceResourceResolver(Map<String, Object> authenticationInfo) throws LoginException {
        return delegatee.getServiceResourceResolver(authenticationInfo);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getAdministrativeResourceResolver(java.util.Map)
     */
    @Override
    public ResourceResolver getAdministrativeResourceResolver(
            final Map<String, Object> authenticationInfo) throws LoginException {
        return delegatee.getAdministrativeResourceResolver(authenticationInfo);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getResourceResolver(java.util.Map)
     */
    @Override
    public ResourceResolver getResourceResolver(final Map<String, Object> arg0)
            throws LoginException {
        return delegatee.getResourceResolver(arg0);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getThreadResourceResolver()
     */
    @Override
    public ResourceResolver getThreadResourceResolver() {
        return delegatee.getThreadResourceResolver();
    }
}