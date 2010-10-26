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
package org.apache.sling.event.impl.jobs.queues;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.sling.event.EventPropertiesMap;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.EnvironmentComponent;
import org.apache.sling.event.impl.jobs.JobEvent;
import org.apache.sling.event.impl.jobs.JobStatusNotifier;
import org.apache.sling.event.impl.jobs.StatisticsImpl;
import org.apache.sling.event.impl.jobs.Utility;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.Statistics;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The job blocking queue extends the blocking queue by some
 * functionality for the job event handling.
 */
public abstract class AbstractJobQueue
    extends StatisticsImpl
    implements JobStatusNotifier, Queue {

    /** Default number of seconds to wait for an ack. */
    private static final long DEFAULT_WAIT_FOR_ACK_IN_MS = 60 * 1000; // by default we wait 60 secs

    /** Default timeout for suspend. */
    private static final long MAX_SUSPEND_TIME = 1000 * 60 * 60; // 60 mins

    /** The logger. */
    protected final Logger logger;

    /** Configuration. */
    protected final InternalQueueConfiguration configuration;

    /** The environment component. */
    private final EnvironmentComponent environment;

    /** The queue name. */
    protected volatile String queueName;

    /** Are we still running? */
    protected volatile boolean running;

    /** Are we marked for cleanup */
    private volatile boolean markedForCleanUp = false;

    /** Is the queue currently waiting(sleeping) */
    protected volatile boolean isWaiting = false;

    /** The map of events we're have started (send). */
    private final Map<String, JobEvent> startedJobsLists = new HashMap<String, JobEvent>();

    /** The map of events we're processing. */
    private final Map<String, JobEvent> processsingJobsLists = new HashMap<String, JobEvent>();

    /** Suspended since. */
    private final AtomicLong suspendedSince = new AtomicLong(-1);

    /** Suspend lock. */
    private final Object suspendLock = new Object();

    /**
     * Start this queue
     * @param name The queue name
     * @param config The queue configuration
     * @param environment The environment component
     */
    public AbstractJobQueue(final String name,
                    final InternalQueueConfiguration config,
                    final EnvironmentComponent environment) {
        this.queueName = name;
        this.configuration = config;
        this.logger = LoggerFactory.getLogger(this.getClass().getName() + '.' + name);
        this.running = true;
        this.environment = environment;
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#getStateInfo()
     */
    public String getStateInfo() {
        return "isWaiting=" + this.isWaiting + ", markedForCleanUp=" + this.markedForCleanUp + ", suspendedSince=" + this.suspendedSince.longValue();
    }

    /**
     * Start the job queue.
     */
    public void start() {
        final Thread queueThread = new Thread(new Runnable() {

            public void run() {
                while ( running ) {
                    logger.info("Starting job queue {}", queueName);
                    logger.debug("Configuration for job queue={}", configuration);

                    try {
                        runJobQueue();
                    } catch (Throwable t) { //NOSONAR
                        logger.error("Job queue " + queueName + " stopped with exception: " + t.getMessage() + ". Restarting.", t);
                    }
                }
            }

        }, "Apache Sling Job Queue " + queueName);
        queueThread.setDaemon(true);
        queueThread.start();
    }

    /**
     * Return the queue configuration
     */
    public InternalQueueConfiguration getConfiguration() {
        return this.configuration;
    }

    /**
     * Close this queue.
     */
    public void close() {
        this.running = false;
        this.logger.debug("Shutting down job queue {}", queueName);
        this.logger.debug("Waking up sleeping queue {}", queueName);
        this.resume();
        if ( this.isWaiting ) {
            this.logger.debug("Waking up waiting queue {}", this.queueName);
            this.notifyFinished(null);
        }
        // continue queue processing to stop the queue
        this.put(new JobEvent(null, null) {
            public boolean lock() { return false; }
            public void unlock() {
                // dummy impl
            }
            public void finished() {
                // dummy impl
            }
            public void restart() {
                // dummy impl
            }
            public boolean remove() { return true; }
            public boolean reschedule() { return false; }
            });

        this.processsingJobsLists.clear();
        this.startedJobsLists.clear();
        this.logger.info("Stopped job queue {}", this.queueName);
    }

    /**
     * Periodically cleanup.
     */
    public void cleanup() {
        if ( this.running ) {
            // check for jobs that were started but never got an aknowledge
            final long tooOld = System.currentTimeMillis() - DEFAULT_WAIT_FOR_ACK_IN_MS;
            // to keep the synchronized block as fast as possible we just store the
            // jobs to be removed in a new list and process this list afterwards
            final List<JobEvent> restartJobs = new ArrayList<JobEvent>();
            synchronized ( this.startedJobsLists ) {
                final Iterator<Map.Entry<String, JobEvent>> i = this.startedJobsLists.entrySet().iterator();
                while ( i.hasNext() ) {
                    final Map.Entry<String, JobEvent> entry = i.next();
                    if ( entry.getValue().started <= tooOld ) {
                        restartJobs.add(entry.getValue());
                    }
                }
            }

            // restart jobs is now a list of potential candidates, we now have to check
            // each candidate separately again!
            if ( restartJobs.size() > 0 ) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // we just ignore this
                    this.ignoreException(e);
                }
            }
            final Iterator<JobEvent> jobIter = restartJobs.iterator();
            while ( jobIter.hasNext() ) {
                final JobEvent info = jobIter.next();
                boolean process = false;
                synchronized ( this.startedJobsLists ) {
                    process = this.startedJobsLists.remove(info.uniqueId) != null;
                }
                if ( process ) {
                    this.logger.info("No acknowledge received for job {} stored at {}. Requeueing job.", EventUtil.toString(info.event), info.uniqueId);
                    this.finishedJob(info.event, true);
                }
            }
        }
    }

    /**
     * @see org.apache.sling.event.impl.jobs.JobStatusNotifier#sendAcknowledge(org.osgi.service.event.Event)
     */
    public boolean sendAcknowledge(final Event job) {
        final String location = (String)job.getProperty(JobUtil.JOB_ID);
        final JobEvent ack;
        synchronized ( this.startedJobsLists ) {
            ack = this.startedJobsLists.remove(location);
        }
        // if the event is still in the processing list, we confirm the ack
        if ( ack != null ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Received ack for job {}", EventUtil.toString(job));
            }
            final long queueTime = ack.started - ack.queued;
            this.addActive(queueTime);
            Utility.sendNotification(this.environment, JobUtil.TOPIC_JOB_STARTED, job, queueTime);
            synchronized ( this.processsingJobsLists ) {
                this.processsingJobsLists.put(location, ack);
            }
        } else {
            this.decQueued();
        }
        return ack != null;
    }

    private boolean handleReschedule(final JobEvent jobEvent, final boolean shouldReschedule) {
        boolean reschedule = shouldReschedule;
        if ( shouldReschedule ) {
            // check if we exceeded the number of retries
            int retries = this.configuration.getMaxRetries();
            if ( jobEvent.event.getProperty(JobUtil.PROPERTY_JOB_RETRIES) != null ) {
                retries = (Integer) jobEvent.event.getProperty(JobUtil.PROPERTY_JOB_RETRIES);
            }
            int retryCount = 0;
            if ( jobEvent.event.getProperty(JobUtil.PROPERTY_JOB_RETRY_COUNT) != null ) {
                retryCount = (Integer)jobEvent.event.getProperty(JobUtil.PROPERTY_JOB_RETRY_COUNT);
            }
            retryCount++;
            if ( retries != -1 && retryCount > retries ) {
                reschedule = false;
            }
            if ( reschedule ) {
                // update event with retry count and retries
                final Dictionary<String, Object> newProperties = new EventPropertiesMap(jobEvent.event);
                newProperties.put(JobUtil.PROPERTY_JOB_RETRY_COUNT, retryCount);
                newProperties.put(JobUtil.PROPERTY_JOB_RETRIES, retries);
                jobEvent.event = new Event(jobEvent.event.getTopic(), newProperties);
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Failed job {}", EventUtil.toString(jobEvent.event));
                }
                this.failedJob();
                jobEvent.queued = System.currentTimeMillis();
                Utility.sendNotification(this.environment, JobUtil.TOPIC_JOB_FAILED, jobEvent.event, null);
            } else {
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Cancelled job {}", EventUtil.toString(jobEvent.event));
                }
                this.cancelledJob();
                Utility.sendNotification(this.environment, JobUtil.TOPIC_JOB_CANCELLED, jobEvent.event, null);
            }
        } else {
            if ( this.logger.isDebugEnabled() ) {
                this.logger.debug("Finished job {}", EventUtil.toString(jobEvent.event));
            }
            final long processingTime = System.currentTimeMillis() - jobEvent.started;
            this.finishedJob(processingTime);
            Utility.sendNotification(this.environment, JobUtil.TOPIC_JOB_FINISHED, jobEvent.event, processingTime);
        }

        return reschedule;
    }

    /**
     * @see org.apache.sling.event.impl.jobs.JobStatusNotifier#finishedJob(org.osgi.service.event.Event, boolean)
     */
    public boolean finishedJob(final Event job, final boolean shouldReschedule) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Received finish for job {}, shouldReschedule={}", EventUtil.toString(job), shouldReschedule);
        }
        if ( !this.running ) {
            if ( this.logger.isDebugEnabled() ) {
                this.logger.debug("Queue is not running anymore. Discarding finish for {}", EventUtil.toString(job));
            }
            return false;
        }
        final String location = (String)job.getProperty(JobUtil.JOB_ID);
        // let's remove the event from our processing list
        // this is just a sanity check, as usually the job should have been
        // removed during sendAcknowledge.
        synchronized ( this.startedJobsLists ) {
            this.startedJobsLists.remove(location);
        }

        // get job event
        final JobEvent info;
        synchronized ( this.processsingJobsLists ) {
            info = this.processsingJobsLists.remove(location);
        }
        if ( info == null ) {
            if ( this.logger.isDebugEnabled() ) {
                this.logger.debug("This job has never been started by this queue: {}", EventUtil.toString(job));
            }
            return false;
        }

        // handle the reschedule, a new job might be returned with updated reschedule info!
        final boolean reschedule = this.handleReschedule(info, shouldReschedule);

        // if this is set after the synchronized block we have an error
        final boolean finishSuccessful;

        if ( !reschedule ) {
            info.finished();
            finishSuccessful = true;
        } else {
            finishSuccessful = info.reschedule();
        }

        if ( !finishSuccessful || !reschedule ) {
            checkForNotify(null);
            return false;
        }
        checkForNotify(info);
        return true;
    }

    private void checkForNotify(final JobEvent info) {
        JobEvent reprocessInfo = null;
        if ( info != null ) {
            reprocessInfo = this.reschedule(info);
        }
        notifyFinished(reprocessInfo);
    }

    protected boolean canBeMarkedForCleanUp() {
        return this.isEmpty() && !this.isWaiting;
    }
    /**
     * Mark this queue for cleanup.
     */
    public void markForCleanUp() {
        if ( this.canBeMarkedForCleanUp() ) {
            this.markedForCleanUp = true;
        }
    }

    /**
     * Check if this queue is marked for cleanup
     */
    public boolean isMarkedForCleanUp() {
        if ( this.markedForCleanUp ) {
            if ( this.canBeMarkedForCleanUp() ) {
                return true;
            }
            this.markedForCleanUp = false;
        }
        return false;
    }

    /**
     * Get the name of the job queue.
     */
    public String getName() {
        return this.queueName;
    }


    /**
     * Add a new job to the queue.
     */
    public void process(final JobEvent event) {
        this.put(event);
        event.queued = System.currentTimeMillis();
        this.incQueued();
    }

    /**
     * Execute the qeue
     */
    private void runJobQueue() {
        JobEvent info = null;
        while ( this.running ) {
            while ( this.suspendedSince.longValue() != -1 ) {
                synchronized ( this.suspendLock ) {
                    try {
                        this.suspendLock.wait(MAX_SUSPEND_TIME);
                    } catch (final InterruptedException ignore) {
                        this.ignoreException(ignore);
                    }
                    if ( System.currentTimeMillis() > this.suspendedSince.longValue() + MAX_SUSPEND_TIME ) {
                        this.suspendedSince.set(-1);
                    }
                }
            }
            if ( info == null ) {
                // so let's wait/get the next job from the queue
                info = this.take();
            }

            if ( info != null && this.running ) {
                info = this.start(info);
            }
        }
    }

    /**
     * Process a job
     */
    protected boolean executeJob(final JobEvent info) {
        if ( logger.isDebugEnabled() ) {
            logger.debug("Executing job {}.", EventUtil.toString(info.event));
        }
        if ( info.lock() ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Starting job {}", EventUtil.toString(info.event));
            }
            boolean unlock = true;
            try {
                final Event jobEvent = this.getJobEvent(info);
                final EventAdmin localEA = this.environment.getEventAdmin();
                info.started = System.currentTimeMillis();
                // let's add the event to our processing list
                synchronized ( this.startedJobsLists ) {
                    this.startedJobsLists.put(info.uniqueId, info);
                }

                // we need async delivery, otherwise we might create a deadlock
                // as this method runs inside a synchronized block and the finishedJob
                // method as well!
                localEA.postEvent(jobEvent);
                // do not unlock if sending was successful
                unlock = false;

                return true;

            } catch (final Exception re) {
                // if an exception occurs, we just log
                this.logger.error("Exception during job processing.", re);
            } finally {
                if ( unlock ) {
                    info.unlock();
                }
            }
        }

        this.decQueued();
        return false;
    }

    /**
     * Create the real job event.
     * This generates a new event object with the same properties, but with the
     * {@link EventUtil#PROPERTY_JOB_TOPIC} topic.
     * @param info The job event.
     * @return The real job event.
     */
    private Event getJobEvent(final JobEvent info) {
        final String eventTopic = (String)info.event.getProperty(JobUtil.PROPERTY_JOB_TOPIC);
        final Dictionary<String, Object> properties = new EventPropertiesMap(info.event);
        // put properties for finished job callback
        properties.put(JobStatusNotifier.CONTEXT_PROPERTY_NAME,
                new JobStatusNotifier.NotifierContext(this));
        // remove app id and distributable flag
        properties.remove(EventUtil.PROPERTY_DISTRIBUTE);
        properties.remove(EventUtil.PROPERTY_APPLICATION);

        // set priority from configuration
        if ( properties.get(JobUtil.PROPERTY_JOB_PRIORITY) == null ) {
            properties.put(JobUtil.PROPERTY_JOB_PRIORITY, this.configuration.getPriority());
        }
        // set queue name
        properties.put(JobUtil.PROPERTY_JOB_QUEUE_NAME, info.queueName);

        return new Event(eventTopic, properties);
    }

    /**
     * Helper method which just logs the exception in debug mode.
     * @param e
     */
    protected void ignoreException(Exception e) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Ignored exception " + e.getMessage(), e);
        }
    }

    /**
     * Rename this queue.
     */
    public void rename(final String name) {
        this.logger.info("Queue reconfiguration: old queue {} is renamed to {}.", this.queueName, name);
        this.queueName = name;
    }

    /**
     * Reschedule a job.
     */
    protected abstract JobEvent reschedule(final JobEvent info);

    /**
     * @see org.apache.sling.event.jobs.Queue#getStatistics()
     */
    public Statistics getStatistics() {
        return this;
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#resume()
     */
    public void resume() {
        if ( this.isSuspended() ) {
            synchronized ( this.suspendLock ) {
                this.suspendLock.notify();
            }
        }
        this.suspendedSince.set(-1);
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#suspend()
     */
    public void suspend() {
        this.suspendedSince.compareAndSet(-1, System.currentTimeMillis());
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#isSuspended()
     */
    public boolean isSuspended() {
        return this.suspendedSince.longValue() != -1;
    }


    /**
     * @see org.apache.sling.event.jobs.Queue#removeAll()
     */
    public synchronized void removeAll() {
        // we suspend the queue
        final boolean wasSuspended = this.isSuspended();
        this.suspend();
        // we copy all events and remove them in the background
        final Collection<JobEvent> events = this.removeAllJobs();
        this.clearQueued();
        final Thread t = new Thread(new Runnable() {

                /**
                 * @see java.lang.Runnable#run()
                 */
                public void run() {
                    for(final JobEvent job : events) {
                        job.remove();
                    }
                }
            }, "Queue RemoveAll Thread for " + this.queueName);
        t.setDaemon(true);
        t.start();
        // start queue again
        if ( !wasSuspended ) {
            this.resume();
        }
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#clear()
     */
    public void clear() {
        this.clearQueued();
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#getState(java.lang.String)
     */
    public Object getState(final String key) {
        // not supported for now
        return null;
    }

    /**
     * Put another job into the queue.
     */
    protected abstract void put(final JobEvent event);

    /**
     * Get another job from the queue.
     */
    protected abstract JobEvent take();

    /**
     * Is the queue empty?
     */
    protected abstract boolean isEmpty();

    /**
     * Remove all events from the queue and return them.
     */
    protected abstract Collection<JobEvent> removeAllJobs();

    protected abstract JobEvent start(final JobEvent event);

    protected abstract void notifyFinished(final JobEvent rescheduleInfo);
}

