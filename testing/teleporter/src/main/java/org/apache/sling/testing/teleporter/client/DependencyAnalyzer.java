/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.teleporter.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.shared.dependency.analyzer.asm.DependencyClassFileVisitor;

/** Find the class dependencies of a class, recursively, 
 *  using the maven-dependency-analyzer and optionally considering only
 *  specific package prefixes to avoid exploring the whole graph.
 */
class DependencyAnalyzer {
    private final Class<?> [] classes;
    private final Set<String> dependencyNames = new HashSet<String>();
    private final Set<String> includes = new HashSet<String>();
    private final Set<String> excludes = new HashSet<String>();
    private final Set<Class<?>> alreadySeen = new HashSet<Class<?>>();
    private Collection<Class<?>> dependencies;
    
    private DependencyAnalyzer(Class <?> ... classes) {
        this.classes = classes;
    }
    
    static DependencyAnalyzer forClass(Class<?> ... classes) {
        return new DependencyAnalyzer(classes);
    }
    
    /** Classes with names that match this prefix will be included,
     *  unless they also match an exclude prefix. 
     */
    DependencyAnalyzer include(String prefix) {
        includes.add(prefix);
        return this;
    }
    
    /** Classes with names that match this prefix will not be included,
     *  even if their name matches an include pattern */
    DependencyAnalyzer exclude(String prefix) {
        excludes.add(prefix);
        return this;
    }
    
    /** Get the aggregate dependencies of our classes, based on a recursive
     *  analysis that takes our include/exclude prefixes into account
     */
    synchronized Collection<Class<?>> getDependencies() {
        if(dependencies != null) {
            return dependencies;
        }
        dependencies = new HashSet<Class<?>>();
        for(Class<?> c : classes) {
            analyze(c);
        }
        for(String dep : dependencyNames) {
            dependencies.add(toClass(dep));
        }
        return dependencies;
    }
    
    /** Analyze a single class, recursively */
    private void analyze(Class<?> c) {
        if(alreadySeen.contains(c)) {
            return;
        }
        alreadySeen.add(c);
        final Set<String> deps = new HashSet<String>();
        final String path = "/" + c.getName().replace('.', '/') + ".class";
        final InputStream input = getClass().getResourceAsStream(path);
        if(input == null) {
            throw new RuntimeException("Class resource not found: " + path);
        }
        try {
            try {
                final DependencyClassFileVisitor v = new DependencyClassFileVisitor();
                v.visitClass(c.getName(), input);
                deps.addAll(v.getDependencies());
            } finally {
                input.close();
            }
        } catch(IOException ioe) {
            throw new RuntimeException("IOException while reading " + path);
        }
        
        // Keep only accepted dependencies, and recursively analyze them
        for(String dep : deps) {
            if(dep.equals(c.getName())) {
                continue;
            }
            if(accept(dep)) {
                dependencyNames.add(dep);
                analyze(toClass(dep));
            }
        }
    }
    
    /** True if given class name matches our include/exclude prefixes */
    private boolean accept(String className) {
        boolean result = false;
        
        for(String s : includes) {
            if(className.startsWith(s)) {
                result = true;
                break;
            }
        }
        
        // Excludes win over includes
        if(result) {
            for(String s : excludes) {
                if(className.startsWith(s)) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
    
    /** Convert a class name to its Class object */
    private Class<?> toClass(String className) {
        try {
            return getClass().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found :" + className, e);
        }
    }
}