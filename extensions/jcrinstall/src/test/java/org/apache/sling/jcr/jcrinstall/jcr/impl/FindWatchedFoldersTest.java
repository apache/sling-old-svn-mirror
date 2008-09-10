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

import java.util.Set;

import javax.jcr.Session;

import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.jcr.api.SlingRepository;

/** Test the "find watched folders" feature of the RepositoryObserver */
public class FindWatchedFoldersTest extends RepositoryTestBase {
    
    SlingRepository repo;
    Session session;
    private EventHelper eventHelper; 
    private ContentHelper contentHelper;
    
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
    }
    
    public void testInitialFind() throws Exception {
    	
    	contentHelper.setupContent();
        final RepositoryObserver ro = MiscHelper.createRepositoryObserver(repo, null);
        ro.activate(null);
        
        final Set<WatchedFolder> wfSet = MiscHelper.getWatchedFolders(ro);
        assertEquals("activate() must find all watched folders", contentHelper.WATCHED_FOLDERS.length, wfSet.size());
        
        for(String folder : contentHelper.WATCHED_FOLDERS) {
            assertTrue("Folder " + folder + " must be watched (watched=" + wfSet + ")", 
                    MiscHelper.folderIsWatched(ro, folder)); 
        }
    }
    
    public void testNewWatchedFolderDetection() throws Exception {
    	contentHelper.setupContent();
        final RepositoryObserver ro = MiscHelper.createRepositoryObserver(repo, null);
        ro.activate(null);

        final String newPaths [] = { "libs/tnwf/install", "apps/tnwf/install" };
        for(String newPath : newPaths) {
            assertFalse(newPath + " must not be watched before test", MiscHelper.folderIsWatched(ro, newPath));
            
            // Create folder, wait for observation event and check that
            // it is detected
            contentHelper.createFolder(newPath);
            eventHelper.waitForEvents(5000L);
            assertFalse(newPath + " must not be watched before calling addNewWatchedFolders()", MiscHelper.folderIsWatched(ro, newPath));
            ro.addNewWatchedFolders();
            assertTrue(newPath + " must be watched before calling addNewWatchedFolders()", MiscHelper.folderIsWatched(ro, newPath));
        }
    }   
}