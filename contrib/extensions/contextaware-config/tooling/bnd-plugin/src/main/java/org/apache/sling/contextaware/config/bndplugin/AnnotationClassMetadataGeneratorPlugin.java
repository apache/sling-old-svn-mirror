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

import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.QUERY;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.Plugin;
import aQute.service.reporter.Reporter;

public class AnnotationClassMetadataGeneratorPlugin implements AnalyzerPlugin, Plugin {
    
    private static final String CONFIGURATION_ANNOTATION_CLASS = "org.apache.sling.contextaware.config.annotation.Configuration";
    
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
        Collection<Clazz> clazzes = getClassesWithAnnotation(CONFIGURATION_ANNOTATION_CLASS, analyzer);

        // parse classes with ASM
        Collection<ClassNode> classNodes = getClassNodes(clazzes, analyzer);
        
        for (ClassNode classNode : classNodes) {
            reporter.warning("Class (asm): " + classNode.name);
        }
        
        
        return false;
    }
    
    /**
     * Get all classes that implement the given annotation via bnd Analyzer.
     * @param analyzer Analyzer
     * @param annotation Annotation
     * @return Class informations
     */
    private Collection<Clazz> getClassesWithAnnotation(String annotationClassName, Analyzer analyzer) {
        List<Clazz> matchingClazzes = new ArrayList<>();
        Collection<Clazz> clazzes = analyzer.getClassspace().values();
        Instruction instruction = new Instruction(annotationClassName);
        for (Clazz clazz : clazzes) {
            try {
                if (clazz.isAnnotation() && clazz.is(QUERY.ANNOTATED, instruction, analyzer)) {
                    matchingClazzes.add(clazz);
                }
            } catch (Exception ex) {
                reporter.exception(ex, "Error querying for classes with annotation: " + annotationClassName);
            }
        }
        return matchingClazzes;
    }
    
    /**
     * Get ASM class nodes for all given classes.
     * @param clazzes Classes
     * @param analyzer Analzyer
     * @return Class nodes
     */
    private Collection<ClassNode> getClassNodes(Collection<Clazz> clazzes, Analyzer analyzer) {
        List<ClassNode> classNodes = new ArrayList<>();
        try (URLClassLoader classLoader = new URLClassLoader(getClassPath(analyzer), getClass().getClassLoader())) {
            for (Clazz clazz : clazzes) {
                try (InputStream is = classLoader.getResourceAsStream(clazz.getAbsolutePath())) {
                    if (is != null) {
                        ClassReader classReader = new ClassReader(is);
                        ClassNode classNode = new ClassNode();
                        classReader.accept(classNode, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);
                        classNodes.add(classNode);
                    }
                }
            }
        }
        catch (IOException ex) {
            reporter.exception(ex, "Error getting class nodes.");
        }
        return classNodes;
    }
    
    /**
     * Get classpath URLs for given analyzer.
     * @param analyzer Analyzer
     * @return Classpath URLs
     */
    private URL[] getClassPath(Analyzer analyzer) {
        final ArrayList<URL> path = new ArrayList<>();
        for (final Jar jar : analyzer.getClasspath()) {
            try {
                path.add(jar.getSource().toURI().toURL());
            }
            catch (MalformedURLException ex) {
                reporter.exception(ex, "Error getting classpath for " + jar.getName());
            }
        }
        return path.toArray(new URL[path.size()]);
    }    

}
