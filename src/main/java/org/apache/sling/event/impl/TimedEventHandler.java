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

import java.io.Serializable;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.util.Locked;
import org.apache.sling.event.EventUtil;
import org.apache.sling.scheduler.Job;
import org.apache.sling.scheduler.JobContext;
import org.apache.sling.scheduler.Scheduler;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


/**
 * An event handler for timed events.
 *
 * @scr.component inherit="true"
 * @scr.property name="event.topics" value="org/apache/sling/event/timed"
 * @scr.property name="repository.path" value="/sling/timed-events"
 */
public class TimedEventHandler
    extends AbstractRepositoryEventHandler
    implements Job {

    /** @scr.reference */
    protected Scheduler scheduler;

    /** @scr.reference */
    protected EventAdmin eventAdmin;

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
                ScheduleInfo scheduleInfo = null;
                try {
                    scheduleInfo = new ScheduleInfo(info.event);
                } catch (IllegalArgumentException iae) {
                    this.logger.error(iae.getMessage());
                }
                if ( scheduleInfo != null ) {
                    try {
                        // if the node path is null, this is a new event
                        if ( info.nodePath == null ) {
                            // write event and update path
                            // if something went wrong we get the node path and reschedule
                            info.nodePath = this.persistEvent(info.event, scheduleInfo);
                            if ( info.nodePath != null ) {
                                try {
                                    this.queue.put(info);
                                } catch (InterruptedException e) {
                                    // this should never happen, so we ignore it
                                    this.ignoreException(e);
                                }
                            }
                        } else {
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
                                    // if something went wrong, we reschedule
                                    if ( !this.processEvent(info.event, scheduleInfo) ) {
                                        try {
                                            this.queue.put(info);
                                        } catch (InterruptedException e) {
                                            // this should never happen, so we ignore it
                                            this.ignoreException(e);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (RepositoryException e) {
                        // ignore
                        this.ignoreException(e);
                    }
                }
            }
        }
    }

    protected String persistEvent(final Event event, final ScheduleInfo scheduleInfo) {
        try {
            final Node parentNode = (Node)this.session.getItem(this.repositoryPath);
            Lock lock = (Lock) new Locked() {

                protected Object run(Node node) throws RepositoryException {
                    final String jobId = scheduleInfo.getJobId();
                    // if there is a node, we know that there is exactly one node
                    final Node foundNode = queryJob(jobId);
                    if ( scheduleInfo.isStopEvent() ) {
                        // if this is a stop event, we should remove the node from the repository
                        // if there is no node someone else was faster and we can ignore this
                        if ( foundNode != null ) {
                            try {
                                foundNode.remove();
                                parentNode.save();
                            } catch (LockException le) {
                                // if someone else has the lock this is fine
                            }
                        }
                        // stop the scheduler
                        processEvent(event, scheduleInfo);
                        return null;
                    }
                    // we only write the event if this is a local one
                    if ( EventUtil.isLocal(event) ) {
                        // if node is not present, we'll write it, lock it and schedule the event
                        if ( foundNode == null ) {
                            final Node eventNode = writeEvent(event);
                            return eventNode.lock(false, true);
                        }
                        // node is already in repository, this is an error as we don't support updates
                        // of timed events!
                        logger.error("Timed event is already scheduled: " + event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_TOPIC) + " (" + scheduleInfo.getJobId() + ")");
                    }
                    return null;
                }
            }.with(parentNode, false);

            if ( lock != null ) {
                // if something went wrong, we reschedule
                if ( !this.processEvent(event, scheduleInfo) ) {
                    final String path = lock.getNode().getPath();
                    lock.getNode().unlock();
                    return path;
                }
            }
        } catch (RepositoryException re ) {
            // something went wrong, so let's log it
            this.logger.error("Exception during writing new job to repository.", re);
        } catch (InterruptedException e) {
            // This should never happen from the lock, so we ignore it
            this.ignoreException(e);
        }
        return null;
    }

    /**
     * Process the event.
     * If a scheduler is available, a job is scheduled or stopped.
     * @param event The incomming event.
     * @return
     */
    protected boolean processEvent(final Event event, final ScheduleInfo scheduleInfo) {
        if ( this.scheduler != null ) {
            // is this a stop event?
            if ( scheduleInfo.isStopEvent() ) {
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Stopping timed event " + event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_TOPIC) + "(" + scheduleInfo.getJobId() + ")");
                }
                try {
                    this.scheduler.removeJob(scheduleInfo.getJobId());
                } catch (NoSuchElementException nsee) {
                    // this can happen if the job is scheduled on another node
                    // so we can just ignore this
                }
                return true;
            }
            // we ignore remote job events
            if ( !EventUtil.isLocal(event) ) {
                return true;
            }

            // Create configuration for scheduled job
            final Map<String, Serializable> config = new HashMap<String, Serializable>();
            // copy properties
            final Hashtable<String, Object> properties = new Hashtable<String, Object>();
            config.put("topic", (String)event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_TOPIC));
            final String[] names = event.getPropertyNames();
            if ( names != null ) {
                for(int i=0; i<names.length; i++) {
                    properties.put(names[i], event.getProperty(names[i]));
                }
            }
            config.put("config", properties);

            try {
                if ( scheduleInfo.expression != null ) {
                    if ( this.logger.isDebugEnabled() ) {
                        this.logger.debug("Adding timed event " + config.get("topic") + "(" + scheduleInfo.getJobId() + ")" + " with cron expression " + scheduleInfo.expression);
                    }
                    this.scheduler.addJob(scheduleInfo.getJobId(), this, config, scheduleInfo.expression, false);
                } else if ( scheduleInfo.period != null ) {
                    if ( this.logger.isDebugEnabled() ) {
                        this.logger.debug("Adding timed event " + config.get("topic") + "(" + scheduleInfo.getJobId() + ")" + " with period " + scheduleInfo.period);
                    }
                    this.scheduler.addPeriodicJob(scheduleInfo.getJobId(), this, config, scheduleInfo.period, false);
                } else {
                    // then it must be date
                    if ( this.logger.isDebugEnabled() ) {
                        this.logger.debug("Adding timed event " + config.get("topic") + "(" + scheduleInfo.getJobId() + ")" + " with date " + scheduleInfo.date);
                    }
                    this.scheduler.fireJobAt(scheduleInfo.getJobId(), this, config, scheduleInfo.date);
                }
                return true;
            } catch (Exception e) {
                this.ignoreException(e);
            }
        } else {
            this.logger.error("No scheduler available to start timed event " + event);
        }
        return false;
    }

    /**
     * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
     */
    public void onEvent(EventIterator iter) {
        // we create an own session here
        Session s = null;
        try {
            s = this.createSession();
            while ( iter.hasNext() ) {
                final javax.jcr.observation.Event event = iter.nextEvent();
                if ( event.getType() == javax.jcr.observation.Event.PROPERTY_CHANGED ) {
                    try {
                        final Node eventNode = (Node) s.getItem(event.getPath());
                        if ( !eventNode.isLocked() ) {
                            final EventInfo info = new EventInfo();
                            info.event = this.readEvent(eventNode);
                            info.nodePath = event.getPath();
                            try {
                                this.queue.put(info);
                            } catch (InterruptedException e) {
                                // we ignore this exception as this should never occur
                                this.ignoreException(e);
                            }
                        }
                    } catch (RepositoryException re) {
                        this.logger.error("Exception during jcr event processing.", re);
                    }
                }
            }
        } catch (RepositoryException re) {
            this.logger.error("Unable to create a session.", re);
        } finally {
            if ( s != null ) {
                s.logout();
            }
        }
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(Event event) {
        // queue the event in order to respond quickly
        final EventInfo info = new EventInfo();
        info.event = event;
        try {
            this.queue.put(info);
        } catch (InterruptedException e) {
            // this should never happen
            this.ignoreException(e);
        }
    }

    /**
     * @see org.apache.sling.scheduler.Job#execute(org.apache.sling.scheduler.JobContext)
     */
    public void execute(JobContext context) {
        final String topic = (String) context.getConfiguration().get("topic");
        final Dictionary<Object, Object> properties = (Dictionary<Object, Object>) context.getConfiguration().get("config");
        if ( this.eventAdmin != null ) {
            try {
                this.eventAdmin.postEvent(new Event(topic, properties));
            } catch (IllegalArgumentException iae) {
                this.logger.error("Scheduled event has illegal topic: " + topic, iae);
            }
        } else {
            this.logger.warn("Unable to send timed event as no event admin service is available.");
        }
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

    /**
     * @see org.apache.sling.core.event.impl.JobPersistenceHandler#addNodeProperties(javax.jcr.Node, org.osgi.service.event.Event)
     */
    protected void addNodeProperties(Node eventNode, Event event)
    throws RepositoryException {
        super.addNodeProperties(eventNode, event);
        eventNode.setProperty(EventHelper.NODE_PROPERTY_TOPIC, (String)event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_TOPIC));
        final ScheduleInfo info = new ScheduleInfo(event);
        eventNode.setProperty(EventHelper.NODE_PROPERTY_JOBID, info.getJobId());
    }

    /**
     * Search for a node with the corresponding topic and unique key.
     * @param topic
     * @param key
     * @return The node or null.
     * @throws RepositoryException
     */
    protected Node queryJob(String jobId) throws RepositoryException {
        final QueryManager qManager = this.session.getWorkspace().getQueryManager();
        final StringBuffer buffer = new StringBuffer("/jcr:root");
        buffer.append(this.repositoryPath);
        buffer.append("//element(*, ");
        buffer.append(this.getEventNodeType());
        buffer.append(") [");
        buffer.append(EventHelper.NODE_PROPERTY_JOBID);
        buffer.append(" = '");
        buffer.append(jobId);
        buffer.append("']");
        final Query q = qManager.createQuery(buffer.toString(), Query.XPATH);
        final NodeIterator result = q.execute().getNodes();
        Node foundNode = null;
        if ( result.hasNext() ) {
            foundNode = result.nextNode();
        }
        return foundNode;
    }

    /**
     * @see org.apache.sling.core.event.impl.JobPersistenceHandler#getContainerNodeType()
     */
    protected String getContainerNodeType() {
        return EventHelper.TIMED_EVENTS_NODE_TYPE;
    }

    /**
     * @see org.apache.sling.core.event.impl.JobPersistenceHandler#getEventNodeType()
     */
    protected String getEventNodeType() {
        return EventHelper.TIMED_EVENT_NODE_TYPE;
    }

    protected static final class ScheduleInfo {

        public final String expression;
        public final Long   period;
        public final Date   date;
        public final String jobId;

        public ScheduleInfo(final Event event)
        throws IllegalArgumentException {
            // let's see if a schedule information is specified or if the job should be stopped
            this.expression = (String) event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_SCHEDULE);
            this.period = (Long) event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_PERIOD);
            this.date = (Date) event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_DATE);
            int count = 0;
            if ( this.expression != null) {
                count++;
            }
            if ( this.period != null ) {
                count++;
            }
            if ( this.date != null ) {
                count++;
            }
            if ( count > 1 ) {
                throw new IllegalArgumentException("Only one configuration property from " + EventUtil.PROPERTY_TIMED_EVENT_SCHEDULE +
                                      ", " + EventUtil.PROPERTY_TIMED_EVENT_PERIOD +
                                      ", or " + EventUtil.PROPERTY_TIMED_EVENT_DATE + " should be used.");
            }
            // we create a job id consisting of the real event topic and an (optional) id
            // if the event contains a timed event id or a job id we'll append that to the name
            String topic = (String)event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_TOPIC);
            if ( topic == null ) {
                throw new IllegalArgumentException("Timed event does not contain required property " + EventUtil.PROPERTY_TIMED_EVENT_TOPIC);
            }
            String id = (String)event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_ID);
            String jId = (String)event.getProperty(EventUtil.PROPERTY_JOB_ID);

            this.jobId = "TimedEvent: " + topic + ':' + (id != null ? id : "") + ':' + (jId != null ? jId : "");
        }

        public boolean isStopEvent() {
            return this.expression == null && this.period == null && this.date == null;
        }

        public String getJobId() {
            return this.jobId;
        }
    }
}
