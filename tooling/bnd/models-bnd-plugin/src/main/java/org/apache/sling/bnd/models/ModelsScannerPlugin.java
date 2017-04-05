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
package org.apache.sling.bnd.models;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.QUERY;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

/**
 * Scans the classpath of the bundle for Sling Models classes.
 * All class names found are stored in a bundle header for processing them at runtime and reading their metadata.
 */
public class ModelsScannerPlugin implements AnalyzerPlugin, Plugin {
    
    static final String MODELS_ANNOTATION_CLASS = "org.apache.sling.models.annotations.Model";
    
    static final String MODELS_PACKAGES_HEADER = "Sling-Model-Packages";
    static final String MODELS_CLASSES_HEADER = "Sling-Model-Classes";
    
    // max length of manifest header value 65535 bytes (see http://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html)
    // fall back to packages header when class names string gets too long
    static final int MODELS_CLASSES_HEADER_MAXLENGTH = 60000;
    
    static final String PROPERTY_GENERATE_PACKAGES_HEADER = "generatePackagesHeader";
    
    private Reporter reporter;
    private Map<String,String> properties;

    @Override
    public void setProperties(Map<String, String> map) throws Exception {
        properties = map;
    }

    @Override
    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public boolean analyzeJar(Analyzer analyzer) throws Exception {
        
        // process only if no models packages or class header was set
        if (analyzer.get(MODELS_PACKAGES_HEADER) == null && analyzer.get(MODELS_CLASSES_HEADER) == null) {

            // get all annotation classes from this project with Configuration annotation
            Collection<String> classNames = getClassesWithAnnotation(MODELS_ANNOTATION_CLASS, analyzer);

            // set bundle header containing all class names found
            if (!classNames.isEmpty()) {
                if (getBooleanProperty(PROPERTY_GENERATE_PACKAGES_HEADER)) {
                    generatePackagesHeader(analyzer, classNames);
                }
                else {
                    generateClassesHeader(analyzer, classNames);
                }
            }

        }
        
        // we did not change any classes - no need to re-analyze
        return false;
    }
    
    private void generateClassesHeader(Analyzer analyzer, Collection<String> classNames) {
        String classNameHeader = StringUtils.join(classNames, ",");
        if (classNameHeader.length() <= MODELS_CLASSES_HEADER_MAXLENGTH) {
            analyzer.set(MODELS_CLASSES_HEADER, classNameHeader);
        }
        else {
            generatePackagesHeader(analyzer, classNames);
        }
    }
    
    private void generatePackagesHeader(Analyzer analyzer, Collection<String> classNames) {
        
        // get all package names
        SortedSet<String> packages = new TreeSet<>();
        for (String className : classNames) {
            if (StringUtils.contains(className, ".")) {
                packages.add(StringUtils.substringBeforeLast(className, "."));
            }
        }
        
        // eliminate package names for which parent packages exist (they are included automatically)
        Set<String> packagesToRemove = new HashSet<>();
        for (String packageName : packages) {
            if (includesParentPackage(packages, packageName)) {
                packagesToRemove.add(packageName);
            }
        }
        packages.removeAll(packagesToRemove);
        
        analyzer.set(MODELS_PACKAGES_HEADER, StringUtils.join(packages, ","));
    }
    
    private boolean includesParentPackage(Set<String> packages, String packageName) {
        if (StringUtils.contains(packageName, ".")) {
            String parentPackageName = StringUtils.substringBeforeLast(packageName, ".");
            if (packages.contains(parentPackageName)) {
                return true;
            }
            else {
                return includesParentPackage(packages, parentPackageName);
            }
        }
        else {
            return false;
        }
    }
    
    /**
     * Get all classes that implement the given annotation via bnd Analyzer.
     * @param analyzer Analyzer
     * @param annotation Annotation
     * @return Class names
     */
    private Collection<String> getClassesWithAnnotation(String annotationClassName, Analyzer analyzer) {
        SortedSet<String> classNames = new TreeSet<>();
        Collection<Clazz> clazzes = analyzer.getClassspace().values();
        Instruction instruction = new Instruction(annotationClassName);
        try {
            for (Clazz clazz : clazzes) {
                if (clazz.is(QUERY.ANNOTATED, instruction, analyzer)) {
                    classNames.add(clazz.getClassName().getFQN());
                }
            }
        }
        catch (Exception ex) {
            reporter.exception(ex, "Error querying for classes with annotation: " + annotationClassName);
        }
        return classNames;
    }
    
    private boolean getBooleanProperty(String propertyName) {
        String value = properties != null ? properties.get(propertyName) : null;
        return BooleanUtils.toBoolean(value);
    }
    
}
