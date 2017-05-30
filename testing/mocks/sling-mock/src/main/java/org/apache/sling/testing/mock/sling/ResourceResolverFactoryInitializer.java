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
package org.apache.sling.testing.mock.sling;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProvider;
import org.apache.sling.resourceresolver.impl.ResourceAccessSecurityTracker;
import org.apache.sling.resourceresolver.impl.ResourceResolverFactoryActivator;
import org.apache.sling.serviceusermapping.ServiceUserMapper;
import org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.apache.sling.testing.mock.osgi.MockEventAdmin;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;

/**
 * Initializes Sling Resource Resolver factories with JCR-resource mapping.
 */
class ResourceResolverFactoryInitializer {
    
    private ResourceResolverFactoryInitializer() {
        // static methods only
    }

    /**
     * Setup resource resolver factory.
     * @param slingRepository Sling repository. If null resource resolver factory is setup without any resource provider.
     * @param bundleContext Bundle context
     */
    public static ResourceResolverFactory setUp(SlingRepository slingRepository, 
            BundleContext bundleContext, NodeTypeMode nodeTypeMode) {
        
        if (slingRepository != null) {
            // register sling repository as OSGi service
            registerServiceIfNotPresent(bundleContext, SlingRepository.class, slingRepository);
            
            // register JCR node types found in classpath
            registerJcrNodeTypes(slingRepository, nodeTypeMode);
            
            // initialize JCR resource provider
            ensureJcrResourceProviderDependencies(bundleContext);
            initializeJcrResourceProvider(bundleContext);
        }
        
        // initialize resource resolver factory activator
        ensureResourceResolverFactoryActivatorDependencies(bundleContext);
        initializeResourceResolverFactoryActivator(bundleContext);

        ServiceReference<ResourceResolverFactory> factoryRef = bundleContext.getServiceReference(ResourceResolverFactory.class);
        if (factoryRef == null) {
            throw new IllegalStateException("Unable to get ResourceResolverFactory.");
        }
        return (ResourceResolverFactory)bundleContext.getService(factoryRef);
    }
    
    /**
     * Ensure dependencies for JcrResourceProvider are present.
     * @param bundleContext Bundle context
     */
    @SuppressWarnings("unchecked")
    private static void ensureJcrResourceProviderDependencies(BundleContext bundleContext) {
        if (bundleContext.getServiceReference(DynamicClassLoaderManager.class) == null) {
            bundleContext.registerService(DynamicClassLoaderManager.class, new MockDynamicClassLoaderManager(), null);
        }
        
        try {
            Class pathMapperClass = Class.forName("org.apache.sling.jcr.resource.internal.helper.jcr.PathMapper");
            registerServiceIfNotPresent(bundleContext, pathMapperClass, pathMapperClass.newInstance());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            // ignore - service was removed in org.apache.sling.jcr.resource 3.0.0
        }
    }
 
    /**
     * Initialize JCR resource provider.
     * @param bundleContext Bundle context
     */
    private static void initializeJcrResourceProvider(BundleContext bundleContext) {
        Dictionary<String, Object> config = new Hashtable<String, Object>();
        JcrResourceProvider provider = new JcrResourceProvider();
        MockOsgi.injectServices(provider, bundleContext);
        MockOsgi.activate(provider, bundleContext, config);
        bundleContext.registerService(ResourceProvider.class, provider, config);
    }
    
    /**
     * Ensure dependencies for ResourceResolverFactoryActivator are present.
     * @param bundleContext Bundle context
     */
    private static void ensureResourceResolverFactoryActivatorDependencies(BundleContext bundleContext) {
        Dictionary<String, Object> config = new Hashtable<String, Object>();
        config.put("user.mapping", bundleContext.getBundle().getSymbolicName() + "=admin");
        registerServiceIfNotPresent(bundleContext, ServiceUserMapper.class, new ServiceUserMapperImpl(), config);
        
        registerServiceIfNotPresent(bundleContext, ResourceAccessSecurityTracker.class, new ResourceAccessSecurityTracker());
        registerServiceIfNotPresent(bundleContext, EventAdmin.class, new MockEventAdmin());
    }
 
    /**
     * Initialize resource resolver factory activator.
     * @param bundleContext Bundle context
     */
    private static void initializeResourceResolverFactoryActivator(BundleContext bundleContext) {
        Dictionary<String, Object> config = new Hashtable<String, Object>();
        // do not required a specific resource provider (otherwise "NONE" will not work)
        config.put("resource.resolver.required.providers", "");
        config.put("resource.resolver.required.providernames", "");
        ResourceResolverFactoryActivator activator = new ResourceResolverFactoryActivator();
        MockOsgi.injectServices(activator, bundleContext);
        MockOsgi.activate(activator, bundleContext, config);
        bundleContext.registerService(ResourceResolverFactoryActivator.class.getName(), activator, config);
    }
    
    /**
     * Registers a service if the service class is found in classpath,
     * and if no service with this class is already registered.
     * @param className Service class name
     * @param serviceClass Service class
     * @param instance Service instance
     */
    private static <T> void registerServiceIfNotPresent(BundleContext bundleContext, Class<T> serviceClass, 
            T instance) {
        registerServiceIfNotPresent(bundleContext, serviceClass, instance, new Hashtable<String, Object>());
    }
    
    /**
     * Registers a service if the service class is found in classpath,
     * and if no service with this class is already registered.
     * @param className Service class name
     * @param serviceClass Service class
     * @param instance Service instance
     * @param config OSGi config
     */
    private static <T> void registerServiceIfNotPresent(BundleContext bundleContext, Class<T> serviceClass, 
            T instance, Dictionary<String, Object> config) {
        if (bundleContext.getServiceReference(serviceClass.getName()) == null) {
            MockOsgi.injectServices(instance, bundleContext);
            MockOsgi.activate(instance, bundleContext, config);
            bundleContext.registerService(serviceClass, instance, config);
        }
    }
    
    /**
     * Registers all JCR node types found in classpath.
     * @param slingRepository Sling repository
     */
    @SuppressWarnings("deprecation")
    private static void registerJcrNodeTypes(final SlingRepository slingRepository, 
            final NodeTypeMode nodeTypeMode) {
      Session session = null;
      try {
          session = slingRepository.loginAdministrative(null);
          NodeTypeDefinitionScanner.get().register(session, nodeTypeMode);
      }
      catch (RepositoryException ex) {
          throw new RuntimeException("Error registering JCR nodetypes: " + ex.getMessage(), ex);
      }
      finally {
          if (session != null) {
              session.logout();
          }
      }
    }
    
}
