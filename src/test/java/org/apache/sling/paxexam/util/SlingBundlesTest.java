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
package org.apache.sling.paxexam.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.apache.sling.launchpad.api.StartupListener;
import org.apache.sling.launchpad.api.StartupMode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
public class SlingBundlesTest {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Inject
    private BundleContext bundleContext;
    
    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        final String paxLogLevel = System.getProperty("pax.exam.log.level", "INFO");
        
        return options(
                junitBundles(),
                systemProperty( "org.ops4j.pax.logging.DefaultServiceLog.level" ).value(paxLogLevel),
                SlingPaxOptions.slingBootstrapBundles(),
                SlingPaxOptions.slingLaunchpadBundles(null)
        );
    }

    private boolean isFragment(Bundle b) {
        return b.getHeaders().get("Fragment-Host") != null;
    }
    
    private void assertBundleActive(String symbolicName) {
        Bundle b = null;
        for(Bundle x : bundleContext.getBundles()) {
            if(symbolicName.equals(x.getSymbolicName())) {
                b = x;
                break;
            }
        }
        assertNotNull("Expecting bundle " + symbolicName + " to be present", b);
        if(!isFragment(b)) {
            assertEquals("Expecting bundle " + symbolicName + " to be active", Bundle.ACTIVE, b.getState());
        }
    }
    
    @Before 
    public void startAllBundles() {
        final List<String> notStarted = new LinkedList<String>();
        int lastNotStarted = Integer.MAX_VALUE;
        
        while(true) {
            notStarted.clear();
            for(Bundle b : bundleContext.getBundles()) {
                if(!isFragment(b) && b.getState() != Bundle.ACTIVE) {
                    notStarted.add(b.getSymbolicName());
                    try {
                        b.start();
                    } catch(Exception e) {
                        fail("Cannot start Bundle " + b.getSymbolicName() + ": " + e);
                    } 
                }
            }
            
            if(notStarted.isEmpty()) {
                break;
            }
            
            if(!notStarted.isEmpty() && notStarted.size() >= lastNotStarted) {
                log.error("No bundles started in the last cycle, inactive bundles={}", notStarted);
                break;
            }
            lastNotStarted = notStarted.size();
        }
    }
    
    @Test
    public void testBundleContext() {
        assertNotNull("Expecting BundleContext to be set", bundleContext);
    }
    
    @Test
    public void testSlingBundles() {
        final String [] bundles = {
                "org.apache.sling.adapter",
                "org.apache.sling.api",
                "org.apache.sling.auth.core",
                "org.apache.sling.auth.form",
                "org.apache.sling.auth.openid",
                "org.apache.sling.auth.selector",
                "org.apache.sling.bundleresource.impl",
                "org.apache.sling.commons.classloader",
                "org.apache.sling.commons.json",
                "org.apache.sling.commons.log",
                "org.apache.sling.commons.logservice",
                "org.apache.sling.commons.mime",
                "org.apache.sling.commons.osgi",
                "org.apache.sling.commons.scheduler",
                "org.apache.sling.commons.threads",
                "org.apache.sling.discovery.api",
                "org.apache.sling.discovery.impl",
                "org.apache.sling.discovery.support",
                "org.apache.sling.engine",
                "org.apache.sling.event",
                "org.apache.sling.extensions.explorer",
                "org.apache.sling.extensions.groovy",
                "org.apache.sling.extensions.threaddump",
                "org.apache.sling.extensions.webconsolebranding",
                "org.apache.sling.extensions.webconsolesecurityprovider",
                "org.apache.sling.fragment.activation",
                "org.apache.sling.fragment.transaction",
                "org.apache.sling.fragment.ws",
                "org.apache.sling.fragment.xml",
                "org.apache.sling.fsresource",
                "org.apache.sling.installer.api",
                "org.apache.sling.installer.console",
                "org.apache.sling.installer.core",
                "org.apache.sling.installer.factory.configuration",
                "org.apache.sling.installer.provider.file",
                "org.apache.sling.installer.provider.jcr",
                "org.apache.sling.jcr.jcr-wrapper",
                "org.apache.sling.jcr.api",
                "org.apache.sling.jcr.base",
                "org.apache.sling.jcr.classloader",
                "org.apache.sling.jcr.contentloader",
                "org.apache.sling.jcr.davex",
                "org.apache.sling.jcr.jackrabbit.accessmanager",
                "org.apache.sling.jcr.jackrabbit.server",
                "org.apache.sling.jcr.jackrabbit.usermanager",
                "org.apache.sling.jcr.ocm",
                "org.apache.sling.jcr.registration",
                "org.apache.sling.jcr.resource",
                "org.apache.sling.jcr.webconsole",
                "org.apache.sling.jcr.webdav",
                "org.apache.sling.launchpad.content",
                "org.apache.sling.launchpad.installer",
                "org.apache.sling.resourceresolver",
                "org.apache.sling.scripting.api",
                "org.apache.sling.scripting.core",
                "org.apache.sling.scripting.javascript",
                "org.apache.sling.scripting.jsp",
                "org.apache.sling.scripting.jsp.taglib",
                "org.apache.sling.servlets.get",
                "org.apache.sling.servlets.post",
                "org.apache.sling.servlets.resolver",
                "org.apache.sling.settings"
        };
        
        for(String bundleName : bundles) {
            assertBundleActive(bundleName);
        }
    }
    
    @Test
    public void testSlingServices() {
        
        class LocalStartupListener implements StartupListener {
            
            boolean testsHaveRun;
            
            public void inform(StartupMode m, boolean finished) {
                log.info("inform(finished={})", finished);
                if(finished) {
                    runTests();
                }
            }

            public void startupFinished(StartupMode m) {
                log.info("Startup finished");
                runTests();
            }

            public void startupProgress(float f) {
                log.info("Startup progress {}", f);
            }
            
            void runTests() {
                if(!testsHaveRun) {
                    testsHaveRun = true;
                }
                
                try {
                    assertBundleActive("org.apache.sling.commons.mime");
                    assertBundleActive("org.apache.sling.engine");
                    
                    final String [] services = {
                            "org.apache.sling.commons.mime.MimeTypeService",
                            "org.apache.sling.engine.SlingRequestProcessor"
                    };
                    
                    for(String svc : services) {
                        final ServiceReference<?> ref = bundleContext.getServiceReference(svc);
                        assertNotNull("Expecting " + svc + " to be available", ref);
                        bundleContext.ungetService(ref);
                    }
                } finally {
                    log.info("Done running tests");
                    synchronized (this) {
                        notify();
                    }
                }
            }
        };
        

        final LocalStartupListener s = new LocalStartupListener();
        s.runTests();
        
        /*
        // TODO generalize this "wait for Sling startup" - assuming we really need it
        final ServiceRegistration<?> reg = bundleContext.registerService(StartupListener.class.getName(), s, null);
        final long timeout = 5000L;
        try {
            synchronized (s) {
                log.info("Waiting for {} to be done running tests", s);
                s.wait(timeout);
            }
        } catch(InterruptedException ignored) {
            fail("InterruptedException waiting tests to be executed");
        } finally {
            reg.unregister();
        }
        
        if(!s.testsHaveRun) {
            fail("Timeout waiting for tests to run, after " + timeout + " msec");
        }
        */
    }
}