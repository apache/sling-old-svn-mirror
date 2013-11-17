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
package org.apache.sling.tenant.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.tenant.Tenant;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource based tenant adapter factory, that adapts <code>ResourceResolver</code>
 * and <code>Resource</code> to <code>Tenant</code>.
 *
 * It tries to resolve the tenant based on logged in user by looking at the user
 * home path i.e. /home/users/tenant1/a/admin
 *
 * For resource, it tries to resolve Tenant using resource path.
 *
 */
class TenantAdapterFactory implements AdapterFactory {

	private final Logger log = LoggerFactory.getLogger(getClass());

	 static final Class<ResourceResolver> RESOURCERESOLVER_CLASS = ResourceResolver.class;
	private static final Class<Resource> RESOURCE_CLASS = Resource.class;
	private static final Class<Tenant> TENANT_CLASS = Tenant.class;

	private final TenantProviderImpl tenantProvider;

	private final ServiceRegistration<?> service;

    private final List<Pattern> pathPatterns;

	TenantAdapterFactory(final BundleContext bundleContext, final TenantProviderImpl tenantProvider, final String[] pathMatchers) {
	    this.tenantProvider = tenantProvider;

        this.pathPatterns = new ArrayList<Pattern>();
        for (String matcherStr : pathMatchers) {
            this.pathPatterns.add(Pattern.compile(matcherStr));
        }

	    Dictionary<String, Object> props = new Hashtable<String, Object>();
	    props.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Tenant Adapter");
	    props.put(AdapterFactory.ADAPTER_CLASSES, new String[]{ TENANT_CLASS.getName() });
	    props.put(AdapterFactory.ADAPTABLE_CLASSES, new String[] { RESOURCERESOLVER_CLASS
	            .getName(), RESOURCE_CLASS.getName() });

	    this.service = bundleContext.registerService(AdapterFactory.SERVICE_NAME, this, props);
    }

	void dispose() {
	    if (this.service != null) {
	        this.service.unregister();
        }
	}

	// ---------- AdapterFactory -----------------------------------------------

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.apache.sling.api.adapter.AdapterFactory#getAdapter(java.lang.Object,
	 * java.lang.Class)
	 */
	public <AdapterType> AdapterType getAdapter(Object adaptable,
			Class<AdapterType> type) {
		if (adaptable instanceof ResourceResolver) {
			return getAdapter(
					((ResourceResolver) adaptable).adaptTo(Session.class), type);
		}

		if (adaptable instanceof Resource) {
			return getAdapter(((Resource) adaptable).getPath(), type);
		}

		log.warn("Unable to handle adaptable {}", adaptable.getClass()
				.getName());
		return null;
	}

	private <AdapterType> AdapterType getAdapter(Session session,
			Class<AdapterType> type) {
	    if ( session instanceof JackrabbitSession) {
    		String userID = session.getUserID();
    		JackrabbitSession jrSession = (JackrabbitSession) session;
    		try {
    			Authorizable authorizable = jrSession.getUserManager()
    					.getAuthorizable(userID);
    			String userHome = authorizable.getPath();

    			// tries to get tenant information from user home
    			// i.e. /home/users/tenant1/a/admin
    			return getAdapter(userHome, type);
    		} catch (Exception e) {
    			log.error("can not get user from session", e);
    		}
	    }
		log.debug("Unable to adapt to resource of type {}", type.getName());
		return null;
	}

	@SuppressWarnings("unchecked")
	private <AdapterType> AdapterType getAdapter(String path,
			Class<AdapterType> type) {
        if (type == TENANT_CLASS) {
            return (AdapterType) resolveTenantByPath(path);
        }
        log.debug("Unable to adapt to resource of type {}", type.getName());
        return null;
	}

    private Tenant resolveTenantByPath(String path) {
        // find matching path identifier
        for (Pattern pathPattern : pathPatterns) {
            Matcher matcher = pathPattern.matcher(path);
            if (matcher.find()) {
                // assuming that first group is tenantId in the path, we can
                // make group number configurable.
                if (matcher.groupCount() >= 1) {
                    String tenantId = matcher.group(1);
                    final Tenant tenant = this.tenantProvider.getTenant(tenantId);
                    if (tenant != null) {
                        return tenant;
                    }
                }
            }
        }

        log.debug("Cannot resolve {} to a Tenant", path);
        return null;
    }

}
