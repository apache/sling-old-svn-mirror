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

import java.util.List;

import javax.jcr.Session;

import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.jcr.api.SlingRepository;

/** Test that added/updated/removed resources are
 * 	correctly translated to OsgiInstaller registration
 *  calls.
 *  TODO: test deleting resources while JcrInstaller is stopped
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
        final List<String> recorded = osgiInstaller.getRecordedCalls();
        for(String path : paths) {
            boolean found = false;
            for(String rec : recorded) {
                if(rec.contains(path)) {
                    found = true;
                    break;
                }
            }
            
            boolean expectFound = !path.contains("NOT");
            assertEquals( 
                    (expectFound ? "Expected " : "Did not expect ") 
                    + path + " to be registered (" + recorded + ")",
                    expectFound, found);
        }
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
    
}