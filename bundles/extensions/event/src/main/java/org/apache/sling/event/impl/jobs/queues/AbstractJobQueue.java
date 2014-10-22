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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.EventingThreadPool;
import org.apache.sling.event.impl.jobs.InternalJobState;
import org.apache.sling.event.impl.jobs.JobExecutionResultImpl;
import org.apache.sling.event.impl.jobs.JobHandler;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.JobTopicTraverser;
import org.apache.sling.event.impl.jobs.Utility;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.deprecated.JobStatusNotifier;
import org.apache.sling.event.impl.jobs.notifications.NotificationUtility;
import org.apache.sling.event.impl.support.BatchResourceRemover;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.Statistics;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The job blocking queue extends the blocking queue by some
 * functionality for the job event handling.
 */
public abstract class AbstractJobQueue
    implements Queue, JobStatusNotifier {

    /** Default timeout for suspend. */
    private static final long MAX_SUSPEND_TIME = 1000 * 60 * 60; // 60 mins

    /** Default number of seconds to wait for an ack. */
    private static final long DEFAULT_WAIT_FOR_ACK_IN_MS = 60 * 1000; // by default we wait 60 secs

    /** The logger. */
    protected final Logger logger;

    /** Configuration. */
    protected final InternalQueueConfiguration configuration;

    /** The queue name. */
    protected volatile String queueName;

    /** Are we still running? */
    protected volatile boolean running;

    /** Suspended since. */
    private volatile long suspendedSince = -1L;

    /** Suspend lock. */
    private final Object suspendLock = new Object();

    /** Services used by the queues. */
    protected final QueueServices services;

    /** The map of events we're processing. */
    private final Map<String, JobHandler> processingJobsLists = new HashMap<String, JobHandler>();

    private final ThreadPool threadPool;

    /** The map of events we're have started (send). */
    private final Map<String, JobHandler> startedJobsLists = new HashMap<String, JobHandler>();

    /** Async counter. */
    private final AtomicInteger asyncCounter = new AtomicInteger();

    /** Is the queue currently waiting(sleeping) */
    protected volatile boolean isWaiting = false;

    /** Flag for outdated. */
    private final AtomicBoolean isOutdated = new AtomicBoolean(false);

    /** A marker for closing the queue. */
    private final AtomicBoolean closeMarker = new AtomicBoolean(false);

    /** The job cache. */
    private final QueueJobCache cache;

    /** Flag to mark whether the queue os waiting for the next job. */
    private volatile boolean isWaitingForNextJob = false;

    /** Sync object for {@link #isWaitingForNextJob}. */
    private final Object nextJobLock = new Object();

    /**
     * Create a new queue
     * @param name The queue name
     * @param config The queue configuration
     */
    public AbstractJobQueue(final String name,
                            final InternalQueueConfiguration config,
                            final QueueServices services,
                            final Set<String> topics) {
        if ( config.getOwnThreadPoolSize() > 0 ) {
            this.threadPool = new EventingThreadPool(services.threadPoolManager, config.getOwnThreadPoolSize());
        } else {
            this.threadPool = Environment.THREAD_POOL;
        }
        this.queueName = name;
        this.configuration = config;
        this.services = services;
        this.logger = LoggerFactory.getLogger(this.getClass().getName() + '.' + name);
        this.running = true;
        this.cache = new QueueJobCache(services.configuration, config.getType(), topics);
    }

    /**
     * Return the queue configuration
     */
    @Override
    public InternalQueueConfiguration getConfiguration() {
        return this.configuration;
    }

    /**
     * Get the name of the job queue.
     */
    @Override
    public String getName() {
        return this.queueName;
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#getStatistics()
     */
    @Override
    public Statistics getStatistics() {
        return this.services.statisticsManager.getQueueStatistics(this.queueName);
    }

    /**
     * Start the job queue.
     */
    public void start() {
        final Thread queueThread = new Thread(new Runnable() {

            @Override
            public void run() {
                while ( running && !isOutdated()) {
                    logger.info("Starting job queue {}", queueName);
                    logger.debug("Configuration for job queue={}", configuration);

                    try {
                        runJobQueue();
                    } catch (final Throwable t) { //NOSONAR
                        logger.error("Job queue " + queueName + " stopped with exception: " + t.getMessage() + ". Restarting.", t);
                    }
                }
            }

        }, "Apache Sling Job Queue " + queueName);
        queueThread.setDaemon(true);
        queueThread.start();
    }

    /**
     * Is the queue outdated?
     */
    protected boolean isOutdated() {
        return this.isOutdated.get();
    }

    /**
     * Outdate this queue.
     */
    public void outdate() {
        if ( this.isOutdated.compareAndSet(false, true) ) {
            final String name = this.getName() + "<outdated>(" + this.hashCode() + ")";
            this.logger.info("Outdating queue {}, renaming to {}.", this.queueName, name);
            this.queueName = name;
        }
    }

    /**
     * Check if the queue can be closed
     */
    public boolean tryToClose() {
        // resume the queue as we want to close it!
        this.resume();
        // check if possible
        if ( this.canBeClosed() ) {
            if ( this.closeMarker.get() ) {
                this.close();
                return true;
            }
            this.closeMarker.set(true);
        }
        return false;
    }

    /**
     * Check whether this queue can be closed
     */
    protected boolean canBeClosed() {
        return !this.isWaiting && !this.isSuspended() && this.cache.isEmpty() && this.asyncCounter.get() == 0;
    }

    /**
     * Close this queue.
     */
    public void close() {
        this.running = false;
        this.logger.debug("Shutting down job queue {}", queueName);
        this.resume();
        if ( this.isWaiting ) {
            this.logger.debug("Waking up waiting queue {}", this.queueName);
            this.notifyFinished(false);
        }
        // stop the queue
        this.stopWaitingForNextJob();

        synchronized ( this.processingJobsLists ) {
            this.processingJobsLists.clear();
        }
        synchronized ( this.startedJobsLists ) {
            this.startedJobsLists.clear();
        }
        if ( this.configuration.getOwnThreadPoolSize() > 0 ) {
            ((EventingThreadPool)this.threadPool).release();
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
                } catch (final InterruptedException e) {
                    this.ignoreException(e);
                    Thread.currentThread().interrupt();
                }
            }
            final Iterator<JobHandler> jobIter = restartJobs.iterator();
            while ( jobIter.hasNext() ) {
                final JobHandler handler = jobIter.next();
                boolean process = false;
                synchronized ( this.startedJobsLists ) {
                    process = this.startedJobsLists.remove(handler.getJob().getId()) != null;
                }
                if ( process ) {
                    if ( handler.reschedule() ) {
                        this.logger.info("No acknowledge received for job {} stored at {}. Requeueing job.", Utility.toString(handler.getJob()), handler.getJob().getId());
                        handler.getJob().retry();
                        this.requeue(handler);
                        this.notifyFinished(true);
                    }
                }
            }
        }
    }

    /**
     * Execute the queue
     */
    private void runJobQueue() {
        while ( this.running && !this.isOutdated()) {
            JobHandler info = null;
            if ( info == null ) {
                // so let's wait/get the next job from the queue
                info = this.take();
            }

            // if we're suspended we drop the current item
            if ( this.running && info != null && !this.isOutdated() && !checkSuspended(info) ) {
                // if we still have a job and are running, let's go
                this.start(info);
            }
        }
    }

    /**
     * Take a new job for this queue.
     * This method blocks until a job is available or the queue is stopped.
     * @param queueName The queue name
     * @return A new job or {@code null} if {@link #stop(String)} is called.
     */
    private JobHandler take() {
        logger.debug("Taking new job for {}", queueName);
        JobImpl result = null;

        boolean doFull = false;

        while ( result == null && !this.isOutdated() && this.running ) {
            this.isWaitingForNextJob = true;

            result = this.cache.getNextJob(doFull);
            if ( result == null && !this.isOutdated() && this.running ) {
                // block
                synchronized ( nextJobLock ) {
                    while ( isWaitingForNextJob ) {
                        try {
                            nextJobLock.wait(20000);
                            isWaitingForNextJob = false;
                            doFull = true;
                        } catch ( final InterruptedException ignore ) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
            this.isWaitingForNextJob = false;
        }

        if ( logger.isDebugEnabled() ) {
            logger.debug("Returning job for {} : {}", queueName, Utility.toString(result));
        }
        return (result != null ? new JobHandler( result, this.services.configuration) : null);
    }

    /**
     * Stop waiting for a job
     * @param queueName The queue name.
     */
    private void stopWaitingForNextJob() {
        synchronized ( nextJobLock ) {
            this.isWaitingForNextJob = false;
            nextJobLock.notify();
        }
    }

    /**
     * Inform the queue about a job for the topic
     * @param topic A new topic.
     */
    public void wakeUpQueue(final Set<String> topics) {
        this.cache.handleNewTopics(topics);
        this.stopWaitingForNextJob();
    }

    /**
     * Put a job back in the queue
     * @param handler The job handler
     */
    private void requeue(final JobHandler handler) {
        this.cache.reschedule(handler);
        synchronized ( this.nextJobLock ) {
            this.nextJobLock.notify();
        }
    }

    /**
     * Check if the queue is suspended and go into suspend mode
     */
    private boolean checkSuspended(final JobHandler handler) {
        boolean wasSuspended = false;
        synchronized ( this.suspendLock ) {
            while ( this.suspendedSince != -1 ) {
                this.requeue(handler);
                logger.debug("Sleeping as queue {} is suspended.", this.getName());
                wasSuspended = true;
                final long diff = System.currentTimeMillis() - this.suspendedSince;
                try {
                    this.suspendLock.wait(MAX_SUSPEND_TIME - diff);
                } catch (final InterruptedException ignore) {
                    this.ignoreException(ignore);
                    Thread.currentThread().interrupt();
                }
                logger.debug("Waking up queue {}.", this.getName());
                if ( System.currentTimeMillis() > this.suspendedSince + MAX_SUSPEND_TIME ) {
                    this.resume();
                }
            }
        }
        return wasSuspended;
    }

    /**
     * Execute a job
     */
    protected boolean executeJob(final JobHandler handler) {
        final JobImpl job = handler.getJob();
        final JobExecutor consumer = this.services.jobConsumerManager.getExecutor(job.getTopic());

        if ( (consumer != null || (job.isBridgedEvent() && this.services.jobConsumerManager.supportsBridgedEvents())) ) {
            final boolean success = this.startJobExecution(handler, consumer);
            return success;
        } else {
            // no consumer on this instance, assign to another instance
            handler.reassign();
            return false;
        }
    }

    private boolean startJobExecution(final JobHandler handler, final JobExecutor consumer) {
        this.closeMarker.set(false);
        final JobImpl job = handler.getJob();
        if ( handler.startProcessing(this) ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Starting job {}", Utility.toString(job));
            }
            try {
                handler.started = System.currentTimeMillis();

                if ( consumer != null ) {
                    final long queueTime = handler.started - handler.queued;
                    NotificationUtility.sendNotification(this.services.eventAdmin, NotificationConstants.TOPIC_JOB_STARTED, job, queueTime);
                    synchronized ( this.processingJobsLists ) {
                        this.processingJobsLists.put(job.getId(), handler);
                    }

                    final Runnable task = new Runnable() {

                        /**
                         * @see java.lang.Runnable#run()
                         */
                        @Override
                        public void run() {
                            final Object lock = new Object();
                            final Thread currentThread = Thread.currentThread();
                            // update priority and name
                            final String oldName = currentThread.getName();
                            final int oldPriority = currentThread.getPriority();

                            currentThread.setName(oldName + "-" + job.getQueueName() + "(" + job.getTopic() + ")");
                            if ( configuration.getThreadPriority() != null ) {
                                switch ( configuration.getThreadPriority() ) {
                                    case NORM : currentThread.setPriority(Thread.NORM_PRIORITY);
                                                break;
                                    case MIN  : currentThread.setPriority(Thread.MIN_PRIORITY);
                                                break;
                                    case MAX  : currentThread.setPriority(Thread.MAX_PRIORITY);
                                                break;
                                }
                            }
                            JobExecutionResultImpl result = JobExecutionResultImpl.CANCELLED;
                            Job.JobState resultState = Job.JobState.ERROR;
                            final AtomicBoolean isAsync = new AtomicBoolean(false);

                            try {
                                synchronized ( lock ) {
                                    final JobExecutionContext ctx = new JobExecutionContext() {

                                        private boolean hasInit = false;

                                        @Override
                                        public void initProgress(final int steps,
                                                final long eta) {
                                            if ( !hasInit ) {
                                                handler.persistJobProperties(job.startProgress(steps, eta));
                                                hasInit = true;
                                            }
                                        }

                                        @Override
                                        public void incrementProgressCount(final int steps) {
                                            if ( hasInit ) {
                                                handler.persistJobProperties(job.setProgress(steps));
                                            }
                                        }

                                        @Override
                                        public void updateProgress(final long eta) {
                                            if ( hasInit ) {
                                                handler.persistJobProperties(job.update(eta));
                                            }
                                        }

                                        @Override
                                        public void log(final String message, Object... args) {
                                            handler.persistJobProperties(job.log(message, args));
                                        }

                                        @Override
                                        public boolean isStopped() {
                                            return handler.isStopped();
                                        }

                                        @Override
                                        public void asyncProcessingFinished(final JobExecutionResult result) {
                                            synchronized ( lock ) {
                                                if ( isAsync.compareAndSet(true, false) ) {
                                                    services.jobConsumerManager.unregisterListener(job.getId());
                                                    Job.JobState state = null;
                                                    if ( result.succeeded() ) {
                                                        state = Job.JobState.SUCCEEDED;
                                                    } else if ( result.failed() ) {
                                                        state = Job.JobState.QUEUED;
                                                    } else if ( result.cancelled() ) {
                                                        if ( handler.isStopped() ) {
                                                            state = Job.JobState.STOPPED;
                                                        } else {
                                                            state = Job.JobState.ERROR;
                                                        }
                                                    }
                                                    finishedJob(job.getId(), state, true);
                                                    asyncCounter.decrementAndGet();
                                                } else {
                                                    throw new IllegalStateException("Job is not processed async " + job.getId());
                                                }
                                            }
                                        }

                                        @Override
                                        public ResultBuilder result() {
                                            return new ResultBuilder() {

                                                private String message;

                                                private Long retryDelayInMs;

                                                @Override
                                                public JobExecutionResult failed(final long retryDelayInMs) {
                                                    this.retryDelayInMs = retryDelayInMs;
                                                    return new JobExecutionResultImpl(InternalJobState.FAILED, message, retryDelayInMs);
                                                }

                                                @Override
                                                public ResultBuilder message(final String message) {
                                                    this.message = message;
                                                    return this;
                                                }

                                                @Override
                                                public JobExecutionResult succeeded() {
                                                    return new JobExecutionResultImpl(InternalJobState.SUCCEEDED, message, retryDelayInMs);
                                                }

                                                @Override
                                                public JobExecutionResult failed() {
                                                    return new JobExecutionResultImpl(InternalJobState.FAILED, message, retryDelayInMs);
                                                }

                                                @Override
                                                public JobExecutionResult cancelled() {
                                                    return new JobExecutionResultImpl(InternalJobState.CANCELLED, message, retryDelayInMs);
                                                }
                                            };
                                        }
                                    };
                                    result = (JobExecutionResultImpl)consumer.process(job, ctx);
                                    if ( result == null ) { // ASYNC processing
                                        services.jobConsumerManager.registerListener(job.getId(), consumer, ctx);
                                        asyncCounter.incrementAndGet();
                                        isAsync.set(true);
                                    } else {
                                        if ( result.succeeded() ) {
                                            resultState = Job.JobState.SUCCEEDED;
                                        } else if ( result.failed() ) {
                                            resultState = Job.JobState.QUEUED;
                                        } else if ( result.cancelled() ) {
                                            if ( handler.isStopped() ) {
                                                resultState = Job.JobState.STOPPED;
                                            } else {
                                                resultState = Job.JobState.ERROR;
                                            }
                                        }
                                    }
                                }
                            } catch (final Throwable t) { //NOSONAR
                                logger.error("Unhandled error occured in job processor " + t.getMessage() + " while processing job " + Utility.toString(job), t);
                                // we don't reschedule if an exception occurs
                                result = JobExecutionResultImpl.CANCELLED;
                                resultState = Job.JobState.ERROR;
                            } finally {
                                currentThread.setPriority(oldPriority);
                                currentThread.setName(oldName);
                                if ( result != null ) {
                                    if ( result.getRetryDelayInMs() != null ) {
                                        job.setProperty(JobImpl.PROPERTY_DELAY_OVERRIDE, result.getRetryDelayInMs());
                                    }
                                    if ( result.getMessage() != null ) {
                                       job.setProperty(Job.PROPERTY_RESULT_MESSAGE, result.getMessage());
                                    }
                                    finishedJob(job.getId(), resultState, false);
                                }
                            }
                        }

                    };
                    // check if the thread pool is available
                    final ThreadPool pool = this.threadPool;
                    if ( pool != null ) {
                        pool.execute(task);
                    } else {
                        // if we don't have a thread pool, we create the thread directly
                        // (this should never happen for jobs, but is a safe fall back)
                        new Thread(task).start();
                    }

                } else {
                    // let's add the event to our started jobs list
                    synchronized ( this.startedJobsLists ) {
                        this.startedJobsLists.put(job.getId(), handler);
                    }
                    final Event jobEvent = this.getJobEvent(handler);
                    // we need async delivery, otherwise we might create a deadlock
                    // as this method runs inside a synchronized block and the finishedJob
                    // method as well!
                    this.services.eventAdmin.postEvent(jobEvent);
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
        return false;
    }

    private static final class RescheduleInfo {
        public boolean reschedule = false;
        public long    processingTime;
    }

    private RescheduleInfo handleReschedule(final JobHandler handler, final Job.JobState resultState) {
        final RescheduleInfo info = new RescheduleInfo();
        switch ( resultState ) {
            case SUCCEEDED : // job is finished
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Finished job {}", Utility.toString(handler.getJob()));
                }
                info.processingTime = System.currentTimeMillis() - handler.started;
                NotificationUtility.sendNotification(this.services.eventAdmin, NotificationConstants.TOPIC_JOB_FINISHED, handler.getJob(), info.processingTime);
                break;
            case QUEUED : // check if we exceeded the number of retries
                final int retries = (Integer) handler.getJob().getProperty(Job.PROPERTY_JOB_RETRIES);
                int retryCount = (Integer)handler.getJob().getProperty(Job.PROPERTY_JOB_RETRY_COUNT);

                retryCount++;
                if ( retries != -1 && retryCount > retries ) {
                    if ( this.logger.isDebugEnabled() ) {
                        this.logger.debug("Cancelled job {}", Utility.toString(handler.getJob()));
                    }
                    NotificationUtility.sendNotification(this.services.eventAdmin, NotificationConstants.TOPIC_JOB_CANCELLED, handler.getJob(), null);
                } else {
                    info.reschedule = true;
                    handler.getJob().retry();
                    if ( this.logger.isDebugEnabled() ) {
                        this.logger.debug("Failed job {}", Utility.toString(handler.getJob()));
                    }
                    handler.queued = System.currentTimeMillis();
                    NotificationUtility.sendNotification(this.services.eventAdmin, NotificationConstants.TOPIC_JOB_FAILED, handler.getJob(), null);
                }
                break;
            default : // consumer cancelled the job (STOPPED, GIVEN_UP, ERROR)
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Cancelled job {}", Utility.toString(handler.getJob()));
                }
                NotificationUtility.sendNotification(this.services.eventAdmin, NotificationConstants.TOPIC_JOB_CANCELLED, handler.getJob(), null);
                break;
        }

        return info;
    }

    /**
     * @see org.apache.sling.event.impl.jobs.deprecated.JobStatusNotifier#finishedJob(org.osgi.service.event.Event, boolean)
     */
    @Override
    public boolean finishedJob(final Event job, final boolean shouldReschedule) {
        final String location = (String)job.getProperty(ResourceHelper.PROPERTY_JOB_ID);
        return this.finishedJob(location, shouldReschedule ? Job.JobState.QUEUED : Job.JobState.SUCCEEDED, false);
    }

    /**
     * Handle job finish and determine whether to reschedule or cancel the job
     */
    private boolean finishedJob(final String jobId,
                                Job.JobState resultState,
                                final boolean isAsync) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Received finish for job {}, resultState={}", jobId, resultState);
        }
        // this is just a sanity check, as usually the job should have been
        // removed during sendAcknowledge.
        synchronized ( this.startedJobsLists ) {
            this.startedJobsLists.remove(jobId);
        }

        // get job handler
        final JobHandler handler;
        // let's remove the event from our processing list
        synchronized ( this.processingJobsLists ) {
            handler = this.processingJobsLists.remove(jobId);
        }

        if ( !this.running ) {
            this.logger.warn("Queue is not running anymore. Discarding finish for {}", jobId);
            return false;
        }

        if ( handler == null ) {
            if ( this.logger.isDebugEnabled() ) {
                this.logger.debug("This job has never been started by this queue: {}", jobId);
            }
            return false;
        }

        // handle the reschedule, a new job might be returned with updated reschedule info!
        final RescheduleInfo rescheduleInfo = this.handleReschedule(handler, resultState);
        if ( resultState == Job.JobState.QUEUED && !rescheduleInfo.reschedule ) {
            resultState = Job.JobState.GIVEN_UP;
        }

        if ( !rescheduleInfo.reschedule ) {
            // we keep cancelled jobs and succeeded jobs if the queue is configured like this.
            final boolean keepJobs = resultState != Job.JobState.SUCCEEDED || this.configuration.isKeepJobs();
            handler.finished(resultState, keepJobs, rescheduleInfo.processingTime);
        } else {
            this.reschedule(handler);
        }
        this.notifyFinished(rescheduleInfo.reschedule);

        return rescheduleInfo.reschedule;
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
     * @see org.apache.sling.event.impl.jobs.deprecated.JobStatusNotifier#sendAcknowledge(org.osgi.service.event.Event)
     */
    @Override
    public boolean sendAcknowledge(final Event job) {
        final String jobId = (String)job.getProperty(ResourceHelper.PROPERTY_JOB_ID);
        final JobHandler ack;
        synchronized ( this.startedJobsLists ) {
            ack = this.startedJobsLists.remove(jobId);
        }
        // if the event is still in the started jobs list, we confirm the ack
        if ( ack != null ) {
            if ( logger.isDebugEnabled() ) {
                logger.debug("Received ack for job {}", Utility.toString(ack.getJob()));
            }
            final long queueTime = ack.started - ack.queued;
            NotificationUtility.sendNotification(this.services.eventAdmin, NotificationConstants.TOPIC_JOB_STARTED, ack.getJob(), queueTime);
            synchronized ( this.processingJobsLists ) {
                this.processingJobsLists.put(jobId, ack);
            }
        }
        return ack != null;
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#resume()
     */
    @Override
    public void resume() {
        synchronized ( this.suspendLock ) {
            if ( this.suspendedSince != -1 ) {
                this.logger.debug("Waking up suspended queue {}", queueName);
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
                this.logger.debug("Suspending queue {}", queueName);
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
        final Set<String> topics = this.cache.getTopics();
        logger.debug("Removing all jobs for queue {} : {}", queueName, topics);

        if ( !topics.isEmpty() ) {

            final ResourceResolver resolver = this.services.configuration.createResourceResolver();
            try {
                final Resource baseResource = resolver.getResource(this.services.configuration.getLocalJobsPath());

                // sanity check - should never be null
                if ( baseResource != null ) {
                    final BatchResourceRemover brr = new BatchResourceRemover();

                    for(final String t : topics) {
                        final Resource topicResource = baseResource.getChild(t.replace('/', '.'));
                        if ( topicResource != null ) {
                            JobTopicTraverser.traverse(logger, topicResource, new JobTopicTraverser.JobCallback() {

                                @Override
                                public boolean handle(final JobImpl job) {
                                    final Resource jobResource = topicResource.getResourceResolver().getResource(job.getResourcePath());
                                    // sanity check
                                    if ( jobResource != null ) {
                                        try {
                                            brr.delete(jobResource);
                                        } catch ( final PersistenceException ignore) {
                                            logger.error("Unable to remove job " + job, ignore);
                                            topicResource.getResourceResolver().revert();
                                            topicResource.getResourceResolver().refresh();
                                        }
                                    }
                                    return true;
                                }
                            });
                        }
                    }
                    try {
                        resolver.commit();
                    } catch ( final PersistenceException ignore) {
                        logger.error("Unable to remove jobs", ignore);
                    }
                }
            } finally {
                resolver.close();
            }
        }
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#clear()
     */
    @Override
    public void clear() {
        // this is a noop
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
     * @see org.apache.sling.event.jobs.Queue#getStateInfo()
     */
    @Override
    public String getStateInfo() {
        synchronized ( this.suspendLock ) {
            return "isWaiting=" + this.isWaiting +
                    ", suspendedSince=" + this.suspendedSince +
                    ", asyncJobs=" + this.asyncCounter.get();
        }
    }

    protected long getRetryDelay(final JobHandler handler) {
        long delay = this.configuration.getRetryDelayInMs();
        if ( handler.getJob().getProperty(JobImpl.PROPERTY_DELAY_OVERRIDE) != null ) {
            delay = handler.getJob().getProperty(JobImpl.PROPERTY_DELAY_OVERRIDE, Long.class);
        } else  if ( handler.getJob().getProperty(Job.PROPERTY_JOB_RETRY_DELAY) != null ) {
            delay = handler.getJob().getProperty(Job.PROPERTY_JOB_RETRY_DELAY, Long.class);
        }
        return delay;
    }

    protected void reschedule(final JobHandler handler) {
        this.requeue(handler);
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

    public boolean stopJob(final JobImpl job) {
        final JobHandler handler;
        synchronized ( this.processingJobsLists ) {
            handler = this.processingJobsLists.get(job.getId());
        }
        if ( handler != null ) {
            handler.stop();
        }
        return handler != null;
    }

    /**
     * Start processing of a new job.
     * @param handler The new job handler
     */
    protected abstract void start(final JobHandler handler);

    protected abstract void notifyFinished(boolean reschedule);

}

