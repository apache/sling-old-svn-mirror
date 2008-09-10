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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.jcrinstall.osgi.OsgiController;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Observes a set of folders in the JCR repository, to
 *  detect added or updated resources that might be of
 *  interest to the OsgiController.
 *  
 *  Calls the OsgiController to install/remove resources.
 *   
 *  @scr.component 
 *      immediate="true"
 *      metatype="no"
 *  @scr.property 
 *      name="service.description" 
 *      value="Sling jcrinstall OsgiController Service"
 *  @scr.property 
 *      name="service.vendor" 
 *      value="The Apache Software Foundation"
 */
public class RepositoryObserver implements Runnable {

    private Set<WatchedFolder> folders;
    private RegexpFilter folderNameFilter;
    private RegexpFilter filenameFilter;
    private boolean running;
    
    /** @scr.reference */
    private OsgiController osgiController;
    
    /** @scr.reference */
    private SlingRepository repository;
    
    private Session session;
    
    private List<WatchedFolderCreationListener> listeners = new LinkedList<WatchedFolderCreationListener>();
    
    /** Default set of root folders to watch */
    public static String[] DEFAULT_ROOTS = {"/libs", "/apps"};
    
    /** Default regexp for watched folders and filenames */
    public static final String DEFAULT_FOLDER_NAME_REGEXP = ".*/install$";
    public static final String DEFAULT_FILENAME_REGEXP = "[a-zA-Z0-9].*\\.[a-zA-Z][a-zA-Z][a-zA-Z]?";
    
    /** Scan delay for watched folders */
    private final long scanDelayMsec = 1000L;
    
    protected static final Logger log = LoggerFactory.getLogger(WatchedFolder.class);
    
    /** Upon activation, find folders to watch under our roots, and observe those
     *  roots to detect new folders to watch.
     */
    protected void activate(ComponentContext context) throws RepositoryException {
    	
    	// TODO make this configurable
    	final String [] roots = DEFAULT_ROOTS; 
    	folderNameFilter = new RegexpFilter(DEFAULT_FOLDER_NAME_REGEXP);
    	filenameFilter = new RegexpFilter(DEFAULT_FILENAME_REGEXP);
    	
        // Listen for any new WatchedFolders created after activation
        session = repository.loginAdministrative(repository.getDefaultWorkspace());
        final int eventTypes = Event.NODE_ADDED;
        final boolean isDeep = true;
        final boolean noLocal = true;
        for (String path : roots) {
            final WatchedFolderCreationListener w = new WatchedFolderCreationListener(folderNameFilter);
            listeners.add(w);
            session.getWorkspace().getObservationManager().addEventListener(w, eventTypes, path,
                    isDeep, null, null, noLocal);
        }

        folders = new HashSet<WatchedFolder>();
        
        for(String root : roots) {
            folders.addAll(findWatchedFolders(root));
        }
        
        // Check if any deletions happened while this
        // service was inactive
        for(WatchedFolder wf : folders) {
            try {
                wf.checkDeletions(osgiController.getInstalledUris());
            } catch(Exception e) {
                log.warn("Exception in activate() / checkDeletions", e);
            }
        }
        
        // start queue processing
        running = true;
        final Thread t = new Thread(this, getClass().getSimpleName() + "_" + System.currentTimeMillis());
        t.setDaemon(true);
        t.start();
    }
    
    protected void deactivate(ComponentContext oldContext) {
    	
        running = false;
        
    	for(WatchedFolder f : folders) {
    		f.cleanup();
    	}
    	
    	if(session != null) {
	    	for(WatchedFolderCreationListener w : listeners) {
	    		try {
		        	session.getWorkspace().getObservationManager().removeEventListener(w);
	    		} catch(RepositoryException re) {
	    			log.warn("RepositoryException in removeEventListener call", re);
	    		}
	    	}
	    	
	    	session.logout();
	    	session = null;
    	}
    	
    	listeners.clear();
        folders.clear();
    }
    
    /** Add WatchedFolders that have been discovered by our WatchedFolderCreationListeners, if any */
    void addNewWatchedFolders() throws RepositoryException {
    	for(WatchedFolderCreationListener w : listeners) {
    		final Set<String> paths = w.getAndClearPaths();
    		if(paths != null) {
    			for(String path : paths) {
    				folders.add(new WatchedFolder(repository, path, osgiController, filenameFilter, scanDelayMsec));
    			}
    		}
    	}
    }

    /** Find all folders to watch under rootPath 
     * @throws RepositoryException */
    Set<WatchedFolder> findWatchedFolders(String rootPath) throws RepositoryException {
        final Set<WatchedFolder> result = new HashSet<WatchedFolder>();
        Session s = null;

        try {
            s = repository.loginAdministrative(repository.getDefaultWorkspace());
            if (!s.getRootNode().hasNode(relPath(rootPath))) {
                log.info("Bundles root node {} not found, ignored", rootPath);
            } else {
                log.debug("Bundles root node {} found, looking for bundle folders inside it", rootPath);
                final Node n = s.getRootNode().getNode(relPath(rootPath));
                findWatchedFolders(n, result);
            }
        } finally {
            if (s != null) {
                s.logout();
            }
        }
        
        return result;
    }
    
    /**
     * Add n to setToUpdate if it is a bundle folder, and recurse into its children
     * to do the same.
     */
    void findWatchedFolders(Node n, Set<WatchedFolder> setToUpdate) throws RepositoryException 
    {
        if (folderNameFilter.accept(n.getPath())) {
            setToUpdate.add(new WatchedFolder(repository, n.getPath(), osgiController, filenameFilter, scanDelayMsec));
        }
        final NodeIterator it = n.getNodes();
        while (it.hasNext()) {
            findWatchedFolders(it.nextNode(), setToUpdate);
        }
    }
    
    /**
     * Return the relative path for supplied path
     */
    static String relPath(String path) {
        if (path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }
    
    /**
     * Scan WatchFolders once their timer expires
     */
    public void run() {
        log.info("{} thread {} starts", getClass().getSimpleName(), Thread.currentThread().getName());

        // We could use the scheduler service but that makes things harder to test
        while (running) {
            try {
                runOneCycle();
            } catch (IllegalArgumentException ie) {
                log.warn("IllegalArgumentException  in " + getClass().getSimpleName(), ie);
            } catch (RepositoryException re) {
                log.warn("RepositoryException in " + getClass().getSimpleName(), re);
            } catch (Throwable t) {
                log.error("Unhandled Throwable in runOneCycle() - "
                        + getClass().getSimpleName() + " thread will be stopped", t);
            } finally {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ignore) {
                    // ignore
                }
            }
        }

        log.info("{} thread {} ends", getClass().getSimpleName(), Thread.currentThread().getName());
    }
    
    /** Let our WatchedFolders run their scanning cycles */ 
    void runOneCycle() throws Exception {
    	for(WatchedFolder wf : folders) {
    		wf.scanIfNeeded();
    	}
    }
}