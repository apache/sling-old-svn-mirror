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
package org.apache.sling.jcr.jcrinstall.jcr.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.jcrinstall.osgi.InstallableData;
import org.apache.sling.jcr.jcrinstall.osgi.JcrInstallException;
import org.apache.sling.jcr.jcrinstall.osgi.OsgiController;
import org.apache.sling.jcr.jcrinstall.osgi.ResourceOverrideRules;
import org.apache.sling.jcr.jcrinstall.osgi.impl.MockInstallableData;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;

/** Test that added/updated/removed resources are
 * 	correctly translated to OsgiController calls.
 */
public class ResourceDetectionTest extends RepositoryTestBase {
    SlingRepository repo;
    Session session;
    private EventHelper eventHelper; 
    private ContentHelper contentHelper;
    private Mockery mockery;
    private Sequence sequence;

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        contentHelper.cleanupContent();
        session.logout();
        eventHelper = null;
        contentHelper = null;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        repo = getRepository();
        session = repo.loginAdministrative(repo.getDefaultWorkspace());
        eventHelper = new EventHelper(session);
        contentHelper = new ContentHelper(session);
        contentHelper.cleanupContent();
        
        // Need the sling namespace for testing
        final NamespaceRegistry r = session.getWorkspace().getNamespaceRegistry();
        try {
        	r.registerNamespace("sling", "http://sling.apache.org/jcr/sling/1.0");
        } catch(RepositoryException ignore) {
        	// don't fail if already registered
        }
        
        mockery = new Mockery();
        sequence = mockery.sequence(getClass().getSimpleName());
    }
    
    protected String getOsgiResourceLocation(String uri) {
        return "jcrinstall://" + uri;
    }
    
    /** Add, update and remove resources from the repository
     *  and verify that the OsgiController receives the
     *  correct messages
     */
    public void testSingleResourceDetection() throws Exception {
        contentHelper.setupContent();
        
        final String dummyJar = "/libs/foo/bar/install/dummy.jar";
        final MockInstallableData da = new MockInstallableData("a");
        final MockInstallableData db = new MockInstallableData("b");
        final Set<String> installedUri = new HashSet<String>();
        final OsgiController c = mockery.mock(OsgiController.class);
        
        // Define the whole sequence of calls to OsgiController,
        // Using getLastModified calls to mark the test phases
        mockery.checking(new Expectations() {{
        	allowing(c).executeScheduledOperations();
            allowing(c).setResourceOverrideRules(with(any(ResourceOverrideRules.class)));
            allowing(c).getInstalledUris(); will(returnValue(installedUri));
            
            one(c).getDigest("phase1"); 
            inSequence(sequence);
            one(c).getDigest(dummyJar); will(returnValue(null));
            inSequence(sequence);
            one(c).scheduleInstallOrUpdate(with(equal(dummyJar)), with(any(InstallableData.class)));
            inSequence(sequence);
            
            one(c).getDigest("phase2"); 
            inSequence(sequence);
            one(c).getDigest(dummyJar); will(returnValue(da.getDigest()));
            inSequence(sequence);
            
            one(c).getDigest("phase3"); 
            inSequence(sequence);
            one(c).getDigest(dummyJar); will(returnValue(da.getDigest()));
            inSequence(sequence);
            one(c).scheduleInstallOrUpdate(with(equal(dummyJar)), with(any(InstallableData.class)));
            inSequence(sequence);
        }});
        
        final RepositoryObserver ro = new MockRepositoryObserver(repo, c);
        ro.activate(null);
        
        // Add two files, run one cycle must install the bundles
        c.getDigest("phase1");
        contentHelper.createOrUpdateFile(dummyJar, da);
        eventHelper.waitForEvents(5000L);
        ro.runOneCycle();
        installedUri.add(dummyJar);
        
        // Updating with the same timestamp must not call install again
        c.getDigest("phase2");
        contentHelper.createOrUpdateFile(dummyJar, da);
        eventHelper.waitForEvents(5000L);
        ro.runOneCycle();
        
        // Updating with a new timestamp must call install again
        c.getDigest("phase3");
        contentHelper.createOrUpdateFile(dummyJar, db);
        eventHelper.waitForEvents(5000L);
        ro.runOneCycle();
        
        mockery.assertIsSatisfied();
    }
    
    public void testMultipleResourceDetection() throws Exception {
        contentHelper.setupContent();
        
        final String [] resources = {
                "/libs/foo/bar/install/dummy.jar",
                "/libs/foo/bar/install/dummy.cfg",
                "/libs/foo/bar/install/dummy.dp"
        };
        final MockInstallableData da = new MockInstallableData("a");
        final Set<String> installedUri = new HashSet<String>();
        final OsgiController c = mockery.mock(OsgiController.class);
        
        // Define the whole sequence of calls to OsgiController,
        mockery.checking(new Expectations() {{
        	allowing(c).executeScheduledOperations();
            allowing(c).setResourceOverrideRules(with(any(ResourceOverrideRules.class)));
            allowing(c).getInstalledUris(); will(returnValue(installedUri));
            allowing(c).getDigest(with(any(String.class))); will(returnValue(null)); 
            one(c).scheduleInstallOrUpdate(with(equal(resources[0])), with(any(InstallableData.class)));
            one(c).scheduleInstallOrUpdate(with(equal(resources[1])), with(any(InstallableData.class)));
            one(c).scheduleInstallOrUpdate(with(equal(resources[2])), with(any(InstallableData.class)));
        }});
        
        final RepositoryObserver ro = new MockRepositoryObserver(repo, c);
        ro.activate(null);
        
        // Add files, run one cycle must install the bundles
        for(String file : resources) {
            contentHelper.createOrUpdateFile(file, da);
        }
        eventHelper.waitForEvents(5000L);
        ro.runOneCycle();
        
        mockery.assertIsSatisfied();
    }
    
    public void testIgnoredFilenames() throws Exception {
        contentHelper.setupContent();
        
        final String [] resources = {
                "/libs/foo/bar/install/dummy.jar",
                "/libs/foo/bar/install/dummy.cfg",
                "/libs/foo/bar/install/dummy.dp"
        };
        
        final String [] ignored  = {
                "/libs/foo/bar/install/_dummy.jar",
                "/libs/foo/bar/install/.dummy.cfg",
                "/libs/foo/bar/install/dummy.longextension"
        };
        
        final MockInstallableData da = new MockInstallableData("a");
        final Set<String> installedUri = new HashSet<String>();
        final OsgiController c = mockery.mock(OsgiController.class);
        
        mockery.checking(new Expectations() {{
        	allowing(c).executeScheduledOperations();
            allowing(c).setResourceOverrideRules(with(any(ResourceOverrideRules.class)));
            allowing(c).getInstalledUris(); will(returnValue(installedUri));
            allowing(c).getDigest(with(any(String.class))); will(returnValue(null)); 
            one(c).scheduleInstallOrUpdate(with(equal(resources[0])), with(any(InstallableData.class)));
            one(c).scheduleInstallOrUpdate(with(equal(resources[1])), with(any(InstallableData.class)));
            one(c).scheduleInstallOrUpdate(with(equal(resources[2])), with(any(InstallableData.class)));
        }});
        
        final RepositoryObserver ro = new MockRepositoryObserver(repo, c);
        ro.activate(null);
        
        for(String file : resources) {
            contentHelper.createOrUpdateFile(file, da);
        }
        for(String file : ignored) {
            contentHelper.createOrUpdateFile(file, da);
        }
        eventHelper.waitForEvents(5000L);
        
        ro.runOneCycle();
        mockery.assertIsSatisfied();
   }
    
    public void testInitialDeletions() throws Exception {
        contentHelper.setupContent();
        
        final Set<String> installedUri = new HashSet<String>();
        installedUri.add("/libs/foo/bar/install/dummy.jar");
        installedUri.add("/libs/foo/bar/install/dummy.cfg");
        installedUri.add("/libs/watchfolder-is-gone/install/gone.cfg");
        
        final OsgiController c = mockery.mock(OsgiController.class);
        final RepositoryObserver ro = new MockRepositoryObserver(repo, c);
        
        mockery.checking(new Expectations() {{
        	allowing(c).executeScheduledOperations();
            allowing(c).setResourceOverrideRules(with(any(ResourceOverrideRules.class)));
            allowing(c).getInstalledUris(); will(returnValue(installedUri));
            allowing(c).getDigest(with(any(String.class))); will(returnValue(null));
            
            // scheduleUninstall might be called multiple times for the same resource
            // during the initial deletion analysis - the OsgiController needs to accept
            // such multiple calls
            atLeast(1).of(c).scheduleUninstall("/libs/foo/bar/install/dummy.jar");
            atLeast(1).of(c).scheduleUninstall("/libs/foo/bar/install/dummy.cfg");
            atLeast(1).of(c).scheduleUninstall("/libs/watchfolder-is-gone/install/gone.cfg");
        }});
        
        ro.activate(null);
        ro.handleInitialUninstalls();
        mockery.assertIsSatisfied();
    }
    
    public void testInitialDeletionsWithException() throws Exception {
        contentHelper.setupContent();
        
        final SortedSet<String> installedUri = new TreeSet<String>();
        installedUri.add("/libs/foo/bar/install/dummy.cfg");
        installedUri.add("/libs/foo/bar/install/dummy.dp");
        installedUri.add("/libs/foo/bar/install/dummy.jar");
        
        final OsgiController c = mockery.mock(OsgiController.class);
        final RepositoryObserver ro = new MockRepositoryObserver(repo, c);
        
        // See testInitialDeletions() about multiple scheduleUninstall calls
        mockery.checking(new Expectations() {{
        	allowing(c).executeScheduledOperations();
            allowing(c).setResourceOverrideRules(with(any(ResourceOverrideRules.class)));
            allowing(c).getInstalledUris(); will(returnValue(installedUri));
            allowing(c).getDigest(with(any(String.class))); will(returnValue(null)); 
            atLeast(1).of(c).scheduleUninstall("/libs/foo/bar/install/dummy.cfg");
            inSequence(sequence);
            atLeast(1).of(c).scheduleUninstall("/libs/foo/bar/install/dummy.dp");
            inSequence(sequence);
            will(throwException(new JcrInstallException("Fake BundleException for testing")));
            atLeast(1).of(c).scheduleUninstall("/libs/foo/bar/install/dummy.jar");
            inSequence(sequence);
        }});
        
        // Activating with installed resources that are not in
        // the repository must cause them to be uninstalled
        ro.activate(null);
        ro.handleInitialUninstalls();
        mockery.assertIsSatisfied();
    }
    
    public void testMultipleResourcesWithException() throws Exception {
        contentHelper.setupContent();
        
        final String [] resources = {
                "/libs/foo/bar/install/dummy.jar",
                "/libs/foo/bar/install/dummy.cfg",
                "/libs/foo/bar/install/dummy.dp"
        };
        final MockInstallableData da = new MockInstallableData("a");
        final Set<String> installedUri = new HashSet<String>();
        final OsgiController c = mockery.mock(OsgiController.class);
        
        // Define the whole sequence of calls to OsgiController,
        mockery.checking(new Expectations() {{
        	allowing(c).executeScheduledOperations();
            allowing(c).setResourceOverrideRules(with(any(ResourceOverrideRules.class)));
            allowing(c).getInstalledUris(); will(returnValue(installedUri));
            allowing(c).getDigest(with(any(String.class))); will(returnValue(null)); 
            one(c).scheduleInstallOrUpdate(with(equal(resources[0])), with(any(InstallableData.class)));
            one(c).scheduleInstallOrUpdate(with(equal(resources[1])), with(any(InstallableData.class)));
            will(throwException(new JcrInstallException("Fake BundleException for testing")));
            one(c).scheduleInstallOrUpdate(with(equal(resources[2])), with(any(InstallableData.class)));
        }});
        
        final RepositoryObserver ro = new MockRepositoryObserver(repo, c);
        ro.activate(null);
        
        // Add files, run one cycle must install the bundles
        for(String file : resources) {
            contentHelper.createOrUpdateFile(file, da);
        }
        eventHelper.waitForEvents(5000L);
        ro.runOneCycle();
        
        mockery.assertIsSatisfied();
    }
    
    /** Verify that resources are correctly uninstalled if the folder name regexp changes */
    public void testFolderRegexpChange() throws Exception {
        final File serviceDataFile = File.createTempFile(getClass().getName(), ".properties");
        serviceDataFile.deleteOnExit();
        contentHelper.setupContent();
        
       final String [] resources = {
                "/libs/foo/bar/install/dummy.jar",
                "/libs/foo/wii/install/dummy.cfg",
                "/libs/foo/bar/install/dummy.dp",
                "/libs/foo/wii/install/more.cfg",
        };
        
        final MockInstallableData da = new MockInstallableData("a");

        for(String file : resources) {
            contentHelper.createOrUpdateFile(file, da);
        }
        
        final Set<String> installedUri = new HashSet<String>();
        final OsgiController c = mockery.mock(OsgiController.class);
        final Properties props = new Properties();
        
        // Test with first regexp
        mockery.checking(new Expectations() {{
        	allowing(c).executeScheduledOperations();
            allowing(c).setResourceOverrideRules(with(any(ResourceOverrideRules.class)));
            allowing(c).getInstalledUris(); will(returnValue(installedUri));
            allowing(c).getDigest(with(any(String.class))); will(returnValue(null)); 
            atLeast(1).of(c).scheduleInstallOrUpdate(with(equal(resources[0])), with(any(InstallableData.class)));
            atLeast(1).of(c).scheduleInstallOrUpdate(with(equal(resources[2])), with(any(InstallableData.class)));
        }});
        
        final MockRepositoryObserver ro = new MockRepositoryObserver(repo, c, serviceDataFile);
        ro.setProperties(props);
        props.setProperty(RepositoryObserver.FOLDER_NAME_REGEXP_PROPERTY, ".*foo/bar/install$");
        ro.activate(null);
        ro.handleInitialUninstalls();
        for(String file : resources) {
            contentHelper.createOrUpdateFile(file, da);
        }
        eventHelper.waitForEvents(5000L);
        ro.runOneCycle();
        mockery.assertIsSatisfied();
        installedUri.add(resources[0]);
        installedUri.add(resources[2]);
        
        // Test with a different regexp, install.A resources must be uninstalled
        mockery.checking(new Expectations() {{
        	allowing(c).executeScheduledOperations();
            allowing(c).setResourceOverrideRules(with(any(ResourceOverrideRules.class)));
            allowing(c).getInstalledUris(); will(returnValue(installedUri));
            allowing(c).getDigest(with(any(String.class))); will(returnValue(null)); 
            atLeast(1).of(c).scheduleUninstall(resources[0]);
            atLeast(1).of(c).scheduleUninstall(resources[2]);
            atLeast(1).of(c).scheduleInstallOrUpdate(with(equal(resources[1])), with(any(InstallableData.class)));
            atLeast(1).of(c).scheduleInstallOrUpdate(with(equal(resources[3])), with(any(InstallableData.class)));
        }});
        
        props.setProperty(RepositoryObserver.FOLDER_NAME_REGEXP_PROPERTY, ".*foo/wii/install$");
        ro.activate(null);
        ro.handleInitialUninstalls();
        for(String file : resources) {
            contentHelper.createOrUpdateFile(file, da);
        }
        eventHelper.waitForEvents(5000L);
        ro.runOneCycle();
        mockery.assertIsSatisfied();
    }
}