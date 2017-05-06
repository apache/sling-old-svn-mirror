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
package org.apache.sling.validation.impl.annotationmodel;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.EnumerationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.ValidationStrategy;
import org.apache.sling.validation.impl.annotationmodel.builders.AnnotationValidationModelBuilder;
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

    private static final Logger LOG = LoggerFactory.getLogger(ValidationPackageBundleListener.class);
    private static final String COMMA_SEPARATOR_REGEX = ",";

    private final BundleTracker bundleTracker;

    private final ValidationModelImplementation validationModelImplementation;

    public ValidationPackageBundleListener(BundleContext bundleContext,
            ValidationModelImplementation validationModelImplementation) {
        this.validationModelImplementation = validationModelImplementation;
        this.bundleTracker = new BundleTracker(bundleContext, Bundle.ACTIVE, this);
        this.bundleTracker.open();
    }

    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        Set<String> classNames = getBundleClasses(bundle);
        classNames.forEach(className -> analyzeClass(bundle, className));
        return bundle;
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        validationModelImplementation.removeValidationModels(bundle);

    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
    }

    public synchronized void unregisterAll() {
        this.bundleTracker.close();
    }

    private Set<String> getBundleClasses(@Nonnull Bundle bundle) {
        Set<String> classNames = new HashSet<>();
        classNames.addAll(getPackageHeaderClassNames(bundle));
        classNames.addAll(getClassHeaderClassNames(bundle.getHeaders()));
        return classNames;
    }

    private void analyzeClass(Bundle bundle, String className) {
        try {
            Class<?> implType = bundle.loadClass(className);
            if (!implType.isAnnotationPresent(Model.class)) {
                return;
            }

            Model model = implType.getAnnotation(Model.class);
            if (model.validation().equals(ValidationStrategy.DISABLED)) {
                return;
            }

            List<ValidationModel> validationModels = new AnnotationValidationModelBuilder().build(implType);
            validationModelImplementation.registerValidationModelsByBundle(bundle, validationModels);

        } catch (ClassNotFoundException e) {
            LOG.warn("Unable to load class", e);
        }
    }

    private HashSet<String> getClassHeaderClassNames(@Nonnull Dictionary<String, String> headers) {
        return Optional.ofNullable(StringUtils.deleteWhitespace(headers.get(CLASSES_HEADER)))
                .map(classes -> classes.split(COMMA_SEPARATOR_REGEX))
                .map(Arrays::asList)
                .map(HashSet::new)
                .orElse(new HashSet<>());
    }

    private Set<String> getPackageHeaderClassNames(@Nonnull Bundle bundle) {
        return Optional.ofNullable(StringUtils.deleteWhitespace(bundle.getHeaders().get(PACKAGE_HEADER)))
                .map(packages -> packages.split(COMMA_SEPARATOR_REGEX))
                .map(Arrays::asList)
                .map(list -> getClassNamesFromPackages(bundle, list))
                .orElse(Collections.emptySet());
    }

    private Set<String> getClassNamesFromPackages(@Nonnull Bundle bundle, @Nonnull List<String> packages) {
        return packages.parallelStream()
                .map(singlePackage -> findEntries(bundle, singlePackage))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private Set<String> findEntries(@Nonnull Bundle bundle, @Nonnull String singlePackage) {
        return Optional.ofNullable(bundle.findEntries("/" + singlePackage.replace('.', '/'), "*.class", true))
                .map(EnumerationUtils::toList)
                .map(this::getClassesFromUrl)
                .orElse(Collections.emptySet());
    }

    private Set<String> getClassesFromUrl(List<URL> urls) {
        return urls.parallelStream()
                .map(this::toClassName)
                .collect(Collectors.toSet());
    }

    /** Convert class URL to class name */
    private String toClassName(URL url) {
        final String f = url.getFile();
        final String cn = f.substring(1, f.length() - ".class".length());
        return cn.replace('/', '.');
    }

}
