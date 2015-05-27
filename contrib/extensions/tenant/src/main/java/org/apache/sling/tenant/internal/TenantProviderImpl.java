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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.osgi.ServiceUtil;
import org.apache.sling.tenant.Tenant;
import org.apache.sling.tenant.TenantManager;
import org.apache.sling.tenant.TenantProvider;
import org.apache.sling.tenant.internal.console.WebConsolePlugin;
import org.apache.sling.tenant.spi.TenantCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource based Tenant Provider implementation.
 */
@Component(
        metatype = true,
        label = "Apache Sling Tenant Provider",
        description = "Service responsible for providing Tenants",
        immediate = true)
@Service
@Properties(value = {
    @Property(name = Constants.SERVICE_DESCRIPTION, value = "Apache Sling Tenant Provider")
})
@Reference(
        name = "tenantSetup",
        referenceInterface = TenantCustomizer.class,
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy = ReferencePolicy.DYNAMIC)
public class TenantProviderImpl implements TenantProvider, TenantManager {

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Root path for tenant
     */
    private static final String RESOURCE_TENANT_ROOT = "/etc/tenants";

    @Property(value = RESOURCE_TENANT_ROOT, label = "Tenants Root Path", description = "Defines tenants root path")
    private static final String TENANT_ROOT = "tenant.root";

    private static final String[] DEFAULT_PATH_MATCHER = {};

    private SortedMap<Comparable<Object>, TenantCustomizer> registeredTenantHandlers = new TreeMap<Comparable<Object>, TenantCustomizer>(
        Collections.reverseOrder());

    @Property(
            value = {},
            unbounded = PropertyUnbounded.ARRAY,
            label = "Tenants Path Matcher",
            description = "Defines tenants path matcher i.e. /content/sample/([^/]+)/*, used while resolving path to tenant")
    private static final String TENANT_PATH_MATCHER = "tenant.path.matcher";

    private String tenantRootPath = RESOURCE_TENANT_ROOT;

    @Reference
    private ResourceResolverFactory factory;

    private TenantAdapterFactory adapterFactory;

    private WebConsolePlugin plugin;

    @Activate
    private void activate(final BundleContext bundleContext, final Map<String, Object> properties) {
        this.tenantRootPath = PropertiesUtil.toString(properties.get(TENANT_ROOT), RESOURCE_TENANT_ROOT);
        this.adapterFactory = new TenantAdapterFactory(bundleContext, this, PropertiesUtil.toStringArray(properties.get(TENANT_PATH_MATCHER), DEFAULT_PATH_MATCHER));
        this.plugin = new WebConsolePlugin(bundleContext, this);
    }

    @Deactivate
    private void deactivate() {
        if (this.adapterFactory != null) {
            this.adapterFactory.dispose();
            this.adapterFactory = null;
        }

        if (this.plugin != null) {
            this.plugin.dispose();
            this.plugin = null;
        }
    }

    @SuppressWarnings("unused")
    private synchronized void bindTenantSetup(TenantCustomizer action, Map<String, Object> config) {
        registeredTenantHandlers.put(ServiceUtil.getComparableForServiceRanking(config), action);
    }

    @SuppressWarnings("unused")
    private synchronized void unbindTenantSetup(TenantCustomizer action, Map<String, Object> config) {
        registeredTenantHandlers.remove(ServiceUtil.getComparableForServiceRanking(config));
    }

    private synchronized Collection<TenantCustomizer> getTenantHandlers() {
        return registeredTenantHandlers.values();
    }

    public Tenant getTenant(final String tenantId) {
        if (tenantId != null && tenantId.length() > 0) {
            return call(new ResourceResolverTask<Tenant>() {
                public Tenant call(ResourceResolver resolver) {
                    Resource tenantRes = getTenantResource(resolver, tenantId);
                    return (tenantRes != null) ? new TenantImpl(tenantRes) : null;
                }
            });
        }

        // in case of some problem
        return null;
    }

    public Iterator<Tenant> getTenants() {
        return getTenants(null);
    }

    public Iterator<Tenant> getTenants(final String tenantFilter) {
        final Filter filter;
        if (tenantFilter != null && tenantFilter.length() > 0) {
            try {
                filter = FrameworkUtil.createFilter(tenantFilter);
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        } else {
            filter = null;
        }

        Iterator<Tenant> result = call(new ResourceResolverTask<Iterator<Tenant>>() {
            public Iterator<Tenant> call(ResourceResolver resolver) {
                Resource tenantRootRes = resolver.getResource(tenantRootPath);

                List<Tenant> tenantList = new ArrayList<Tenant>();
                Iterator<Resource> tenantResourceList = tenantRootRes.listChildren();
                while (tenantResourceList.hasNext()) {
                    Resource tenantRes = tenantResourceList.next();

                    if (filter == null || filter.matches(ResourceUtil.getValueMap(tenantRes))) {
                        TenantImpl tenant = new TenantImpl(tenantRes);
                        tenantList.add(tenant);
                    }
                }
                return tenantList.iterator();
            }
        });

        if (result == null) {
            // no filter or no resource resolver for calling
            result = Collections.<Tenant> emptyList().iterator();
        }

        return result;
    }

    public Tenant create(final String tenantId, final Map<String, Object> properties) {
        return call(new ResourceResolverTask<Tenant>() {
            public Tenant call(ResourceResolver adminResolver) {
                try {
                    // create the tenant
                    Resource tenantRes = createTenantResource(adminResolver, tenantId, properties);
                    TenantImpl tenant = new TenantImpl(tenantRes);
                    customizeTenant(tenantRes, tenant);
                    adminResolver.commit();

                    // refresh tenant instance, as it copies property from
                    // resource
                    tenant.loadProperties(tenantRes);

                    return tenant;

                } catch (PersistenceException e) {
                    log.error("create: Failed creating Tenant {}", tenantId, e);
                } finally {
                    adminResolver.close();
                }

                // no new tenant in case of problems
                return null;
            }
        });
    }

    public void remove(final Tenant tenant) {
        call(new ResourceResolverTask<Void>() {
            public Void call(ResourceResolver resolver) {
                try {
                    Resource tenantRes = getTenantResource(resolver, tenant.getId());
                    if (tenantRes != null) {
                        // call tenant setup handler
                        for (TenantCustomizer ts : getTenantHandlers()) {
                            try {
                                ts.remove(tenant, resolver);
                            } catch (Exception e) {
                                log.info("removeTenant: Unexpected problem calling TenantCustomizer " + ts, e);
                            }
                        }

                        resolver.delete(tenantRes);
                        resolver.commit();
                    }
                } catch (PersistenceException e) {
                    log.error("remove({}): Cannot persist Tenant removal", tenant.getId(), e);
                }

                return null;
            }
        });
    }

    public void setProperty(final Tenant tenant, final String name, final Object value) {
        updateProperties(tenant, new PropertiesUpdater() {
            public void update(ModifiableValueMap properties) {
                if (value != null) {
                    properties.put(name, value);
                } else {
                    properties.remove(name);
                }
            }
        });
    }

    public void setProperties(final Tenant tenant, final Map<String, Object> properties) {
        updateProperties(tenant, new PropertiesUpdater() {
            public void update(ModifiableValueMap vm) {
                for (Entry<String, Object> entry : properties.entrySet()) {
                    if (entry.getValue() != null) {
                        vm.put(entry.getKey(), entry.getValue());
                    } else {
                        vm.remove(entry.getKey());
                    }
                }
            }
        });
    }

    public void removeProperties(final Tenant tenant, final String... propertyNames) {
        updateProperties(tenant, new PropertiesUpdater() {
            public void update(ModifiableValueMap properties) {
                for (String name : propertyNames) {
                    properties.remove(name);
                }
            }
        });
    }

    @SuppressWarnings("serial")
    private Resource createTenantResource(final ResourceResolver resolver, final String tenantId,
            final Map<String, Object> properties) throws PersistenceException {

        // check for duplicate first
        if (getTenantResource(resolver, tenantId) != null) {
            throw new PersistenceException("Tenant '" + tenantId + "' already exists");
        }

        Resource tenantRoot = resolver.getResource(tenantRootPath);

        if (tenantRoot == null) {
            Resource current = resolver.getResource("/");
            if (current == null) {
                throw new PersistenceException("Cannot get root Resource");
            }

            String[] segments = this.tenantRootPath.split("/");
            for (String segment : segments) {
                Resource child = current.getChild(segment);
                if (child == null) {
                    child = resolver.create(current, segment, new HashMap<String, Object>() {
                        {
                            put("jcr:primaryType", "sling:Folder");
                        }
                    });
                }
                current = child;
            }

            tenantRoot = current;
        }

        return resolver.create(tenantRoot, tenantId, properties);
    }

    private Resource getTenantResource(final ResourceResolver resolver, final String tenantId) {
        return resolver.getResource(tenantRootPath + "/" + tenantId);
    }

    private void customizeTenant(final Resource tenantRes, final Tenant tenant) {

        // call tenant setup handler
        Map<String, Object> tenantProps = tenantRes.adaptTo(ModifiableValueMap.class);
        if (tenantProps == null) {
            log.warn(
                "create({}): Cannot get ModifiableValueMap for new tenant; will not store changed properties of TenantCustomizers",
                tenant.getId());
            tenantProps = new HashMap<String, Object>();
        }

        for (TenantCustomizer ts : getTenantHandlers()) {
            try {
                Map<String, Object> props = ts.setup(tenant, tenantRes.getResourceResolver());
                if (props != null) {
                    tenantProps.putAll(props);
                }
            } catch (Exception e) {
                log.info("addTenant: Unexpected problem calling TenantCustomizer " + ts, e);
            }
        }
    }

    private <T> T call(ResourceResolverTask<T> task) {
        ResourceResolver resolver = null;
        T result = null;

        try {
            resolver = factory.getAdministrativeResourceResolver(null);
            result = task.call(resolver);
        } catch (LoginException le) {
            // unexpected, thus ignore
        } finally {
            if (resolver != null) {
                resolver.close();
            }
        }

        return result;
    }

    private void updateProperties(final Tenant tenant, final PropertiesUpdater updater) {
        call(new ResourceResolverTask<Void>() {
            public Void call(ResourceResolver resolver) {
                try {
                    Resource tenantRes = getTenantResource(resolver, tenant.getId());
                    if (tenantRes != null) {
                        updater.update(tenantRes.adaptTo(ModifiableValueMap.class));

                        //refresh so that customizer gets a refreshed tenant instance
                        if (tenant instanceof TenantImpl) {
                            ((TenantImpl) tenant).loadProperties(tenantRes);
                        }

                        customizeTenant(tenantRes, tenant);
                        resolver.commit();

                        if (tenant instanceof TenantImpl) {
                            ((TenantImpl) tenant).loadProperties(tenantRes);
                        }
                    }
                } catch (PersistenceException pe) {
                    log.error("setProperty({}): Cannot persist Tenant removal", tenant.getId(), pe);
                }

                return null;
            }
        });
    }

    private static interface ResourceResolverTask<T> {
        T call(ResourceResolver resolver);
    }

    private static interface PropertiesUpdater {
        void update(ModifiableValueMap properties);
    }
}
