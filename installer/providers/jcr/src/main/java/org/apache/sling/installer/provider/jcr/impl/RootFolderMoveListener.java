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

import org.apache.jackrabbit.api.observation.JackrabbitEventFilter;
import org.apache.jackrabbit.api.observation.JackrabbitObservationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import java.util.Arrays;

/**
 * Listen for JCR move events under one of our roots in order to detect installer artifacts that are moved from/to
 * the installer roots.
 */
class RootFolderMoveListener implements EventListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final RescanTimer timer;

    private final String[] watchedPaths;
    
    private EventListener toCleanup;

    RootFolderMoveListener(final Session session, final String[] rootFolders,  final RescanTimer timer) throws RepositoryException {
        this.timer = timer;
        this.watchedPaths = rootFolders;

        if (watchedPaths != null && watchedPaths.length > 0) {
            JackrabbitEventFilter eventFilter = new JackrabbitEventFilter()
                    .setAdditionalPaths(watchedPaths)
                    .setEventTypes(Event.NODE_MOVED)
                    .setIsDeep(true)
                    .setNoLocal(true)
                    .setNoExternal(false);
            ObservationManager obsManager = session.getWorkspace().getObservationManager();
            if(obsManager instanceof  JackrabbitObservationManager){
                JackrabbitObservationManager observationManager = (JackrabbitObservationManager)obsManager;
                observationManager.addEventListener(this, eventFilter);
                toCleanup = this;
                logger.info("Watching {} to detect move changes in subfolders", Arrays.toString(watchedPaths));
            } else {
                logger.warn("ObservationManager is not a JackrabbitObservationManager, won't observe moves for {}", 
                        Arrays.asList(rootFolders));
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + Arrays.asList(watchedPaths) + ")";
    }

    void cleanup(final Session session) throws RepositoryException {
        if(toCleanup != null) {
            session.getWorkspace().getObservationManager().removeEventListener(toCleanup);
        }
    }

    /**
     * Schedule a scan.
     */
    public void onEvent(final EventIterator events) {
        timer.scheduleScan();
    }
}
