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
package org.apache.sling.event.impl;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;


/**
 * An event handler for timed events.
 *
 * scr.component inherit="true"
 * @scr.property name="event.topics" value="org/apache/sling/event/timed"
 * @scr.property name="repository.path" value="/sling/timed-events"
 */
public abstract class TimedEventHandler
    extends AbstractRepositoryEventHandler{


    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#activate(org.osgi.service.component.ComponentContext)
     */
    protected void activate(ComponentContext context)
    throws RepositoryException {
        super.activate(context);
        // load timed events from repository
        this.loadEvents();
    }

    /**
     * Start the repository session and add this handler as an observer
     * for new events created on other nodes.
     * @throws RepositoryException
     */
    protected void startSession() throws RepositoryException {
        super.startSession();
        this.session.getWorkspace().getObservationManager()
            .addEventListener(this, javax.jcr.observation.Event.PROPERTY_CHANGED, this.repositoryPath, true, null, null, true);
    }

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#cleanUpRepository()
     */
    protected void cleanUpRepository() {
        // nothing to do right now
    }

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#runInBackground()
     */
    protected void runInBackground() {
        while ( this.running ) {
            // so let's wait/get the next info from the queue
            EventInfo info = null;
            try {
                info = this.queue.take();
            } catch (InterruptedException e) {
                // we ignore this
                this.ignoreException(e);
            }
            if ( info != null && this.running ) {
                try {
                    this.session.refresh(true);
                    final Node eventNode = (Node) this.session.getItem(info.nodePath);
                    if ( !eventNode.isLocked() ) {
                        // lock node
                        Lock lock = null;
                        try {
                            lock = eventNode.lock(false, true);
                        } catch (RepositoryException re) {
                            // lock failed which means that the node is locked by someone else, so we don't have to requeue
                        }
                        if ( lock != null ) {
                            this.processEvent(info.event, eventNode);
                        }
                    }
                } catch (RepositoryException e) {
                    // ignore
                    this.ignoreException(e);
                }
            }
        }
    }

    protected void processEvent(Event event, Node eventNode) {
        // TODO
    }

    public void onEvent(EventIterator events) {
        // TODO Auto-generated method stub

    }

    public void handleEvent(Event event) {
        // TODO Auto-generated method stub

    }

    /**
     * Load all active timed events from the repository.
     * @throws RepositoryException
     */
    protected void loadEvents() throws RepositoryException {
        final QueryManager qManager = this.session.getWorkspace().getQueryManager();
        final StringBuffer buffer = new StringBuffer("/jcr:root");
        buffer.append(this.repositoryPath);
        buffer.append("//element(*, ");
        buffer.append(this.getEventNodeType());
        buffer.append(")");
        final Query q = qManager.createQuery(buffer.toString(), Query.XPATH);
        final NodeIterator result = q.execute().getNodes();
        while ( result.hasNext() ) {
            final Node eventNode = result.nextNode();
            if ( !eventNode.isLocked() ) {
                final Event event = this.readEvent(eventNode);
                final EventInfo info = new EventInfo();
                info.event = event;
                info.nodePath = eventNode.getPath();
                try {
                    this.queue.put(info);
                } catch (InterruptedException e) {
                    // we ignore this exception as this should never occur
                    this.ignoreException(e);
                }
            }
        }
    }

}
