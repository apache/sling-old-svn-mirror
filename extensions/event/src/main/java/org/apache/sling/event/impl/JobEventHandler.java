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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jcr.Item;
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
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.event.EventPropertiesMap;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.JobStatusProvider;
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

    /** A map for keeping track of currently processed job topics. */
    private final Map<String, Boolean> processingMap = new HashMap<String, Boolean>();

    /** A map for the different job queues. */
    private final Map<String, JobBlockingQueue> jobQueues = new HashMap<String, JobBlockingQueue>();

    /** Default sleep time. */
    private static final long DEFAULT_SLEEP_TIME = 30;

    /** @scr.property valueRef="DEFAULT_SLEEP_TIME" */
    private static final String CONFIG_PROPERTY_SLEEP_TIME = "sleep.time";

    /** Default number of job retries. */
    private static final int DEFAULT_MAX_JOB_RETRIES = 10;

    /** @scr.property valueRef="DEFAULT_MAX_JOB_RETRIES" */
    private static final String CONFIG_PROPERTY_MAX_JOB_RETRIES = "max.job.retries";

    /** We check every 30 secs by default. */
    private long sleepTime;

    /** How often should a job be retried by default. */
    private int maxJobRetries;

    /** Background session. */
    private Session backgroundSession;

    /** Unloaded jobs. */
    private Set<String>unloadedJobs = new HashSet<String>();

    /** List of deleted jobs. */
    private Set<String>deletedJobs = new HashSet<String>();

    /** Default clean up time is 10 minutes. */
    private static final int DEFAULT_CLEANUP_PERIOD = 10;

    /** @scr.property valueRef="DEFAULT_CLEANUP_PERIOD" type="Integer" */
    private static final String CONFIG_PROPERTY_CLEANUP_PERIOD = "cleanup.period";

    /** We remove everything which is older than 5 min by default. */
    private int cleanupPeriod = DEFAULT_CLEANUP_PERIOD;

    /** The scheduler for rescheduling jobs. @scr.reference */
    private Scheduler scheduler;

    public static ThreadPool JOB_THREAD_POOL;

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
        JOB_THREAD_POOL = this.threadPool;
    }

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#deactivate(org.osgi.service.component.ComponentContext)
     */
    protected void deactivate(final ComponentContext context) {
        super.deactivate(context);
        synchronized ( this.jobQueues ) {
            final Iterator<JobBlockingQueue> i = this.jobQueues.values().iterator();
            while ( i.hasNext() ) {
                try {
                    i.next().put(new EventInfo());
                } catch (InterruptedException e) {
                    this.ignoreException(e);
                }
            }
        }
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
        JOB_THREAD_POOL = null;
    }

    /**
     * Return the query string for the clean up.
     */
    private String getCleanUpQueryString() {
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
        if ( this.cleanupPeriod > 0 && this.running ) {
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
                final String jobId = (String)event.getProperty(EventUtil.PROPERTY_JOB_ID);
                final String jobTopic = (String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC);
                final String nodePath = this.getNodePath(jobTopic, jobId);

                // if the job has no job id, we can just write the job to the repo and don't
                // need locking
                if ( jobId == null ) {
                    try {
                        final Node eventNode = this.writeEvent(event, nodePath);
                        info.nodePath = eventNode.getPath();
                    } catch (RepositoryException re ) {
                        // something went wrong, so let's log it
                        this.logger.error("Exception during writing new job '" + nodePath + "' to repository.", re);
                    }
                } else {
                    try {
                        // let's first search for an existing node with the same id
                        final Node parentNode = this.ensureRepositoryPath();
                        Node foundNode = null;
                        if ( parentNode.hasNode(nodePath) ) {
                            foundNode = parentNode.getNode(nodePath);
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
                                final Node eventNode = this.writeEvent(event, nodePath);
                                info.nodePath = eventNode.getPath();
                            } catch (ItemExistsException iee) {
                                // someone else did already write this node in the meantime
                                // nothing to do for us
                            }
                        }
                    } catch (RepositoryException re ) {
                        // something went wrong, so let's log it
                        this.logger.error("Exception during writing new job '" + nodePath + "' to repository.", re);
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
                                  javax.jcr.observation.Event.PROPERTY_REMOVED
                                    |javax.jcr.observation.Event.NODE_REMOVED,
                                  this.repositoryPath,
                                  true,
                                  null,
                                  new String[] {this.getEventNodeType()},
                                  true);
        // give the system some time to start
        try {
            Thread.sleep(1000 * 60 * 1); // 1min
        } catch (InterruptedException e) {
            this.ignoreException(e);
        }
        // load unprocessed jobs from repository
        if ( this.running ) {
            this.loadJobs();
        }
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
                // check for local only jobs and remove them from the queue if they're meant
                // for another application node
                final String appId = (String)info.event.getProperty(EventUtil.PROPERTY_APPLICATION);
                if ( info.event.getProperty(EventUtil.PROPERTY_JOB_RUN_LOCAL) != null
                    && appId != null && !this.applicationId.equals(appId) ) {
                    info = null;
                }

                // check if we should put this into a separate queue
                if ( info != null && info.event.getProperty(EventUtil.PROPERTY_JOB_QUEUE_NAME) != null ) {
                    final String queueName = (String)info.event.getProperty(EventUtil.PROPERTY_JOB_QUEUE_NAME);
                    synchronized ( this.jobQueues ) {
                        BlockingQueue<EventInfo> jobQueue = this.jobQueues.get(queueName);
                        if ( jobQueue == null ) {
                            final JobBlockingQueue jq = new JobBlockingQueue();
                            jobQueue = jq;
                            this.jobQueues.put(queueName, jq);
                            // Start background thread
                            this.threadPool.execute(new Runnable() {

                                /**
                                 * @see java.lang.Runnable#run()
                                 */
                                public void run() {
                                    runJobQueue(queueName, jq);
                                }

                            });
                        }
                        try {
                            jobQueue.put(info);
                        } catch (InterruptedException e) {
                            // this should never happen
                            this.ignoreException(e);
                        }
                    }
                    // don't process this here
                    info = null;
                }

                // if we still have a job, process it
                if ( info != null ) {
                    this.executeJob(info, null);
                }
            }
        }
    }

    /**
     * Execute a job queue
     * @param queueName The name of the job queue
     * @param jobQueue The job queue
     */
    private void runJobQueue(final String queueName, final JobBlockingQueue jobQueue) {
        EventInfo info = null;
        while ( this.running ) {
            if ( info == null ) {
                // so let's wait/get the next job from the queue
                try {
                    info = jobQueue.take();
                } catch (InterruptedException e) {
                    // we ignore this
                    this.ignoreException(e);
                }
            }

            if ( info != null && this.running ) {
                synchronized ( jobQueue.getLock()) {
                    final EventInfo processInfo = info;
                    info = null;
                    if ( this.executeJob(processInfo, jobQueue) ) {
                        EventInfo newInfo = null;
                        try {
                            newInfo = jobQueue.waitForFinish();
                        } catch (InterruptedException e) {
                            this.ignoreException(e);
                        }
                        // if we have an info, this is a reschedule
                        if ( newInfo != null ) {
                            final EventInfo newEventInfo = newInfo;
                            final Event job = newInfo.event;

                            // is this an ordered queue?
                            final boolean orderedQueue = job.getProperty(EventUtil.PROPERTY_JOB_QUEUE_ORDERED) != null;

                            if ( orderedQueue ) {
                                // we just sleep for the delay time - if none, we continue and retry
                                // this job again
                                if ( job.getProperty(EventUtil.PROPERTY_JOB_RETRY_DELAY) != null ) {
                                    final long delay = (Long)job.getProperty(EventUtil.PROPERTY_JOB_RETRY_DELAY);
                                    try {
                                        Thread.sleep(delay);
                                    } catch (InterruptedException e) {
                                        this.ignoreException(e);
                                    }
                                }
                                info = newInfo;
                            } else {
                                // delay rescheduling?
                                if ( job.getProperty(EventUtil.PROPERTY_JOB_RETRY_DELAY) != null ) {
                                    final long delay = (Long)job.getProperty(EventUtil.PROPERTY_JOB_RETRY_DELAY);
                                    final Date fireDate = new Date();
                                    fireDate.setTime(System.currentTimeMillis() + delay);

                                    final Runnable t = new Runnable() {
                                        public void run() {
                                            try {
                                                jobQueue.put(newEventInfo);
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
                                        jobQueue.put(newInfo);
                                    } catch (InterruptedException e) {
                                        // this should never happen
                                        this.ignoreException(e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Process a job
     */
    private boolean executeJob(final EventInfo info, final BlockingQueue<EventInfo> jobQueue) {
        // check if the node still exists
        synchronized (this.backgroundSession) {
            try {
                this.backgroundSession.refresh(false);
                if ( this.backgroundSession.itemExists(info.nodePath)
                     && !this.backgroundSession.itemExists(info.nodePath + "/" + EventHelper.NODE_PROPERTY_FINISHED)) {
                    final Event event = info.event;
                    final String jobTopic = (String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC);
                    final boolean parallelProcessing = event.getProperty(EventUtil.PROPERTY_JOB_QUEUE_NAME) != null
                                                    || event.getProperty(EventUtil.PROPERTY_JOB_PARALLEL) != null;

                    // check how we can process this job
                    // if parallel processing is allowed, we can just process
                    // if not we should check if any other job with the same topic is currently running
                    boolean process = parallelProcessing;
                    if ( !parallelProcessing ) {
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
                                    return true;
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
        return false;
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

    /**
     * Create a unique node path (folder and name) for the job.
     */
    private String getNodePath(final String jobTopic, final String jobId) {
        if ( jobId != null ) {
            return jobTopic.replace('/', '.') + "/" + EventHelper.filter(jobId);
        }
        return jobTopic.replace('/', '.') + "/Job " + UUID.randomUUID().toString();
    }

    /**
     * Process a job and unlock the node in the repository.
     * @param event The original event.
     * @param eventNode The node in the repository where the job is stored.
     */
    private void processJob(Event event, Node eventNode)  {
        final boolean parallelProcessing = event.getProperty(EventUtil.PROPERTY_JOB_QUEUE_NAME) != null
                                           || event.getProperty(EventUtil.PROPERTY_JOB_PARALLEL) != null;
        final String jobTopic = (String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC);
        boolean unlock = true;
        try {
            final Event jobEvent = this.getJobEvent(event, eventNode.getPath());
            eventNode.setProperty(EventHelper.NODE_PROPERTY_PROCESSOR, this.applicationId);
            eventNode.save();
            final EventAdmin localEA = this.eventAdmin;
            if ( localEA != null ) {
                // we need async delivery, otherwise we might create a deadlock
                // as this method runs inside a synchronized block and the finishedJob
                // method as well!
                localEA.postEvent(jobEvent);
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
     * Create the real job event.
     * This generates a new event object with the same properties, but with the
     * {@link EventUtil#PROPERTY_JOB_TOPIC} topic.
     * @param e The job event.
     * @return The real job event.
     */
    private Event getJobEvent(Event e, String nodePath) {
        final String eventTopic = (String)e.getProperty(EventUtil.PROPERTY_JOB_TOPIC);
        final Dictionary<String, Object> properties = new EventPropertiesMap(e);
        // put properties for finished job callback
        properties.put(EventUtil.JobStatusNotifier.CONTEXT_PROPERTY_NAME,
                new EventUtil.JobStatusNotifier.NotifierContext(this, nodePath));
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
        // add application id
        properties.put(EventUtil.PROPERTY_APPLICATION, eventNode.getProperty(EventHelper.NODE_PROPERTY_APPLICATION).getString());
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
    private void loadJobs() {
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
     * This is a notification from the component which processed the job.
     *
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
            if ( reschedule ) {
                // update event with retry count and retries
                final Dictionary<String, Object> newProperties = new EventPropertiesMap(job);
                newProperties.put(EventUtil.PROPERTY_JOB_RETRY_COUNT, retryCount);
                newProperties.put(EventUtil.PROPERTY_JOB_RETRIES, retries);
                job = new Event(job.getTopic(), newProperties);
            }
        }
        final boolean parallelProcessing = job.getProperty(EventUtil.PROPERTY_JOB_QUEUE_NAME) != null
                                        || job.getProperty(EventUtil.PROPERTY_JOB_PARALLEL) != null;
        // we have to use the same session for unlocking that we used for locking!
        synchronized ( this.backgroundSession ) {
            try {
                this.backgroundSession.refresh(false);
                // check if the job has been cancelled
                if ( !this.backgroundSession.itemExists(eventNodePath) ) {
                    return true;
                }
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
                            // remove node from repository if no job is set
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
                    // if this is an own job queue, we simply signal the queue to continue
                    // it will pick up the event and either reschedule or wait
                    if ( job.getProperty(EventUtil.PROPERTY_JOB_QUEUE_NAME) != null ) {
                        // we know the queue exists
                        final JobBlockingQueue jobQueue;
                        synchronized ( this.jobQueues ) {
                            jobQueue = this.jobQueues.get(job.getProperty(EventUtil.PROPERTY_JOB_QUEUE_NAME));
                        }
                        synchronized ( jobQueue.getLock()) {
                            jobQueue.notifyFinish(info);
                        }
                    } else {

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
                                queue.put(info);
                            } catch (InterruptedException e) {
                                // this should never happen
                                this.ignoreException(e);
                            }
                        }
                    }
                } else {
                    // if this is an own job queue, we simply signal the queue to continue
                    // it will pick up the event continue with the next event
                    if ( job.getProperty(EventUtil.PROPERTY_JOB_QUEUE_NAME) != null ) {
                        // we know the queue exists
                        final JobBlockingQueue jobQueue;
                        synchronized ( this.jobQueues ) {
                            jobQueue = this.jobQueues.get(job.getProperty(EventUtil.PROPERTY_JOB_QUEUE_NAME));
                        }
                        synchronized ( jobQueue.getLock()) {
                            jobQueue.notifyFinish(null);
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
     * Search for job nodes
     * @param topic The job topic
     * @param filterProps optional filter props
     * @param locked only active jobs?
     * @return
     * @throws RepositoryException
     */
    private Collection<Event> queryJobs(final String topic,
                                        final Boolean locked,
                                        final Map<String, Object>... filterProps)  {
        // we create a new session
        Session s = null;
        final List<Event> jobs = new ArrayList<Event>();
        try {
            s = this.createSession();
            final QueryManager qManager = s.getWorkspace().getQueryManager();
            final StringBuffer buffer = new StringBuffer("/jcr:root");
            buffer.append(this.repositoryPath);
            if ( topic != null ) {
                buffer.append('/');
                buffer.append(topic.replace('/', '.'));
            }
            buffer.append("//element(*, ");
            buffer.append(this.getEventNodeType());
            buffer.append(") [not(@");
            buffer.append(EventHelper.NODE_PROPERTY_FINISHED);
            buffer.append(")");
            if ( locked != null ) {
                if ( locked ) {
                    buffer.append(" and @jcr:lockOwner");
                } else {
                    buffer.append(" and not(@jcr:lockOwner)");
                }
            }
            if ( filterProps != null && filterProps.length > 0 ) {
                buffer.append(" and (");
                int index = 0;
                for (Map<String,Object> template : filterProps) {
                    if ( index > 0 ) {
                        buffer.append(" or ");
                    }
                    buffer.append('(');
                    final Iterator<Map.Entry<String, Object>> i = template.entrySet().iterator();
                    boolean first = true;
                    while ( i.hasNext() ) {
                        final Map.Entry<String, Object> current = i.next();
                        // check prop name first
                        final String propName = EventUtil.getNodePropertyName(current.getKey());
                        if ( propName != null ) {
                            // check value
                            final Value value = EventUtil.getNodePropertyValue(s.getValueFactory(), current.getValue());
                            if ( value != null ) {
                                if ( first ) {
                                    first = false;
                                    buffer.append('@');
                                } else {
                                    buffer.append(" and @");
                                }
                                buffer.append(propName);
                                buffer.append(" = '");
                                buffer.append(current.getValue());
                                buffer.append("'");
                            }
                        }
                    }
                    buffer.append(')');
                    index++;
                }
                buffer.append(')');
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
        return this.getCurrentJobs(topic, (Map<String, Object>[])null);
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
        return this.getScheduledJobs(topic, (Map<String, Object>[])null);
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#getCurrentJobs(java.lang.String, java.util.Map...)
     */
    public Collection<Event> getCurrentJobs(String topic, Map<String, Object>... filterProps) {
        return this.queryJobs(topic, true, filterProps);
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#getScheduledJobs(java.lang.String, java.util.Map...)
     */
    public Collection<Event> getScheduledJobs(String topic, Map<String, Object>... filterProps) {
        return this.queryJobs(topic, false, filterProps);
    }


    /**
     * @see org.apache.sling.event.JobStatusProvider#getAllJobs(java.lang.String, java.util.Map...)
     */
    public Collection<Event> getAllJobs(String topic, Map<String, Object>... filterProps) {
        return this.queryJobs(topic, null, filterProps);
    }


    /**
     * @see org.apache.sling.event.JobStatusProvider#cancelJob(java.lang.String, java.lang.String)
     */
    public void cancelJob(String topic, String jobId) {
        if ( jobId != null && topic != null ) {
            this.cancelJob(this.getNodePath(topic, jobId));
        }
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#cancelJob(java.lang.String)
     */
    public void cancelJob(String jobId) {
        if ( jobId != null ) {
            synchronized ( this.writerSession ) {
                try {
                    this.writerSession.refresh(false);
                } catch (RepositoryException e) {
                    this.ignoreException(e);
                }
                try {
                    if ( this.writerSession.itemExists(jobId) ) {
                        final Item item = this.writerSession.getItem(jobId);
                        final Node parentNode = item.getParent();
                        item.remove();
                        parentNode.save();
                    }
                } catch (RepositoryException e) {
                    this.logger.error("Error during cancelling job at " + jobId, e);
                }
            }
        }
    }


    private static final class JobBlockingQueue extends LinkedBlockingQueue<EventInfo> {

        private EventInfo eventInfo;

        private final Object lock = new Object();

        public EventInfo waitForFinish() throws InterruptedException {
            this.lock.wait();
            final EventInfo object = this.eventInfo;
            this.eventInfo = null;
            return object;
        }

        public void notifyFinish(EventInfo i) {
            this.eventInfo = i;
            this.lock.notify();
        }

        public Object getLock() {
            return lock;
        }
    }
}
