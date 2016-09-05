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
package org.apache.sling.contextaware.config.impl.metadata;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects configuration annotation classes deployed by any bundle via OSGi extender pattern.
 */
@Component(immediate = true)
public class AnnotationClassDetector {
    
    static final String HEADER = "Sling-ContextAware-Config-Packages";
        
    private BundleTracker<ServiceRegistration[]> bundleTracker;
    
    private ConcurrentMap<String, Class<?>> annotationClasses = new ConcurrentHashMap<>();
    
    private static final Logger log = LoggerFactory.getLogger(AnnotationClassDetector.class);
        
    @Activate
    private void activate(BundleContext bundleContext) {
        ConfigPackageBundleTackerCustomizer bundlerTrackerCustomizer = new ConfigPackageBundleTackerCustomizer(bundleContext, this);
        bundleTracker = new BundleTracker<ServiceRegistration[]>(bundleContext, Bundle.ACTIVE, bundlerTrackerCustomizer);
        bundleTracker.open();
    }
    
    @Deactivate
    private void deactivate() {
        bundleTracker.close();
        bundleTracker = null;
    }
    
    public Map<String,Class<?>> getAnnotationClasses() {
        return annotationClasses;
    }

    void addClassMapping(Class<?> configClass) {
        annotationClasses.put(configClass.getName(), configClass);
    }

    void removeClassMapping(String configClassName) {
        annotationClasses.remove(configClassName);
    }
    

    static class ConfigPackageBundleTackerCustomizer implements BundleTrackerCustomizer<ServiceRegistration[]> {

        private final BundleContext bundleContext;
        private final AnnotationClassDetector adapterFactory;
        
        public ConfigPackageBundleTackerCustomizer(BundleContext bundleContext, AnnotationClassDetector adapterFactory) {
            this.bundleContext = bundleContext;
            this.adapterFactory = adapterFactory;
        }

        @Override
        public ServiceRegistration[] addingBundle(Bundle bundle, BundleEvent event) {
            Dictionary<String, String> headers = bundle.getHeaders();
            String packageList = headers.get(HEADER);
            if (packageList == null) {
                return null;
            }
            
            List<ServiceRegistration> regs = new ArrayList<ServiceRegistration>();

            packageList = StringUtils.deleteWhitespace(packageList);
            String[] packages = StringUtils.split(packageList, ",");
            for (String singlePackage : packages) {
                Enumeration<URL> classUrls = bundle.findEntries("/" + singlePackage.replace('.', '/'), "*.class", true);

                if (classUrls == null) {
                    log.warn("No configuration classes found in package {}, ignoring.", singlePackage);
                    continue;
                }

                while (classUrls.hasMoreElements()) {
                    String className = toClassName(classUrls.nextElement());
                    try {
                        Class<?> configType = bundle.loadClass(className);
                        
                        // TODO: check for specific annotation on config class
                        if (configType.isAnnotation()) {
                            log.debug("{}: Add configuration class {}", bundle.getSymbolicName(), className);
                            
                            adapterFactory.addClassMapping(configType);
                            ServiceRegistration reg = registerAdapterFactory(Resource.class, configType);
                            regs.add(reg);
                        }
                    }
                    catch (ClassNotFoundException ex) {
                        log.warn("Unable to load class: " + className, ex);
                    }

                }
            }
            return regs.toArray(new ServiceRegistration[regs.size()]);
       }

        /**
         * Convert class URL to class name.
         */
        private String toClassName(URL url) {
            final String f = url.getFile();
            final String cn = f.substring(1, f.length() - ".class".length());
            return cn.replace('/', '.');
        }
        
        /**
         * Registers an adapter factory for a annotated sling models class.
         * @param adapterTypes Adapter (either the class itself, or interface or superclass of it)
         * @param adaptableTypes Classes to adapt from
         * @param implType Type of the implementation class
         * @param condition Condition (optional)
         * @return Service registration
         */
        private ServiceRegistration registerAdapterFactory(Class<?> adaptableType, Class<?> adapterType) {
            Dictionary<String, Object> registrationProps = new Hashtable<String, Object>();
            registrationProps.put(AdapterFactory.ADAPTABLE_CLASSES, adaptableType.getName());
            registrationProps.put(AdapterFactory.ADAPTER_CLASSES, adapterType.getName());
            return bundleContext.registerService(AdapterFactory.SERVICE_NAME, adapterFactory, registrationProps);
        }

        @Override
        public void modifiedBundle(Bundle bundle, BundleEvent event, ServiceRegistration[] serviceRegistrations) {
            // nothing to do   
        }

        @Override
        public void removedBundle(Bundle bundle, BundleEvent event, ServiceRegistration[] serviceRegistrations) {
            for (ServiceRegistration reg : serviceRegistrations) {
                ServiceReference ref = reg.getReference();
                String adapterTypeName = PropertiesUtil.toString(ref.getProperty(AdapterFactory.ADAPTER_CLASSES), null);
                adapterFactory.removeClassMapping(adapterTypeName);
                reg.unregister();
            }
        }

    }

}
