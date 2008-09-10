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

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.jcrinstall.osgi.JcrInstallException;
import org.apache.sling.jcr.jcrinstall.osgi.OsgiController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Watch a single folder in the JCR Repository, detecting changes
 *  to it (non-recursively) and sending the appropriate messages
 *  to the OsgiController service.
 */
class WatchedFolder implements EventListener {
    private final String path;
    private final OsgiController controller;
    private long nextScan;
    private final Session session;
    protected static final Logger log = LoggerFactory.getLogger(WatchedFolder.class);
    
    /**
     * After receiving JCR events, we wait for this many msec before
     * re-scanning the folder, as events often come in bursts.
     */
    private final long scanDelayMsec;

    WatchedFolder(SlingRepository repository, String path, OsgiController ctrl, long scanDelayMsec) throws RepositoryException {
        this.path = path;
        this.controller = ctrl;
        this.scanDelayMsec = scanDelayMsec;
        session = repository.loginAdministrative(repository.getDefaultWorkspace());
        
        // observe any changes in our folder, but not recursively
        final int eventTypes = Event.NODE_ADDED | Event.NODE_REMOVED
                | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
        final boolean isDeep = false;
        final boolean noLocal = true;
        session.getWorkspace().getObservationManager().addEventListener(this, eventTypes, path,
                isDeep, null, null, noLocal);
        
        log.info("Watching folder " + path);
    }
    
    void cleanup() {
    	try {
	    	session.getWorkspace().getObservationManager().removeEventListener(this);
	    	session.logout();
    	} catch(RepositoryException re) {
    		log.warn("RepositoryException in cleanup()", re);
    	}
    }
    
    @Override
    public String toString() {
    	return getClass().getSimpleName() + ":" + path;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WatchedFolder)) {
            return false;
        }

        final WatchedFolder other = (WatchedFolder) obj;
        return path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
    
    String getPath() {
        return path;
    }
    
    /** Set our timer whenever an event occurs, to wait
     * 	a bit before processing event bursts.
     */
    public void onEvent(EventIterator it) {
        nextScan = System.currentTimeMillis() + scanDelayMsec;
    }
    
    /**
     * 	If our timer allows it, scan our folder Node for updates
     * 	and deletes.
     */
    void scanIfNeeded() throws Exception {
        if (nextScan != -1 && System.currentTimeMillis() > nextScan) {
            nextScan = -1;
        }
        scan();
    }
    
    /** Scan our folder and inform OsgiController of any changes */
    protected void scan() throws Exception {
        log.debug("Scanning {}", path);
        
        checkDeletions();
        
        Node folder = null;
        if(session.itemExists(path)) {
        	Item i = session.getItem(path);
        	if(i.isNode()) {
        		folder = (Node)i;
        	}
        }
        
        if(folder == null) {
        	log.info("Folder {} does not exist (or not anymore), cannot scan", path);
        	return;
        }
        
        // Check adds and updates, for all child nodes that are files
        final NodeIterator it = folder.getNodes();
        while(it.hasNext()) {
        	final Node n = it.nextNode();
        	final FileDataProvider dp = new FileDataProvider(n);
        	if(!dp.isFile()) {
        		log.debug("Node {} does not seem to be a file, ignored", n.getPath());
        	}
        	installOrUpdate(n.getPath(), dp.getInputStream(), dp.getLastModified());
        }
    }
    
    /** Check for deleted resources and uninstall them */
    void checkDeletions() throws Exception {
        // Check deletions
        for(String uri : controller.getInstalledUris()) {
            if(uri.startsWith(path)) {
                if(!session.itemExists(uri)) {
                    log.debug("Resource {} has been deleted, uninstalling");
                    controller.uninstall(uri);
                }
            }
        }
    }
    
    /** Install or update the given resource, as needed */ 
    protected void installOrUpdate(String path, InputStream data, Long lastModified) throws IOException, JcrInstallException {
    	final long currentLastModified = controller.getLastModified(path);
    	if(currentLastModified == -1) {
    		log.info("Resource {} was not installed yet, installing in OsgiController", path);
    		controller.installOrUpdate(path, lastModified, data);
    	} else if(currentLastModified < lastModified) {
    		log.info("Resource {} has been updated, updating in OsgiController", path);
    		controller.installOrUpdate(path, lastModified, data);
    	} else {
    		log.info("Resource {} not modified, ignoring", path);
    	}
    }
}
