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
package org.apache.sling.resourceresolver.impl.legacy;

import static org.apache.sling.api.resource.QueriableResourceProvider.LANGUAGES;
import static org.apache.sling.api.resource.ResourceProvider.OWNS_ROOTS;
import static org.apache.sling.api.resource.ResourceProvider.ROOTS;
import static org.apache.sling.api.resource.ResourceProvider.USE_RESOURCE_ACCESS_SECURITY;
import static org.apache.sling.api.resource.ResourceProviderFactory.PROPERTY_REQUIRED;
import static org.apache.sling.commons.osgi.PropertiesUtil.toBoolean;
import static org.apache.sling.spi.resource.provider.ResourceProvider.PROPERTY_ADAPTABLE;
import static org.apache.sling.spi.resource.provider.ResourceProvider.PROPERTY_ATTRIBUTABLE;
import static org.apache.sling.spi.resource.provider.ResourceProvider.PROPERTY_AUTHENTICATE;
import static org.apache.sling.spi.resource.provider.ResourceProvider.PROPERTY_MODIFIABLE;
import static org.apache.sling.spi.resource.provider.ResourceProvider.PROPERTY_NAME;
import static org.apache.sling.spi.resource.provider.ResourceProvider.PROPERTY_REFRESHABLE;
import static org.apache.sling.spi.resource.provider.ResourceProvider.PROPERTY_ROOT;
import static org.apache.sling.spi.resource.provider.ResourceProvider.PROPERTY_USE_RESOURCE_ACCESS_SECURITY;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.framework.Constants.SERVICE_RANKING;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.AttributableResourceProvider;
import org.apache.sling.api.resource.ModifyingResourceProvider;
import org.apache.sling.api.resource.RefreshableResourceProvider;
import org.apache.sling.api.resource.ResourceProvider;
import org.apache.sling.api.resource.ResourceProviderFactory;
import org.apache.sling.api.resource.runtime.dto.AuthType;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@SuppressWarnings("deprecation")
@Component
public class LegacyResourceProviderWhiteboard {

    public static final String ORIGINAL_SERVICE_PID = "original.service.pid";

    private Map<Object, List<ServiceRegistration>> registrations = new HashMap<>();

    @Reference(service = ResourceProvider.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindResourceProvider(final ServiceReference<ResourceProvider> ref) {
        final BundleContext bundleContext = ref.getBundle().getBundleContext();
        final ResourceProvider provider = bundleContext.getService(ref);
        if ( provider != null ) {
            final String[] propertyNames = ref.getPropertyKeys();
            final boolean ownsRoot = toBoolean(ref.getProperty(OWNS_ROOTS), false);

            final List<ServiceRegistration> newServices = new ArrayList<>();
            for (final String path : PropertiesUtil.toStringArray(ref.getProperty(ROOTS), new String[0])) {
                if ( path != null && !path.isEmpty() ) {
                    final Dictionary<String, Object> newProps = new Hashtable<>();
                    newProps.put(PROPERTY_AUTHENTICATE, AuthType.no.toString());
                    newProps.put(PROPERTY_MODIFIABLE, provider instanceof ModifyingResourceProvider);
                    newProps.put(PROPERTY_ADAPTABLE, provider instanceof Adaptable);
                    newProps.put(PROPERTY_ATTRIBUTABLE, provider instanceof AttributableResourceProvider);
                    newProps.put(PROPERTY_REFRESHABLE, provider instanceof RefreshableResourceProvider);
                    newProps.put(PROPERTY_NAME, provider.getClass().getName());
                    newProps.put(PROPERTY_ROOT, normalizePath(path));
                    if (ArrayUtils.contains(propertyNames, SERVICE_PID)) {
                        newProps.put(ORIGINAL_SERVICE_PID, ref.getProperty(SERVICE_PID));
                    }
                    if (ArrayUtils.contains(propertyNames, USE_RESOURCE_ACCESS_SECURITY)) {
                        newProps.put(PROPERTY_USE_RESOURCE_ACCESS_SECURITY, ref.getProperty(USE_RESOURCE_ACCESS_SECURITY));
                    }
                    if (ArrayUtils.contains(propertyNames, SERVICE_RANKING)) {
                        newProps.put(SERVICE_RANKING, ref.getProperty(SERVICE_RANKING));
                    }

                    String[] languages = PropertiesUtil.toStringArray(ref.getProperty(LANGUAGES), new String[0]);
                    ServiceRegistration reg = bundleContext.registerService(
                            org.apache.sling.spi.resource.provider.ResourceProvider.class.getName(),
                            new LegacyResourceProviderAdapter(provider, languages, ownsRoot), newProps);
                    newServices.add(reg);
                }
            }
            registrations.put(provider, newServices);
        }
    }

    protected void unbindResourceProvider(final ResourceProvider provider, final Map<String, Object> props) {
        for (ServiceRegistration r : registrations.remove(provider)) {
            r.unregister();
        }
    }

    @Reference(service = ResourceProviderFactory.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindResourceProviderFactory(final ServiceReference<ResourceProviderFactory> ref) {
        final BundleContext bundleContext = ref.getBundle().getBundleContext();
        final ResourceProviderFactory factory = bundleContext.getService(ref);
        if ( factory != null ) {
            final String[] propertyNames = ref.getPropertyKeys();
            final boolean ownsRoot = toBoolean(ref.getProperty(OWNS_ROOTS), false);

            final List<ServiceRegistration> newServices = new ArrayList<>();
            for (final String path : PropertiesUtil.toStringArray(ref.getProperty(ROOTS), new String[0])) {
                if ( path != null && !path.isEmpty() ) {
                    final Dictionary<String, Object> newProps = new Hashtable<>();
                    if (PropertiesUtil.toBoolean(ref.getProperty(PROPERTY_REQUIRED), false)) {
                        newProps.put(PROPERTY_AUTHENTICATE, AuthType.required.toString());
                    } else {
                        newProps.put(PROPERTY_AUTHENTICATE, AuthType.lazy.toString());
                    }
                    newProps.put(PROPERTY_MODIFIABLE, true);
                    newProps.put(PROPERTY_ADAPTABLE, true);
                    newProps.put(PROPERTY_ATTRIBUTABLE, true);
                    newProps.put(PROPERTY_REFRESHABLE, true);
                    newProps.put(PROPERTY_NAME, factory.getClass().getName());
                    newProps.put(PROPERTY_ROOT, normalizePath(path));
                    if (ArrayUtils.contains(propertyNames, SERVICE_PID)) {
                        newProps.put(ORIGINAL_SERVICE_PID, ref.getProperty(SERVICE_PID));
                    }
                    if (ArrayUtils.contains(propertyNames, USE_RESOURCE_ACCESS_SECURITY)) {
                        newProps.put(PROPERTY_USE_RESOURCE_ACCESS_SECURITY, ref.getProperty(USE_RESOURCE_ACCESS_SECURITY));
                    }
                    if (ArrayUtils.contains(propertyNames, SERVICE_RANKING)) {
                        newProps.put(SERVICE_RANKING, ref.getProperty(SERVICE_RANKING));
                    }
                    String[] languages = PropertiesUtil.toStringArray(ref.getProperty(LANGUAGES), new String[0]);
                    ServiceRegistration reg = bundleContext.registerService(
                            org.apache.sling.spi.resource.provider.ResourceProvider.class.getName(),
                            new LegacyResourceProviderFactoryAdapter(factory, languages, ownsRoot), newProps);
                    newServices.add(reg);
                }
            }
            registrations.put(factory, newServices);
        }
    }

    protected void unbindResourceProviderFactory(final ResourceProviderFactory factory,
            final Map<String, Object> props) {
        for (ServiceRegistration r : registrations.remove(factory)) {
            r.unregister();
        }
    }

    private static String normalizePath(final String path) {
        String result = path;
        result = StringUtils.removeEnd(path, "/");
        if (result != null && !result.startsWith("/")) {
            result = "/" + result;
        }
        return result;
    }
}
