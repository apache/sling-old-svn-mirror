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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.Locked;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.JobStatusProvider;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;


/**
 * An event handler handling special job events.
 *
 * @scr.component inherit="true"
 * @scr.service interface="org.apache.sling.event.JobStatusProvider"
 * @scr.property name="event.topics" value="org/apache/sling/event/job"
 *
 */
public class JobEventHandler
    extends AbstractRepositoryEventHandler
    implements Runnable, EventUtil.JobStatusNotifier, JobStatusProvider {

    /** @scr.property value="/sling/jobs" */
    protected static final String CONFIG_PROPERTY_REPO_PATH = "repository.job.path";

    /** @scr.property value="20" type="Long" */
    protected static final String CONFIG_PROPERTY_SLEEP_TIME = "sleep.time";

    /** A local queue for serialising the job processing. */
    protected final BlockingQueue<JobInfo> queue = new LinkedBlockingQueue<JobInfo>();

    /** A flag indicating if this handler is currently processing a job. */
    protected boolean isProcessing = false;

    /** Is the background task still running? */
    protected boolean running;

    /** We check every 20 secs by default. */
    protected long sleepTime;

    /**
     * Activate this component.
     * @param context
     * @throws RepositoryException
     */
    protected void activate(final ComponentContext context)
    throws RepositoryException {
        this.repositoryPath = (String)context.getProperties().get(CONFIG_PROPERTY_REPO_PATH);
        this.sleepTime = (Long)context.getProperties().get(CONFIG_PROPERTY_SLEEP_TIME) * 1000;

        super.activate(context);
        // load unprocessed jobs from repository
        this.loadJobs();

        // start background thread
        this.running = true;
        final Thread t = new Thread() {
            public void run() {
                JobEventHandler.this.runInBackground();
            }
        };
        t.start();
    }

    /**
     * Clean up the repository.
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
            new Locked() {

                protected Object run(Node node) throws RepositoryException {
                    final QueryManager qManager = node.getSession().getWorkspace().getQueryManager();
                    final StringBuffer buffer = new StringBuffer("/jcr:root");
                    buffer.append(JobEventHandler.this.repositoryPath);
                    buffer.append("//element(*, ");
                    buffer.append(JobEventHandler.this.getEventNodeType());
                    buffer.append(") [");
                    buffer.append(EventHelper.NODE_PROPERTY_ACTIVE);
                    buffer.append(" = 'false' and ");
                    buffer.append(EventHelper.NODE_PROPERTY_FINISHED);
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
                    return null;
                }
            }.with(parentNode, false);
        } catch (RepositoryException e) {
            // in the case of an error, we just log this as a warning
            this.logger.warn("Exception during repository cleanup.", e);
        } catch (InterruptedException e) {
            // we ignore this
            this.ignoreException(e);
        } finally {
            if ( s != null ) {
                s.logout();
            }
        }
    }

    /**
     * This method runs in the background and processes the local queue.
     */
    protected void runInBackground() {
        while ( this.running ) {
            // so let's wait/get the next job from the queue
            JobInfo info = null;
            try {
                info = this.queue.take();
            } catch (InterruptedException e) {
                // we ignore this
                this.ignoreException(e);
            }
            if ( info != null && this.running ) {
                if ( info.nodePath == null ) {
                    this.processEvent(info.event);
                } else {
                    boolean process = false;
                    synchronized ( this ) {
                        if ( !this.isProcessing ) {
                            this.isProcessing = true;
                            process = true;
                        }
                    }
                    if ( process ) {
                        boolean unlock = true;
                        try {
                            this.session.refresh(true);
                            final Node eventNode = (Node) this.session.getItem(info.nodePath);
                            if ( !eventNode.isLocked() && eventNode.getProperty(EventHelper.NODE_PROPERTY_ACTIVE).getBoolean() ) {
                                // lock node
                                Lock lock = null;
                                try {
                                    lock = eventNode.lock(false, true);
                                } catch (RepositoryException re) {
                                    // lock failed which means that the node is locked by someone else, so we don't have to requeue
                                    process = false;
                                }
                                if ( process ) {
                                    // check if event is still active
                                    eventNode.refresh(true);
                                    if ( eventNode.getProperty(EventHelper.NODE_PROPERTY_ACTIVE).getBoolean() ) {
                                        unlock = false;
                                        this.processJob(info.event, eventNode, lock.getLockToken());
                                    } else {
                                        eventNode.unlock();
                                    }
                                }
                            }
                        } catch (RepositoryException e) {
                            // ignore
                            this.ignoreException(e);
                        } finally {
                            if ( unlock ) {
                                synchronized ( this ) {
                                    this.isProcessing = false;
                                }
                            }
                        }
                    } else {
                        try {
                            // check if the node is in processing or already finished
                            final Node eventNode = (Node) this.session.getItem(info.nodePath);
                            if ( !eventNode.isLocked() && eventNode.getProperty(EventHelper.NODE_PROPERTY_ACTIVE).getBoolean() ) {
                                try {
                                    this.queue.put(info);
                                } catch (InterruptedException e) {
                                    // ignore
                                    this.ignoreException(e);
                                }
                                // wait time before we restart the cycle, if there is only one job in the queue!
                                if ( this.queue.size() == 1 ) {
                                    try {
                                        Thread.sleep(this.sleepTime);
                                    } catch (InterruptedException e) {
                                        // ignore
                                        this.ignoreException(e);
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
    }

    /**
     * @see org.apache.sling.core.event.impl.AbstractRepositoryEventHandler#deactivate(org.osgi.service.component.ComponentContext)
     */
    protected void deactivate(ComponentContext context) {
        // stop background thread, by adding a job info to wake it up
        this.running = false;
        try {
            this.queue.put(new JobInfo());
        } catch (InterruptedException e) {
            // we ignore this
            this.ignoreException(e);
        }
        super.deactivate(context);
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
     * @see org.apache.sling.core.event.impl.AbstractRepositoryEventHandler#getContainerNodeType()
     */
    protected String getContainerNodeType() {
        return EventHelper.JOBS_NODE_TYPE;
    }

    /**
     * @see org.apache.sling.core.event.impl.AbstractRepositoryEventHandler#getEventNodeType()
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
            final String jobId = (String)event.getProperty(EventUtil.PROPERTY_JOB_ID);
            final String jobTopic = (String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC);

            //  job id  and job topic must be set, otherwise we ignore this event!
            if ( jobId != null && jobTopic != null ) {
                // queue the event in order to respond quickly
                final JobInfo info = new JobInfo();
                info.event = event;
                try {
                    this.queue.put(info);
                } catch (InterruptedException e) {
                    // this should never happen
                    this.ignoreException(e);
                }
            } else {
                this.logger.warn("Event does not contain job topic or job id properties: {}", event);
            }
        }
    }

    protected void processEvent(final Event event) {
        final String jobId = (String)event.getProperty(EventUtil.PROPERTY_JOB_ID);
        final String jobTopic = (String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC);
        // we lock the parent node to ensure that noone else tries to add the same job to the repository
        // while we are doing it
        Lock lock = null;
        try {
            final Node parentNode = (Node)this.session.getItem(this.repositoryPath);
            lock = (Lock) new Locked() {

                protected Object run(Node node) throws RepositoryException {
                    // if there is a node, we know that there is exactly one node
                    final Node foundNode = JobEventHandler.this.queryJob(jobTopic, jobId);
                    boolean writeAndSend =false;
                    // if node is not present, we'll write it, lock it and send the event
                    if ( foundNode == null ) {
                        writeAndSend = true;
                    } else {
                        // node is already in repository, let's check the application id
                        if ( foundNode.getProperty(EventHelper.NODE_PROPERTY_APPLICATION).getString().equals(JobEventHandler.this.applicationId) ) {
                            // delete old node (deleting is easier than updating...)
                            foundNode.remove();
                            parentNode.save();
                            writeAndSend = true;
                        }
                    }
                    if ( writeAndSend ) {
                        final Node eventNode = JobEventHandler.this.writeEvent(event);
                        return eventNode.lock(false, true);
                    }
                    return null;
                }
            }.with(parentNode, false);
        } catch (RepositoryException re ) {
            // something went wrong, so let's log it
            this.logger.error("Exception during writing new job to repository.", re);
        } catch (InterruptedException e) {
            // This should never happen from the lock, so we ignore it
            this.ignoreException(e);
        }

        // if we have a lock, we will try to fire the job
        if ( lock != null ) {
            final Node eventNode = lock.getNode();
            final boolean parallelProcessing = event.getProperty(EventUtil.PROPERTY_JOB_PARALLEL) != null;

            if ( parallelProcessing ) {
                // if the job can be run in parallel, we'll just run it
                this.processJob(event, eventNode, lock.getLockToken());
            } else {
                // we need to serialize the jobs
                // lets check if we are currently processing a job
                boolean process = false;
                synchronized ( this ) {
                    if ( !this.isProcessing ) {
                        this.isProcessing = true;
                        process = true;
                    }
                }
                if ( process ) {
                    this.processJob(event, eventNode, lock.getLockToken());
                } else {
                    // we don't process the job right now, so unlock and put in local queue
                    final JobInfo info = new JobInfo();
                    info.event = event;
                    try {
                        info.nodePath = eventNode.getPath();
                        this.queue.put(info);
                    } catch (RepositoryException e) {
                        // getPath() should work at this stage, so we ignore it
                        this.ignoreException(e);
                    } catch (InterruptedException e) {
                        // this should never happen so we ignore it
                        this.ignoreException(e);
                    }
                    try {
                        eventNode.unlock();
                    } catch (RepositoryException e) {
                        // if unlocking fails, we will just ignore it
                        this.ignoreException(e);
                    }
                }
            }
        }
    }

    /**
     * Process a job and unlock the node in the repository.
     * @param event The original event.
     * @param eventNode The node in the repository where the job is stored.
     */
    protected void processJob(Event event, Node eventNode, String lockToken)  {
        final boolean parallelProcessing = event.getProperty(EventUtil.PROPERTY_JOB_PARALLEL) != null;
        boolean unlock = true;
        try {
            final Event jobEvent = this.getJobEvent(event, eventNode, lockToken);
            eventNode.setProperty(EventHelper.NODE_PROPERTY_PROCESSOR, this.applicationId);
            eventNode.save();
            this.eventAdmin.sendEvent(jobEvent);
            // do not unlock if sending was successful
            unlock = false;
        } catch (RepositoryException re) {
            // if an exception occurs, we just log
            this.logger.error("Exception during job processing.", re);
        } finally {
            if ( unlock ) {
                if ( !parallelProcessing ) {
                    synchronized ( this ) {
                        this.isProcessing = false;
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
    protected Event getJobEvent(Event e, Node eventNode, String lockToken)
    throws RepositoryException {
        final String eventTopic = (String)e.getProperty(EventUtil.PROPERTY_JOB_TOPIC);
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        final String[] propertyNames = e.getPropertyNames();
        for(int i=0; i<propertyNames.length; i++) {
            properties.put(propertyNames[i], e.getProperty(propertyNames[i]));
        }
        // put properties for finished job callback
        properties.put(EventUtil.JobStatusNotifier.CONTEXT_PROPERTY_NAME, new EventUtil.JobStatusNotifier.NotifierContext(this, eventNode.getPath(), lockToken));
        return new Event(eventTopic, properties);
    }

    /**
     * @see org.apache.sling.core.event.impl.AbstractRepositoryEventHandler#addNodeProperties(javax.jcr.Node, org.osgi.service.event.Event)
     */
    protected void addNodeProperties(Node eventNode, Event event)
    throws RepositoryException {
        super.addNodeProperties(eventNode, event);
        eventNode.setProperty(EventHelper.NODE_PROPERTY_TOPIC, (String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC));
        eventNode.setProperty(EventHelper.NODE_PROPERTY_ACTIVE, true);
        eventNode.setProperty(EventHelper.NODE_PROPERTY_JOBID, (String)event.getProperty(EventUtil.PROPERTY_JOB_ID));
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
                        if ( !eventNode.isLocked()
                             && eventNode.hasProperty(EventHelper.NODE_PROPERTY_ACTIVE)
                             && eventNode.getProperty(EventHelper.NODE_PROPERTY_ACTIVE).getBoolean() ) {
                            final JobInfo info = new JobInfo();
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
     * Search for all a node with the corresponding topic and unique key.
     * @param topic
     * @param key
     * @return The node or null.
     * @throws RepositoryException
     */
    protected Node queryJob(String topic, String key) throws RepositoryException {
        final QueryManager qManager = this.session.getWorkspace().getQueryManager();
        final StringBuffer buffer = new StringBuffer("/jcr:root");
        buffer.append(this.repositoryPath);
        buffer.append("//element(*, ");
        buffer.append(this.getEventNodeType());
        buffer.append(") [");
        buffer.append(EventHelper.NODE_PROPERTY_TOPIC);
        buffer.append(" = '");
        buffer.append(topic);
        buffer.append("' and ");
        buffer.append(EventHelper.NODE_PROPERTY_JOBID);
        buffer.append(" = '");
        buffer.append(key);
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
     * Load all active jobs from the repository.
     * @throws RepositoryException
     */
    protected void loadJobs() throws RepositoryException {
        final QueryManager qManager = this.session.getWorkspace().getQueryManager();
        final StringBuffer buffer = new StringBuffer("/jcr:root");
        buffer.append(this.repositoryPath);
        buffer.append("//element(*, ");
        buffer.append(this.getEventNodeType());
        buffer.append(") [");
        buffer.append(EventHelper.NODE_PROPERTY_ACTIVE);
        buffer.append(" = 'true']");
        final Query q = qManager.createQuery(buffer.toString(), Query.XPATH);
        final NodeIterator result = q.execute().getNodes();
        while ( result.hasNext() ) {
            final Node eventNode = result.nextNode();
            if ( !eventNode.isLocked() ) {
                final Event event = this.readEvent(eventNode);
                final JobInfo info = new JobInfo();
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
     * @see org.apache.sling.core.event.EventUtil.JobStatusNotifier#finishedJob(org.osgi.service.event.Event, String, String, boolean)
     */
    public void finishedJob(Event job, String eventNodePath, String lockToken, boolean reschedule) {
        final boolean parallelProcessing = job.getProperty(EventUtil.PROPERTY_JOB_PARALLEL) != null;
        Session s = null;
        try {
            s = this.createSession();
            // remove lock token from shared session and add it to current session
            this.session.removeLockToken(lockToken);
            s.addLockToken(lockToken);
            final Node eventNode = (Node) s.getItem(eventNodePath);
            try {
                if ( !reschedule ) {
                    eventNode.setProperty(EventHelper.NODE_PROPERTY_FINISHED, Calendar.getInstance());
                    eventNode.setProperty(EventHelper.NODE_PROPERTY_ACTIVE, false);
                    eventNode.save();
                }
            } catch (RepositoryException re) {
                // if an exception occurs, we just log
                this.logger.error("Exception during job finishing.", re);
            } finally {
                if ( !parallelProcessing) {
                    synchronized ( this ) {
                        this.isProcessing = false;
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
            if ( reschedule ) {
                final JobInfo info = new JobInfo();
                try {
                    info.event = job;
                    info.nodePath = eventNode.getPath();
                    this.queue.put(info);
                } catch (InterruptedException e) {
                    // this should never happen
                    this.ignoreException(e);
                } catch (RepositoryException e) {
                    // this should never happen
                    this.ignoreException(e);
                }
            }
        } catch (RepositoryException re) {
            this.logger.error("Unable to create new session.", re);
        } finally {
            if ( s != null ) {
                s.logout();
            }
        }
    }

    /**
     * Search for active nodes
     * @param topic
     * @return
     * @throws RepositoryException
     */
    protected Collection<Event> queryCurrentJobs(String topic, boolean locked)  {
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
            buffer.append(") [");
            buffer.append(EventHelper.NODE_PROPERTY_ACTIVE);
            buffer.append(" = 'true'");
            if ( topic != null ) {
                buffer.append(" and ");
                buffer.append(EventHelper.NODE_PROPERTY_TOPIC);
                buffer.append(" = '");
                buffer.append(topic);
                buffer.append("'");
            }
            if ( locked ) {
                buffer.append(" and ");
                buffer.append(JcrConstants.JCR_LOCKOWNER);
            }
            buffer.append("]");
            final Query q = qManager.createQuery(buffer.toString(), Query.XPATH);
            final NodeIterator iter = q.execute().getNodes();
            while ( iter.hasNext() ) {
                final Node eventNode = iter.nextNode();
                final Event event = this.readEvent(eventNode);
                jobs.add(event);
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
     * @see org.apache.sling.core.event.JobStatusProvider#getCurrentJobs(java.lang.String)
     */
    public Collection<Event> getCurrentJobs(String topic) {
        return this.queryCurrentJobs(topic, true);
    }

    /**
     * @see org.apache.sling.core.event.JobStatusProvider#scheduledJobs(java.lang.String)
     */
    public Collection<Event> scheduledJobs(String topic) {
        return this.queryCurrentJobs(topic, false);
    }


    protected static final class JobInfo {
        public Event event;
        public String nodePath;
    }
}
