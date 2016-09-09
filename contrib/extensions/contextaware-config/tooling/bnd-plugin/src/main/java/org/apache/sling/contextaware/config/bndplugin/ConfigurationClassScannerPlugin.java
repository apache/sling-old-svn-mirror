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
package org.apache.sling.contextaware.config.bndplugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.QUERY;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

/**
 * Scans the classpath of the bundle for annotation classes with the "Configuration" annotation
 * from the Sling Context-Aware Configuraiton API.
 * All class names found are stored in a bundle header for processing them at runtime and reading their metadata.
 */
public class ConfigurationClassScannerPlugin implements AnalyzerPlugin, Plugin {
    
    private static final String CONFIGURATION_ANNOTATION_CLASS = "org.apache.sling.contextaware.config.annotation.Configuration";
    
    private static final String CONFIGURATION_CLASSES_HEADER = "Sling-ContextAware-Configuration-Classes";
    
    private Reporter reporter;

    @Override
    public void setProperties(Map<String, String> map) throws Exception {
        // ignore
    }

    @Override
    public void setReporter(Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public boolean analyzeJar(Analyzer analyzer) throws Exception {
        
        // get all annotation classes from this project with Configuration annotation
        Collection<String> classNames = getClassesWithAnnotation(CONFIGURATION_ANNOTATION_CLASS, analyzer);

        // set bundle header containing all class names found
        analyzer.set(CONFIGURATION_CLASSES_HEADER, StringUtils.join(classNames, ","));
        
        // we did not change any classes - no need to re-analyze
        return false;
    }
    
    /**
     * Get all classes that implement the given annotation via bnd Analyzer.
     * @param analyzer Analyzer
     * @param annotation Annotation
     * @return Class names
     */
    private Collection<String> getClassesWithAnnotation(String annotationClassName, Analyzer analyzer) {
        List<String> classNames = new ArrayList<>();
        Collection<Clazz> clazzes = analyzer.getClassspace().values();
        Instruction instruction = new Instruction(annotationClassName);
        try {
            for (Clazz clazz : clazzes) {
                if (clazz.isAnnotation() && clazz.is(QUERY.ANNOTATED, instruction, analyzer)) {
                    classNames.add(clazz.getClassName().getFQN());
                }
            }
        }
        catch (Exception ex) {
            reporter.exception(ex, "Error querying for classes with annotation: " + annotationClassName);
        }
        return classNames;
    }
    
}
