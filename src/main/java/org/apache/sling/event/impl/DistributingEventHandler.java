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

import java.util.Calendar;
import java.util.Dictionary;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.util.ISO8601;
import org.apache.sling.event.EventUtil;
import org.osgi.service.event.Event;

/**
 * This event handler distributes events across an application cluster.
 * @scr.component inherit="true"
 * @scr.property name="event.topics" value="*"
 * @scr.property name="event.filter" value="(event.distribute=*)"
 * @scr.property name="repository.path" value="/sling/events"
 */
public class DistributingEventHandler
    extends AbstractRepositoryEventHandler {

    /**
     * @see org.apache.sling.core.event.impl.JobPersistenceHandler#cleanUpRepository()
     */
    protected void cleanUpRepository() {
        // we create an own session for concurrency issues
        Session s = null;
        try {
            s = this.createSession();
            final Node parentNode = (Node)s.getItem(this.repositoryPath);
            final Calendar deleteBefore = Calendar.getInstance();
            deleteBefore.add(Calendar.MINUTE, -this.cleanupPeriod);
            final String dateString = ISO8601.format(deleteBefore);

            final QueryManager qManager = parentNode.getSession().getWorkspace().getQueryManager();
            final StringBuffer buffer = new StringBuffer("/jcr:root");
            buffer.append(this.repositoryPath);
            buffer.append("//element(*, ");
            buffer.append(getEventNodeType());
            buffer.append(") [");
            buffer.append(EventHelper.NODE_PROPERTY_CREATED);
            buffer.append(" < '");
            buffer.append(dateString);
            buffer.append("']");

            final Query q = qManager.createQuery(buffer.toString(), Query.XPATH);
            final NodeIterator iter = q.execute().getNodes();
            while ( iter.hasNext() ) {
                final Node eventNode = iter.nextNode();
                eventNode.remove();
            }
            parentNode.save();
        } catch (RepositoryException e) {
            // in the case of an error, we just log this as a warning
            this.logger.warn("Exception during repository cleanup.", e);
        } finally {
            if ( s != null ) {
                s.logout();
            }
        }
    }

    protected void runInBackground() {
        while ( this.running ) {
            // so let's wait/get the next job from the queue
            EventInfo info = null;
            try {
                info = this.queue.take();
            } catch (InterruptedException e) {
                // we ignore this
                this.ignoreException(e);
            }
            if ( info != null && this.running ) {
                if ( info.event != null ) {
                    try {
                        this.writeEvent(info.event);
                    } catch (Exception e) {
                        this.logger.error("Exception during writing the event to the repository.", e);
                    }
                } else if ( info.nodePath != null) {
                    try {
                        final Node eventNode = (Node) this.session.getItem(info.nodePath);
                        this.eventAdmin.postEvent(this.readEvent(eventNode));
                    } catch (Exception ex) {
                        this.logger.error("Exception during reading the event from the repository.", ex);
                    }
                }
            }
        }
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(final Event event) {
        try {
            final EventInfo info = new EventInfo();
            info.event = event;
            this.queue.put(info);
        } catch (InterruptedException ex) {
            // we ignore this
            this.ignoreException(ex);
        }
    }

    /**
     * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
     */
    public void onEvent(final EventIterator iterator) {
        while ( iterator.hasNext() ) {
            final javax.jcr.observation.Event event = iterator.nextEvent();
            try {
                final EventInfo info = new EventInfo();
                info.nodePath = event.getPath();
                this.queue.put(info);
            } catch (InterruptedException ex) {
                // we ignore this
                this.ignoreException(ex);
            } catch (RepositoryException ex) {
                this.logger.error("Exception during reading the event from the repository.", ex);
            }
        }
    }

    /**
     * @see org.apache.sling.core.event.impl.JobPersistenceHandler#addEventProperties(Node, java.util.Dictionary)
     */
    protected void addEventProperties(Node eventNode, Dictionary<String, Object> properties)
    throws RepositoryException {
        super.addEventProperties(eventNode, properties);
        properties.put(EventUtil.PROPERTY_APPLICATION, eventNode.getProperty(EventHelper.NODE_PROPERTY_APPLICATION).getString());
    }


    /**
     * Start the repository session and add this handler as an observer
     * for new events created on other nodes.
     * @throws RepositoryException
     */
    protected void startSession() throws RepositoryException {
        super.startSession();
        this.session.getWorkspace().getObservationManager()
            .addEventListener(this, javax.jcr.observation.Event.NODE_ADDED, this.repositoryPath, true, null, null, true);
    }
}
