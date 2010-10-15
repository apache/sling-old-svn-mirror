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
import javax.jcr.query.qom.QueryObjectModelFactory;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.jobs.jcr.JCRHelper;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This event handler distributes events across an application cluster.
 *
 * We schedule this event handler to run in the background and clean up
 * obsolete events.
 */
@Component(label="%dist.events.name",
           description="%dist.events.description",
           immediate=true,
           metatype=true,
           policy=ConfigurationPolicy.REQUIRE)
@Service(value=Runnable.class)
@Properties({
    @Property(name="event.topics",value="*",propertyPrivate=true),
    @Property(name="event.filter",value="(event.distribute=*)",propertyPrivate=true),
    @Property(name="repository.path",value="/var/eventing/distribution",propertyPrivate=true),
    @Property(name="scheduler.period", longValue=1800),
    @Property(name="scheduler.concurrent", boolValue=false, propertyPrivate=true)
})
public class DistributingEventHandler
    extends AbstractRepositoryEventHandler
    implements Runnable {

    /** Default clean up time is 15 minutes. */
    private static final int DEFAULT_CLEANUP_PERIOD = 15;

    @Property(intValue=DEFAULT_CLEANUP_PERIOD)
    private static final String CONFIG_PROPERTY_CLEANUP_PERIOD = "cleanup.period";

    /** We remove everything which is older than 15min by default. */
    private int cleanupPeriod = DEFAULT_CLEANUP_PERIOD;

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#activate(org.osgi.service.component.ComponentContext)
     */
    protected void activate(ComponentContext context) {
        @SuppressWarnings("unchecked")
        final Dictionary<String, Object> props = context.getProperties();
        this.cleanupPeriod = OsgiUtil.toInteger(props.get(CONFIG_PROPERTY_CLEANUP_PERIOD), DEFAULT_CLEANUP_PERIOD);
        super.activate(context);
    }

    /**
     * Return the query for the clean up.
     */
    protected Query getCleanUpQuery(final Session s)
    throws RepositoryException {
        final String selectorName = "nodetype";
        final Calendar deleteBefore = Calendar.getInstance();
        deleteBefore.add(Calendar.MINUTE, -this.cleanupPeriod);

        final QueryObjectModelFactory qomf = s.getWorkspace().getQueryManager().getQOMFactory();

        final Query q = qomf.createQuery(
                qomf.selector(getEventNodeType(), selectorName),
                qomf.and(qomf.descendantNode(selectorName, this.repositoryPath),
                         qomf.comparison(qomf.propertyValue(selectorName, JCRHelper.NODE_PROPERTY_CREATED),
                                       QueryObjectModelFactory.JCR_OPERATOR_LESS_THAN,
                                       qomf.literal(s.getValueFactory().createValue(deleteBefore)))),
                null,
                null
        );
        return q;
    }

    /**
     * This method is invoked periodically.
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if ( this.cleanupPeriod > 0 ) {
            this.logger.debug("Cleaning up repository, removing all entries older than {} minutes.", this.cleanupPeriod);

            // we create an own session for concurrency issues
            Session s = null;
            try {
                s = this.environment.createAdminSession();
                final Query q = this.getCleanUpQuery(s);
                if ( logger.isDebugEnabled() ) {
                    logger.debug("Executing query {}", q.getStatement());
                }
                final NodeIterator iter = q.execute().getNodes();
                int count = 0;
                while ( iter.hasNext() ) {
                    final Node eventNode = iter.nextNode();
                    eventNode.remove();
                    count++;
                }
                s.save();
                logger.debug("Removed {} entries from the repository.", count);

            } catch (RepositoryException e) {
                // in the case of an error, we just log this as a warning
                this.logger.warn("Exception during repository cleanup.", e);
            } finally {
                if ( s != null ) {
                    s.logout();
                }
            }
        }
    }

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#processWriteQueue()
     */
    protected void processWriteQueue() {
        while ( this.running ) {
            // so let's wait/get the next job from the queue
            Event event = null;
            try {
                event = this.writeQueue.take();
            } catch (InterruptedException e) {
                // we ignore this
                this.ignoreException(e);
            }
            if ( event != null && this.running ) {
                try {
                    this.writeEvent(event, null);
                } catch (Exception e) {
                    this.logger.error("Exception during writing the event to the repository.", e);
                }
            }
        }
    }

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#runInBackground()
     */
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
                if ( info.nodePath != null) {
                    Session session = null;
                    try {
                        session = this.environment.createAdminSession();
                        final Node eventNode = (Node)session.getItem(info.nodePath);
                        if ( eventNode.isNodeType(this.getEventNodeType()) ) {
                            final EventAdmin localEA = this.environment.getEventAdmin();
                            if ( localEA != null ) {
                                localEA.postEvent(this.readEvent(eventNode));
                            } else {
                                this.logger.error("Unable to post event as no event admin is available.");
                            }
                        }
                    } catch (Exception ex) {
                        this.logger.error("Exception during reading the event from the repository.", ex);
                    } finally {
                        if ( session != null ) {
                            session.logout();
                        }
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
            this.writeQueue.put(event);
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
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#addEventProperties(javax.jcr.Node, java.util.Dictionary)
     */
    protected void addEventProperties(Node eventNode, Dictionary<String, Object> properties)
    throws RepositoryException {
        super.addEventProperties(eventNode, properties);
        properties.put(EventUtil.PROPERTY_APPLICATION, eventNode.getProperty(JCRHelper.NODE_PROPERTY_APPLICATION).getString());
    }


    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#startWriterSession()
     */
    protected void startWriterSession() throws RepositoryException {
        super.startWriterSession();
        this.writerSession.getWorkspace().getObservationManager()
            .addEventListener(this, javax.jcr.observation.Event.NODE_ADDED, this.repositoryPath, true, null, null, true);
    }
}
