/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.models.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.models.annotations.Model;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelPackageBundleListener implements BundleTrackerCustomizer {

    private static final String HEADER = "Sling-Model-Packages";

    private static final Logger log = LoggerFactory.getLogger(ModelPackageBundleListener.class);
    
    private final BundleContext bundleContext;

    private final BundleTracker bundleTracker;

    private final AdapterFactory factory;
    
    public ModelPackageBundleListener(BundleContext bundleContext, AdapterFactory factory) {
        this.bundleContext = bundleContext;
        this.factory = factory;
        this.bundleTracker = new BundleTracker(bundleContext, Bundle.ACTIVE, this);
        this.bundleTracker.open();
    }
    
    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        List<ServiceRegistration> regs = new ArrayList<ServiceRegistration>();

        Dictionary<?, ?> headers = bundle.getHeaders();
        String packageList = PropertiesUtil.toString(headers.get(HEADER), null);
        if (packageList != null) {

            packageList = StringUtils.deleteWhitespace(packageList);
            String[] packages = packageList.split(",");
            for (String singlePackage : packages) {
                @SuppressWarnings("unchecked")
                Enumeration<URL> classUrls = bundle.findEntries("/" + singlePackage.replace('.', '/'), "*.class",
                        true);

                if (classUrls == null) {
                    log.warn("No adaptable classes found in package {}, ignoring", singlePackage);
                    continue;
                }

                while (classUrls.hasMoreElements()) {
                    URL url = classUrls.nextElement();
                    String className = toClassName(url);
                    try {
                        Class<?> clazz = bundle.loadClass(className);
                        Model annotation = clazz.getAnnotation(Model.class);
                        if (annotation != null) {
                            Class<?>[] adaptables = annotation.adaptables();
                            String[] classNames = toStringArray(adaptables);
                            Dictionary<String, Object> registrationProps = new Hashtable<String, Object>();
                            registrationProps.put(AdapterFactory.ADAPTER_CLASSES, className);
                            registrationProps.put(AdapterFactory.ADAPTABLE_CLASSES, classNames);
                            ServiceRegistration reg = bundleContext.registerService(AdapterFactory.SERVICE_NAME,
                                    factory, registrationProps);
                            regs.add(reg);
                        }
                    } catch (ClassNotFoundException e) {
                        log.warn("Unable to load class", e);
                    }

                }
            }
        }
        return regs.toArray(new ServiceRegistration[0]);
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        if (object instanceof ServiceRegistration[]) {
            for (ServiceRegistration reg : (ServiceRegistration[]) object) {
                reg.unregister();
            }
        }
    }

    public synchronized void unregisterAll() {
        this.bundleTracker.close();
    }

    /** Convert class URL to class name */
    private String toClassName(URL url) {
        final String f = url.getFile();
        final String cn = f.substring(1, f.length() - ".class".length());
        return cn.replace('/', '.');
    }

    private String[] toStringArray(Class<?>[] classes) {
        String[] arr = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            arr[i] = classes[i].getName();
        }
        return arr;
    }

}
