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

import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listen for JCR events under one of our roots, to find out
 * when new WatchedFolders must be created, or when some might
 * have been deleted.
 */
class RootFolderListener implements EventListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final RescanTimer timer;
    private final String watchedPath;

    private final InstallerConfig cfg;

    private final String pathWithSlash;

    RootFolderListener(final Session session, final String path, final RescanTimer timer, final InstallerConfig cfg)
    throws RepositoryException {
        this.timer = timer;
        this.watchedPath = path;
        this.pathWithSlash = path;
        this.cfg = cfg;

        final int eventTypes = Event.NODE_ADDED | Event.NODE_REMOVED
                | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED;
        final boolean isDeep = true;
        final boolean noLocal = true;
        session.getWorkspace().getObservationManager().addEventListener(this, eventTypes, watchedPath,
                isDeep, null, null, noLocal);

        logger.info("Watching {} to detect potential changes in subfolders", watchedPath);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + watchedPath + ")";
    }

    void cleanup(final Session session) throws RepositoryException {
        session.getWorkspace().getObservationManager().removeEventListener(this);
    }

    /**
     * Schedule a scan.
     */
    public void onEvent(final EventIterator it) {
        // we only do the global scan for node changes
        boolean globalScan = false;
        // copy watched folders and remove all for other roots
        final List<WatchedFolder> checkFolders = cfg.cloneWatchedFolders();
        final Iterator<WatchedFolder> i = checkFolders.iterator();
        while ( i.hasNext() ) {
            final WatchedFolder wf = i.next();
            if ( !wf.getPathWithSlash().startsWith(this.pathWithSlash)) {
                i.remove();
            }
        }
        while(it.hasNext()) {
            final Event e = it.nextEvent();
            logger.debug("Got event {}", e);
            if ( e.getType() == Event.NODE_ADDED || e.getType() == Event.NODE_REMOVED ) {
                globalScan = true;
            }
            try {
                final String path = e.getPath();

                final Iterator<WatchedFolder> ii = checkFolders.iterator();
                while ( ii.hasNext() ) {
                    final WatchedFolder folder = ii.next();
                    if ( path.startsWith(folder.getPathWithSlash()) ) {
                        folder.markForScan();
                        ii.remove();
                        break;
                    }
                }
            } catch ( final RepositoryException re ) {
                logger.warn("Error while getting path from event", re);
            }
        }
        if ( globalScan ) {
            timer.scheduleScan();
        }
    }
}
