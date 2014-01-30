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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrItemAdapterFactory;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

/**
 * The <code>JcrResourceResolverFactoryImpl</code> is the
 * {@link JcrResourceResolverFactory} service providing the following
 * functionality:
 * <ul>
 * <li><code>JcrResourceResolverFactory</code> service
 * </ul>
 */
@Component(name = "org.apache.sling.jcr.resource.internal.LegacyJcrResourceResolverFactoryImpl")
@Service(value = JcrResourceResolverFactory.class)
@Properties({
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling JcrResourceResolverFactory Implementation"),
        @Property(name = Constants.SERVICE_VENDOR, value = "The Apache Software Foundation")

})
public class JcrResourceResolverFactoryImpl implements
        JcrResourceResolverFactory {

    @Reference
    private ResourceResolverFactory delegatee;

    /** The dynamic class loader */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private DynamicClassLoaderManager dynamicClassLoaderManager;

    private JcrItemAdapterFactory jcrItemAdapterFactory;

    @Activate
    protected void activate(final ComponentContext componentContext) {
        jcrItemAdapterFactory = new JcrItemAdapterFactory(
                componentContext.getBundleContext(), this);
    }

    /** Deativates this component, called by SCR to take out of service */
    protected void deactivate(final ComponentContext componentContext) {
        if (jcrItemAdapterFactory != null) {
            jcrItemAdapterFactory.dispose();
            jcrItemAdapterFactory = null;
        }
    }

    /** Get the dynamic class loader if available */
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
    public ResourceResolver getServiceResourceResolver(Map<String, Object> authenticationInfo) throws LoginException {
        return delegatee.getServiceResourceResolver(authenticationInfo);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getAdministrativeResourceResolver(java.util.Map)
     */
    public ResourceResolver getAdministrativeResourceResolver(
            final Map<String, Object> authenticationInfo) throws LoginException {
        return delegatee.getAdministrativeResourceResolver(authenticationInfo);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolverFactory#getResourceResolver(java.util.Map)
     */
    public ResourceResolver getResourceResolver(final Map<String, Object> arg0)
            throws LoginException {
        return delegatee.getResourceResolver(arg0);
    }
}