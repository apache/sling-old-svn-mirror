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
package org.apache.sling.installer.provider.jcr.impl;

import javax.jcr.Node;

import org.apache.sling.installer.provider.jcr.impl.JcrInstaller;
import org.osgi.service.component.ComponentContext;

/** Test that added/updated/removed resources are
 * 	correctly translated to OsgiInstaller registration
 *  calls.
 */
public class ResourceDetectionTest extends JcrInstallTestBase {

    public void testInitialResourceDetection() throws Exception {
        assertRegisteredPaths(contentHelper.FAKE_RESOURCES);
        assertRegisteredPaths(contentHelper.FAKE_CONFIGS);
    }

    protected boolean needsTestContent() {
        return true;
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
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
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
        
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
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
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        
        for(int i=0; i < contentHelper.FAKE_RESOURCES.length; i++) {
        	assertRegistered(contentHelper.FAKE_RESOURCES[i], i != toRemove);
        }
        for(int i=0; i < contentHelper.FAKE_CONFIGS.length; i++) {
        	assertRegistered(contentHelper.FAKE_CONFIGS[i], i != toRemove);
        }
     
        assertEquals("Expecting only remove calls when resources are deleted", 
        		2, osgiInstaller.getRecordedCalls().size());
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
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        
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
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        for(String path : contentHelper.FAKE_RESOURCES) {
        	assertRegistered(path, !path.startsWith("/libs"));
        }
        for(String path : contentHelper.FAKE_CONFIGS) {
        	assertRegistered(path, !path.startsWith("/libs"));
        }
    }
    
    public void testFileUpdate() throws Exception {
    	final String path = contentHelper.FAKE_RESOURCES[0];
    	assertRegistered(path, true);
    	
    	// Make a change that does not influence the file's digest,
    	// and verify that no OSGi registrations result
    	int nCalls = osgiInstaller.getRecordedCalls().size();
    	((Node)session.getItem(path + "/jcr:content")).setProperty("jcr:mimeType", "application/" + getClass().getName());
    	session.save();
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        assertEquals("Expected no OsgiInstaller calls for no-impact file change",
        		nCalls, osgiInstaller.getRecordedCalls().size());
        
        // Make a content change -> resource must be re-registered
        osgiInstaller.clearRecordedCalls();
        contentHelper.createOrUpdateFile(path, null, System.currentTimeMillis());
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        assertEquals("Expected one OsgiInstaller call for file content change",
        		1, osgiInstaller.getRecordedCalls().size());
        assertRecordedCall("add", path);
    }
    
    public void testConfigUpdate() throws Exception {
       	final String path = contentHelper.FAKE_CONFIGS[0];
    	assertRegistered(path, true);
    	
    	// Setup a known value for the config
        osgiInstaller.clearRecordedCalls();
    	final String key = "foo" + System.currentTimeMillis();
    	final String value = "value" + System.currentTimeMillis();
    	((Node)session.getItem(path)).setProperty(key, value);
    	session.save();
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        assertEquals("Expected one OsgiInstaller call for initial config change",
                1, osgiInstaller.getRecordedCalls().size());
    			
    	// Make a change that does not influence the configs's digest,
    	// and verify that no OSGi registrations result
        osgiInstaller.clearRecordedCalls();
    	((Node)session.getItem(path)).setProperty(key, value);
    	session.save();
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        assertEquals(
                "Expected no OsgiInstaller calls for no-impact config change, got " 
                + osgiInstaller.getRecordedCalls(),
        		0, osgiInstaller.getRecordedCalls().size());
        
        // Make a content change -> resource must be re-registered
        osgiInstaller.clearRecordedCalls();
    	((Node)session.getItem(path)).setProperty(key, value + "-changed");
        MiscUtil.waitAfterContentChanges(eventHelper, installer);
        assertEquals("Expected one OsgiInstaller call for config content change",
        		1, osgiInstaller.getRecordedCalls().size());
        assertRecordedCall("add", path);
   }
}