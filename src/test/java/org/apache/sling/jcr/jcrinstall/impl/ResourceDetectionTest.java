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
package org.apache.sling.jcr.jcrinstall.impl;

import javax.jcr.Session;

import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;

/** Test that added/updated/removed resources are
 * 	correctly translated to OsgiInstaller registration
 *  calls.
 */
public class ResourceDetectionTest extends RepositoryTestBase {
    public static final long TIMEOUT = 5000L;
    SlingRepository repo;
    Session session;
    private EventHelper eventHelper; 
    private ContentHelper contentHelper;
    private JcrInstaller installer;
    private MockOsgiInstaller osgiInstaller;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        repo = getRepository();
        session = repo.loginAdministrative(repo.getDefaultWorkspace());
        eventHelper = new EventHelper(session);
        contentHelper = new ContentHelper(session);
        contentHelper.cleanupContent();
        contentHelper.setupContent();
        osgiInstaller = new MockOsgiInstaller();
        installer = MiscUtil.getJcrInstaller(repo, osgiInstaller);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        contentHelper.cleanupContent();
        session.logout();
        eventHelper = null;
        contentHelper = null;
    }

    private void assertRegisteredPaths(String [] paths) {
        for(String path : paths) {
        	assertRegistered(path, !path.contains("NOT"));
        }
    }
    
    private void assertRegistered(String path, boolean registered) {
       	if(registered) {
    		assertTrue("Expected " + path + " to be registered",
    				osgiInstaller.isRegistered(JcrInstaller.URL_SCHEME, path));
    	} else {
    		assertFalse("Expected " + path + " to be unregistered",
    				osgiInstaller.isRegistered(JcrInstaller.URL_SCHEME, path));
    	}
    }
    
    private void assertRecordedCall(String action, String path) {
    	final String callStr = action + ":" + JcrInstaller.URL_SCHEME + ":" + path;
    	boolean found = false;
    	for(String call : osgiInstaller.getRecordedCalls()) {
    		if(call.startsWith(callStr)) {
    			found = true;
    			break;
    		}
    	}
    	assertTrue("Expecting '" + callStr + "' in recorded calls (" + osgiInstaller.getRecordedCalls() + ")", found);
    }
    
    public void testInitialResourceDetection() throws Exception {
        assertRegisteredPaths(contentHelper.FAKE_RESOURCES);
        assertRegisteredPaths(contentHelper.FAKE_CONFIGS);
    }
    
    public void testAddFiles() throws Exception {
        final String [] paths = {
            "/libs/foo/bar/install/" + System.currentTimeMillis() + ".jar",
            "/libs/foo/wii/install/" + + System.currentTimeMillis() + ".jar",
            "/libs/foo/wii/install/" + + System.currentTimeMillis() + ".properties",
            "/libs/foo/wii/install/" + + System.currentTimeMillis() + ".cfg",
            "/libs/foo/wii/install/NOT_" + + System.currentTimeMillis() + ".badExtension",
            "/libs/foo/wii/install/.NOT_startsWithDot" + + System.currentTimeMillis() + ".jar",
        };
        
        for(String path : paths) {
            contentHelper.createOrUpdateFile(path);
        }
        eventHelper.waitForEvents(TIMEOUT);
        MiscUtil.waitForCycles(installer, 2, TIMEOUT);
        assertRegisteredPaths(paths);
    }
    
    public void testAddConfigs() throws Exception {
        final String [] paths = {
            "/libs/foo/bar/install/" + System.currentTimeMillis() + ".jar",
            "/libs/foo/wii/install/" + + System.currentTimeMillis() + ".jar",
            "/libs/foo/wii/install/" + + System.currentTimeMillis() + ".properties",
            "/libs/foo/wii/install/" + + System.currentTimeMillis() + ".cfg",
            "/libs/foo/wii/install/YES_" + + System.currentTimeMillis() + ".badExtension",
            "/libs/foo/wii/install/.YES_startsWithDot" + + System.currentTimeMillis() + ".jar",
        };
        
        for(String path : paths) {
            contentHelper.createConfig(path, null);
        }
        
        eventHelper.waitForEvents(TIMEOUT);
        MiscUtil.waitForCycles(installer, 2, TIMEOUT);
        assertRegisteredPaths(paths);
    }
    
    public void testDeleteResources() throws Exception {
        assertRegisteredPaths(contentHelper.FAKE_RESOURCES);
        assertRegisteredPaths(contentHelper.FAKE_CONFIGS);
        
        osgiInstaller.clearRecordedCalls();
        assertTrue("Expecting recorded calls to be empty", osgiInstaller.getRecordedCalls().isEmpty());

        final int toRemove = 1;
        contentHelper.delete(contentHelper.FAKE_RESOURCES[toRemove]);
        contentHelper.delete(contentHelper.FAKE_CONFIGS[toRemove]);
        eventHelper.waitForEvents(TIMEOUT);
        MiscUtil.waitForCycles(installer, 2, TIMEOUT);
        
        for(int i=0; i < contentHelper.FAKE_RESOURCES.length; i++) {
        	assertRegistered(contentHelper.FAKE_RESOURCES[i], i != toRemove);
        }
        for(int i=0; i < contentHelper.FAKE_CONFIGS.length; i++) {
        	assertRegistered(contentHelper.FAKE_CONFIGS[i], i != toRemove);
        }
     
        final int nCalls = contentHelper.FAKE_RESOURCES.length + contentHelper.FAKE_CONFIGS.length;
        assertEquals("Expecting both remove and add calls when resources are deleted", 
        		nCalls, osgiInstaller.getRecordedCalls().size());
    }
    
    public void testStopAndRestart() throws Exception {
        assertRegisteredPaths(contentHelper.FAKE_RESOURCES);
        assertRegisteredPaths(contentHelper.FAKE_CONFIGS);
        final ComponentContext cc = MiscUtil.getMockComponentContext();
        
        // With the installer deactivated, remove two resources and add some new ones 
        osgiInstaller.clearRecordedCalls();
        installer.deactivate(cc);
        assertEquals("Expected no calls to OsgiInstaller when deactivating JcrInstaller", 
        		0, osgiInstaller.getRecordedCalls().size());
        final int toRemove = 2;
        contentHelper.delete(contentHelper.FAKE_RESOURCES[toRemove]);
        contentHelper.delete(contentHelper.FAKE_CONFIGS[toRemove]);
        final String [] toAdd = {
                "/libs/foo/bar/install/" + System.currentTimeMillis() + ".jar",
                "/libs/foo/wii/install/" + + System.currentTimeMillis() + ".properties",
                "/libs/foo/wii/install/" + + System.currentTimeMillis() + ".cfg",
        };
        for(String path : toAdd) {
        	contentHelper.createOrUpdateFile(path);
        }
            
        // Verify that no calls have been made to OSGi installer
        eventHelper.waitForEvents(TIMEOUT);
        Thread.sleep(JcrInstaller.RUN_LOOP_DELAY_MSEC * 3);
        assertEquals("Expected no calls to OsgiInstaller while JcrInstaller is stopped", 
        		0, osgiInstaller.getRecordedCalls().size());
        
        // Restart JcrInstaller and verify that all remaining resources are re-registered
        installer.activate(cc);
        MiscUtil.waitForCycles(installer, 2, TIMEOUT);
        
        for(int i=0; i < contentHelper.FAKE_RESOURCES.length; i++) {
        	final String path = contentHelper.FAKE_RESOURCES[i];
        	if(i == toRemove) {
        		assertFalse("Path should be absent from recorded calls", osgiInstaller.getRecordedCalls().contains(path));
        	} else {
            	assertRecordedCall("register",path);
        	}
        }
        for(int i=0; i < contentHelper.FAKE_CONFIGS.length; i++) {
        	final String path = contentHelper.FAKE_CONFIGS[i];
        	if(i == toRemove) {
        		assertFalse("Path should be absent from recorded calls", osgiInstaller.getRecordedCalls().contains(path));
        	} else {
            	assertRecordedCall("register",path);
        	}
        }
        for(String path : toAdd) {
        	assertRecordedCall("register",path);
        }
   }
    
    public void testFolderRemoval() throws Exception {
        assertRegisteredPaths(contentHelper.FAKE_RESOURCES);
        assertRegisteredPaths(contentHelper.FAKE_CONFIGS);
        
        // Removing a folder, all resources that it contains must be unregistered
        contentHelper.delete("/libs");
        eventHelper.waitForEvents(TIMEOUT);
        MiscUtil.waitForCycles(installer, 2, TIMEOUT);
        for(String path : contentHelper.FAKE_RESOURCES) {
        	assertRegistered(path, !path.startsWith("/libs"));
        }
        for(String path : contentHelper.FAKE_CONFIGS) {
        	assertRegistered(path, !path.startsWith("/libs"));
        }
    }
}