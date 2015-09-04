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
package org.apache.sling.jcr.contentloader.it;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.launchpad.api.StartupHandler;
import org.apache.sling.launchpad.api.StartupMode;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for testing bundles that provide initial content */
@RunWith(PaxExam.class)
public abstract class ContentBundleTestBase {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    @Rule
    public final RetryRule retry = new RetryRule(RETRY_TIMEOUT, RETRY_INTERVAL);
    
    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected SlingRepository repository;
    
    protected static Session session;
    protected static String bundleSymbolicName;
    protected static String contentRootPath;
    private static final List<Bundle> bundlesToRemove = new ArrayList<Bundle>();
    
    protected static final int RETRY_TIMEOUT = 5000;
    protected static final int RETRY_INTERVAL = 100;
    protected static final String SLING_INITIAL_CONTENT_HEADER = "Sling-Initial-Content";
    protected static final String DEFAULT_PATH_IN_BUNDLE = "test-initial-content";
    
    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return PaxExamUtilities.paxConfig();
    }
    
    @BeforeClass
    public static void setupClass() {
        bundleSymbolicName = "TEST-" + UUID.randomUUID();
        contentRootPath = "/test-content/" + bundleSymbolicName;
    }
    
    @Before
    public void setup() throws Exception {
        registerStartupHandler();

        session = repository.loginAdministrative(null);
        
        // The RetryRule executes this method on every retry, make
        // sure to install our test bundle only once
        if(!bundlesToRemove.isEmpty()) {
            return;
        }
        assertFalse("Expecting no content before test", session.itemExists(contentRootPath));
        
        // Create, install and start a bundle that has initial content
        final InputStream is = getTestBundleStream();
        try {
            final Bundle b = bundleContext.installBundle(bundleSymbolicName, is);
            bundlesToRemove.add(b);
            b.start();
        } finally {
            is.close();
        }
        
        maybeDumpTestBundle();
    }
    
    /** Optionally dump our test bundle, for troubleshooting it */
    private void maybeDumpTestBundle() throws Exception {
        final boolean doDump = Boolean.valueOf(System.getProperty("dump.test.bundles", "false"));
        if(doDump) {
            final File target = File.createTempFile(bundleSymbolicName, ".jar");
            FileOutputStream fos = new FileOutputStream(target);
            try {
                IOUtils.copy(getTestBundleStream(), fos);
                log.info("Dumped test bundle to {} for troubleshooting", target.getAbsolutePath());
            } finally {
                IOUtils.closeQuietly(fos);
            }
        }
    }
    
    private InputStream getTestBundleStream() throws Exception {
        return setupTestBundle(TinyBundles.bundle()
            .set(Constants.BUNDLE_SYMBOLICNAME, bundleSymbolicName)
            ).build(TinyBundles.withBnd());
    }
    
    abstract protected TinyBundle setupTestBundle(TinyBundle b) throws Exception;
    
    /** Add content to our test bundle */
    protected void addContent(TinyBundle b, String pathInBundle, String resourcePath) throws IOException {
        pathInBundle += "/" + resourcePath;
        resourcePath = "/initial-content/" + resourcePath;
        final InputStream is = getClass().getResourceAsStream(resourcePath);
        try {
            assertNotNull("Expecting resource to be found:" + resourcePath, is);
            log.info("Adding resource to bundle, path={}, resource={}", pathInBundle, resourcePath);
            b.add(pathInBundle, is);
        } finally {
            if(is != null) {
                is.close();
            }
        }
    }
    
    @AfterClass
    public static void cleanupClass() throws BundleException {
        for(Bundle b : bundlesToRemove) {
            b.uninstall();
        }
        bundlesToRemove.clear();
        
        session.logout();
        session = null;
    }

    private void registerStartupHandler() {
        // SLING-4917 (org.apache.sling.paxexam.util.SlingSetupTest)
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

        bundleContext.registerService(StartupHandler.class.getName(), h, null);
    }

}