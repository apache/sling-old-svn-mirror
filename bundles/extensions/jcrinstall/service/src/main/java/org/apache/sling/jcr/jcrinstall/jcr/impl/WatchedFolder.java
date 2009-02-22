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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.jcrinstall.jcr.NodeConverter;
import org.apache.sling.jcr.jcrinstall.osgi.InstallableData;
import org.apache.sling.jcr.jcrinstall.osgi.JcrInstallException;
import org.apache.sling.jcr.jcrinstall.osgi.OsgiController;
import org.apache.sling.jcr.jcrinstall.osgi.ResourceOverrideRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Watch a single folder in the JCR Repository, detecting changes
 *  to it (non-recursively) and sending the appropriate messages
 *  to the OsgiController service.
 */
class WatchedFolder implements EventListener, Comparable<Object> {
    private final String path;
    private final OsgiController controller;
    private long nextScan;
    private final Session session;
    private final ResourceOverrideRules roRules;
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    private static List<WatchedFolder> allFolders = new ArrayList<WatchedFolder>();
    
    /**
     * After receiving JCR events, we wait for this many msec before
     * re-scanning the folder, as events often come in bursts.
     */
    private final long scanDelayMsec;

    WatchedFolder(SlingRepository repository, String path, OsgiController ctrl, 
            RegexpFilter filenameFilter, long scanDelayMsec, ResourceOverrideRules ror) throws RepositoryException {
        this.path = path;
        this.controller = ctrl;
        this.scanDelayMsec = scanDelayMsec;
        this.roRules = ror;
        
        session = repository.loginAdministrative(repository.getDefaultWorkspace());
        
        // observe any changes in our folder (and under it, as changes to properties
        // might be lower in the hierarchy)
        final int eventTypes = Event.NODE_ADDED | Event.NODE_REMOVED
                | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
        final boolean isDeep = true;
        final boolean noLocal = true;
        session.getWorkspace().getObservationManager().addEventListener(this, eventTypes, path,
                isDeep, null, null, noLocal);

        synchronized(allFolders) {
            allFolders.add(this);
        }
        log.info("Watching folder " + path);
    }
    
    void cleanup() {
        synchronized(allFolders) {
            allFolders.remove(this);
        }
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
    
    public int compareTo(Object obj) {
        if (!(obj instanceof WatchedFolder)) {
            return 1;
        }
        final WatchedFolder other = (WatchedFolder) obj;
		return path.compareTo(other.path);
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
        scheduleScan();
    }
    
    /** Trigger a scan, after the usual delay */
    void scheduleScan() {
        nextScan = System.currentTimeMillis() + scanDelayMsec;
    }
    
    /**
     * 	If our timer allows it, scan our folder Node for updates
     * 	and deletes.
     */
    void scanIfNeeded(Collection<NodeConverter> converters) throws Exception {
        if (nextScan != -1 && System.currentTimeMillis() > nextScan) {
            nextScan = -1;
            scan(converters);
        }
    }
    
    /** Scan our folder and inform OsgiController of any changes */
    protected void scan(Collection<NodeConverter> converters) throws Exception {
        log.debug("Scanning {}", path);
        
        checkDeletions(controller.getInstalledUris());
        
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
        
        // Check adds and updates, for all child nodes for which we have a NodeConverter
        final NodeIterator it = folder.getNodes();
        while(it.hasNext()) {
        	final Node n = it.nextNode();
        	for(NodeConverter nc : converters) {
        		final InstallableData d = nc.convertNode(n);
        		if(d != null) {
        			log.debug("Installing or updating {}", d);
            		// a single failure must not block the whole thing (SLING-655)
            		try {
            			installOrUpdate(n.getPath(), d);
            		} catch(JcrInstallException jie) {
            			log.warn("Failed to install resource " + n.getPath(), jie);
            		}
            		break;
        		}
        	}
        }
    }
    
    /** Check for deleted resources and uninstall them */
    void checkDeletions(Set<String> installedUri) throws Exception {
        // Check deletions
        int count = 0;
        for(String uri : installedUri) {
            if(uri.startsWith(path)) {
                if(!session.itemExists(uri)) {
                    count++;
                    log.info("Resource {} has been deleted, uninstalling", uri);
            		// a single failure must not block the whole thing (SLING-655)
                    try {
                    	controller.scheduleUninstall(uri);
                    } catch(JcrInstallException jie) {
                    	log.warn("Failed to uninstall " + uri, jie);
                    }
                }
            }
        }

        // If any deletions, resources in lower/higher priority folders might need to
        // be re-installed
        if(count > 0 && roRules!=null) {
            for(String str : roRules.getLowerPriorityResources(path)) {
                rescanFoldersForPath(str, "Scheduling scan of lower priority {} folder after deletes in {} folder");
            }
            for(String str : roRules.getHigherPriorityResources(path)) {
                rescanFoldersForPath(str, "Scheduling scan of higher priority {} folder after deletes in {} folder");
            }
        }
    }
    
    private void rescanFoldersForPath(String pathToScan, String logFormat) {
        for(WatchedFolder wf : allFolders) {
            if(pathToScan.equals(wf.path)) {
                log.info(logFormat, wf.path, pathToScan);
                wf.scheduleScan();
            }
        }
    }
    
    /** Install or update the given resource, as needed */ 
    protected void installOrUpdate(String path, InstallableData fdp) throws IOException, JcrInstallException {
    	final String digest = controller.getDigest(path);
    	if(digest == null) {
    		log.info("Resource {} was not installed yet, installing in OsgiController", path);
    		controller.scheduleInstallOrUpdate(path, fdp);
    	} else if(!digest.equals(fdp.getDigest())) {
    		log.info("Resource {} has been updated, updating in OsgiController", path);
    		controller.scheduleInstallOrUpdate(path, fdp);
    	} else {
    		log.info("Resource {} not modified, ignoring", path);
    	}
    }
}
