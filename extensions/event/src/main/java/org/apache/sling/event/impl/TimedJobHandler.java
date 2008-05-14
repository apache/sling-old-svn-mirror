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
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.util.Text;
import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.JobContext;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.event.EventUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


/**
 * An event handler for timed events.
 *
 * @scr.component
 * @scr.property name="event.topics" refValues="EventUtil.TOPIC_TIMED_EVENT"
 *               values.updated="org/osgi/framework/BundleEvent/UPDATED"
 *               values.started="org/osgi/framework/BundleEvent/STARTED"
 * @scr.property name="repository.path" value="/var/eventing/timed-jobs"
 */
public class TimedJobHandler
    extends AbstractRepositoryEventHandler
    implements Job {

    protected static final String JOB_TOPIC = "topic";

    protected static final String JOB_CONFIG = "config";

    protected static final String JOB_SCHEDULE_INFO = "info";

    /** @scr.reference */
    protected Scheduler scheduler;

    /** Unloaded events. */
    protected Set<String>unloadedEvents = new HashSet<String>();

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#startWriterSession()
     */
    protected void startWriterSession() throws RepositoryException {
        super.startWriterSession();
        // load timed events from repository
        this.loadEvents();
        this.writerSession.getWorkspace().getObservationManager()
            .addEventListener(this, javax.jcr.observation.Event.PROPERTY_CHANGED, this.repositoryPath, true, null, null, true);
    }

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#processWriteQueue()
     */
    protected void processWriteQueue() {
        while ( this.running ) {
            Event event = null;
            try {
                event = this.writeQueue.take();
            } catch (InterruptedException e) {
                // we ignore this
                this.ignoreException(e);
            }
            if ( this.running && event != null ) {
                ScheduleInfo scheduleInfo = null;
                try {
                    scheduleInfo = new ScheduleInfo(event);
                } catch (IllegalArgumentException iae) {
                    this.logger.error(iae.getMessage());
                }
                if ( scheduleInfo != null ) {
                    final EventInfo info = new EventInfo();
                    info.event = event;

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
                }
            }
        }
    }

    /**
     * Create a unique node name for this timed job.
     */
    protected String getNodeName(final ScheduleInfo info) {
        return Text.escapeIllegalJcrChars(info.jobId);
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
                        this.writerSession.refresh(true);
                        final Node eventNode = (Node) this.writerSession.getItem(info.nodePath);
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
            // get parent node
            final Node parentNode = (Node)this.writerSession.getItem(this.repositoryPath);
            final String nodeName = this.getNodeName(scheduleInfo);
            // is there already a node?
            final Node foundNode = parentNode.hasNode(nodeName) ? parentNode.getNode(nodeName) : null;
            Lock lock = null;
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
            } else {
                // if there is already a node, it means we must handle an update
                if ( foundNode != null ) {
                    try {
                        foundNode.remove();
                        parentNode.save();
                    } catch (LockException le) {
                        // if someone else has the lock this is fine
                    }
                    // create a stop event
                    processEvent(event, scheduleInfo.getStopInfo());
                }
                // we only write the event if this is a local one
                if ( EventUtil.isLocal(event) ) {

                    // write event to repository, lock it and schedule the event
                    final Node eventNode = writeEvent(event, nodeName);
                    lock = eventNode.lock(false, true);
                }
            }

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
        final Scheduler localScheduler = this.scheduler;
        if ( localScheduler != null ) {
            // is this a stop event?
            if ( scheduleInfo.isStopEvent() ) {
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Stopping timed event " + event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_TOPIC) + "(" + scheduleInfo.jobId + ")");
                }
                try {
                    localScheduler.removeJob(scheduleInfo.jobId);
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
            config.put(JOB_TOPIC, (String)event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_TOPIC));
            final String[] names = event.getPropertyNames();
            if ( names != null ) {
                for(int i=0; i<names.length; i++) {
                    properties.put(names[i], event.getProperty(names[i]));
                }
            }
            config.put(JOB_CONFIG, properties);
            config.put(JOB_SCHEDULE_INFO, scheduleInfo);

            try {
                if ( scheduleInfo.expression != null ) {
                    if ( this.logger.isDebugEnabled() ) {
                        this.logger.debug("Adding timed event " + config.get(JOB_TOPIC) + "(" + scheduleInfo.jobId + ")" + " with cron expression " + scheduleInfo.expression);
                    }
                    localScheduler.addJob(scheduleInfo.jobId, this, config, scheduleInfo.expression, false);
                } else if ( scheduleInfo.period != null ) {
                    if ( this.logger.isDebugEnabled() ) {
                        this.logger.debug("Adding timed event " + config.get(JOB_TOPIC) + "(" + scheduleInfo.jobId + ")" + " with period " + scheduleInfo.period);
                    }
                    localScheduler.addPeriodicJob(scheduleInfo.jobId, this, config, scheduleInfo.period, false);
                } else {
                    // then it must be date
                    if ( this.logger.isDebugEnabled() ) {
                        this.logger.debug("Adding timed event " + config.get(JOB_TOPIC) + "(" + scheduleInfo.jobId + ")" + " with date " + scheduleInfo.date);
                    }
                    localScheduler.fireJobAt(scheduleInfo.jobId, this, config, scheduleInfo.date);
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
                            final String nodePath = event.getPath();
                            try {
                                final EventInfo info = new EventInfo();
                                info.event = this.readEvent(eventNode);
                                info.nodePath =nodePath;
                                try {
                                    this.queue.put(info);
                                } catch (InterruptedException e) {
                                    // we ignore this exception as this should never occur
                                    this.ignoreException(e);
                                }
                            } catch (ClassNotFoundException cnfe) {
                                // add it to the unloaded set
                                synchronized (unloadedEvents) {
                                    this.unloadedEvents.add(nodePath);
                                }
                                this.ignoreException(cnfe);
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
        if ( event.getTopic().equals(EventUtil.TOPIC_TIMED_EVENT) ) {
            // queue the event in order to respond quickly
            try {
                this.writeQueue.put(event);
            } catch (InterruptedException e) {
                // this should never happen
                this.ignoreException(e);
            }
        } else {
            // bundle event started or updated
            boolean doIt = false;
            synchronized ( this.unloadedEvents ) {
                if ( this.unloadedEvents.size() > 0 ) {
                    doIt = true;
                }
            }
            if ( doIt ) {
                final Runnable t = new Runnable() {

                    public void run() {
                        synchronized (unloadedEvents) {
                            Session s = null;
                            final Set<String> newUnloadedEvents = new HashSet<String>();
                            newUnloadedEvents.addAll(unloadedEvents);
                            try {
                                s = createSession();
                                for(String path : unloadedEvents ) {
                                    newUnloadedEvents.remove(path);
                                    try {
                                        if ( s.itemExists(path) ) {
                                            final Node eventNode = (Node) s.getItem(path);
                                            if ( !eventNode.isLocked() ) {
                                                try {
                                                    final EventInfo info = new EventInfo();
                                                    info.event = readEvent(eventNode);
                                                    info.nodePath = path;
                                                    try {
                                                        queue.put(info);
                                                    } catch (InterruptedException e) {
                                                        // we ignore this exception as this should never occur
                                                        ignoreException(e);
                                                    }
                                                } catch (ClassNotFoundException cnfe) {
                                                    newUnloadedEvents.add(path);
                                                    ignoreException(cnfe);
                                                }
                                            }
                                        }
                                    } catch (RepositoryException re) {
                                        // we ignore this and readd
                                        newUnloadedEvents.add(path);
                                        ignoreException(re);
                                    }
                                }
                            } catch (RepositoryException re) {
                                // unable to create session, so we try it again next time
                                ignoreException(re);
                            } finally {
                                if ( s != null ) {
                                    s.logout();
                                }
                                unloadedEvents.clear();
                                unloadedEvents.addAll(newUnloadedEvents);
                            }
                        }
                    }

                };
                this.threadPool.execute(t);
            }
        }
    }

    /**
     * @see org.apache.sling.commons.scheduler.Job#execute(org.apache.sling.commons.scheduler.JobContext)
     */
    public void execute(JobContext context) {
        final String topic = (String) context.getConfiguration().get(JOB_TOPIC);
        @SuppressWarnings("unchecked")
        final Dictionary<Object, Object> properties = (Dictionary<Object, Object>) context.getConfiguration().get(JOB_CONFIG);
        final EventAdmin ea = this.eventAdmin;
        if ( ea != null ) {
            try {
                ea.postEvent(new Event(topic, properties));
            } catch (IllegalArgumentException iae) {
                this.logger.error("Scheduled event has illegal topic: " + topic, iae);
            }
        } else {
            this.logger.warn("Unable to send timed event as no event admin service is available.");
        }
        final ScheduleInfo info = (ScheduleInfo) context.getConfiguration().get(JOB_SCHEDULE_INFO);
        // is this job scheduled for a specific date?
        if ( info.date != null ) {
            // we can remove it from the repository
            // we create an own session here
            Session s = null;
            try {
                s = this.createSession();
                final Node parentNode = (Node)s.getItem(this.repositoryPath);
                final String nodeName = this.getNodeName(info);
                final Node eventNode = parentNode.hasNode(nodeName) ? parentNode.getNode(nodeName) : null;
                if ( eventNode != null ) {
                    try {
                        eventNode.remove();
                        parentNode.save();
                    } catch (RepositoryException re) {
                        // we ignore the exception if removing fails
                        ignoreException(re);
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
    }

    /**
     * Load all active timed events from the repository.
     * @throws RepositoryException
     */
    protected void loadEvents() {
        try {
            final QueryManager qManager = this.writerSession.getWorkspace().getQueryManager();
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
                    final String nodePath = eventNode.getPath();
                    try {
                        final Event event = this.readEvent(eventNode);
                        final EventInfo info = new EventInfo();
                        info.event = event;
                        info.nodePath = nodePath;
                        try {
                            this.queue.put(info);
                        } catch (InterruptedException e) {
                            // we ignore this exception as this should never occur
                            this.ignoreException(e);
                        }
                    } catch (ClassNotFoundException cnfe) {
                        // add it to the unloaded set
                        synchronized (unloadedEvents) {
                            this.unloadedEvents.add(nodePath);
                        }
                        this.ignoreException(cnfe);
                    } catch (RepositoryException re) {
                        // if reading an event fails, we ignore this
                        this.ignoreException(re);
                    }
                }
            }
        } catch (RepositoryException re) {
            this.logger.error("Exception during initial loading of stored timed events.", re);
        }
    }

    /**
     * @see org.apache.sling.engine.event.impl.JobPersistenceHandler#addNodeProperties(javax.jcr.Node, org.osgi.service.event.Event)
     */
    protected void addNodeProperties(Node eventNode, Event event)
    throws RepositoryException {
        super.addNodeProperties(eventNode, event);
        eventNode.setProperty(EventHelper.NODE_PROPERTY_TOPIC, (String)event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_TOPIC));
        final ScheduleInfo info = new ScheduleInfo(event);
        eventNode.setProperty(EventHelper.NODE_PROPERTY_JOBID, info.jobId);
        if ( info.date != null ) {
            final Calendar c = Calendar.getInstance();
            c.setTime(info.date);
            eventNode.setProperty(EventHelper.NODE_PROPERTY_TE_DATE, c);
        }
        if ( info.expression != null ) {
            eventNode.setProperty(EventHelper.NODE_PROPERTY_TE_EXPRESSION, info.expression);
        }
        if ( info.period != null ) {
            eventNode.setProperty(EventHelper.NODE_PROPERTY_TE_PERIOD, info.period.longValue());
        }
    }

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#getContainerNodeType()
     */
    protected String getContainerNodeType() {
        return EventHelper.TIMED_EVENTS_NODE_TYPE;
    }

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#getEventNodeType()
     */
    protected String getEventNodeType() {
        return EventHelper.TIMED_EVENT_NODE_TYPE;
    }

    protected static final class ScheduleInfo implements Serializable {

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

        private ScheduleInfo(String jobId) {
            this.expression = null;
            this.period = null;
            this.date = null;
            this.jobId = jobId;
        }

        public ScheduleInfo getStopInfo() {
            return new ScheduleInfo(this.jobId);
        }

        public boolean isStopEvent() {
            return this.expression == null && this.period == null && this.date == null;
        }
    }
}
