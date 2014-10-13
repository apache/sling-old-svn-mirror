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
package org.apache.sling.testing.mock.sling.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.impl.ModelAdapterFactory;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;
import org.reflections.Reflections;

/**
 * Mock {@link ModelAdapterFactory} implementation.
 */
@Component(inherit = false)
@Service(AdapterFactory.class)
public final class MockModelAdapterFactory extends ModelAdapterFactory {

    private final BundleContext bundleContext;

    /**
     * @param componentContext OSGi component context
     */
    public MockModelAdapterFactory(ComponentContext componentContext) {
        bundleContext = componentContext.getBundleContext();

        // register service listener to collect injectors
        // this allows detecting injectors even if they are registered after
        // this bundle
        // (which is otherwise currently not supported in the osgi mock
        // environment)
        bundleContext.addServiceListener(new InjectorServiceListener());

        // activate service in simulated OSGi environment
        activate(componentContext);
    }

    /**
     * Constructor with default component context
     */
    public MockModelAdapterFactory() {
        this(MockOsgi.newComponentContext());
    }

    private class InjectorServiceListener implements ServiceListener {

        @Override
        public void serviceChanged(ServiceEvent event) {
            Object service = bundleContext.getService(event.getServiceReference());
            if (service instanceof Injector) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    bindInjector((Injector) service, getServiceProperties(event.getServiceReference()));
                } else if (event.getType() == ServiceEvent.UNREGISTERING) {
                    unbindInjector((Injector) service, getServiceProperties(event.getServiceReference()));
                }
            }
            if (service instanceof InjectAnnotationProcessorFactory) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    bindInjectAnnotationProcessorFactory((InjectAnnotationProcessorFactory) service,
                            getServiceProperties(event.getServiceReference()));
                } else if (event.getType() == ServiceEvent.UNREGISTERING) {
                    unbindInjectAnnotationProcessorFactory((InjectAnnotationProcessorFactory) service,
                            getServiceProperties(event.getServiceReference()));
                }
            }
            if (service instanceof ImplementationPicker) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    bindImplementationPicker((ImplementationPicker) service,
                            getServiceProperties(event.getServiceReference()));
                } else if (event.getType() == ServiceEvent.UNREGISTERING) {
                    unbindImplementationPicker((ImplementationPicker) service,
                            getServiceProperties(event.getServiceReference()));
                }
            }
        }

        private Map<String, Object> getServiceProperties(ServiceReference reference) {
            Map<String, Object> props = new HashMap<String, Object>();
            String[] propertyKeys = reference.getPropertyKeys();
            for (String key : propertyKeys) {
                props.put(key, reference.getProperty(key));
            }
            return props;
        }

    }

    /**
     * Scan classpaths for given package name (and sub packages) to scan for and
     * register all classes with @Model annotation.
     * @param packageName Java package name
     */
    public void addModelsForPackage(String packageName) {
        Bundle bundle = new ModelsPackageBundle(packageName, Bundle.ACTIVE);
        BundleEvent event = new BundleEvent(BundleEvent.STARTED, bundle);
        MockOsgi.sendBundleEvent(this.bundleContext, event);
    }

    @SuppressWarnings("unused")
    private class ModelsPackageBundle implements Bundle {

        private final String packageName;
        private final int state;

        public ModelsPackageBundle(String packageName, int state) {
            this.packageName = packageName;
            this.state = state;
        }

        @Override
        public int getState() {
            return this.state;
        }

        @Override
        public Dictionary getHeaders() {
            Dictionary<String, Object> headers = new Hashtable<String, Object>();
            headers.put("Sling-Model-Packages", this.packageName);
            return headers;
        }

        @Override
        public Enumeration findEntries(String path, String filePattern, boolean recurse) {
            Reflections reflections = new Reflections(this.packageName);
            Set<Class<?>> types = reflections.getTypesAnnotatedWith(Model.class);
            Vector<URL> urls = new Vector<URL>(); // NOPMD
            try {
                for (Class<?> type : types) {
                    urls.add(new URL("file:/" + type.getName().replace('.', '/') + ".class"));
                }
            } catch (MalformedURLException ex) {
                throw new RuntimeException("Malformed URL.", ex);
            }
            return urls.elements();
        }

        @Override
        public Class loadClass(String name) throws ClassNotFoundException {
            return getClass().getClassLoader().loadClass(name);
        }

        @Override
        public BundleContext getBundleContext() {
            return bundleContext;
        }

        @Override
        public void start(int options) throws BundleException {
            // do nothing
        }

        @Override
        public void start() throws BundleException {
            // do nothing
        }

        @Override
        public void stop(int options) throws BundleException {
            // do nothing
        }

        @Override
        public void stop() throws BundleException {
            // do nothing
        }

        @Override
        public void update(InputStream input) throws BundleException {
            // do nothing
        }

        @Override
        public void update() throws BundleException {
            // do nothing
        }

        @Override
        public void uninstall() throws BundleException {
            // do nothing
        }

        @Override
        public long getBundleId() {
            return 0;
        }

        @Override
        public String getLocation() {
            return null;
        }

        @Override
        public ServiceReference[] getRegisteredServices() { // NOPMD
            return null;
        }

        @Override
        public ServiceReference[] getServicesInUse() { // NOPMD
            return null;
        }

        @Override
        public boolean hasPermission(Object permission) {
            return false;
        }

        @Override
        public URL getResource(String name) {
            return null;
        }

        @Override
        public Dictionary getHeaders(String locale) {
            return null;
        }

        @Override
        public String getSymbolicName() {
            return null;
        }

        @Override
        public Enumeration getResources(String name) throws IOException {
            return null;
        }

        @Override
        public Enumeration getEntryPaths(String path) {
            return null;
        }

        @Override
        public URL getEntry(String path) {
            return null;
        }

        @Override
        public long getLastModified() {
            return 0;
        }

        // this is part of org.osgi 4.2.0
        public Map getSignerCertificates(int signersType) {
            return null;
        }

        // this is part of org.osgi 4.2.0
        public Version getVersion() {
            return null;
        }

    }

}
