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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.jobs.JobConsumerManager;
import org.apache.sling.event.impl.jobs.JobHandler;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.Utility;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.deprecated.JobStatusNotifier;
import org.apache.sling.event.impl.jobs.stats.StatisticsImpl;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.Statistics;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.event.jobs.consumer.JobConsumer.JobResult;
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

    /** The event admin. */
    private final EventAdmin eventAdmin;

    /** The job consumer manager. */
    private final JobConsumerManager jobConsumerManager;

    /** The queue name. */
    protected volatile String queueName;

    /** Are we still running? */
    protected volatile boolean running;

    /** Are we marked for removal */
    private volatile boolean markedForRemoval = false;

    /** Is the queue currently waiting(sleeping) */
    protected volatile boolean isWaiting = false;

    /** The map of events we're have started (send). */
    private final Map<String, JobHandler> startedJobsLists = new HashMap<String, JobHandler>();

    /** The map of events we're processing. */
    private final Map<String, JobHandler> processsingJobsLists = new HashMap<String, JobHandler>();

    /** Suspended since. */
    private volatile long suspendedSince = -1L;

    /** Suspend lock. */
    private final Object suspendLock = new Object();

    /** Async counter. */
    private final AtomicInteger asyncCounter = new AtomicInteger();

    /**
     * Start this queue
     * @param name The queue name
     * @param config The queue configuration
     * @param environment The environment component
     */
    public AbstractJobQueue(final String name,
                    final InternalQueueConfiguration config,
                    final JobConsumerManager jobConsumerManager,
                    final EventAdmin eventAdmin) {
        this.queueName = name;
        this.configuration = config;
        this.logger = LoggerFactory.getLogger(this.getClass().getName() + '.' + name);
        this.running = true;
        this.eventAdmin = eventAdmin;
        this.jobConsumerManager = jobConsumerManager;
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#getStateInfo()
     */
    @Override
    public String getStateInfo() {
        synchronized ( this.suspendLock ) {
            return "isWaiting=" + this.isWaiting +
                    ", markedForRemoval=" + this.markedForRemoval +
                    ", suspendedSince=" + this.suspendedSince +
                    ", asyncJobs=" + this.asyncCounter.get();
        }
    }

    /**
     * Start the job queue.
     */
    public void start() {
        final Thread queueThread = new Thread(new Runnable() {

            @Override
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
    @Override
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
        this.put(new JobHandler(null, null));

        synchronized ( this.processsingJobsLists ) {
            this.processsingJobsLists.clear();
        }
        synchronized ( this.startedJobsLists ) {
            this.startedJobsLists.clear();
        }
        this.logger.info("Stopped job queue {}", this.queueName);
    }

    /**
     * Periodically check for started jobs without an acknowledge.
     */
    public void checkForUnprocessedJobs() {
        if ( this.running ) {
            // check for jobs that were started but never got an acknowledge
            final long tooOld = System.currentTimeMillis() - DEFAULT_WAIT_FOR_ACK_IN_MS;
            // to keep the synchronized block as fast as possible we just store the
            // jobs to be removed in a new list and process this list afterwards
            final List<JobHandler> restartJobs = new ArrayList<JobHandler>();
            synchronized ( this.startedJobsLists ) {
                final Iterator<Map.Entry<String, JobHandler>> i = this.startedJobsLists.entrySet().iterator();
                while ( i.hasNext() ) {
                    final Map.Entry<String, JobHandler> entry = i.next();
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
            final Iterator<JobHandler> jobIter = restartJobs.iterator();
            while ( jobIter.hasNext() ) {
                final JobHandler info = jobIter.next();
                boolean process = false;
                synchronized ( this.startedJobsLists ) {
                    process = this.startedJobsLists.remove(info.getJob().getId()) != null;
                }
                if ( process ) {
                    if ( !info.reschedule() ) {
                        this.decQueued();
                        checkForNotify(null);
                    } else {
                        this.logger.info("No acknowledge received for job {} stored at {}. Requeueing job.", Utility.toString(info.getJob()), info.getJob().getId());
                        checkForNotify(info);
                    }
                }
            }
        }
    }

    /**
     * @see org.apache.sling.event.impl.jobs.deprecated.JobStatusNotifier#sendAcknowledge(org.osgi.service.event.Event)
     */
    @Override
    public boolean sendAcknowledge(final Event job) {
        final String jobId = (String)job.getProperty(JobUtil.JOB_ID);
        final JobHandler ack;
        synchronized ( this.startedJobsLists ) {
            ack = this.startedJobsLists.remove(jobId);
        }
        // if the event is still in the processing list, we confirm the ack
        if ( ack != null ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Received ack for job {}", Utility.toString(ack.getJob()));
            }
            final long queueTime = ack.started - ack.queued;
            this.addActive(queueTime);
            Utility.sendNotification(this.eventAdmin, JobUtil.TOPIC_JOB_STARTED, ack.getJob(), queueTime);
            synchronized ( this.processsingJobsLists ) {
                this.processsingJobsLists.put(jobId, ack);
            }
        } else {
            this.decQueued();
        }
        return ack != null;
    }

    private boolean handleReschedule(final JobHandler jobEvent, final JobConsumer.JobResult result) {
        boolean reschedule = false;
        switch ( result ) {
            case OK : // job is finished
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Finished job {}", Utility.toString(jobEvent.getJob()));
                }
                final long processingTime = System.currentTimeMillis() - jobEvent.started;
                this.finishedJob(processingTime);
                Utility.sendNotification(this.eventAdmin, JobUtil.TOPIC_JOB_FINISHED, jobEvent.getJob(), processingTime);
                break;
            case FAILED : // check if we exceeded the number of retries
                int retries = (Integer) jobEvent.getJob().getProperty(Job.PROPERTY_JOB_RETRIES);
                int retryCount = (Integer)jobEvent.getJob().getProperty(Job.PROPERTY_JOB_RETRY_COUNT);

                retryCount++;
                if ( retries != -1 && retryCount > retries ) {
                    reschedule = false;
                    if ( this.logger.isDebugEnabled() ) {
                        this.logger.debug("Cancelled job {}", Utility.toString(jobEvent.getJob()));
                    }
                    this.cancelledJob();
                    Utility.sendNotification(this.eventAdmin, JobUtil.TOPIC_JOB_CANCELLED, jobEvent.getJob(), null);
                } else {
                    reschedule = true;
                    // update event with retry count and retries
                    jobEvent.getJob().retry();
                    if ( this.logger.isDebugEnabled() ) {
                        this.logger.debug("Failed job {}", Utility.toString(jobEvent.getJob()));
                    }
                    this.failedJob();
                    jobEvent.queued = System.currentTimeMillis();
                    Utility.sendNotification(this.eventAdmin, JobUtil.TOPIC_JOB_FAILED, jobEvent.getJob(), null);
                }
                break;
            case CANCEL : // consumer cancelled the job
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Cancelled job {}", Utility.toString(jobEvent.getJob()));
                }
                this.cancelledJob();
                Utility.sendNotification(this.eventAdmin, JobUtil.TOPIC_JOB_CANCELLED, jobEvent.getJob(), null);
                break;
            case ASYNC: // nothing to do here
                break;
        }

        return reschedule;
    }

    /**
     * @see org.apache.sling.event.impl.jobs.deprecated.JobStatusNotifier#finishedJob(org.osgi.service.event.Event, boolean)
     */
    @Override
    public boolean finishedJob(final Event job, final boolean shouldReschedule) {
        final String location = (String)job.getProperty(JobUtil.JOB_ID);
        return this.finishedJob(location, shouldReschedule ? JobResult.FAILED : JobResult.OK, false);
    }

    private boolean finishedJob(final String jobId,
                                final JobConsumer.JobResult result,
                                final boolean isAsync) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Received finish for job {}, result={}", jobId, result);
        }
        // let's remove the event from our processing list
        // this is just a sanity check, as usually the job should have been
        // removed during sendAcknowledge.
        synchronized ( this.startedJobsLists ) {
            this.startedJobsLists.remove(jobId);
        }

        // get job handler
        final JobHandler info;
        synchronized ( this.processsingJobsLists ) {
            info = this.processsingJobsLists.remove(jobId);
        }

        if ( !this.running ) {
            this.logger.warn("Queue is not running anymore. Discarding finish for {}", jobId);
            return false;
        }

        if ( info == null ) {
            if ( this.logger.isDebugEnabled() ) {
                this.logger.debug("This job has never been started by this queue: {}", jobId);
            }
            return false;
        }

        // handle the reschedule, a new job might be returned with updated reschedule info!
        final boolean reschedule = this.handleReschedule(info, result);

        // if this is set after the synchronized block we have an error
        final boolean finishSuccessful;

        if ( !reschedule ) {
            info.finished();
            finishSuccessful = true;
        } else {
            finishSuccessful = info.reschedule();
        }

        if ( !isAsync ) {
            if ( !finishSuccessful || !reschedule ) {
                checkForNotify(null);
                return false;
            }
            checkForNotify(info);
            return true;
        } else {
            // async result
            if ( finishSuccessful && reschedule ) {
                final JobHandler reprocessHandler = this.reschedule(info);
                if ( reprocessHandler != null ) {
                    this.put(reprocessHandler);
                }
            }
            return true;
        }
    }

    private void checkForNotify(final JobHandler info) {
        JobHandler reprocessInfo = null;
        if ( info != null ) {
            reprocessInfo = this.reschedule(info);
        }
        notifyFinished(reprocessInfo);
    }

    protected boolean canBeMarkedForRemoval() {
        return this.isEmpty() && !this.isWaiting && !this.isSuspended() && this.asyncCounter.get() == 0;
    }

    /**
     * Mark this queue for removal.
     */
    public void markForRemoval() {
        if ( this.canBeMarkedForRemoval() ) {
            this.markedForRemoval = true;
        }
    }

    /**
     * Check if this queue is marked for removal
     */
    public boolean isMarkedForRemoval() {
        if ( this.markedForRemoval ) {
            if ( this.canBeMarkedForRemoval() ) {
                return true;
            }
            this.markedForRemoval = false;
        }
        return false;
    }

    /**
     * Get the name of the job queue.
     */
    @Override
    public String getName() {
        return this.queueName;
    }


    /**
     * Add a new job to the queue.
     */
    public void process(final JobHandler event) {
        this.put(event);
        event.queued = System.currentTimeMillis();
        this.incQueued();
    }

    /**
     * Check if the queue is suspended and go into suspend mode
     */
    private void checkSuspended() {
        synchronized ( this.suspendLock ) {
            while ( this.suspendedSince != -1 ) {
                try {
                    this.suspendLock.wait(MAX_SUSPEND_TIME);
                } catch (final InterruptedException ignore) {
                    this.ignoreException(ignore);
                }
                if ( System.currentTimeMillis() > this.suspendedSince + MAX_SUSPEND_TIME ) {
                    this.resume();
                }
            }
        }
    }

    /**
     * Execute the queue
     */
    private void runJobQueue() {
        JobHandler info = null;
        while ( this.running ) {
            if ( info == null ) {
                // so let's wait/get the next job from the queue
                info = this.take();
            }

            if ( this.running ) {
                checkSuspended();
            }
            if ( info != null && this.running ) {
                info = this.start(info);
            }
        }
    }

    /**
     * Execute a job
     */
    protected boolean executeJob(final JobHandler handler) {
        final JobImpl job = handler.getJob();
        final JobConsumer consumer = this.jobConsumerManager.getConsumer(job.getTopic());

        if ( (consumer != null || (job.isBridgedEvent() && this.jobConsumerManager.supportsBridgedEvents())) ) {
            if ( handler.start() ) {
                if ( logger.isDebugEnabled() ) {
                    logger.debug("Starting job {}", Utility.toString(job));
                }
                try {
                    handler.started = System.currentTimeMillis();

                    if ( consumer != null ) {
                        final long queueTime = handler.started - handler.queued;
                        this.addActive(queueTime);
                        Utility.sendNotification(this.eventAdmin, JobUtil.TOPIC_JOB_STARTED, job, queueTime);
                        synchronized ( this.processsingJobsLists ) {
                            this.processsingJobsLists.put(job.getId(), handler);
                        }

                        final Runnable task = new Runnable() {

                            /**
                             * @see java.lang.Runnable#run()
                             */
                            @Override
                            public void run() {
                                final Thread currentThread = Thread.currentThread();
                                // update priority and name
                                final String oldName = currentThread.getName();
                                final int oldPriority = currentThread.getPriority();

                                currentThread.setName(oldName + "-" + job.getQueueName() + "(" + job.getTopic() + ")");
                                if ( job.getJobPriority() != null ) {
                                    switch ( job.getJobPriority() ) {
                                        case NORM : currentThread.setPriority(Thread.NORM_PRIORITY);
                                                    break;
                                        case MIN  : currentThread.setPriority(Thread.MIN_PRIORITY);
                                                    break;
                                        case MAX  : currentThread.setPriority(Thread.MAX_PRIORITY);
                                                    break;
                                    }
                                }
                                JobConsumer.JobResult result = JobConsumer.JobResult.CANCEL;
                                final JobConsumer.AsyncHandler asyncHandler =
                                        new JobConsumer.AsyncHandler() {

                                            final Object asyncLock = new Object();
                                            final AtomicBoolean asyncDone = new AtomicBoolean(false);

                                            private void check(final JobConsumer.JobResult result) {
                                                synchronized ( asyncLock ) {
                                                    if ( !asyncDone.get() ) {
                                                        asyncDone.set(true);
                                                        finishedJob(job.getId(), result, true);
                                                        asyncCounter.decrementAndGet();
                                                    } else {
                                                        throw new IllegalStateException("Job is already marked as processed");
                                                    }
                                                }
                                            }

                                            @Override
                                            public void ok() {
                                                this.check(JobConsumer.JobResult.OK);
                                            }

                                            @Override
                                            public void failed() {
                                                this.check(JobConsumer.JobResult.FAILED);
                                            }

                                            @Override
                                            public void cancel() {
                                                this.check(JobConsumer.JobResult.CANCEL);
                                            }
                                        };
                                job.setProperty(JobConsumer.PROPERTY_JOB_ASYNC_HANDLER, asyncHandler);
                                try {
                                    result = consumer.process(job);
                                } catch (final Throwable t) { //NOSONAR
                                    logger.error("Unhandled error occured in job processor " + t.getMessage() + " while processing job " + Utility.toString(job), t);
                                    // we don't reschedule if an exception occurs
                                    result = JobConsumer.JobResult.CANCEL;
                                } finally {
                                    currentThread.setPriority(oldPriority);
                                    currentThread.setName(oldName);
                                    if ( result != JobConsumer.JobResult.ASYNC ) {
                                        finishedJob(job.getId(), result, false);
                                    }
                                }
                                if ( result == JobConsumer.JobResult.ASYNC ) {
                                    asyncCounter.incrementAndGet();
                                    notifyFinished(null);
                                }
                            }

                        };
                        // check if the thread pool is available
                        final ThreadPool pool = Environment.THREAD_POOL;
                        if ( pool != null ) {
                            pool.execute(task);
                        } else {
                            // if we don't have a thread pool, we create the thread directly
                            // (this should never happen for jobs, but is a safe fallback and
                            // allows to call this method for other background processing.
                            new Thread(task).start();
                        }

                    } else {
                        // let's add the event to our processing list
                        synchronized ( this.startedJobsLists ) {
                            this.startedJobsLists.put(job.getId(), handler);
                        }
                        final Event jobEvent = this.getJobEvent(handler);
                        // we need async delivery, otherwise we might create a deadlock
                        // as this method runs inside a synchronized block and the finishedJob
                        // method as well!
                        this.eventAdmin.postEvent(jobEvent);
                    }
                    return true;

                } catch (final Exception re) {
                    // if an exception occurs, we just log
                    this.logger.error("Exception during job processing.", re);
                }
            } else {
                if ( logger.isDebugEnabled() ) {
                    logger.debug("Discarding removed job {}", Utility.toString(job));
                }
            }
        } else {
            handler.reassign();
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
    private Event getJobEvent(final JobHandler info) {
        final String eventTopic = info.getJob().getTopic();
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        for(final String name : info.getJob().getPropertyNames()) {
            properties.put(name, info.getJob().getProperty(name));
        }

        // put properties for finished job callback
        properties.put(JobStatusNotifier.CONTEXT_PROPERTY_NAME, new JobStatusNotifier.NotifierContext(this));

        // remove app id and distributable flag
        properties.remove(EventUtil.PROPERTY_DISTRIBUTE);
        properties.remove(EventUtil.PROPERTY_APPLICATION);

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
     * @see org.apache.sling.event.jobs.Queue#getStatistics()
     */
    @Override
    public Statistics getStatistics() {
        return this;
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#resume()
     */
    @Override
    public void resume() {
        synchronized ( this.suspendLock ) {
            if ( this.suspendedSince != -1 ) {
                this.suspendedSince = -1;
                this.suspendLock.notify();
            }
        }
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#suspend()
     */
    @Override
    public void suspend() {
        synchronized ( this.suspendLock ) {
            if ( this.suspendedSince == -1 ) {
                this.suspendedSince = System.currentTimeMillis();
            }
        }
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#isSuspended()
     */
    @Override
    public boolean isSuspended() {
        synchronized ( this.suspendLock ) {
            return this.suspendedSince != -1;
        }
    }


    /**
     * @see org.apache.sling.event.jobs.Queue#removeAll()
     */
    @Override
    public synchronized void removeAll() {
        // we suspend the queue
        final boolean wasSuspended = this.isSuspended();
        this.suspend();
        // we copy all events and remove them in the background
        final Collection<JobHandler> events = this.removeAllJobs();
        this.clearQueued();
        final Thread t = new Thread(new Runnable() {

                /**
                 * @see java.lang.Runnable#run()
                 */
                @Override
                public void run() {
                    for(final JobHandler job : events) {
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
    @Override
    public void clear() {
        this.clearQueued();
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#getState(java.lang.String)
     */
    @Override
    public Object getState(final String key) {
        // not supported for now
        return null;
    }

    /**
     * Reschedule a job.
     */
    protected abstract JobHandler reschedule(final JobHandler info);

    /**
     * Put another job into the queue.
     */
    protected abstract void put(final JobHandler event);

    /**
     * Get another job from the queue.
     */
    protected abstract JobHandler take();

    /**
     * Is the queue empty?
     */
    protected abstract boolean isEmpty();

    /**
     * Remove all events from the queue and return them.
     */
    protected abstract Collection<JobHandler> removeAllJobs();

    protected abstract JobHandler start(final JobHandler event);

    protected abstract void notifyFinished(final JobHandler rescheduleInfo);
}

