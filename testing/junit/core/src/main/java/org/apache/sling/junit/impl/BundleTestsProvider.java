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
package org.apache.sling.junit.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.junit.TestsProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A TestProvider that gets test classes from bundles
 *  that have a Sling-Test-Regexp header and corresponding
 *  exported classes.
 */
@Component
@Service
public class BundleTestsProvider implements TestsProvider, BundleListener {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private long lastModified;
    private BundleContext bundleContext;
    private String pid;
    
    public static final String SLING_TEST_REGEXP = "Sling-Test-Regexp";
    
    /** Symbolic names of bundles that changed state - if not empty, need
     *  to adjust the list of tests
     */
    private final List<String> changedBundles = new ArrayList<String>();
    
    /** List of (candidate) test classes, keyed by bundle so that we can
     *  update them easily when bundles come and go 
     */
    private final Map<String, List<String>> testClassesMap = new HashMap<String, List<String>>();

    protected void activate(ComponentContext ctx) {
        bundleContext = ctx.getBundleContext();
        bundleContext.addBundleListener(this);
        
        // Initially consider all bundles as "changed"
        for(Bundle b : bundleContext.getBundles()) {
            if(getSlingTestRegexp(b) != null) {
                changedBundles.add(b.getSymbolicName());
                log.debug("Will look for test classes inside bundle {}", b.getSymbolicName());
            }
        }
        
        lastModified = System.currentTimeMillis();
        pid = (String)ctx.getProperties().get(Constants.SERVICE_PID);
    }
    
    protected void deactivate(ComponentContext ctx) {
        bundleContext.removeBundleListener(this);
        bundleContext = null;
        changedBundles.clear();
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + ", pid=" + pid;
    }

    /** Update testClasses if bundle changes require it */
    private void maybeUpdateTestClasses() {
        if(changedBundles.isEmpty()) {
            return;
        }

        // Get the list of bundles that have changed
        final List<String> bundlesToUpdate = new ArrayList<String>();
        synchronized (changedBundles) {
            bundlesToUpdate.addAll(changedBundles);
            changedBundles.clear();
        }
        
        // Remove test classes that belong to changed bundles
        for(String symbolicName : bundlesToUpdate) {
            testClassesMap.remove(symbolicName);
        }
        
        // Get test classes from bundles that are in our list
        for(Bundle b : bundleContext.getBundles()) {
            if(bundlesToUpdate.contains(b.getSymbolicName())) {
                final List<String> testClasses = getTestClasses(b);
                if(testClasses != null) {
                    testClassesMap.put(b.getSymbolicName(), testClasses);
                    log.debug("{} test classes found in bundle {}, added to our list", 
                            testClasses.size(), b.getSymbolicName());
                } else {
                    log.debug("No test classes found in bundle {}", b.getSymbolicName());
                }
            }
        }
    }

    /** Called when a bundle changes state */
    public void bundleChanged(BundleEvent event) {
        // Only consider bundles which contain tests
        final Bundle b = event.getBundle();
        if(getSlingTestRegexp(b) == null) {
            log.debug("Bundle {} does not have {} header, ignored", 
                    b.getSymbolicName(), SLING_TEST_REGEXP);
            return;
        }
        synchronized (changedBundles) {
            log.debug("Got BundleEvent for Bundle {}, will rebuild its lists of tests");
            changedBundles.add(b.getSymbolicName());
        }
        lastModified = System.currentTimeMillis();
    }
    
    private String getSlingTestRegexp(Bundle b) {
        return (String)b.getHeaders().get(SLING_TEST_REGEXP);
    }
    
    /** Get test classes that bundle b provides (as done in Felix/Sigil) */
    private List<String> getTestClasses(Bundle b) {
        final List<String> result = new ArrayList<String>();
        Pattern testClassRegexp = null;
        final String headerValue = getSlingTestRegexp(b);
        if (headerValue != null) {
            try {
                testClassRegexp = Pattern.compile(headerValue);
            }
            catch (PatternSyntaxException pse) {
                log.warn("Invalid pattern '" + headerValue + "' for bundle "
                                + b.getSymbolicName() + ", ignored", pse);
            }
        }
        
        if (testClassRegexp == null) {
            log.info("Bundle {} does not have {} header, not looking for test classes", SLING_TEST_REGEXP);
        } else if (Bundle.ACTIVE != b.getState()) {
            log.info("Bundle {} is not active, no test classes considered", b.getSymbolicName());
        } else {
            @SuppressWarnings("unchecked")
            Enumeration<URL> classUrls = b.findEntries("", "*.class", true);
            while (classUrls.hasMoreElements()) {
                URL url = classUrls.nextElement();
                final String name = toClassName(url);
                if(testClassRegexp.matcher(name).matches()) {
                    result.add(name);
                } else {
                    log.debug("Class {} does not match {} pattern {} of bundle {}, ignored",
                            new Object[] { name, SLING_TEST_REGEXP, testClassRegexp, b.getSymbolicName() });
                }
            }
            log.info("{} test classes found in bundle {}", result.size(), b.getSymbolicName());
        }
        
        return result;
    }
    
    /** Convert class URL to class name */
    private String toClassName(URL url) {
        final String f = url.getFile();
        final String cn = f.substring(1, f.length() - ".class".length());
        return cn.replace('/', '.');
    }

    /** Find bundle by symbolic name */
    private Bundle findBundle(String symbolicName) {
        for(Bundle b : bundleContext.getBundles()) {
            if(b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        return null;
    }
    
    public Class<?> createTestClass(String testName) throws ClassNotFoundException {
        // Find the bundle to which the class belongs
        Bundle b = null;
        for(Map.Entry<String, List<String>> e : testClassesMap.entrySet()) {
            if(e.getValue().contains(testName)) {
                b = findBundle(e.getKey());
                break;
            }
        }
        
        if(b == null) {
            throw new IllegalArgumentException("No Bundle found that supplies test class " + testName);
        }
        return b.loadClass(testName);
    }

    public long lastModified() {
        return lastModified;
    }

    public String getServicePid() {
        return pid;
    }

    public List<String> getTestNames() {
        maybeUpdateTestClasses();
        final List<String> result = new ArrayList<String>();
        for(List<String> list : testClassesMap.values()) {
            result.addAll(list);
        }
        return result;
    }
}
