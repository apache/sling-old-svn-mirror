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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.util.ISO8601;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.JobStatusProvider;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


/**
 * An event handler for special job events.
 *
 * @scr.component label="%job.events.name" description="%job.events.description"
 * @scr.service interface="org.apache.sling.event.JobStatusProvider"
 * @scr.property name="event.topics" refValues="EventUtil.TOPIC_JOB"
 *               values.updated="org/osgi/framework/BundleEvent/UPDATED"
 *               values.started="org/osgi/framework/BundleEvent/STARTED"
 *               private="true"
 * @scr.property name="repository.path" value="/var/eventing/jobs" private="true"
 * We schedule this event handler to run in the background and clean up
 * obsolete events.
 * @scr.service interface="java.lang.Runnable"
 * @scr.property name="scheduler.period" value="600" type="Long"
 * @scr.property name="scheduler.concurrent" value="false" type="Boolean" private="true"
 */
public class JobEventHandler
    extends AbstractRepositoryEventHandler
    implements EventUtil.JobStatusNotifier, JobStatusProvider, Runnable {

    /** The topic prefix for bundle events. */
    protected static final String BUNDLE_EVENT_PREFIX = BundleEvent.class.getName().replace('.', '/') + '/';

    /** A map for keeping track of currently processed job topics. */
    protected final Map<String, Boolean> processingMap = new HashMap<String, Boolean>();

    /** Default sleep time. */
    protected static final long DEFAULT_SLEEP_TIME = 30;

    /** @scr.property valueRef="DEFAULT_SLEEP_TIME" */
    protected static final String CONFIG_PROPERTY_SLEEP_TIME = "sleep.time";

    /** Default number of job retries. */
    protected static final int DEFAULT_MAX_JOB_RETRIES = 10;

    /** @scr.property valueRef="DEFAULT_MAX_JOB_RETRIES" */
    protected static final String CONFIG_PROPERTY_MAX_JOB_RETRIES = "max.job.retries";

    /** We check every 30 secs by default. */
    protected long sleepTime;

    /** How often should a job be retried by default. */
    protected int maxJobRetries;

    /** Background session. */
    protected Session backgroundSession;

    /** Unloaded jobs. */
    protected Set<String>unloadedJobs = new HashSet<String>();

    /** List of deleted jobs. */
    protected Set<String>deletedJobs = new HashSet<String>();

    /** Default clean up time is 10 minutes. */
    protected static final int DEFAULT_CLEANUP_PERIOD = 10;

    /** @scr.property valueRef="DEFAULT_CLEANUP_PERIOD" type="Integer" */
    protected static final String CONFIG_PROPERTY_CLEANUP_PERIOD = "cleanup.period";

    /** We remove everything which is older than 5 min by default. */
    protected int cleanupPeriod = DEFAULT_CLEANUP_PERIOD;

    /** The scheduler for rescheduling jobs. @scr.reference */
    protected Scheduler scheduler;

    /**
     * Activate this component.
     * @param context
     * @throws RepositoryException
     */
    protected void activate(final ComponentContext context)
    throws Exception {
        @SuppressWarnings("unchecked")
        final Dictionary<String, Object> props = context.getProperties();
        this.cleanupPeriod = OsgiUtil.toInteger(props.get(CONFIG_PROPERTY_CLEANUP_PERIOD), DEFAULT_CLEANUP_PERIOD);
        this.sleepTime = OsgiUtil.toLong(props.get(CONFIG_PROPERTY_SLEEP_TIME), DEFAULT_SLEEP_TIME);
        this.maxJobRetries = OsgiUtil.toInteger(props.get(CONFIG_PROPERTY_MAX_JOB_RETRIES), DEFAULT_MAX_JOB_RETRIES);
        super.activate(context);
    }

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#deactivate(org.osgi.service.component.ComponentContext)
     */
    protected void deactivate(final ComponentContext context) {
        super.deactivate(context);
        if ( this.backgroundSession != null ) {
            try {
                this.backgroundSession.getWorkspace().getObservationManager().removeEventListener(this);
            } catch (RepositoryException e) {
                // we just ignore it
                this.logger.warn("Unable to remove event listener.", e);
            }
            this.backgroundSession.logout();
            this.backgroundSession = null;
        }
    }

    /**
     * Return the query string for the clean up.
     */
    protected String getCleanUpQueryString() {
        final Calendar deleteBefore = Calendar.getInstance();
        deleteBefore.add(Calendar.MINUTE, -this.cleanupPeriod);
        final String dateString = ISO8601.format(deleteBefore);

        final StringBuffer buffer = new StringBuffer("/jcr:root");
        buffer.append(this.repositoryPath);
        buffer.append("//element(*, ");
        buffer.append(getEventNodeType());
        buffer.append(")[@");
        buffer.append(EventHelper.NODE_PROPERTY_FINISHED);
        buffer.append(" < xs:dateTime('");
        buffer.append(dateString);
        buffer.append("')]");

        return buffer.toString();
    }

    /**
     * This method is invoked periodically.
     * @see java.lang.Runnable#run()
     */
    public void run() {
        if ( this.cleanupPeriod > 0 ) {
            this.logger.debug("Cleaning up repository, removing all finished jobs older than {} minutes.", this.cleanupPeriod);

            final String queryString = this.getCleanUpQueryString();
            // we create an own session for concurrency issues
            Session s = null;
            try {
                s = this.createSession();
                final Node parentNode = (Node)s.getItem(this.repositoryPath);
                logger.debug("Executing query {}", queryString);
                final Query q = s.getWorkspace().getQueryManager().createQuery(queryString, Query.XPATH);
                final NodeIterator iter = q.execute().getNodes();
                int count = 0;
                while ( iter.hasNext() ) {
                    final Node eventNode = iter.nextNode();
                    eventNode.remove();
                    count++;
                }
                parentNode.save();
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
                    this.writerSession.refresh(false);
                } catch (RepositoryException re) {
                    // we just ignore this
                    this.ignoreException(re);
                }
                final EventInfo info = new EventInfo();
                info.event = event;
                final String nodeName = this.getNodeName(event);

                // if the job has no job id, we can just write the job to the repo and don't
                // need locking
                final String jobId = (String)event.getProperty(EventUtil.PROPERTY_JOB_ID);
                if ( jobId == null ) {
                    try {
                        final Node eventNode = this.writeEvent(event, nodeName);
                        info.nodePath = eventNode.getPath();
                    } catch (RepositoryException re ) {
                        // something went wrong, so let's log it
                        this.logger.error("Exception during writing new job '" + nodeName + "' to repository.", re);
                    }
                } else {
                    try {
                        // let's first search for an existing node with the same id
                        final Node parentNode = (Node)this.writerSession.getItem(this.repositoryPath);
                        Node foundNode = null;
                        if ( parentNode.hasNode(nodeName) ) {
                            foundNode = parentNode.getNode(nodeName);
                        }
                        if ( foundNode != null ) {
                            // if the node is locked, someone else was quicker
                            // and we don't have to process this job
                            if ( !foundNode.isLocked() ) {
                                // node is already in repository, so if not finished we just use it
                                // otherwise it has already been processed
                                try {
                                    if ( !foundNode.hasProperty(EventHelper.NODE_PROPERTY_FINISHED) ) {
                                        info.nodePath = foundNode.getPath();
                                    }
                                } catch (RepositoryException re) {
                                    // if anything goes wrong, it means that (hopefully) someone
                                    // else is processing this node
                                }
                            }
                        } else {
                            // We now write the event into the repository
                            try {
                                final Node eventNode = this.writeEvent(event, nodeName);
                                info.nodePath = eventNode.getPath();
                            } catch (ItemExistsException iee) {
                                // someone else did already write this node in the meantime
                                // nothing to do for us
                            }
                        }
                    } catch (RepositoryException re ) {
                        // something went wrong, so let's log it
                        this.logger.error("Exception during writing new job '" + nodeName + "' to repository.", re);
                    }
                }
                // if we were able to write the event into the repository
                // we will queue it for processing
                if ( info.nodePath != null ) {
                    try {
                        this.queue.put(info);
                    } catch (InterruptedException e) {
                        // this should never happen
                        this.ignoreException(e);
                    }
                }
            }
        }
    }

    /**
     * This method runs in the background and processes the local queue.
     */
    protected void runInBackground() throws RepositoryException {
        this.backgroundSession = this.createSession();
        this.backgroundSession.getWorkspace().getObservationManager()
        .addEventListener(this,
                          javax.jcr.observation.Event.PROPERTY_REMOVED,
                          this.repositoryPath,
                          true,
                          null,
                          new String[] {this.getEventNodeType()},
                          true);
        // load unprocessed jobs from repository
        this.loadJobs();
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

                // check if the node still exists
                synchronized (this.backgroundSession) {
                    try {
                        this.backgroundSession.refresh(false);
                        if ( this.backgroundSession.itemExists(info.nodePath)
                             && !this.backgroundSession.itemExists(info.nodePath + "/" + EventHelper.NODE_PROPERTY_FINISHED)) {
                            final Event event = info.event;
                            final String jobTopic = (String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC);
                            final boolean parallelProcessing = event.getProperty(EventUtil.PROPERTY_JOB_PARALLEL) != null;

                            // check how we can process this job
                            // if parallel processing is allowed, we can just process
                            // if not we should check if any other job with the same topic is currently running
                            boolean process = parallelProcessing;
                            if ( !process ) {
                                synchronized ( this.processingMap ) {
                                    final Boolean value = this.processingMap.get(jobTopic);
                                    if ( value == null || !value.booleanValue() ) {
                                        this.processingMap.put(jobTopic, Boolean.TRUE);
                                        process = true;
                                    }
                                }

                            }
                            if ( process ) {
                                boolean unlock = true;
                                try {
                                    final Node eventNode = (Node) this.backgroundSession.getItem(info.nodePath);
                                    if ( !eventNode.isLocked() ) {
                                        // lock node
                                        try {
                                            eventNode.lock(false, true);
                                        } catch (RepositoryException re) {
                                            // lock failed which means that the node is locked by someone else, so we don't have to requeue
                                            process = false;
                                        }
                                        if ( process ) {
                                            unlock = false;
                                            this.processJob(info.event, eventNode);
                                        }
                                    }
                                } catch (RepositoryException e) {
                                    // ignore
                                    this.ignoreException(e);
                                } finally {
                                    if ( unlock && !parallelProcessing ) {
                                        synchronized ( this.processingMap ) {
                                            this.processingMap.put(jobTopic, Boolean.FALSE);
                                        }
                                    }
                                }
                            } else {
                                try {
                                    // check if the node is in processing or already finished
                                    final Node eventNode = (Node) this.backgroundSession.getItem(info.nodePath);
                                    if ( !eventNode.isLocked() && !eventNode.hasProperty(EventHelper.NODE_PROPERTY_FINISHED)) {
                                        final EventInfo eInfo = info;
                                        final Date fireDate = new Date();
                                        fireDate.setTime(System.currentTimeMillis() + this.sleepTime * 1000);

                                            // we put it back into the queue after a specific time
                                        final Runnable r = new Runnable() {

                                            /**
                                             * @see java.lang.Runnable#run()
                                             */
                                            public void run() {
                                                try {
                                                    queue.put(eInfo);
                                                } catch (InterruptedException e) {
                                                    // ignore
                                                    ignoreException(e);
                                                }
                                            }

                                        };
                                        try {
                                            this.scheduler.fireJobAt(null, r, null, fireDate);
                                        } catch (Exception e) {
                                            // we ignore the exception
                                            ignoreException(e);
                                            // then wait for the time and readd the job
                                            try {
                                                Thread.sleep(sleepTime * 1000);
                                            } catch (InterruptedException ie) {
                                                // ignore
                                                ignoreException(ie);
                                            }
                                            r.run();
                                        }

                                    }
                                } catch (RepositoryException e) {
                                    // ignore
                                    this.ignoreException(e);
                                }
                            }
                        }
                    } catch (RepositoryException re) {
                        this.ignoreException(re);
                    }

                }
            }
        }
    }

    /**
     * @see org.apache.sling.engine.event.impl.JobPersistenceHandler#getContainerNodeType()
     */
    protected String getContainerNodeType() {
        return EventHelper.JOBS_NODE_TYPE;
    }

    /**
     * @see org.apache.sling.engine.event.impl.JobPersistenceHandler#getEventNodeType()
     */
    protected String getEventNodeType() {
        return EventHelper.JOB_NODE_TYPE;
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(final Event event) {
        // we ignore remote job events
        if ( EventUtil.isLocal(event) ) {
            // check for bundle event
            if ( event.getTopic().equals(EventUtil.TOPIC_JOB)) {
                // job event
                final String jobTopic = (String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC);

                //  job topic must be set, otherwise we ignore this event!
                if ( jobTopic != null ) {
                    // queue the event in order to respond quickly
                    try {
                        this.writeQueue.put(event);
                    } catch (InterruptedException e) {
                        // this should never happen
                        this.ignoreException(e);
                    }
                } else {
                    this.logger.warn("Event does not contain job topic: {}", event);
                }

            } else {
                // bundle event started or updated
                boolean doIt = false;
                synchronized ( this.unloadedJobs ) {
                    if ( this.unloadedJobs.size() > 0 ) {
                        doIt = true;
                    }
                }
                if ( doIt ) {
                    final Runnable t = new Runnable() {

                        public void run() {
                            synchronized (unloadedJobs) {
                                Session s = null;
                                final Set<String> newUnloadedJobs = new HashSet<String>();
                                newUnloadedJobs.addAll(unloadedJobs);
                                try {
                                    s = createSession();
                                    for(String path : unloadedJobs ) {
                                        newUnloadedJobs.remove(path);
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
                                                        newUnloadedJobs.add(path);
                                                        ignoreException(cnfe);
                                                    }
                                                }
                                            }
                                        } catch (RepositoryException re) {
                                            // we ignore this and readd
                                            newUnloadedJobs.add(path);
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
                                    unloadedJobs.clear();
                                    unloadedJobs.addAll(newUnloadedJobs);
                                }
                            }
                        }

                    };
                    this.threadPool.execute(t);
                }
            }
        }
    }

    public static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz0123456789_,.-+*#!¤$%&()=[]?";
    public static final char REPLACEMENT_CHAR = '_';

    public static String filter(final String nodeName) {
        final StringBuffer sb  = new StringBuffer();
        char lastAdded = 0;

        for(int i=0; i < nodeName.length(); i++) {
            final char c = nodeName.charAt(i);
            char toAdd = c;

            if (ALLOWED_CHARS.indexOf(c) < 0) {
                if (lastAdded == REPLACEMENT_CHAR) {
                    // do not add several _ in a row
                    continue;
                }
                toAdd = REPLACEMENT_CHAR;

            } else if(i == 0 && Character.isDigit(c)) {
                sb.append(REPLACEMENT_CHAR);
            }

            sb.append(toAdd);
            lastAdded = toAdd;
        }

        if (sb.length()==0) {
            sb.append(REPLACEMENT_CHAR);
        }

        return sb.toString();
    }

    /**
     * Create a unique node name for the job.
     */
    protected String getNodeName(Event event) {
        final String jobId = (String)event.getProperty(EventUtil.PROPERTY_JOB_ID);
        final String name;
        if ( jobId != null ) {
            final String jobTopic = ((String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC));
            name = jobTopic + " " + jobId;
        } else {
            name = "Job " + UUID.randomUUID().toString();
        }
        return filter(name);
    }

    /**
     * Process a job and unlock the node in the repository.
     * @param event The original event.
     * @param eventNode The node in the repository where the job is stored.
     */
    protected void processJob(Event event, Node eventNode)  {
        final boolean parallelProcessing = event.getProperty(EventUtil.PROPERTY_JOB_PARALLEL) != null;
        final String jobTopic = (String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC);
        boolean unlock = true;
        try {
            final Event jobEvent = this.getJobEvent(event, eventNode.getPath());
            eventNode.setProperty(EventHelper.NODE_PROPERTY_PROCESSOR, this.applicationId);
            eventNode.save();
            final EventAdmin localEA = this.eventAdmin;
            if ( localEA != null ) {
                localEA.sendEvent(jobEvent);
                // do not unlock if sending was successful
                unlock = false;
            } else {
                this.logger.error("Job event can't be sent as no event admin is available.");
            }
        } catch (RepositoryException re) {
            // if an exception occurs, we just log
            this.logger.error("Exception during job processing.", re);
        } finally {
            if ( unlock ) {
                if ( !parallelProcessing ) {
                    synchronized ( this.processingMap ) {
                        this.processingMap.put(jobTopic, Boolean.FALSE);
                    }
                }
                // unlock node
                try {
                    eventNode.unlock();
                } catch (RepositoryException e) {
                    // if unlock fails, we silently ignore this
                    this.ignoreException(e);
                }
            }
        }
    }

    /**
     * Create the job event.
     * @param e
     * @return
     */
    protected Event getJobEvent(Event e, String nodePath) {
        final String eventTopic = (String)e.getProperty(EventUtil.PROPERTY_JOB_TOPIC);
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        final String[] propertyNames = e.getPropertyNames();
        for(int i=0; i<propertyNames.length; i++) {
            properties.put(propertyNames[i], e.getProperty(propertyNames[i]));
        }
        // put properties for finished job callback
        properties.put(EventUtil.JobStatusNotifier.CONTEXT_PROPERTY_NAME, new EventUtil.JobStatusNotifier.NotifierContext(this, nodePath));
        return new Event(eventTopic, properties);
    }

    /**
     * @see org.apache.sling.engine.event.impl.JobPersistenceHandler#addNodeProperties(javax.jcr.Node, org.osgi.service.event.Event)
     */
    protected void addNodeProperties(Node eventNode, Event event)
    throws RepositoryException {
        super.addNodeProperties(eventNode, event);
        eventNode.setProperty(EventHelper.NODE_PROPERTY_TOPIC, (String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC));
        final String jobId = (String)event.getProperty(EventUtil.PROPERTY_JOB_ID);
        if ( jobId != null ) {
            eventNode.setProperty(EventHelper.NODE_PROPERTY_JOBID, jobId);
        }
        final long retryCount = OsgiUtil.toLong(event.getProperty(EventUtil.PROPERTY_JOB_RETRY_COUNT), 0);
        final long retries = OsgiUtil.toLong(event.getProperty(EventUtil.PROPERTY_JOB_RETRIES), this.maxJobRetries);
        eventNode.setProperty(EventUtil.PROPERTY_JOB_RETRY_COUNT, retryCount);
        eventNode.setProperty(EventUtil.PROPERTY_JOB_RETRIES, retries);
    }

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#addEventProperties(javax.jcr.Node, java.util.Dictionary)
     */
    protected void addEventProperties(Node eventNode,
                                      Dictionary<String, Object> properties)
    throws RepositoryException {
        super.addEventProperties(eventNode, properties);
        // convert to integers (jcr only supports long)
        if ( properties.get(EventUtil.PROPERTY_JOB_RETRIES) != null ) {
            properties.put(EventUtil.PROPERTY_JOB_RETRIES, Integer.valueOf(properties.get(EventUtil.PROPERTY_JOB_RETRIES).toString()));
        }
        if ( properties.get(EventUtil.PROPERTY_JOB_RETRY_COUNT) != null ) {
            properties.put(EventUtil.PROPERTY_JOB_RETRY_COUNT, Integer.valueOf(properties.get(EventUtil.PROPERTY_JOB_RETRY_COUNT).toString()));
        }
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
                if ( event.getType() == javax.jcr.observation.Event.PROPERTY_CHANGED
                   || event.getType() == javax.jcr.observation.Event.PROPERTY_REMOVED) {
                    try {
                        final String propPath = event.getPath();
                        int pos = propPath.lastIndexOf('/');
                        final String nodePath = propPath.substring(0, pos);
                        final String propertyName = propPath.substring(pos+1);

                        // we are only interested in unlocks
                        if ( "jcr:lockOwner".equals(propertyName) ) {
                            boolean doNotProcess = false;
                            synchronized ( this.deletedJobs ) {
                                doNotProcess = this.deletedJobs.remove(nodePath);
                            }
                            if ( !doNotProcess ) {
                                final Node eventNode = (Node) s.getItem(nodePath);
                                if ( !eventNode.isLocked() && !eventNode.hasProperty(EventHelper.NODE_PROPERTY_FINISHED)) {
                                    try {
                                        final EventInfo info = new EventInfo();
                                        info.event = this.readEvent(eventNode);
                                        info.nodePath = nodePath;
                                        try {
                                            this.queue.put(info);
                                        } catch (InterruptedException e) {
                                            // we ignore this exception as this should never occur
                                            this.ignoreException(e);
                                        }
                                    } catch (ClassNotFoundException cnfe) {
                                        // store path for lazy loading
                                        synchronized ( this.unloadedJobs ) {
                                            this.unloadedJobs.add(nodePath);
                                        }
                                        this.ignoreException(cnfe);
                                    }
                                }
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
     * Load all active jobs from the repository.
     * @throws RepositoryException
     */
    protected void loadJobs() {
        try {
            final QueryManager qManager = this.backgroundSession.getWorkspace().getQueryManager();
            final StringBuffer buffer = new StringBuffer("/jcr:root");
            buffer.append(this.repositoryPath);
            buffer.append("//element(*, ");
            buffer.append(this.getEventNodeType());
            buffer.append(")");
            final Query q = qManager.createQuery(buffer.toString(), Query.XPATH);
            final NodeIterator result = q.execute().getNodes();
            while ( result.hasNext() ) {
                final Node eventNode = result.nextNode();
                if ( !eventNode.isLocked() && !eventNode.hasProperty(EventHelper.NODE_PROPERTY_FINISHED)) {
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
                        // store path for lazy loading
                        synchronized ( this.unloadedJobs ) {
                            this.unloadedJobs.add(nodePath);
                        }
                        this.ignoreException(cnfe);
                    } catch (RepositoryException re) {
                        this.logger.error("Unable to load stored job from " + nodePath, re);
                    }
                }
            }
        } catch (RepositoryException re) {
            this.logger.error("Exception during initial loading of stored jobs.", re);
        }
    }

    /**
     * @see org.apache.sling.event.EventUtil.JobStatusNotifier#finishedJob(org.osgi.service.event.Event, String, boolean)
     */
    public boolean finishedJob(Event job, String eventNodePath, boolean shouldReschedule) {
        boolean reschedule = shouldReschedule;
        if ( shouldReschedule ) {
            // check if we exceeded the number of retries
            int retries = this.maxJobRetries;
            if ( job.getProperty(EventUtil.PROPERTY_JOB_RETRIES) != null ) {
                retries = (Integer) job.getProperty(EventUtil.PROPERTY_JOB_RETRIES);
            }
            int retryCount = 0;
            if ( job.getProperty(EventUtil.PROPERTY_JOB_RETRY_COUNT) != null ) {
                retryCount = (Integer)job.getProperty(EventUtil.PROPERTY_JOB_RETRY_COUNT);
            }
            retryCount++;
            if ( retries != -1 && retryCount > retries ) {
                reschedule = false;
            }
            if ( !reschedule ) {
                // update event with retry count and retries
                final Dictionary<String, Object> newProperties;
                // create a new dictionary
                newProperties = new Hashtable<String, Object>();
                final String[] names = job.getPropertyNames();
                for(int i=0; i<names.length; i++ ) {
                    newProperties.put(names[i], job.getProperty(names[i]));
                }
                newProperties.put(EventUtil.PROPERTY_JOB_RETRY_COUNT, retryCount);
                newProperties.put(EventUtil.PROPERTY_JOB_RETRIES, retries);
                job = new Event(job.getTopic(), newProperties);
            }
        }
        final boolean parallelProcessing = job.getProperty(EventUtil.PROPERTY_JOB_PARALLEL) != null;
        // we have to use the same session for unlocking that we used for locking!
        synchronized ( this.backgroundSession ) {
            try {
                this.backgroundSession.refresh(false);
                final Node eventNode = (Node) this.backgroundSession.getItem(eventNodePath);
                boolean unlock = true;
                try {
                    if ( !reschedule ) {
                        synchronized ( this.deletedJobs ) {
                            this.deletedJobs.add(eventNodePath);
                        }
                        // unlock node
                        try {
                            eventNode.unlock();
                        } catch (RepositoryException e) {
                            // if unlock fails, we silently ignore this
                            this.ignoreException(e);
                        }
                        unlock = false;
                        final String jobId = (String)job.getProperty(EventUtil.PROPERTY_JOB_ID);
                        if ( jobId == null ) {
                            // remove node from repository if no job id set
                            final Node parentNode = eventNode.getParent();
                            eventNode.remove();
                            parentNode.save();
                        } else {
                            eventNode.setProperty(EventHelper.NODE_PROPERTY_FINISHED, Calendar.getInstance());
                            eventNode.save();
                        }
                    }
                } catch (RepositoryException re) {
                    // if an exception occurs, we just log
                    this.logger.error("Exception during job finishing.", re);
                } finally {
                    if ( !parallelProcessing) {
                        final String jobTopic = (String)job.getProperty(EventUtil.PROPERTY_JOB_TOPIC);
                        synchronized ( this.processingMap ) {
                            this.processingMap.put(jobTopic, Boolean.FALSE);
                        }
                    }
                    if ( unlock ) {
                        synchronized ( this.deletedJobs ) {
                            this.deletedJobs.add(eventNodePath);
                        }
                        // unlock node
                        try {
                            eventNode.unlock();
                        } catch (RepositoryException e) {
                            // if unlock fails, we silently ignore this
                            this.ignoreException(e);
                        }
                    }
                }
                if ( reschedule ) {
                    // update retry count and retries in the repository
                    try {
                        eventNode.setProperty(EventUtil.PROPERTY_JOB_RETRIES, (Integer)job.getProperty(EventUtil.PROPERTY_JOB_RETRIES));
                        eventNode.setProperty(EventUtil.PROPERTY_JOB_RETRY_COUNT, (Integer)job.getProperty(EventUtil.PROPERTY_JOB_RETRY_COUNT));
                        eventNode.save();
                    } catch (RepositoryException re) {
                        // if an exception occurs, we just log
                        this.logger.error("Exception during job updating job rescheduling information.", re);
                    }
                    final EventInfo info = new EventInfo();
                    try {
                        info.event = job;
                        info.nodePath = eventNode.getPath();
                    } catch (RepositoryException e) {
                        // this should never happen
                        this.ignoreException(e);
                    }
                    // delay rescheduling?
                    if ( job.getProperty(EventUtil.PROPERTY_JOB_RETRY_DELAY) != null ) {
                        final long delay = (Long)job.getProperty(EventUtil.PROPERTY_JOB_RETRY_DELAY);
                        final Date fireDate = new Date();
                        fireDate.setTime(System.currentTimeMillis() + delay);

                        final Runnable t = new Runnable() {
                            public void run() {
                                try {
                                    queue.put(info);
                                } catch (InterruptedException e) {
                                    // this should never happen
                                    ignoreException(e);
                                }
                            }
                        };
                        try {
                            this.scheduler.fireJobAt(null, t, null, fireDate);
                        } catch (Exception e) {
                            // we ignore the exception and just put back the job in the queue
                            ignoreException(e);
                            t.run();
                        }
                    } else {
                        // put directly into queue
                        try {
                            this.queue.put(info);
                        } catch (InterruptedException e) {
                            // this should never happen
                            this.ignoreException(e);
                        }
                    }
                }
                if ( !shouldReschedule ) {
                    return true;
                }
                return reschedule;
            } catch (RepositoryException re) {
                this.logger.error("Unable to create new session.", re);
                return false;
            }
        }
    }

    /**
     * @see org.apache.sling.event.EventUtil.JobStatusNotifier#execute(java.lang.Runnable)
     */
    public void execute(Runnable job) {
        this.threadPool.execute(job);
    }

    /**
     * Search for job nodes
     * @param topic The job topic
     * @param filterProps optional filter props
     * @param locked only active jobs?
     * @return
     * @throws RepositoryException
     */
    private Collection<Event> queryCurrentJobs(final String topic,
                                               final Map<String, Object> filterProps,
                                               final boolean locked)  {
        // we create a new session
        Session s = null;
        final List<Event> jobs = new ArrayList<Event>();
        try {
            s = this.createSession();
            final QueryManager qManager = s.getWorkspace().getQueryManager();
            final StringBuffer buffer = new StringBuffer("/jcr:root");
            buffer.append(this.repositoryPath);
            buffer.append("//element(*, ");
            buffer.append(this.getEventNodeType());
            buffer.append(") [not(@");
            buffer.append(EventHelper.NODE_PROPERTY_FINISHED);
            buffer.append(")");
            if ( topic != null ) {
                buffer.append(" @");
                buffer.append(EventHelper.NODE_PROPERTY_TOPIC);
                buffer.append(" = '");
                buffer.append(topic);
                buffer.append("'");
            }
            if ( locked ) {
                buffer.append(" and ");
                buffer.append("@jcr:lockOwner");
            }
            if ( filterProps != null ) {
                final Iterator<Map.Entry<String, Object>> i = filterProps.entrySet().iterator();
                while ( i.hasNext() ) {
                    final Map.Entry<String, Object> current = i.next();
                    // check prop name first
                    final String propName = this.getNodePropertyName(current.getKey());
                    if ( propName != null ) {
                        // check value
                        final Value value = this.getNodePropertyValue(s.getValueFactory(), current.getValue());
                        if ( value != null ) {
                            buffer.append(" and @");
                            buffer.append(propName);
                            buffer.append(" = '");
                            buffer.append(current.getValue());
                            buffer.append("'");
                        }
                    }
                }
            }
            buffer.append("]");
            final String queryString = buffer.toString();
            logger.debug("Executing job query {}.", queryString);

            final Query q = qManager.createQuery(queryString, Query.XPATH);
            final NodeIterator iter = q.execute().getNodes();
            while ( iter.hasNext() ) {
                final Node eventNode = iter.nextNode();
                try {
                    final Event event = this.readEvent(eventNode);
                    jobs.add(event);
                } catch (ClassNotFoundException cnfe) {
                    // in the case of a class not found exception we just ignore the exception
                    this.ignoreException(cnfe);
                }
            }
        } catch (RepositoryException e) {
            // in the case of an error, we return an empty list
            this.ignoreException(e);
        } finally {
            if ( s != null) {
                s.logout();
            }
        }
        return jobs;
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#getCurrentJobs(java.lang.String)
     */
    public Collection<Event> getCurrentJobs(String topic) {
        return this.getCurrentJobs(topic, null);
    }

    /**
     * This is deprecated.
     */
    public Collection<Event> scheduledJobs(String topic) {
        return this.getScheduledJobs(topic);
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#getScheduledJobs(java.lang.String)
     */
    public Collection<Event> getScheduledJobs(String topic) {
        return this.getScheduledJobs(topic, null);
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#getCurrentJobs(java.lang.String, java.util.Map)
     */
    public Collection<Event> getCurrentJobs(String topic, Map<String, Object> filterProps) {
        return this.queryCurrentJobs(topic, null, true);
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#getScheduledJobs(java.lang.String, java.util.Map)
     */
    public Collection<Event> getScheduledJobs(String topic, Map<String, Object> filterProps) {
        return this.queryCurrentJobs(topic, null, false);
    }
}
