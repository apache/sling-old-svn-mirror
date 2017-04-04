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
package org.apache.sling.validation.impl.annotations;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.ValidationStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.validation.annotations.ChildrenValidator;
import org.apache.sling.validation.annotations.FieldValidator;
import org.apache.sling.validation.annotations.NameRegex;
import org.apache.sling.validation.impl.model.ChildResourceBuilder;
import org.apache.sling.validation.impl.model.ResourcePropertyBuilder;
import org.apache.sling.validation.impl.model.ValidationModelBuilder;
import org.apache.sling.validation.model.ValidationModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationPackageBundleListener implements BundleTrackerCustomizer {

    static final String PACKAGE_HEADER = "Sling-Model-Packages";
    static final String CLASSES_HEADER = "Sling-Model-Classes";

    private static final Logger log = LoggerFactory.getLogger(ValidationPackageBundleListener.class);

    private final BundleTracker bundleTracker;

    private final ValidationModelImplementations validationModelImplementations;

    public ValidationPackageBundleListener(BundleContext bundleContext,
            ValidationModelImplementations validationModelImplementations) {
        this.validationModelImplementations = validationModelImplementations;
        this.bundleTracker = new BundleTracker(bundleContext, Bundle.ACTIVE, this);
        this.bundleTracker.open();
    }

    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {

        Dictionary<String, String> headers = bundle.getHeaders();
        String packageList = headers.get(PACKAGE_HEADER);
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
                    analyzeClass(bundle, className);

                }
            }
        }
        String classesList = headers.get(CLASSES_HEADER);
        if (classesList != null) {
            classesList = StringUtils.deleteWhitespace(classesList);
            String[] classes = classesList.split(",");
            for (String className : classes) {
                analyzeClass(bundle, className);
            }
        }

        return bundle;
    }

    private void analyzeClass(Bundle bundle, String className) {
        try {
            Class<?> implType = bundle.loadClass(className);
            Model model = implType.getAnnotation(Model.class);

            if (model == null) {
                return;
            }
            ValidationStrategy validationStrategy = model.validation();
            if (validationStrategy.equals(ValidationStrategy.DISABLED)) {
                log.info("Validation not required for Model: {}", implType.getName());
                return;
            }

            ValidationModelBuilder modelBuilder = new ValidationModelBuilder();
            // TODO: add applicable paths.
            modelBuilder.addApplicablePaths(new String[] {});

            DefaultInjectionStrategy defaultInjectionStrategy = model.defaultInjectionStrategy();
            log.info("Validation Model for: {}", implType.getName());
            Field[] fields = implType.getDeclaredFields();


            for (Field field : fields) {

                if (field.isAnnotationPresent(ValueMapValue.class)) {
                    ResourcePropertyBuilder builder = new ResourcePropertyBuilder();
                    builder.addValueMapValue(defaultInjectionStrategy, field);

                    if(field.isAnnotationPresent(FieldValidator.class)) {
                        builder.addFieldValidator(field.getAnnotation(FieldValidator.class));
                    }

                    if(field.isAnnotationPresent(NameRegex.class)) {
                        builder.addNameRegex(field.getAnnotation(NameRegex.class));
                    }
                    modelBuilder.resourceProperty(builder.build());
                }

                //TODO: add child resources.
                if(field.isAnnotationPresent(ChildrenValidator.class)) {
                    ChildResourceBuilder builder = new ChildResourceBuilder();
                    builder.getChildResource(defaultInjectionStrategy, field);
                }

            }



            for (String resourceType : model.resourceType()) {
                if (StringUtils.isNotEmpty(resourceType)) {
                    ValidationModel vm = modelBuilder.build(resourceType, StringUtils.EMPTY);
                    validationModelImplementations.registerValidationModelByBundle(bundle, resourceType, vm);
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
        validationModelImplementations.removeValidationModels(bundle);

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

}
