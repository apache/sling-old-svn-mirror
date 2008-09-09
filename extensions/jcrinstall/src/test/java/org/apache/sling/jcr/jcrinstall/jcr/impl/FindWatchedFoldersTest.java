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

import java.lang.reflect.Field;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.jcr.api.SlingRepository;

/** Test the "find watched folders" feature of the RepositoryObserver */
public class FindWatchedFoldersTest extends RepositoryTestBase implements EventListener {
    
    SlingRepository repo;
    Session session;
    String eventPathToWatch;
    int eventCount;
    
    private static final String [] WATCHED_FOLDERS = {
            "libs/foo/bar/install",
            "libs/foo/wii/install",
            "apps/install"
    };
    
    private static final String [] IGNORED_FOLDERS = {
            "libs/foo/bar/installed",
            "apps/noninstall"
    };
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        session.getWorkspace().getObservationManager().removeEventListener(this);
        cleanupContent();
        session.logout();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        repo = getRepository();
        session = repo.loginAdministrative(repo.getDefaultWorkspace());
        cleanupContent();
        
        final int eventTypes = Event.NODE_ADDED;
        final boolean isDeep = true;
        final boolean noLocal = false;
        session.getWorkspace().getObservationManager().addEventListener(
        		this, eventTypes, "/", isDeep, null, null, noLocal);
    }
    
    void cleanupContent() throws Exception {
    	final String [] paths = { "libs", "apps" }; 
    	for(String path : paths) {
            if(session.getRootNode().hasNode(path)) {
                session.getRootNode().getNode(path).remove();
            }
    	}
    }
    
    void setupContent() throws Exception {
    	cleanupContent();
        for(String folder : WATCHED_FOLDERS) {
            createFolder(folder);
        }
        for(String folder : IGNORED_FOLDERS) {
            createFolder(folder);
        }
    }
    
    public void onEvent(EventIterator it) {
        try {
            while(it.hasNext()) {
                final Event e = it.nextEvent();
                if(e.getPath().equals(eventPathToWatch)) {
                	eventCount++;
                }
            }
        } catch(RepositoryException ignored) {
            // ignore
        }
    }

    @SuppressWarnings("unchecked")
    public void testInitialFind() throws Exception {
    	
    	setupContent();
        final RepositoryObserver ro = createRepositoryObserver();
        ro.activate(null);
        
        final Set<WatchedFolder> wfSet = getWatchedFolders(ro);
        assertEquals("activate() must find all watched folders", WATCHED_FOLDERS.length, wfSet.size());
        
        for(String folder : WATCHED_FOLDERS) {
            assertTrue("Folder " + folder + " must be watched (watched=" + wfSet + ")", 
                    folderIsWatched(ro, folder)); 
        }
    }
    
    public void testNewWatchedFolderDetection() throws Exception {
    	setupContent();
        final RepositoryObserver ro = createRepositoryObserver();
        ro.activate(null);

        final String newPaths [] = { "libs/tnwf/install", "apps/tnwf/install" };
        for(String newPath : newPaths) {
            assertFalse(newPath + " must not be watched before test", folderIsWatched(ro, newPath));
            
            // Create folder, wait for observation event and check that
            // it is detected
            final int ec = eventCount;
            eventPathToWatch = "/" + newPath;
            createFolder(newPath);
            waitForEventCount(ec + 1, 5000L);
            assertFalse(newPath + " must not be watched before calling addNewWatchedFolders()", folderIsWatched(ro, newPath));
            ro.addNewWatchedFolders();
            assertTrue(newPath + " must be watched before calling addNewWatchedFolders()", folderIsWatched(ro, newPath));
        }
    }
    
    void waitForEventCount(int count, long timeoutMsec) {
    	final long end = System.currentTimeMillis() + timeoutMsec;
    	while(eventCount < count && System.currentTimeMillis() < end) {
    		try {
    			Thread.sleep(100);
    		} catch(InterruptedException ignored) {
    		}
    	}
    	
    	if(eventCount < count) {
    		throw new IllegalStateException("Event counter did not reach " + count + ", waited " + timeoutMsec + " msec");
    	}
    }
    
    @SuppressWarnings("unchecked")
	Set<WatchedFolder> getWatchedFolders(RepositoryObserver ro) throws Exception {
        final Field f = ro.getClass().getDeclaredField("folders");
        f.setAccessible(true);
        return (Set<WatchedFolder>)f.get(ro);
    }
    
    boolean folderIsWatched(RepositoryObserver ro, String path) throws Exception {
    	boolean result = false;
    	final Set<WatchedFolder> wfSet = getWatchedFolders(ro);
    	for(WatchedFolder wf : wfSet) {
    		if(wf.getPath().equals("/" + path)) {
    			result = true;
    			break;
    		}
    	}
    	return result;
    }
    
    void createFolder(String path) throws Exception {
        final String [] parts = path.split("/");
        Node n = session.getRootNode();
        for(String part : parts) {
            if(n.hasNode(part)) {
                n = n.getNode(part);
            } else {
                n = n.addNode(part);
            }
        }
        session.save();
    }
    
    RepositoryObserver createRepositoryObserver() throws Exception {
        final RepositoryObserver result = new RepositoryObserver();
        setField(result, "repository", repo);
        return result;
    }
    
    static void setField(Object target, String name, Object value) throws Exception, IllegalAccessException {
        final Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);

    }
}
