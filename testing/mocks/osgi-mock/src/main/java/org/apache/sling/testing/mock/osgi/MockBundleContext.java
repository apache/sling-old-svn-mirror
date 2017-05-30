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
package org.apache.sling.testing.mock.osgi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.framework.FilterImpl;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.Reference;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtil.ReferenceInfo;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtil.ServiceInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

/**
 * Mock {@link BundleContext} implementation.
 */
class MockBundleContext implements BundleContext {

    private final MockBundle bundle;
    private final SortedSet<MockServiceRegistration> registeredServices = new ConcurrentSkipListSet<MockServiceRegistration>();
    private final Map<ServiceListener, Filter> serviceListeners = new ConcurrentHashMap<ServiceListener, Filter>();
    private final Queue<BundleListener> bundleListeners = new ConcurrentLinkedQueue<BundleListener>();
    private final ConfigurationAdmin configAdmin = new MockConfigurationAdmin();
    private File dataFileBaseDir;
    
    private final Bundle systemBundle;

    public MockBundleContext() {
        this.systemBundle = new MockBundle(this, Constants.SYSTEM_BUNDLE_ID);
        this.bundle = new MockBundle(this);
        
        // register configuration admin by default
        registerService(ConfigurationAdmin.class.getName(), configAdmin, null);
    }

    @Override
    public Bundle getBundle() {
        return this.bundle;
    }

    @Override
    public Filter createFilter(final String s) throws InvalidSyntaxException {
        if (s == null) {
            return new MatchAllFilter();
        }
        else {
            return new FilterImpl(s);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceRegistration registerService(final String clazz, final Object service, final Dictionary properties) {
        String[] clazzes;
        if (StringUtils.isBlank(clazz)) {
            clazzes = new String[0];
        } else {
            clazzes = new String[] { clazz };
        }
        return registerService(clazzes, service, properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
        return registerService(clazz.getName(), service, properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceRegistration registerService(final String[] clazzes, final Object service, final Dictionary properties) {
        Dictionary<String, Object> mergedPropertes = MapMergeUtil.propertiesMergeWithOsgiMetadata(service, configAdmin, properties);
        MockServiceRegistration registration = new MockServiceRegistration(this.bundle, clazzes, service, mergedPropertes, this);
        this.registeredServices.add(registration);
        handleRefsUpdateOnRegister(registration);
        notifyServiceListeners(ServiceEvent.REGISTERED, registration.getReference());
        return registration;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, ServiceFactory<S> factory, Dictionary<String, ?> properties) {
        return registerService(clazz.getName(), factory, properties);
    }
    
    /**
     * Check for already registered services that may be affected by the service registration - either
     * adding by additional optional references, or creating a conflict in the dependencies.
     * @param registration
     */
    private void handleRefsUpdateOnRegister(MockServiceRegistration registration) {
        
        // handle DYNAMIC references to this registration
        List<ReferenceInfo> affectedDynamicReferences = OsgiServiceUtil.getMatchingDynamicReferences(registeredServices, registration);
        for (ReferenceInfo referenceInfo : affectedDynamicReferences) {
            Reference reference = referenceInfo.getReference();
            if (reference.matchesTargetFilter(registration.getReference())) {
                switch (reference.getCardinality()) {
                case MANDATORY_UNARY:
                    throw new ReferenceViolationException("Mandatory unary reference of type " + reference.getInterfaceType() + " already fulfilled "
                            + "for service " + reference.getServiceClass().getName() + ", registration of new service with this interface failed.");
                case MANDATORY_MULTIPLE:
                case OPTIONAL_MULTIPLE:
                case OPTIONAL_UNARY:
                    OsgiServiceUtil.invokeBindMethod(reference, referenceInfo.getServiceRegistration().getService(),
                            new ServiceInfo(registration));
                    break;
                default:
                    throw new RuntimeException("Unepxected cardinality: " + reference.getCardinality());
                }
            }
        }

        // handle STATIC+GREEDY references to this registration
        List<ReferenceInfo> affectedStaticGreedyReferences = OsgiServiceUtil.getMatchingStaticGreedyReferences(registeredServices, registration);
        for (ReferenceInfo referenceInfo : affectedStaticGreedyReferences) {
            Reference reference = referenceInfo.getReference();
            switch (reference.getCardinality()) {
            case MANDATORY_UNARY:
                throw new ReferenceViolationException("Mandatory unary reference of type " + reference.getInterfaceType() + " already fulfilled "
                        + "for service " + reference.getServiceClass().getName() + ", registration of new service with this interface failed.");
            case MANDATORY_MULTIPLE:
            case OPTIONAL_MULTIPLE:
            case OPTIONAL_UNARY:
                restartService(referenceInfo.getServiceRegistration());
                break;
            default:
                throw new RuntimeException("Unepxected cardinality: " + reference.getCardinality());
            }
        }
    }
    
    void unregisterService(MockServiceRegistration registration) {
        this.registeredServices.remove(registration);
        handleRefsUpdateOnUnregister(registration);
        notifyServiceListeners(ServiceEvent.UNREGISTERING, registration.getReference());
    }
    
    @SuppressWarnings("unchecked")
    void restartService(MockServiceRegistration registration) {
        // get current service properties
        Class<?> serviceClass = registration.getService().getClass();
        Map<String,Object> properties = MapUtil.toMap(registration.getProperties());
        
        // deactivate & unregister service
        MockOsgi.deactivate(registration.getService(), this);
        unregisterService(registration);
        
        // newly create and register service
        Object newService;
        try {
            newService = serviceClass.newInstance();
        }
        catch (InstantiationException e) {
            throw new RuntimeException("Unable to instantiate service: " + serviceClass);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to access service class: " + serviceClass);
        }
        MockOsgi.injectServices(newService, this);
        MockOsgi.activate(newService, this, properties);
        registerService(serviceClass.getName(), newService, MapUtil.toDictionary(properties));
    }

    /**
     * Check for already registered services that may be affected by the service unregistration - either
     * adding by removing optional references, or creating a conflict in the dependencies.
     * @param registration
     */
    private void handleRefsUpdateOnUnregister(MockServiceRegistration registration) {

        // handle DYNAMIC references to this registration
        List<ReferenceInfo> affectedDynamicReferences = OsgiServiceUtil.getMatchingDynamicReferences(registeredServices, registration);
        for (ReferenceInfo referenceInfo : affectedDynamicReferences) {
            Reference reference = referenceInfo.getReference();
            if (reference.matchesTargetFilter(registration.getReference())) {
                switch (reference.getCardinality()) {
                case MANDATORY_UNARY:
                    throw new ReferenceViolationException("Reference of type " + reference.getInterfaceType() + " "
                            + "for service " + reference.getServiceClass().getName() + " is mandatory unary, "
                            + "unregistration of service with this interface failed.");
                case MANDATORY_MULTIPLE:
                case OPTIONAL_MULTIPLE:
                case OPTIONAL_UNARY:
                    // it is currently not checked if for a MANDATORY_MULTIPLE reference the last reference is removed
                    OsgiServiceUtil.invokeUnbindMethod(reference, referenceInfo.getServiceRegistration().getService(),
                            new ServiceInfo(registration));
                    break;
                default:
                    throw new RuntimeException("Unepxected cardinality: " + reference.getCardinality());
                }
            }
        }

        // handle STATIC+GREEDY references to this registration
        List<ReferenceInfo> affectedStaticGreedyReferences = OsgiServiceUtil.getMatchingStaticGreedyReferences(registeredServices, registration);
        for (ReferenceInfo referenceInfo : affectedStaticGreedyReferences) {
            Reference reference = referenceInfo.getReference();
            switch (reference.getCardinality()) {
            case MANDATORY_UNARY:
                throw new ReferenceViolationException("Reference of type " + reference.getInterfaceType() + " "
                        + "for service " + reference.getServiceClass().getName() + " is mandatory unary, "
                        + "unregistration of service with this interface failed.");
            case MANDATORY_MULTIPLE:
            case OPTIONAL_MULTIPLE:
            case OPTIONAL_UNARY:
                restartService(referenceInfo.getServiceRegistration());
                break;
            default:
                throw new RuntimeException("Unepxected cardinality: " + reference.getCardinality());
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public ServiceReference getServiceReference(final String clazz) {
        try {
            ServiceReference[] serviceRefs = getServiceReferences(clazz, null);
            if (serviceRefs != null && serviceRefs.length > 0) {
                return serviceRefs[0];
            }
        }
        catch (InvalidSyntaxException ex) {
            // should not happen
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
        return getServiceReference(clazz.getName());
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceReference[] getServiceReferences(final String clazz, final String filter) throws InvalidSyntaxException {
        Set<ServiceReference> result = new TreeSet<ServiceReference>();
        for (MockServiceRegistration serviceRegistration : this.registeredServices) {
            if (serviceRegistration.matches(clazz, filter)) {
                result.add(serviceRegistration.getReference());
            }
        }
        if (result.isEmpty()) {
            return null;
        } else {
            return result.toArray(new ServiceReference[result.size()]);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter) throws InvalidSyntaxException {
        ServiceReference<S>[] result = getServiceReferences(clazz.getName(), filter);
        if (result == null) {
            return ImmutableList.<ServiceReference<S>>of();
        }
        else {
            return ImmutableList.<ServiceReference<S>>copyOf(result);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServiceReference[] getAllServiceReferences(final String clazz, final String filter) throws InvalidSyntaxException {
        // for now just do the same as getServiceReferences
        return getServiceReferences(clazz, filter);
    }

    @Override
    public <S> S getService(final ServiceReference<S> serviceReference) {
        return ((MockServiceReference<S>)serviceReference).getService();
    }

    @Override
    public boolean ungetService(final ServiceReference serviceReference) {
        // do nothing for now
        return false;
    }

    @Override
    public void addServiceListener(final ServiceListener serviceListener) {
        try {
            addServiceListener(serviceListener, null);
        }
        catch (InvalidSyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addServiceListener(final ServiceListener serviceListener, final String filter) throws InvalidSyntaxException {
        serviceListeners.put(serviceListener, createFilter(filter));
    }

    @Override
    public void removeServiceListener(final ServiceListener serviceListener) {
        serviceListeners.remove(serviceListener);
    }

    private void notifyServiceListeners(int eventType, ServiceReference serviceReference) {
        final ServiceEvent event = new ServiceEvent(eventType, serviceReference);
        for ( Map.Entry<ServiceListener, Filter> entry : serviceListeners.entrySet()) {
            if ( entry.getValue() == null || entry.getValue().match(serviceReference)) {
                entry.getKey().serviceChanged(event);
            }
        }
    }

    @Override
    public void addBundleListener(final BundleListener bundleListener) {
        if (!bundleListeners.contains(bundleListener)) {
            bundleListeners.add(bundleListener);
        }
    }

    @Override
    public void removeBundleListener(final BundleListener bundleListener) {
        bundleListeners.remove(bundleListener);
    }

    void sendBundleEvent(BundleEvent bundleEvent) {
        for (BundleListener bundleListener : bundleListeners) {
            bundleListener.bundleChanged(bundleEvent);
        }
    }

    @Override
    public void addFrameworkListener(final FrameworkListener frameworkListener) {
        // accept method, but ignore it
    }

    @Override
    public void removeFrameworkListener(final FrameworkListener frameworkListener) {
        // accept method, but ignore it
    }

    @SuppressWarnings("unchecked")
    <S> S locateService(final String name, final ServiceReference<S> reference) {
        for (MockServiceRegistration<?> serviceRegistration : this.registeredServices) {
            if (serviceRegistration.getReference() == reference) {
                return (S)serviceRegistration.getService();
            }
        }
        return null;
    }

    @Override
    public Bundle[] getBundles() {
        return new Bundle[0];
    }

    @Override
    public String getProperty(final String s) {
        // no mock implementation, simulate that no property is found and return null
        return null;
    }
    
    @Override
    public File getDataFile(final String path) {
        if (path == null) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
        synchronized (this) {
            if (dataFileBaseDir == null) {
                dataFileBaseDir = Files.createTempDir();
            }
        }
        if (path.isEmpty()) { 
            return dataFileBaseDir;
        }
        else {
            return new File(dataFileBaseDir, path);
        }
    }

    /**
     * Deactivates all bundles registered in this mocked bundle context.
     */
    public void shutdown() {
        for (MockServiceRegistration<?> serviceRegistration : ImmutableList.copyOf(registeredServices).reverse()) {
            try {
                MockOsgi.deactivate(serviceRegistration.getService(), this, serviceRegistration.getProperties());
            }
            catch (NoScrMetadataException ex) {
                // ignore, no deactivate method is available then
            }
        }
        if (dataFileBaseDir != null) {
            try {
                FileUtils.deleteDirectory(dataFileBaseDir);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    public Bundle getBundle(final long bundleId) {
        if (bundleId == Constants.SYSTEM_BUNDLE_ID) {
            return systemBundle;
        }
        // otherwise return null - no bundle found
        return null;
    }

    @Override
    public Bundle getBundle(String location) {
        if (StringUtils.equals(location, Constants.SYSTEM_BUNDLE_LOCATION)) {
            return systemBundle;
        }
        // otherwise return null - no bundle found
        return null;
    }

    // --- unsupported operations ---
    @Override
    public Bundle installBundle(final String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle installBundle(final String s, final InputStream inputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) {
        throw new UnsupportedOperationException();
    }

}
