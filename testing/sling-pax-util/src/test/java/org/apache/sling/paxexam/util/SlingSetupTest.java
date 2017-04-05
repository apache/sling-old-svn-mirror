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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.sling.launchpad.api.StartupHandler;
import org.apache.sling.launchpad.api.StartupMode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/** Verify that we get a working Sling launchpad with what SlingPaxOptions provide */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SlingSetupTest {
    
    /** Use a released launchpad for this example */
    public static final String SLING_LAUNCHPAD_VERSION = "7";
    
    @Inject
    private BundleContext bundleContext;
    
    private ServiceRegistration<?> startupHandlerRegistration;
    
    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return SlingPaxOptions.defaultLaunchpadOptions(SLING_LAUNCHPAD_VERSION).getOptions();
    }
    
    @Before
    public void setup() {
        // In Sling launchpad 7 the SlingSettings service
        // requires a StartupHandler, and that's usually provided
        // by the launchpad bootstrap code. Supply our own so that
        // everything starts properly.
        // TODO should be provided by a utility/bootstrap bundle
        final StartupHandler h = new StartupHandler() {
            public void waitWithStartup(boolean b) {
            }
            
            public boolean isFinished() {
                return true;
            }
            
            public StartupMode getMode() {
                return StartupMode.INSTALL;
            }
        };
        startupHandlerRegistration = bundleContext.registerService(StartupHandler.class.getName(), h, null);
    }
    
    @After
    public void cleanup() {
        if(startupHandlerRegistration != null) {
            startupHandlerRegistration.unregister();
            startupHandlerRegistration = null;
        }
    }

    private void assertBundleActive(String symbolicName) {
        assertEquals("Expecting bundle to be active:" + symbolicName, Bundle.ACTIVE, getBundleState(symbolicName));
    }
    
    private boolean isFragment(Bundle b) {
        return b.getHeaders().get("Fragment-Host") != null;
    }
    
    private Bundle getBundle(String symbolicName) {
        return getBundle(bundleContext, symbolicName);
    }
    
    static Bundle getBundle(BundleContext bc, String symbolicName) {
        for(Bundle b : bc.getBundles()) {
            if(symbolicName.equals(b.getSymbolicName())) {
                return b;
            }
        }
        return null;
    }
    
    /** @return bundle state, UNINSTALLED if absent */
    private int getBundleState(String symbolicName) {
        return getBundleState(getBundle(symbolicName));
    }
    
    /** @return bundle state, UNINSTALLED if absent, ACTIVE  */
    private int getBundleState(Bundle b) {
        if(b == null) {
            return Bundle.UNINSTALLED; 
        } else if(isFragment(b)) {
            return Bundle.ACTIVE;
        } else {
            return b.getState();
        }
    }
    
    @Test
    public void testBundleContext() {
        assertNotNull("Expecting BundleContext to be set", bundleContext);
    }
    
    @Test
    public void testSlingBundles() {
        final String [] LAUNCHPAD_7_BUNDLES = {
                "derby",
                "jcl.over.slf4j",
                "log4j.over.slf4j",
                "org.apache.aries.jmx.api",
                "org.apache.aries.jmx.core",
                "org.apache.aries.jmx.whiteboard",
                "org.apache.aries.util",
                "org.apache.commons.codec",
                "org.apache.commons.collections",
                "org.apache.commons.fileupload",
                "org.apache.commons.io",
                "org.apache.commons.lang",
                "org.apache.commons.math",
                "org.apache.commons.pool",
                "org.apache.felix.bundlerepository",
                "org.apache.felix.configadmin",
                "org.apache.felix.eventadmin",
                "org.apache.felix.framework",
                //"org.apache.felix.http.api",
                "org.apache.felix.http.jetty",
                //"org.apache.felix.http.servlet-api",
                "org.apache.felix.http.whiteboard",
                "org.apache.felix.inventory",
                "org.apache.felix.metatype",
                "org.apache.felix.prefs",
                "org.apache.felix.scr",
                "org.apache.felix.webconsole",
                "org.apache.felix.webconsole.plugins.ds",
                "org.apache.felix.webconsole.plugins.event",
                "org.apache.felix.webconsole.plugins.memoryusage",
                "org.apache.felix.webconsole.plugins.obr",
                "org.apache.felix.webconsole.plugins.packageadmin",
                "org.apache.geronimo.bundles.commons-httpclient",
                "org.apache.geronimo.bundles.json",
                "org.apache.geronimo.bundles.jstl",
                "org.apache.jackrabbit.jackrabbit-api",
                "org.apache.jackrabbit.jackrabbit-jcr-commons",
                "org.apache.jackrabbit.jackrabbit-jcr-rmi",
                "org.apache.jackrabbit.jackrabbit-spi",
                "org.apache.jackrabbit.jackrabbit-spi-commons",
                "org.apache.jackrabbit.jackrabbit-webdav",
                "org.apache.servicemix.bundles.concurrent",
                "org.apache.sling.adapter",
                "org.apache.sling.api",
                "org.apache.sling.auth.core",
                "org.apache.sling.auth.form",
                "org.apache.sling.bundleresource.impl",
                "org.apache.sling.commons.classloader",
                "org.apache.sling.commons.compiler",
                IgnoredBundlesTest.JSON_BUNDLE_SN,
                "org.apache.sling.commons.log",
                "org.apache.sling.commons.logservice",
                IgnoredBundlesTest.MIME_BUNDLE_SN,
                "org.apache.sling.commons.osgi",
                "org.apache.sling.commons.scheduler",
                "org.apache.sling.commons.threads",
                "org.apache.sling.discovery.api",
                "org.apache.sling.discovery.impl",
                "org.apache.sling.discovery.support",
                "org.apache.sling.engine",
                "org.apache.sling.event",
                "org.apache.sling.extensions.explorer",
                "org.apache.sling.extensions.threaddump",
                "org.apache.sling.extensions.webconsolebranding",
                "org.apache.sling.extensions.webconsolesecurityprovider",
                "org.apache.sling.fragment.transaction",
                "org.apache.sling.fragment.ws",
                "org.apache.sling.fragment.xml",
                "org.apache.sling.fsresource",
                "org.apache.sling.installer.console",
                "org.apache.sling.installer.core",
                "org.apache.sling.installer.factory.configuration",
                "org.apache.sling.installer.provider.file",
                "org.apache.sling.installer.provider.jcr",
                "org.apache.sling.javax.activation",
                "org.apache.sling.jcr.api",
                "org.apache.sling.jcr.base",
                "org.apache.sling.jcr.classloader",
                "org.apache.sling.jcr.contentloader",
                "org.apache.sling.jcr.davex",
                "org.apache.sling.jcr.jackrabbit.accessmanager",
                "org.apache.sling.jcr.jackrabbit.server",
                "org.apache.sling.jcr.jackrabbit.usermanager",
                "org.apache.sling.jcr.jcr-wrapper",
                "org.apache.sling.jcr.registration",
                "org.apache.sling.jcr.resource",
                "org.apache.sling.jcr.webconsole",
                "org.apache.sling.jcr.webdav",
                "org.apache.sling.launchpad.content",
                "org.apache.sling.launchpad.installer",
                "org.apache.sling.models.api",
                "org.apache.sling.models.impl",
                "org.apache.sling.resourceresolver",
                "org.apache.sling.scripting.api",
                "org.apache.sling.scripting.core",
                "org.apache.sling.scripting.javascript",
                "org.apache.sling.scripting.jsp",
                "org.apache.sling.scripting.jsp.taglib",
                "org.apache.sling.serviceusermapper",
                "org.apache.sling.servlets.get",
                "org.apache.sling.servlets.post",
                "org.apache.sling.servlets.resolver",
                "org.apache.sling.settings",
                "org.apache.tika.bundle",
                "org.apache.tika.core",
                "slf4j.api"
        };
        
        final List<String> missing = new ArrayList<String>();
        for(String bundleName : LAUNCHPAD_7_BUNDLES) {
            final int state = getBundleState(bundleName); 
            if(state != Bundle.ACTIVE) {
                missing.add(bundleName + " (state=" + state + ")");
            }
        }
        
        if(!missing.isEmpty()) {
            fail("Some required bundles are missing or inactive:" + missing);
        }
    }
    
    @Test
    public void testSlingServices() {
        assertBundleActive("org.apache.sling.commons.mime");
        assertBundleActive("org.apache.sling.engine");
        
        final String [] services = {
                "org.apache.sling.engine.SlingRequestProcessor",
                "org.apache.sling.commons.mime.MimeTypeService",
                "org.apache.sling.jcr.api.SlingRepository",
                "org.apache.sling.settings.SlingSettingsService"
        };
        
        final List<String> missing = new ArrayList<String>();
        for(String svc : services) {
            final ServiceReference<?> ref = bundleContext.getServiceReference(svc);
            if(ref == null) {
                missing.add(svc);
            } else {
                bundleContext.ungetService(ref);
            }
        }
        if(!missing.isEmpty()) {
            fail("Some required services are missing:" + missing);
        }
    }
}
