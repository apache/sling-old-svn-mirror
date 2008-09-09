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
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Listen for JCR events to find out when new WatchedFolders
 * 	must be created.
 */
class WatchedFolderCreationListener implements EventListener {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private final FolderNameFilter folderNameFilter;
    private Set<String> paths = new HashSet<String>();
    
    WatchedFolderCreationListener(FolderNameFilter fnf) {
    	folderNameFilter = fnf;
	}
    
    /** Return our saved paths and clear the list
     * 	@return null if no paths have been saved 
     */
    Set<String> getAndClearPaths() {
    	if(paths.isEmpty()) {
    		return null;
    	}
    	
        synchronized(paths) {
            Set<String> result = paths; 
            paths = new HashSet<String>();
            return result;
        }
    }
    
    /** Store the paths of new WatchedFolders to create */
    public void onEvent(EventIterator it) {
        try {
            while(it.hasNext()) {
                final Event e = it.nextEvent();
                if(folderNameFilter.accept(e.getPath())) {
                    synchronized(paths) {
                        paths.add(e.getPath());
                    }
                }
            }
        } catch(RepositoryException re) {
            log.warn("RepositoryException in onEvent", re);
        }
    }

}
