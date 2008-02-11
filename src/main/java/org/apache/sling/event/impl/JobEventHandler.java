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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jcr.ItemExistsException;
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
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.JobStatusProvider;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


/**
 * An event handler handling special job events.
 *
 * @scr.component
 * @scr.service interface="org.apache.sling.event.JobStatusProvider"
 * @scr.property name="event.topics" valueRef="EventUtil.TOPIC_JOB"
 * @scr.property name="repository.path" value="/sling/jobs"
 */
public class JobEventHandler
    extends AbstractRepositoryEventHandler
    implements EventUtil.JobStatusNotifier, JobStatusProvider {

    /** A map for keeping track of currently processed job topics. */
    protected final Map<String, Boolean> processingMap = new HashMap<String, Boolean>();

    /** Default sleep time. */
    protected static final long DEFAULT_SLEEP_TIME = 20;

    /** @scr.property valueRef="DEFAULT_SLEEP_TIME" */
    protected static final String CONFIG_PROPERTY_SLEEP_TIME = "sleep.time";

    /** We check every 20 secs by default. */
    protected long sleepTime;

    /** Background session. */
    protected Session backgroundSession;

    /**
     * Activate this component.
     * @param context
     * @throws RepositoryException
     */
    protected void activate(final ComponentContext context)
    throws RepositoryException {
        if ( context.getProperties().get(CONFIG_PROPERTY_SLEEP_TIME) != null ) {
            this.sleepTime = (Long)context.getProperties().get(CONFIG_PROPERTY_SLEEP_TIME) * 1000;
        } else {
            this.sleepTime = DEFAULT_SLEEP_TIME;
        }
        this.backgroundSession = this.createSession();
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
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#processWriteQueue()
     */
    protected void processWriteQueue() {
        while ( this.running ) {
            // so let's wait/get the next job from the queue
            EventInfo info = null;
            try {
                info = this.writeQueue.take();
            } catch (InterruptedException e) {
                // we ignore this
                this.ignoreException(e);
            }
            if ( info != null && this.running ) {
                final Event event = info.event;
                final String jobId = (String)event.getProperty(EventUtil.PROPERTY_JOB_ID);

                // if the job has no job id, we can just write the job to the repo and don't
                // need locking
                if ( jobId == null ) {
                    try {
                        final Node eventNode = this.writeEvent(event);
                        info.nodePath = eventNode.getPath();
                    } catch (RepositoryException re ) {
                        // something went wrong, so let's log it
                        this.logger.error("Exception during writing new job to repository.", re);
                    }
                } else {
                    try {
                        // let's first search for an existing node with the same id
                        final Node parentNode = (Node)this.writerSession.getItem(this.repositoryPath);
                        final String nodeName = this.getNodeName(event);
                        Node foundNode = null;
                        if ( parentNode.hasNode(nodeName) ) {
                            foundNode = parentNode.getNode(nodeName);
                        }
                        if ( foundNode != null ) {
                            // if the node is locked, someone else was quicker
                            // and we don't have to process this job
                            if ( foundNode.isLocked() ) {
                                foundNode = null;
                            } else {
                                // node is already in repository, so we just overwrite it
                                try {
                                    foundNode.remove();
                                    parentNode.save();
                                } catch (RepositoryException re) {
                                    // if anything goes wrong, it means that (hopefully) someone
                                    // else is processing this node
                                    foundNode = null;
                                }

                            }
                        }
                        if ( foundNode == null ) {
                            // We now write the event into the repository
                            try {
                                final Node eventNode = this.writeEvent(event);
                                info.nodePath = eventNode.getPath();
                            } catch (ItemExistsException iee) {
                                // someone else did already write this node in the meantime
                                // nothing to do for us
                            }
                        }
                    } catch (RepositoryException re ) {
                        // something went wrong, so let's log it
                        this.logger.error("Exception during writing new job to repository.", re);
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
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#getCleanUpQueryString()
     */
    protected String getCleanUpQueryString() {
        final Calendar deleteBefore = Calendar.getInstance();
        deleteBefore.add(Calendar.MINUTE, -this.cleanupPeriod);
        final String dateString = ISO8601.format(deleteBefore);
        final StringBuffer buffer = new StringBuffer("/jcr:root");
        buffer.append(JobEventHandler.this.repositoryPath);
        buffer.append("//element(*, ");
        buffer.append(JobEventHandler.this.getEventNodeType());
        buffer.append(") [@");
        buffer.append(EventHelper.NODE_PROPERTY_ACTIVE);
        buffer.append(" = 'false' and @");
        buffer.append(EventHelper.NODE_PROPERTY_FINISHED);
        buffer.append(" < xs:dateTime('");
        buffer.append(dateString);
        buffer.append("')]");

        return buffer.toString();
    }

    /**
     * This method runs in the background and processes the local queue.
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

    /**
     * Start the repository session and add this handler as an observer
     * for new events created on other nodes.
     * @throws RepositoryException
     */
    protected void startWriterSession() throws RepositoryException {
        super.startWriterSession();
        // load unprocessed jobs from repository
        this.loadJobs();
        this.backgroundSession.getWorkspace().getObservationManager()
            .addEventListener(this,
                              javax.jcr.observation.Event.PROPERTY_CHANGED | javax.jcr.observation.Event.PROPERTY_REMOVED,
                              this.repositoryPath,
                              true,
                              null,
                              new String[] {this.getEventNodeType()},
                              true);
    }

    /**
     * @see org.apache.sling.core.event.impl.JobPersistenceHandler#getContainerNodeType()
     */
    protected String getContainerNodeType() {
        return EventHelper.JOBS_NODE_TYPE;
    }

    /**
     * @see org.apache.sling.core.event.impl.JobPersistenceHandler#getEventNodeType()
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
            final String jobTopic = (String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC);

            //  job topic must be set, otherwise we ignore this event!
            if ( jobTopic != null ) {
                // queue the event in order to respond quickly
                final EventInfo info = new EventInfo();
                info.event = event;
                try {
                    this.writeQueue.put(info);
                } catch (InterruptedException e) {
                    // this should never happen
                    this.ignoreException(e);
                }
            } else {
                this.logger.warn("Event does not contain job topic: {}", event);
            }
        }
    }

    /**
     * @see org.apache.sling.event.impl.AbstractRepositoryEventHandler#getNodeName(org.osgi.service.event.Event)
     */
    protected String getNodeName(Event event) {
        final String jobId = (String)event.getProperty(EventUtil.PROPERTY_JOB_ID);
        if ( jobId != null ) {
            final String jobTopic = ((String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC)).replace('/', '.');
            return jobTopic + " " + jobId.replace('/', '.');
        }

        return "Job " + UUID.randomUUID().toString();
    }

    /**
     * Process a job and unlock the node in the repository.
     * @param event The original event.
     * @param eventNode The node in the repository where the job is stored.
     */
    protected void processJob(Event event, Node eventNode, String lockToken)  {
        final boolean parallelProcessing = event.getProperty(EventUtil.PROPERTY_JOB_PARALLEL) != null;
        final String jobTopic = (String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC);
        boolean unlock = true;
        try {
            final Event jobEvent = this.getJobEvent(event, eventNode, lockToken);
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
     * @see org.apache.sling.core.event.impl.JobPersistenceHandler#addNodeProperties(javax.jcr.Node, org.osgi.service.event.Event)
     */
    protected void addNodeProperties(Node eventNode, Event event)
    throws RepositoryException {
        super.addNodeProperties(eventNode, event);
        eventNode.setProperty(EventHelper.NODE_PROPERTY_TOPIC, (String)event.getProperty(EventUtil.PROPERTY_JOB_TOPIC));
        eventNode.setProperty(EventHelper.NODE_PROPERTY_ACTIVE, true);
        final String jobId = (String)event.getProperty(EventUtil.PROPERTY_JOB_ID);
        if ( jobId != null ) {
            eventNode.setProperty(EventHelper.NODE_PROPERTY_JOBID, jobId);
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
                            final Node eventNode = (Node) s.getItem(nodePath);
                            if ( !eventNode.isLocked()
                                 && eventNode.hasProperty(EventHelper.NODE_PROPERTY_ACTIVE)
                                 && eventNode.getProperty(EventHelper.NODE_PROPERTY_ACTIVE).getBoolean() ) {
                                final EventInfo info = new EventInfo();
                                info.event = this.readEvent(eventNode);
                                info.nodePath = nodePath;
                                try {
                                    this.queue.put(info);
                                } catch (InterruptedException e) {
                                    // we ignore this exception as this should never occur
                                    this.ignoreException(e);
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
    protected void loadJobs() throws RepositoryException {
        final QueryManager qManager = this.writerSession.getWorkspace().getQueryManager();
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
     * @see org.apache.sling.event.EventUtil.JobStatusNotifier#finishedJob(org.osgi.service.event.Event, String, String, boolean)
     */
    public boolean finishedJob(Event job, String eventNodePath, String lockToken, boolean shouldReschedule) {
        boolean reschedule = shouldReschedule;
        if ( shouldReschedule ) {
            // check if we exceeded the number of retries
            if ( job.getProperty(EventUtil.PROPERTY_JOB_RETRIES) != null ) {
                int retries = (Integer) job.getProperty(EventUtil.PROPERTY_JOB_RETRIES);
                int retryCount = 0;
                if ( job.getProperty(EventUtil.PROPERTY_JOB_RETRY_COUNT) != null ) {
                    retryCount = (Integer)job.getProperty(EventUtil.PROPERTY_JOB_RETRY_COUNT);
                }
                retryCount++;
                if ( retryCount >= retries ) {
                    reschedule = false;
                }
                // update event with retry count
                final Dictionary<String, Object> newProperties;
                // create a new dictionary
                newProperties = new Hashtable<String, Object>();
                final String[] names = job.getPropertyNames();
                for(int i=0; i<names.length; i++ ) {
                    newProperties.put(names[i], job.getProperty(names[i]));
                }
                newProperties.put(EventUtil.PROPERTY_JOB_RETRY_COUNT, retryCount);
                job = new Event(job.getTopic(), newProperties);
            }
        }
        final boolean parallelProcessing = job.getProperty(EventUtil.PROPERTY_JOB_PARALLEL) != null;
        Session s = null;
        try {
            s = this.createSession();
            // remove lock token from shared session and add it to current session
            this.backgroundSession.removeLockToken(lockToken);
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
                    final String jobTopic = (String)job.getProperty(EventUtil.PROPERTY_JOB_TOPIC);
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
            if ( reschedule ) {
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
                    final Thread t = new Thread() {
                        public void run() {
                            try {
                                Thread.sleep(delay);
                            } catch (InterruptedException e) {
                                // this should never happen
                                ignoreException(e);
                            }
                            try {
                                queue.put(info);
                            } catch (InterruptedException e) {
                                // this should never happen
                                ignoreException(e);
                            }
                        }
                    };
                    t.start();
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
     * @see org.apache.sling.event.JobStatusProvider#getCurrentJobs(java.lang.String)
     */
    public Collection<Event> getCurrentJobs(String topic) {
        return this.queryCurrentJobs(topic, true);
    }

    /**
     * @see org.apache.sling.event.JobStatusProvider#scheduledJobs(java.lang.String)
     */
    public Collection<Event> scheduledJobs(String topic) {
        return this.queryCurrentJobs(topic, false);
    }
}
