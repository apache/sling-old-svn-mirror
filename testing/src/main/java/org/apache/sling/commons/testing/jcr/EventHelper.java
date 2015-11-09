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
package org.apache.sling.commons.testing.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

/** Used by tests to wait until JCR notification events
 * 	have been delivered.
 */
public class EventHelper implements EventListener {
	private final Session session;
	private int eventCount;
	public static final String WAIT_NODE_FOLDER = "WAIT_NODE";
	public static final String WAIT_NODE_NODE = EventHelper.class.getSimpleName();
	private final Node waitNodeFolder;

	public EventHelper(Session s) throws RepositoryException {
		session = s;

        final int eventTypes = Event.NODE_ADDED | Event.NODE_REMOVED;
        final boolean isDeep = true;
        final boolean noLocal = false;
        session.getWorkspace().getObservationManager().addEventListener(
        		this, eventTypes, "/" + WAIT_NODE_FOLDER, isDeep, null, null, noLocal);

        if(session.getRootNode().hasNode(WAIT_NODE_FOLDER)) {
        	waitNodeFolder = session.getRootNode().getNode(WAIT_NODE_FOLDER);
        } else {
        	waitNodeFolder = session.getRootNode().addNode(WAIT_NODE_FOLDER, "nt:unstructured");
        }
        session.save();
	}

    public void onEvent(EventIterator it) {
        eventCount++;
    }

    /** To make sure observation events have been delivered,
     * 	create or delete a a node and wait for the corresponding
     * 	events to be received.
     */
	public void waitForEvents(long timeoutMsec) throws RepositoryException {
		final int targetEventCount = eventCount + 1;

		if(waitNodeFolder.hasNode(WAIT_NODE_NODE)) {
			waitNodeFolder.getNode(WAIT_NODE_NODE).remove();
		} else {
			waitNodeFolder.addNode(WAIT_NODE_NODE);
		}
		session.save();

    	final long end = System.currentTimeMillis() + timeoutMsec;
    	while(eventCount < targetEventCount && System.currentTimeMillis() < end) {
    		try {
    			Thread.sleep(100);
    		} catch(InterruptedException ignored) {
    		}
    	}

    	if(eventCount < targetEventCount) {
    		throw new IllegalStateException("Event counter did not reach " + targetEventCount + ", waited " + timeoutMsec + " msec");
    	}
    }

    /**
     * Remove the event listener from the observation listener.
     */
    public void dispose() {
        try {
            session.getWorkspace().getObservationManager().removeEventListener(this);
        } catch (RepositoryException e) {
        }
    }
}
