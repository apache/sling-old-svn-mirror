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
package org.apache.sling.jcr.jcrbundles;

import static org.apache.sling.jcr.jcrbundles.JcrBundlesConstants.BUNDLES_NODENAME;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** EventListener that collects the paths of new "bundles"
 *  nodes.
 */
class BundlesFolderCreationListener implements EventListener {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private Set<String> paths = new HashSet<String>();
    
    /** Return our saved paths and clear the list */
    Set<String> getAndClearPaths() {
        synchronized(paths) {
            Set<String> result = paths; 
            paths = new HashSet<String>();
            return result;
        }
    }
    
    /** Store the paths of new "bundles" nodes */
    public void onEvent(EventIterator it) {
        try {
            while(it.hasNext()) {
                final Event e = it.nextEvent();
                if(e.getPath().endsWith("/" + BUNDLES_NODENAME)) {
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
