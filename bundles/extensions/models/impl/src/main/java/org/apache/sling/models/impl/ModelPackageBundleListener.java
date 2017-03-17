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
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.ExporterOption;
import org.apache.sling.models.annotations.Exporters;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngineFactory;
import javax.servlet.Servlet;

public class ModelPackageBundleListener implements BundleTrackerCustomizer {

    static final String PACKAGE_HEADER = "Sling-Model-Packages";
    static final String CLASSES_HEADER = "Sling-Model-Classes";

    static final String PROP_EXPORTER_SERVLET_CLASS = "sling.models.exporter.servlet.class";
    static final String PROP_EXPORTER_SERVLET_NAME = "sling.models.exporter.servlet.name";
    
    /**
     * Service registration property for the adapter condition.
     */
    private static final String PROP_ADAPTER_CONDITION = "adapter.condition";

    /**
     * The model implementation class that initiated the service registration.
     */
    private static final String PROP_IMPLEMENTATION_CLASS = "models.adapter.implementationClass";

    private static final Logger log = LoggerFactory.getLogger(ModelPackageBundleListener.class);

    private final BundleContext bundleContext;

    private final BundleTracker bundleTracker;

    private final ModelAdapterFactory factory;
    
    private final AdapterImplementations adapterImplementations;

    private final BindingsValuesProvidersByContext bindingsValuesProvidersByContext;

    private final ScriptEngineFactory scriptEngineFactory;
    
    public ModelPackageBundleListener(BundleContext bundleContext,
                                      ModelAdapterFactory factory,
                                      AdapterImplementations adapterImplementations,
                                      BindingsValuesProvidersByContext bindingsValuesProvidersByContext) {
        this.bundleContext = bundleContext;
        this.factory = factory;
        this.adapterImplementations = adapterImplementations;
        this.bindingsValuesProvidersByContext = bindingsValuesProvidersByContext;
        this.scriptEngineFactory = new ExporterScriptEngineFactory(bundleContext.getBundle());
        this.bundleTracker = new BundleTracker(bundleContext, Bundle.ACTIVE, this);
        this.bundleTracker.open();
    }
    
    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        List<ServiceRegistration> regs = new ArrayList<ServiceRegistration>();

        Dictionary<?, ?> headers = bundle.getHeaders();
        String packageList = PropertiesUtil.toString(headers.get(PACKAGE_HEADER), null);
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
                    analyzeClass(bundle, className, regs);

                }
            }
        }
        String classesList = PropertiesUtil.toString(headers.get(CLASSES_HEADER), null);
        if (classesList != null) {
            classesList = StringUtils.deleteWhitespace(classesList);
            String[] classes = classesList.split(",");
            for (String className : classes) {
                analyzeClass(bundle, className, regs);
            }
        }

        return regs.toArray(new ServiceRegistration[0]);
    }

    private void analyzeClass(Bundle bundle, String className, List<ServiceRegistration> regs) {
        try {
            Class<?> implType = bundle.loadClass(className);
            Model annotation = implType.getAnnotation(Model.class);
            if (annotation != null) {

                // get list of adapters from annotation - if not given use annotated class itself
                Class<?>[] adapterTypes = annotation.adapters();
                if (adapterTypes.length == 0) {
                    adapterTypes = new Class<?>[] { implType };
                } else if (!ArrayUtils.contains(adapterTypes, implType)) {
                    adapterTypes = (Class<?>[]) ArrayUtils.add(adapterTypes, implType);
                }
                // register adapter only if given adapters are valid
                if (validateAdapterClasses(implType, adapterTypes)) {
                    for (Class<?> adapterType : adapterTypes) {
                        adapterImplementations.add(adapterType, implType);
                    }
                    ServiceRegistration reg = registerAdapterFactory(adapterTypes, annotation.adaptables(), implType, annotation.condition());
                    regs.add(reg);

                    String[] resourceTypes = annotation.resourceType();
                    for (String resourceType : resourceTypes) {
                        if (StringUtils.isNotEmpty(resourceType)) {
                            for (Class<?> adaptable : annotation.adaptables()) {
                                adapterImplementations.registerModelToResourceType(bundle, resourceType, adaptable, implType);
                                ExportServlet.ExportedObjectAccessor accessor = null;
                                if (adaptable == Resource.class) {
                                    accessor = new ExportServlet.ResourceAccessor(implType);
                                } else if (adaptable == SlingHttpServletRequest.class) {
                                    accessor = new ExportServlet.RequestAccessor(implType);
                                }
                                Exporter exporterAnnotation = implType.getAnnotation(Exporter.class);
                                if (exporterAnnotation != null) {
                                    registerExporter(bundle, implType, resourceType, exporterAnnotation, regs, accessor);
                                }
                                Exporters exportersAnnotation = implType.getAnnotation(Exporters.class);
                                if (exportersAnnotation != null) {
                                    for (Exporter ann : exportersAnnotation.value()) {
                                        registerExporter(bundle, implType, resourceType, ann, regs, accessor);
                                    }
                                }

                            }
                        }
                    }
                }

            }
        } catch (ClassNotFoundException e) {
            log.warn("Unable to load class", e);
        }
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        if (object instanceof ServiceRegistration[]) {
            for (ServiceRegistration reg : (ServiceRegistration[]) object) {
                ServiceReference ref = reg.getReference();
                String[] adapterTypeNames = PropertiesUtil.toStringArray(ref.getProperty(AdapterFactory.ADAPTER_CLASSES));
                if (adapterTypeNames != null) {
                    String implTypeName = PropertiesUtil.toString(ref.getProperty(PROP_IMPLEMENTATION_CLASS), null);
                    for (String adapterTypeName : adapterTypeNames) {
                        adapterImplementations.remove(adapterTypeName, implTypeName);
                    }
                }
                reg.unregister();
            }
        }
        adapterImplementations.removeResourceTypeBindings(bundle);

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
    
    /**
     * Validate list of adapter classes. Make sure all given are either the annotated class itself,
     * or an interface or superclass of it.
     * A warning is written if this it not the case, and false is returned.
     * @param clazz Annotated class
     * @param adapterClasses Adapter classes
     * @return true if validation was successful
     */
    private boolean validateAdapterClasses(Class<?> clazz, Class<?>[] adapterClasses) {
        for (Class<?> adapterClass : adapterClasses) {
            if (!adapterClass.isAssignableFrom(clazz)) {
                log.warn("Unable to register model class {} because adapter class {} is not valid.",
                        clazz.getName(), adapterClass.getName());
                return false;
            }
        }
        return true;
    }
    
    /**
     * Registers an adapter factory for a annotated sling models class.
     * @param adapterTypes Adapter (either the class itself, or interface or superclass of it)
     * @param adaptableTypes Classes to adapt from
     * @param implType Type of the implementation class
     * @param condition Condition (optional)
     * @return Service registration
     */
    private ServiceRegistration registerAdapterFactory(Class<?>[] adapterTypes, Class<?>[] adaptableTypes, Class<?> implType, String condition) {
        Dictionary<String, Object> registrationProps = new Hashtable<String, Object>();
        registrationProps.put(AdapterFactory.ADAPTER_CLASSES, toStringArray(adapterTypes));
        registrationProps.put(AdapterFactory.ADAPTABLE_CLASSES, toStringArray(adaptableTypes));
        registrationProps.put(PROP_IMPLEMENTATION_CLASS, implType.getName());

        if (StringUtils.isNotBlank(condition)) {
            registrationProps.put(PROP_ADAPTER_CONDITION, condition);
        }
        return bundleContext.registerService(AdapterFactory.SERVICE_NAME, factory, registrationProps);
    }


    private void registerExporter(Bundle bundle, Class<?> annotatedClass, String resourceType, Exporter exporterAnnotation,
                                  List<ServiceRegistration> regs, ExportServlet.ExportedObjectAccessor accessor) {
        if (accessor != null) {
            Map<String, String> baseOptions = getOptions(exporterAnnotation);
            ExportServlet servlet = new ExportServlet(bundle.getBundleContext(), factory, bindingsValuesProvidersByContext,
                    scriptEngineFactory, annotatedClass, exporterAnnotation.selector(), exporterAnnotation.name(), accessor, baseOptions);
            Dictionary<String, Object> registrationProps = new Hashtable<String, Object>();
            registrationProps.put("sling.servlet.resourceTypes", resourceType);
            registrationProps.put("sling.servlet.selectors", exporterAnnotation.selector());
            registrationProps.put("sling.servlet.extensions", exporterAnnotation.extensions());
            registrationProps.put(PROP_EXPORTER_SERVLET_CLASS, annotatedClass.getName());
            registrationProps.put(PROP_EXPORTER_SERVLET_NAME, exporterAnnotation.name());

            log.info("registering servlet for {}, {}, {}", new Object[]{resourceType, exporterAnnotation.selector(), exporterAnnotation.extensions()});

            ServiceRegistration reg = bundleContext.registerService(Servlet.class.getName(), servlet, registrationProps);
            regs.add(reg);
        }
    }

    private Map<String, String> getOptions(Exporter annotation) {
        ExporterOption[] options = annotation.options();
        if (options.length == 0) {
            return Collections.emptyMap();
        } else {
            Map<String, String> map = new HashMap<String, String>(options.length);
            for (ExporterOption option : options) {
                map.put(option.name(), option.value());
            }
            return map;
        }
    }

}
