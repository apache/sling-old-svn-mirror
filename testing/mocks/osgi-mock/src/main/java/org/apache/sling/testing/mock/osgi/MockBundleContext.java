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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.testing.mock.osgi.OsgiMetadataUtil.Reference;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtil.ReferenceInfo;
import org.apache.sling.testing.mock.osgi.OsgiServiceUtil.ServiceInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Mock {@link BundleContext} implementation.
 */
class MockBundleContext implements BundleContext {

    private final MockBundle bundle;
    private final SortedSet<MockServiceRegistration> registeredServices = new TreeSet<MockServiceRegistration>();
    private final List<ServiceListener> serviceListeners = new ArrayList<ServiceListener>();
    private final List<BundleListener> bundleListeners = new ArrayList<BundleListener>();

    public MockBundleContext() {
        this.bundle = new MockBundle(this);
    }

    @Override
    public Bundle getBundle() {
        return this.bundle;
    }

    @Override
    public Filter createFilter(final String s) {
        // return filter that denies all
        return new MockFilter();
    }

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
    public ServiceRegistration registerService(final String[] clazzes, final Object service, final Dictionary properties) {
        MockServiceRegistration registration = new MockServiceRegistration(this.bundle, clazzes, service, properties, this);
        handleRefsUpdateOnRegister(registration);
        this.registeredServices.add(registration);
        notifyServiceListeners(ServiceEvent.REGISTERED, registration.getReference());
        return registration;
    }
    
    /**
     * Check for already registered services that may be affected by the service registration - either
     * adding by additional optional references, or creating a conflict in the dependencies.
     * @param registration
     */
    private void handleRefsUpdateOnRegister(MockServiceRegistration registration) {
        List<ReferenceInfo> affectedReferences = OsgiServiceUtil.getMatchingReferences(registeredServices, registration);
        for (ReferenceInfo referenceInfo : affectedReferences) {
            Reference reference = referenceInfo.getReference();
            switch (reference.getCardinality()) {
            case MANDATORY_UNARY:
                throw new ReferenceViolationException("Mandatory unary reference of type " + reference.getInterfaceType() + " already fulfilled "
                        + "for service " + reference.getServiceClass().getName() + ", registration of new service with this interface failed.");
            case MANDATORY_MULTIPLE:
            case OPTIONAL_MULTIPLE:
            case OPTIONAL_UNARY:
                OsgiServiceUtil.invokeBindMethod(reference, registration.getService(),
                        new ServiceInfo(referenceInfo.getServiceRegistration()));
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

    /**
     * Check for already registered services that may be affected by the service unregistration - either
     * adding by removing optional references, or creating a conflict in the dependencies.
     * @param registration
     */
    private void handleRefsUpdateOnUnregister(MockServiceRegistration registration) {
        List<ReferenceInfo> affectedReferences = OsgiServiceUtil.getMatchingReferences(registeredServices, registration);
        for (ReferenceInfo referenceInfo : affectedReferences) {
            Reference reference = referenceInfo.getReference();
            switch (reference.getCardinality()) {
            case MANDATORY_UNARY:
                throw new ReferenceViolationException("Reference of type " + reference.getInterfaceType() + " "
                        + "for service " + reference.getServiceClass().getName() + " is mandatory unary, "
                        + "unregistration of service with this interface failed.");
            case MANDATORY_MULTIPLE:
            case OPTIONAL_MULTIPLE:
            case OPTIONAL_UNARY:
                // it is currently not checked if for a MANDATORY_MULTIPLE reference the last reference is removed
                OsgiServiceUtil.invokeUnbindMethod(reference, registration.getService(),
                        new ServiceInfo(referenceInfo.getServiceRegistration()));
                break;
            default:
                throw new RuntimeException("Unepxected cardinality: " + reference.getCardinality());
            }
        }
    }
    
    @Override
    public ServiceReference getServiceReference(final String clazz) {
        ServiceReference[] serviceRefs = getServiceReferences(clazz, null);
        if (serviceRefs != null && serviceRefs.length > 0) {
            return serviceRefs[0];
        } else {
            return null;
        }
    }

    @Override
    public ServiceReference[] getServiceReferences(final String clazz, final String filter) {
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

    @Override
    public ServiceReference[] getAllServiceReferences(final String clazz, final String filter) {
        // for now just do the same as getServiceReferences
        return getServiceReferences(clazz, filter);
    }

    @Override
    public Object getService(final ServiceReference serviceReference) {
        return ((MockServiceReference) serviceReference).getService();
    }

    @Override
    public boolean ungetService(final ServiceReference serviceReference) {
        // do nothing for now
        return false;
    }

    @Override
    public void addServiceListener(final ServiceListener serviceListener) {
        addServiceListener(serviceListener, null);
    }

    @Override
    public void addServiceListener(final ServiceListener serviceListener, final String s) {
        if (!serviceListeners.contains(serviceListener)) {
            serviceListeners.add(serviceListener);
        }
    }

    @Override
    public void removeServiceListener(final ServiceListener serviceListener) {
        serviceListeners.remove(serviceListener);
    }

    private void notifyServiceListeners(int eventType, ServiceReference serviceReference) {
        final ServiceEvent event = new ServiceEvent(eventType, serviceReference);
        for (ServiceListener serviceListener : serviceListeners) {
            serviceListener.serviceChanged(event);
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

    Object locateService(final String name, final ServiceReference reference) {
        for (MockServiceRegistration serviceRegistration : this.registeredServices) {
            if (serviceRegistration.getReference() == reference) {
                return serviceRegistration.getService();
            }
        }
        return null;
    }

    @Override
    public Bundle[] getBundles() {
        return new Bundle[0];
    }

    // --- unsupported operations ---
    @Override
    public String getProperty(final String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle installBundle(final String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle installBundle(final String s, final InputStream inputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getBundle(final long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getDataFile(final String s) {
        throw new UnsupportedOperationException();
    }

}
