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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/** Verify that we get a working Sling launchpad with what SlingPaxOptions provide */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SlingSetupTest {
    
    @Inject
    private BundleContext bundleContext;
    
    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return SlingPaxOptions.defaultLaunchpadOptions("7-SNAPSHOT").getOptions();
    }

    private void assertBundleActive(String symbolicName) {
        assertEquals("Expecting bundle to be active:" + symbolicName, Bundle.ACTIVE, getBundleState(symbolicName));
    }
    
    private boolean isFragment(Bundle b) {
        return b.getHeaders().get("Fragment-Host") != null;
    }
    
    private Bundle getBundle(String symbolicName) {
        for(Bundle b : bundleContext.getBundles()) {
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
                "org.apache.sling.fragment.transaction",
                "org.apache.sling.fragment.ws",
                "org.apache.sling.fragment.xml",
                "org.apache.sling.fsresource",
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
        
        final List<String> missing = new ArrayList<String>();
        for(String bundleName : bundles) {
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
                "org.apache.sling.jcr.api.SlingRepository"
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