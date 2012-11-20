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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.tenant.Tenant;
import org.apache.sling.tenant.TenantProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

/**
 * JCR Tenant Provider implementation.
 */
@Component(
        metatype = true,
        label = "Apache Sling JCR Tenant Provider",
        description = "Service responsible for providing Tenants")
@Service
@Properties(value = {
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling JCR Tenant Provider")
})
public class TenantProviderImpl implements TenantProvider {
    /**
     * Root path for tenant
     */
    private static final String JCR_TENANT_ROOT = "/etc/tenants";

    @Property(value = JCR_TENANT_ROOT, label = "Tenants Root Path", description = "Defines tenants root path")
    private static final String TENANT_ROOT = "tenant.root";

    private static final String[] DEFAULT_PATH_MATCHER = {};

    @Property(
            value = {},
            unbounded = PropertyUnbounded.ARRAY,
            label = "Tenants Path Matcher",
            description = "Defines tenants path matcher i.e. /content/sample/([^/]+)/*, used while resolving path to tenant")
    private static final String TENANT_PATH_MATCHER = "tenant.path.matcher";

    private String[] pathMatchers;

    private List<Pattern> pathPatterns = new ArrayList<Pattern>();

    private String tenantRootPath = JCR_TENANT_ROOT;

    @Reference
    private ResourceResolverFactory factory;

    private TenantAdapterFactory adapterFactory;

    @Activate
    private void activate(final BundleContext bundleContext, final Map<String, Object> properties) {
        this.tenantRootPath = PropertiesUtil.toString(properties.get(TENANT_ROOT), JCR_TENANT_ROOT);
        this.pathMatchers = PropertiesUtil.toStringArray(properties.get(TENANT_PATH_MATCHER), DEFAULT_PATH_MATCHER);

        this.pathPatterns.clear();
        for (String matcherStr : this.pathMatchers) {
            this.pathPatterns.add(Pattern.compile(matcherStr));
        }

        this.adapterFactory = new TenantAdapterFactory(bundleContext, this);
    }

    @Deactivate
    private void deactivate() {
        if (this.adapterFactory != null) {
            this.adapterFactory.dispose();
            this.adapterFactory = null;
        }
    }

    public Tenant getTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return null;
        }

        final ResourceResolver adminResolver = getAdminResolver();
        if (adminResolver != null) {
            try {
                Resource tenantRootRes = adminResolver.getResource(tenantRootPath);

                Resource tenantRes = tenantRootRes.getChild(tenantId);
                if (tenantRes != null) {
                    return new TenantImpl(tenantRes);
                }
            } finally {
                adminResolver.close();
            }
        }

        // in case of some problem
        return null;
    }

    public Iterator<Tenant> getTenants() {
        final ResourceResolver adminResolver = getAdminResolver();
        if (adminResolver != null) {
            try {
                Resource tenantRootRes = adminResolver.getResource(tenantRootPath);

                List<Tenant> tenantList = new ArrayList<Tenant>();
                Iterator<Resource> tenantResourceList = tenantRootRes.listChildren();
                while (tenantResourceList.hasNext()) {
                    Resource tenantRes = tenantResourceList.next();
                    tenantList.add(new TenantImpl(tenantRes));
                }
                return tenantList.iterator();
            } finally {
                adminResolver.close();
            }
        }

        // in case of some problem return an empty iterator
        return Collections.<Tenant> emptyList().iterator();
    }

    public Tenant addTenant(String name, String tenantId) throws PersistenceException {
        final ResourceResolver adminResolver = getAdminResolver();
        if (adminResolver != null) {
            try {
                Resource tenantRootRes = adminResolver.getResource(tenantRootPath);

                // check if tenantId already exists
                Resource child = tenantRootRes.getChild(tenantId);

                if (child != null) {
                    throw new PersistenceException("Tenant already exists with Id " + tenantId);
                } else {
                    // create the tenant
                    Node rootNode = tenantRootRes.adaptTo(Node.class);
                    Node tenantNode = rootNode.addNode(tenantId);
                    tenantNode.setProperty(Tenant.PROP_NAME, name);
                    adminResolver.adaptTo(Session.class).save();
                    return new TenantImpl(adminResolver.getResource(tenantNode.getPath()));
                }
            } catch (RepositoryException e) {
                throw new PersistenceException("Unexpected RepositoryException while adding tenant", e);
            } finally {
                adminResolver.close();
            }
        }

        throw new PersistenceException("Cannot create the tenant");
    }

    public Iterator<Tenant> getTenants(String tenantFilter) {
        if (StringUtils.isBlank(tenantFilter)) {
            return null;
        }

        final ResourceResolver adminResolver = getAdminResolver();
        if (adminResolver != null) {
            try {
                Resource tenantRootRes = adminResolver.getResource(tenantRootPath);

                List<Tenant> tenantList = new ArrayList<Tenant>();
                Iterator<Resource> tenantResourceList = tenantRootRes.listChildren();
                while (tenantResourceList.hasNext()) {
                    Resource tenantRes = tenantResourceList.next();
                    ValueMap vm = ResourceUtil.getValueMap(tenantRes);

                    Filter filter = FrameworkUtil.createFilter(tenantFilter);
                    if (filter.matches(vm)) {
                        TenantImpl tenant = new TenantImpl(tenantRes);
                        tenantList.add(tenant);
                    }
                }
                return tenantList.iterator();
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            } finally {
                adminResolver.close();
            }
        }

        // in case of some problem return an empty iterator
        return Collections.<Tenant> emptyList().iterator();
    }

    /**
     * Helper for the {@link JcrTenantAdapterFactory} to resolve any resource
     * path to a tenant.
     */
    Tenant resolveTenantByPath(String path) {
        // find matching path identifier
        for (Pattern pathPattern : pathPatterns) {
            Matcher matcher = pathPattern.matcher(path);
            if (matcher.find()) {
                // assuming that first group is tenantId in the path, we can
                // make group number configurable.
                if (matcher.groupCount() >= 1) {
                    String tenantId = matcher.group(1);
                    return getTenant(tenantId);
                }
            }
        }
        return null;
    }

    private ResourceResolver getAdminResolver() {
        try {
            return factory.getAdministrativeResourceResolver(null);
        } catch (LoginException le) {
            // unexpected, thus ignore
        }

        return null;
    }
}
