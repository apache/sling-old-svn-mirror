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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.osgi.installer.InstallableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Watch a single folder in the JCR Repository, detecting changes
 *  to it and providing InstallableData for its contents.
 */
class WatchedFolder implements EventListener{
    private final String path;
    private final int priority;
    private final Session session;
    private static long nextScanTime;
    private boolean needsScan;
    private final String urlScheme;
    private final Collection <JcrInstaller.NodeConverter> converters;
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    // WatchedFolders that need a rescan will be scanned
    // once no JCR events have been received for this amount of time.
    public static final long SCAN_DELAY_MSEC = 500L;
    
    WatchedFolder(Session session, String path, int priority, 
    		String urlScheme, Collection<JcrInstaller.NodeConverter> converters) throws RepositoryException {
        this.path = path;
        this.converters = converters;
        this.priority = priority;
        this.urlScheme = urlScheme;
        
        this.session = session;
        
        // observe any changes in our folder (and under it, as changes to properties
        // might be lower in the hierarchy)
        final int eventTypes = Event.NODE_ADDED | Event.NODE_REMOVED
                | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
        final boolean isDeep = true;
        final boolean noLocal = true;
        session.getWorkspace().getObservationManager().addEventListener(this, eventTypes, path,
                isDeep, null, null, noLocal);

        log.info("Watching folder " + path);
    }
    
    void cleanup() {
    	try {
	    	session.getWorkspace().getObservationManager().removeEventListener(this);
    	} catch(RepositoryException re) {
    		log.warn("RepositoryException in cleanup()", re);
    	}
    }
    
    @Override
    public String toString() {
    	return getClass().getSimpleName() + ":" + path;
    }
    
    String getPath() {
        return path;
    }
    
    /** Set a static "timer" whenever an event occurs */
    public void onEvent(EventIterator it) {
    	nextScanTime = System.currentTimeMillis() + SCAN_DELAY_MSEC; 
    	needsScan = true;
    	log.debug("Event received, scheduling scan of {}", path);
    }
    
    boolean needsScan() {
    	return needsScan;
    }
    
    long getNextScanTime() {
    	return nextScanTime;
    }
    
    /** Scan the contents of our folder and return the corresponding InstallableResource */
    List<InstallableResource> scan() throws Exception {
        log.debug("Scanning {}", path);
        
        Node folder = null;
        if(session.itemExists(path)) {
        	Item i = session.getItem(path);
        	if(i.isNode()) {
        		folder = (Node)i;
        	}
        }
        
        if(folder == null) {
        	log.info("Folder {} does not exist (or not anymore), cannot scan", path);
        	return null;
        }
        
        // Return an InstallableResource for all child nodes for which we have a NodeConverter
        final List<InstallableResource> result = new ArrayList<InstallableResource>();
        final NodeIterator it = folder.getNodes();
        while(it.hasNext()) {
        	final Node n = it.nextNode();
        	for(JcrInstaller.NodeConverter nc : converters) {
        		final InstallableResource r = nc.convertNode(urlScheme, n);
        		if(r != null) {
        			r.setPriority(priority);
        			result.add(r);
        			break;
        		}
        	}
        }
        
        return result;
    }
}
