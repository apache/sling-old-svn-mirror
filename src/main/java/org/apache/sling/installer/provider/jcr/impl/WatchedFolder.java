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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.sling.installer.api.InstallableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Watch a single folder in the JCR Repository, detecting changes
 *  to it and providing InstallableData for its contents.
 */
class WatchedFolder implements EventListener{

    private final String path;
    private final int priority;
    private final Session session;
    private static RescanTimer rescanTimer = new RescanTimer();
    private boolean needsScan;
    private final Collection <JcrInstaller.NodeConverter> converters;
    private final Set<String> existingResourceUrls = new HashSet<String>();
    protected final Logger log = LoggerFactory.getLogger(getClass());

    static class ScanResult {
        List<InstallableResource> toAdd = new ArrayList<InstallableResource>();
        List<String> toRemove = new ArrayList<String>();
    };

    /** Store the digests of the last returned resources, keyed by path, to detect changes */
    private final Map<String, String> digests = new HashMap<String, String>();

    WatchedFolder(final Session session,
            final String path,
            final int priority,
    		final Collection<JcrInstaller.NodeConverter> converters)
    throws RepositoryException {
        if(priority < 1) {
            throw new IllegalArgumentException("Cannot watch folder with priority 0:" + path);
        }

        this.path = path;
        this.converters = converters;
        this.priority = priority;

        this.session = session;

        // observe any changes in our folder (and under it, as changes to properties
        // might be lower in the hierarchy)
        final int eventTypes = Event.NODE_ADDED | Event.NODE_REMOVED
                | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
        final boolean isDeep = true;
        final boolean noLocal = true;
        session.getWorkspace().getObservationManager().addEventListener(this, eventTypes, path,
                isDeep, null, null, noLocal);

        log.info("Watching folder {} (priority {})", path, priority);
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
        log.debug("JCR event received for path {}", path);
    	scheduleScan();
    }

    void scheduleScan() {
        log.debug("Scheduling scan of {}", path);
        rescanTimer.scheduleScan();
        needsScan = true;
    }

    boolean needsScan() {
    	return needsScan;
    }

    static RescanTimer getRescanTimer() {
    	return rescanTimer;
    }

    /** Scan the contents of our folder and return the corresponding InstallableResource */
    ScanResult scan() throws RepositoryException {
        log.debug("Scanning {}", path);
        needsScan = false;

        Node folder = null;
        if(session.itemExists(path)) {
        	Item i = session.getItem(path);
        	if(i.isNode()) {
        		folder = (Node)i;
        	}
        }

        // Return an InstallableResource for all child nodes for which we have a NodeConverter
        final ScanResult result = new ScanResult();
        final Set<String> resourcesSeen = new HashSet<String>();
        if(folder != null) {
            final NodeIterator it = folder.getNodes();
            while(it.hasNext()) {
            	final Node n = it.nextNode();
            	for(JcrInstaller.NodeConverter nc : converters) {
            		final InstallableResource r = nc.convertNode(n, priority);
            		if(r != null) {
            			resourcesSeen.add(r.getId());
            		    final String oldDigest = digests.get(r.getId());
            		    if(r.getDigest().equals(oldDigest)) {
            		    	log.debug("Digest didn't change, ignoring " + r);
            		    } else {
                            result.toAdd.add(r);
            		    }
            			break;
            		}
            	}
            }
        }

        // Resources that existed but are not in resourcesSeen need to be
        // unregistered from OsgiInstaller
        for(String url : existingResourceUrls) {
        	if(!resourcesSeen.contains(url)) {
                result.toRemove.add(url);
        	}
        }
        for(String u : result.toRemove) {
        	existingResourceUrls.remove(u);
        	digests.remove(u);
        }

        // Update saved digests of the resources that we're returning
        for(InstallableResource r : result.toAdd) {
            existingResourceUrls.add(r.getId());
            digests.put(r.getId(), r.getDigest());
        }

        return result;
    }
}
