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
package org.apache.sling.crankstart.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.ModelUtility.VariableResolver;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Launch an OSGi app instance using the Sling provisioning model */
public class Launcher {
    private Model model = new Model();
    private static final Logger log = LoggerFactory.getLogger(Launcher.class);
    
    public static final String CRANKSTART_FEATURE = ":crankstart";
    public static final String MODEL_KEY = "model";
    public static final String FRAMEWORK_KEY = "framework";
    
    public static final String VARIABLE_OVERRIDE_PREFIX = "crankstart.model.";

    /** Default variable resolver using system properties */
    public static final VariableResolver DEFAULT_VARIABLE_RESOLVER = 
        new PropertiesVariableResolver(System.getProperties(), VARIABLE_OVERRIDE_PREFIX) {
            @Override
            protected void onOverride(String variableName, String value, String propertyName) {
                log.info("Overriding model variable {}={} (from system property {})", variableName, value, propertyName);
            }
        };
    
    /** Allow for overriding model variables */ 
    private VariableResolver variableResolver; 
    
    public static final FeatureFilter NOT_CRANKSTART_FILTER = new FeatureFilter() {
        @Override
        public boolean ignoreFeature(Feature f) {
            return Launcher.CRANKSTART_FEATURE.equals(f.getName());
        }
    };
    
    public static final FeatureFilter ONLY_CRANKSTART_FILTER = new FeatureFilter() {
        @Override
        public boolean ignoreFeature(Feature f) {
            return !Launcher.CRANKSTART_FEATURE.equals(f.getName());
        }
    };
    
    public Launcher(String ... args) throws Exception {
        MavenResolver.setup();
        withVariableResolver(null);
        withModelPaths(args);
    }
    
    /** Use the supplied VariableResolver. Defaults to DEFAULT_VARIABLE_RESOLVER if v
     *  is null or if this is not called. 
     */
    public Launcher withVariableResolver(VariableResolver v) {
        variableResolver = (v == null ? DEFAULT_VARIABLE_RESOLVER : v);
        return this;
    }

    /** Add models from the supplied paths, can be either files or folders */ 
    public Launcher withModelPaths(String ... paths) throws Exception {
        // Find all files to read and sort the list, to be deterministic
        final SortedSet<File> toRead = new TreeSet<File>();
        
        for(String name : paths) {
            final File f = new File(name);
            if(f.isDirectory()) {
                final String [] list = f.list();
                for(String s : list) {
                    toRead.add(new File(f, s));
                }
            } else {
                toRead.add(f);
            }
        }

        // Merge all model files 
        for(File f : toRead) {
            mergeModel(f);
        }
        
        computeEffectiveModel();
        return this;
    }
    
    public void computeEffectiveModel() throws Exception {
        new NestedModelsMerger(model).visit();
        model = ModelUtility.getEffectiveModel(model, variableResolver);
    }
    
    public Model getModel() {
        return model;
    }
    
    /** Can be called before launch() to read and merge additional models.
     *  @param r provisioning model to read, closed by this method after reading */ 
    public static void mergeModel(Model mergeInto, Reader r, String sourceInfo) throws IOException {
        log.info("Merging provisioning model {}", sourceInfo);
        try {
            final Model m = ModelReader.read(r, sourceInfo);
            ModelUtility.merge(mergeInto, m);
        } finally {
            r.close();
        }
    }
    
    /** Can be called before launch() to read and merge additional models */
    public void mergeModel(File f) throws IOException {
        mergeModel(model, new BufferedReader(new FileReader(f)), f.getAbsolutePath());
    }
    
    public void launch() throws Exception {
        // Setup initial classpath to launch the OSGi framework
        for(URL u : getClasspathURLs(model, CRANKSTART_FEATURE)) {
            addToClasspath(u);
        }

        // Need to load FrameworkSetup in this way to avoid any static references to OSGi classes in this class, as those are
        // not available yet when this class is loaded.
        final Callable<?> c = (Callable<?>) getClass().getClassLoader().loadClass("org.apache.sling.crankstart.launcher.FrameworkSetup").newInstance();
        @SuppressWarnings("unchecked") final Map<String, Object> cmap = (Map<String, Object>)c; 
        cmap.put(MODEL_KEY, model);
        c.call();
    }
    
    /** Slightly hacky way to add URLs to the system classloader,
     *  based on http://stackoverflow.com/questions/60764/how-should-i-load-jars-dynamically-at-runtime
     */  
    private void addToClasspath(URL u) throws IOException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final URLClassLoader sysLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();
        final Method m = URLClassLoader.class.getDeclaredMethod("addURL",new Class[]{URL.class});
        m.setAccessible(true);
        m.invoke(sysLoader,new Object[]{ u });
        log.info("Added to classpath: {}", u);
    }
    
    /** Convert a Model feature to a set of URLs meant to setup
     *  an URLClassLoader.
     */
    List<URL> getClasspathURLs(Model m, String featureName) throws MalformedURLException {
        final List<URL> result = new ArrayList<URL>();
        
        // Add all URLs from the special feature to our classpath
        final Feature f = m.getFeature(featureName);
        if(f == null) {
            log.warn("No {} feature found in provisioning model, nothing to add to our classpath", featureName);
        } else {
            for(RunMode rm : f.getRunModes()) {
                for(ArtifactGroup g : rm.getArtifactGroups()) {
                    for(Artifact a : g) {
                        final String url = MavenResolver.mvnUrl(a);
                        try {
                            result.add(new URL(url));
                        } catch(MalformedURLException e) {
                            final MalformedURLException up = new MalformedURLException("Invalid URL [" + url + "]");
                            up.initCause(e);
                            throw up;
                        }
                    }
                }
            }
        }
        return result;
    }
    
     public static void main(String [] args) throws Exception {
        if(args.length < 1) {
            System.err.println("Usage: " + Launcher.class.getSimpleName() + " provisioning-model [provisioning-model ...]"); 
            System.err.println("Where provisioning-model is either a Sling provisioning model file");
            System.err.println("or a folder that contains oseveral of those.");
            System.exit(0);
        }
        
        new Launcher(args).launch();
    }
}