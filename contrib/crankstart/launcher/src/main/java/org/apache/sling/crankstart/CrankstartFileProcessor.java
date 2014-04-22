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
package org.apache.sling.crankstart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Process a crankstart file */
public class CrankstartFileProcessor {
    public static final String I_BUNDLE = "bundle ";
    public static final String I_START_ALL_BUNDLES = "start.all.bundles";
    public static final String I_LOG = "log";
    public static final String I_START_FRAMEWORK = "start.framework";
    public static final String I_OSGI_PROPERTY = "osgi.property";
    
    private Framework framework;
    private final List<Bundle> bundles = new LinkedList<Bundle>();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Map<String, String> osgiProperties = new HashMap<String, String>();
    
    public CrankstartFileProcessor() {
        System.setProperty( "java.protocol.handler.pkgs", "org.ops4j.pax.url" );
    }
    
    public void process(Reader input) throws IOException, BundleException {
        final BufferedReader r = new BufferedReader(input);
        String line = null;
        while((line = r.readLine()) != null) {
            processLine(line);
        }
    }
    
    private String removePrefix(String line, String prefix) {
        return line.substring(prefix.length()).trim();
    }
    
    private void processLine(String line) throws IOException, BundleException {
        line = line.trim();
        if(line.length() == 0 || line.startsWith("#")) {
            // ignore comments and blank lines
        } else if(line.startsWith(I_BUNDLE)) {
            bundle(removePrefix(line, I_BUNDLE));
        } else if(line.startsWith(I_START_ALL_BUNDLES)) {
            startAllBundles();
        } else if(line.startsWith(I_LOG)) {
            log.info(removePrefix(line, I_LOG));
        } else if(line.startsWith(I_START_FRAMEWORK)) {
            startFramework();
        } else if(line.startsWith(I_OSGI_PROPERTY)) {
            osgiProperty(removePrefix(line, I_OSGI_PROPERTY));
        } else {
            log.warn("Invalid command line: [{}]", line);
        }
    }
    
    private void osgiProperty(String line) {
        final String [] parts = line.split(" ");
        if(parts.length != 2) {
            log.warn("Invalid OSGi property [{}]", line);
            return;
        }
        final String key = parts[0].trim();
        final String value = parts[1].trim();
        log.info("Setting OSGI property {}={}", key, value);
        osgiProperties.put(key, value);
    }
    
    private void startFramework() throws BundleException {
        if(framework != null) {
            throw new IllegalStateException("OSGi framework already created");
        }
        
        // TODO get framework as a Maven artifact?
        FrameworkFactory frameworkFactory = java.util.ServiceLoader.load(FrameworkFactory.class).iterator().next();
        framework = frameworkFactory.newFramework(osgiProperties);
        framework.start();
        
        log.info("OSGi framework started");
    }
    
    private void bundle(String line) throws IOException, BundleException {
        final URL url = new URL( "mvn:" + line);
        final BundleContext ctx = framework.getBundleContext();
        final String ref = "crankstart://" + line;
        final InputStream bundleStream = url.openStream();
        try {
            bundles.add(ctx.installBundle(ref, url.openStream()));
            log.info("bundle installed: {}", ref);
        } finally {
            bundleStream.close();
        }
    }
    
    public void waitForExit() throws InterruptedException {
        log.info("Waiting for OSGi framework to exit...");
        framework.waitForStop(0);
    }
    
    private void startAllBundles() throws BundleException {
        for (Bundle bundle : bundles) {
            log.info("Starting bundle {}", bundle.getSymbolicName());
            bundle.start();
        }
        
        // TODO check that all bundles have started? 
        // or use a crankstart instruction for that? 
        
        log.info("{} bundles installed", bundles.size());
    }

}
