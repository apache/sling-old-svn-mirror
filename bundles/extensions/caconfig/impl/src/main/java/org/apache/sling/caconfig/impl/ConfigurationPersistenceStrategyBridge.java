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
package org.apache.sling.caconfig.impl;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.caconfig.spi.ConfigurationCollectionPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistData;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy;
import org.apache.sling.caconfig.spi.ConfigurationPersistenceStrategy2;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.ServiceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Bridges services implementing the deprecated {@link ConfigurationPersistenceStrategy} interface
 * to the {@link ConfigurationPersistenceStrategy2} interface for backwards compatibility.
 */
@Component(reference={
        @Reference(name="configurationPersistenceStrategy", service=ConfigurationPersistenceStrategy.class,
                bind="bindConfigurationPersistenceStrategy", unbind="unbindConfigurationPersistenceStrategy",
                cardinality=ReferenceCardinality.MULTIPLE,
                policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
})
@SuppressWarnings("deprecation")
public final class ConfigurationPersistenceStrategyBridge {
    
    private volatile BundleContext bundleContext;
    private final ConcurrentMap<Comparable<Object>, ServiceRegistration<ConfigurationPersistenceStrategy2>> services = new ConcurrentHashMap<>();
    private final ConcurrentMap<Comparable<Object>, ServiceInfo> preActivateServices = new ConcurrentHashMap<>();
    
    protected void bindConfigurationPersistenceStrategy(ConfigurationPersistenceStrategy item, Map<String, Object> props) {
        ServiceInfo serviceInfo = new ServiceInfo(item, props);
        Comparable<Object> key = ServiceUtil.getComparableForServiceRanking(props, Order.ASCENDING);
        if (bundleContext != null) {
            services.put(key, registerBridgeService(serviceInfo));
        }
        else {
            preActivateServices.put(key, serviceInfo);
        }
    }
    
    protected void unbindConfigurationPersistenceStrategy(ConfigurationPersistenceStrategy item, Map<String, Object> props) {
        Comparable<Object> key = ServiceUtil.getComparableForServiceRanking(props, Order.ASCENDING);
        unregisterBridgeService(services.remove(key));
    }
    
    /**
     * Register {@link ConfigurationPersistenceStrategy2} bridge service for {@link ConfigurationPersistenceStrategy} service.
     * @param serviceInfo Service information
     * @return Service registration
     */
    private ServiceRegistration<ConfigurationPersistenceStrategy2> registerBridgeService(ServiceInfo serviceInfo) {
        return bundleContext.registerService(ConfigurationPersistenceStrategy2.class,
                new Adapter(serviceInfo.getService()), new Hashtable<>(serviceInfo.getProps()));
    }
    
    /**
     * Unregister {@link ConfigurationPersistenceStrategy2} bridge service.
     * @param service Service registration
     */
    private void unregisterBridgeService(ServiceRegistration<ConfigurationPersistenceStrategy2> service) {
        if (service != null) {
            service.unregister();
        }
    }
    
    @Activate
    private void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        for (Map.Entry<Comparable<Object>, ServiceInfo> entry : preActivateServices.entrySet()) {
            services.put(entry.getKey(), registerBridgeService(entry.getValue()));
        }
    }
   
    
    private static class ServiceInfo {
        private final ConfigurationPersistenceStrategy service;
        private final Map<String,Object> props;
        
        public ServiceInfo(ConfigurationPersistenceStrategy service, Map<String, Object> props) {
            this.service = service;
            this.props = props;
        }
        public ConfigurationPersistenceStrategy getService() {
            return service;
        }
        public Map<String, Object> getProps() {
            return props;
        }
    }
    
    
    /**
     * Adapter which delegates {@link ConfigurationPersistenceStrategy2} methods to a {@link ConfigurationPersistenceStrategy} service.
     */
    public static class Adapter implements ConfigurationPersistenceStrategy2 {
        private final ConfigurationPersistenceStrategy delegate;

        public Adapter(ConfigurationPersistenceStrategy delegate) {
            this.delegate = delegate;
        }
        
        /**
         * @return Implementation class of the original service.
         */
        public Class<?> getOriginalServiceClass() {
            return delegate.getClass();
        }

        @Override
        public Resource getResource(Resource resource) {
            return delegate.getResource(resource);
        }

        @Override
        public Resource getCollectionParentResource(Resource resource) {
            // with SPI/Impl 1.2 it was not possible to manipulate collection parent resource
            return resource;
        }

        @Override
        public Resource getCollectionItemResource(Resource resource) {
            return delegate.getResource(resource);
        }

        @Override
        public String getResourcePath(String resourcePath) {
            return delegate.getResourcePath(resourcePath);
        }

        @Override
        public String getCollectionParentResourcePath(String resourcePath) {
            // with SPI/Impl 1.2 it was not possible to manipulate collection parent resource
            return resourcePath;
        }

        @Override
        public String getCollectionItemResourcePath(String resourcePath) {
            return delegate.getResourcePath(resourcePath);
        }

        @Override
        public String getConfigName(String configName, String relatedConfigPath) {
            return delegate.getResourcePath(configName);
        }

        @Override
        public String getCollectionParentConfigName(String configName, String relatedConfigPath) {
            // with SPI/Impl 1.2 it was not possible to manipulate collection parent resource
            return configName;
        }

        @Override
        public String getCollectionItemConfigName(String configName, String relatedConfigPath) {
            return delegate.getResourcePath(configName);
        }

        @Override
        public boolean persistConfiguration(ResourceResolver resourceResolver, String configResourcePath,
                ConfigurationPersistData data) {
            return delegate.persistConfiguration(resourceResolver, configResourcePath, data);
        }

        @Override
        public boolean persistConfigurationCollection(ResourceResolver resourceResolver,
                String configResourceCollectionParentPath, ConfigurationCollectionPersistData data) {
            return delegate.persistConfigurationCollection(resourceResolver, configResourceCollectionParentPath, data);
        }

        @Override
        public boolean deleteConfiguration(ResourceResolver resourceResolver, String configResourcePath) {
            return delegate.deleteConfiguration(resourceResolver, configResourcePath);
        }
        
    }
    
}
