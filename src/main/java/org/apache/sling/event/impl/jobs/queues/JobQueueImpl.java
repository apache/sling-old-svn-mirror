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

import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.EventingThreadPool;
import org.apache.sling.event.impl.jobs.InternalJobState;
import org.apache.sling.event.impl.jobs.JobHandler;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.JobTopicTraverser;
import org.apache.sling.event.impl.jobs.Utility;
import org.apache.sling.event.impl.jobs.config.InternalQueueConfiguration;
import org.apache.sling.event.impl.jobs.deprecated.JobStatusNotifier;
import org.apache.sling.event.impl.jobs.deprecated.JobStatusNotifierImpl;
import org.apache.sling.event.impl.jobs.notifications.NotificationUtility;
import org.apache.sling.event.impl.support.BatchResourceRemover;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.Job.JobState;
import org.apache.sling.event.jobs.JobProcessor;
import org.apache.sling.event.jobs.JobUtil;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.QueueConfiguration.Type;
import org.apache.sling.event.jobs.Statistics;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The job blocking queue extends the blocking queue by some
 * functionality for the job event handling.
 */
public class JobQueueImpl
    implements Queue {

    /** Default timeout for suspend. */
    private static final long MAX_SUSPEND_TIME = 1000 * 60 * 60; // 60 mins

    /** Default number of milliseconds to wait for an ack. */
    private static final long DEFAULT_WAIT_FOR_ACK_IN_MS = 60 * 1000; // by default we wait 60 secs

    /** The logger. */
    private final Logger logger;

    /** Configuration. */
    private final InternalQueueConfiguration configuration;

    /** The queue name. */
    private volatile String queueName;

    /** Are we still running? */
    private volatile boolean running;

    /** Suspended since. */
    private final AtomicLong suspendedSince = new AtomicLong(-1);

    /** Services used by the queues. */
    private final QueueServices services;

    /** The map of events we're processing. */
    private final Map<String, JobHandler> processingJobsLists = new HashMap<String, JobHandler>();

    private final ThreadPool threadPool;

    /** Async counter. */
    private final AtomicInteger asyncCounter = new AtomicInteger();

    /** Flag for outdated. */
    private final AtomicBoolean isOutdated = new AtomicBoolean(false);

    /** A marker for closing the queue. */
    private final AtomicBoolean closeMarker = new AtomicBoolean(false);

    /** A marker for doing a full cache search. */
    private final AtomicBoolean doFullCacheSearch = new AtomicBoolean(false);

    /** A counter for rescheduling. */
    private final AtomicInteger waitCounter = new AtomicInteger();

    /** The job cache. */
    private final QueueJobCache cache;

    /** Semaphore for handling the max number of jobs. */
    private final Semaphore available;

    /** Guard for having only one thread executing start jobs. */
    private final AtomicBoolean startJobsGuard = new AtomicBoolean(false);

    /** Lock for close/start. */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /** Sleeping until is only set for ordered queues if a job is rescheduled. */
    private volatile long isSleepingUntil = -1;

    /**
     * Create a new queue.
     *
     * @param name The queue name
     * @param config The queue configuration
     * @param services The queue services
     * @param topics The topics handled by this queue
     *
     * @return {@code JobQueueImpl} if there are jobs to process, {@code null} otherwise.
     */
    public static JobQueueImpl createQueue(final String name,
                        final InternalQueueConfiguration config,
                        final QueueServices services,
                        final Set<String> topics) {
        final QueueJobCache cache = new QueueJobCache(services.configuration, name, services.statisticsManager, config.getType(), topics);
        if ( cache.isEmpty() ) {
            return null;
        }
        return new JobQueueImpl(name, config, services, cache);
    }

    /**
     * Create a new queue.
     *
     * @param name The queue name
     * @param config The queue configuration
     * @param services The queue services
     * @param cache The job cache
     */
    private JobQueueImpl(final String name,
                        final InternalQueueConfiguration config,
                        final QueueServices services,
                        final QueueJobCache cache) {
        if ( config.getOwnThreadPoolSize() > 0 ) {
            this.threadPool = new EventingThreadPool(services.threadPoolManager, config.getOwnThreadPoolSize());
        } else {
            this.threadPool = services.eventingThreadPool;
        }
        this.queueName = name;
        this.configuration = config;
        this.services = services;
        this.logger = LoggerFactory.getLogger(this.getClass().getName() + '.' + name);
        this.running = true;
        this.cache = cache;
        this.available = new Semaphore(config.getMaxParallel(), true);
        logger.info("Starting job queue {}", queueName);
        logger.debug("Configuration for job queue={}", configuration);
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
     * This method might be called concurrently, therefore we use a guard
     */
    public void startJobs() {
        if ( this.startJobsGuard.compareAndSet(false, true) ) {
            // we start as many jobs in parallel as possible
            while ( this.running && !this.isOutdated.get() && !this.isSuspended() && this.available.tryAcquire() ) {
                boolean started = false;
                this.lock.writeLock().lock();
                try {
                    final JobHandler handler = this.cache.getNextJob(this.services.jobConsumerManager,
                            this.services.statisticsManager, this, this.doFullCacheSearch.getAndSet(false));
                    if ( handler != null ) {
                        started = true;
                        this.threadPool.execute(new Runnable() {

                            @Override
                            public void run() {
                                // update thread priority and name
                                final Thread currentThread = Thread.currentThread();
                                final String oldName = currentThread.getName();
                                final int oldPriority = currentThread.getPriority();

                                currentThread.setName(oldName + "-" + handler.getJob().getQueueName() + "(" + handler.getJob().getTopic() + ")");
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

                                try {
                                    startJob(handler);
                                } finally {
                                    currentThread.setPriority(oldPriority);
                                    currentThread.setName(oldName);
                                }
                                // and try to launch another job
                                startJobs();
                            }
                        });
                    } else {
                        // no job available, stop look
                        break;
                    }

                } finally {
                    if ( !started ) {
                        this.available.release();
                    }
                    this.lock.writeLock().unlock();
                }
            }
            this.startJobsGuard.set(false);
        }
    }

    private void startJob(final JobHandler handler) {
        try {
            this.closeMarker.set(false);
            try {
                final JobImpl job = handler.getJob();
                handler.started = System.currentTimeMillis();

                if ( handler.getConsumer() != null ) {
                    this.services.configuration.getAuditLogger().debug("START OK : {}", job.getId());
                    // sanity check for the queued property
                    Calendar queued = job.getProperty(JobImpl.PROPERTY_JOB_QUEUED, Calendar.class);
                    if ( queued == null ) {
                        // we simply use a date of ten seconds ago
                        queued = Calendar.getInstance();
                        queued.setTimeInMillis(System.currentTimeMillis() - 10000);
                    }
                    final long queueTime = handler.started - queued.getTimeInMillis();
                    // update statistics
                    this.services.statisticsManager.jobStarted(this.queueName, job.getTopic(), queueTime);
                    // send notification
                    NotificationUtility.sendNotification(this.services.eventAdmin, NotificationConstants.TOPIC_JOB_STARTED, job, queueTime);

                    synchronized ( this.processingJobsLists ) {
                        this.processingJobsLists.put(job.getId(), handler);
                    }

                    JobExecutionResultImpl result = JobExecutionResultImpl.CANCELLED;
                    Job.JobState resultState = Job.JobState.ERROR;
                    final JobExecutionContextImpl ctx = new JobExecutionContextImpl(handler, new JobExecutionContextImpl.ASyncHandler() {

                        @Override
                        public void finished(final JobState state) {
                            services.jobConsumerManager.unregisterListener(job.getId());
                            finishedJob(job.getId(), state, true);
                            asyncCounter.decrementAndGet();
                        }
                    });

                    try {
                        synchronized ( ctx ) {
                            result = (JobExecutionResultImpl)handler.getConsumer().process(job, ctx);
                            if ( result == null ) { // ASYNC processing
                                services.jobConsumerManager.registerListener(job.getId(), handler.getConsumer(), ctx);
                                asyncCounter.incrementAndGet();
                                ctx.markAsync();
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
                        if ( result != null ) {
                            if ( result.getRetryDelayInMs() != null ) {
                                job.setProperty(JobImpl.PROPERTY_DELAY_OVERRIDE, result.getRetryDelayInMs());
                            }
                            if ( result.getMessage() != null ) {
                               job.setProperty(Job.PROPERTY_RESULT_MESSAGE, result.getMessage());
                            }
                            this.finishedJob(job.getId(), resultState, false);
                        }
                    }

                } else {
                    final Event jobEvent = this.getJobEvent(handler);
                    final JobStatusNotifierImpl notifier = (JobStatusNotifierImpl) jobEvent.getProperty(JobStatusNotifier.CONTEXT_PROPERTY_NAME);
                    // we need async delivery, otherwise we might create a deadlock
                    // as this method runs inside a synchronized block and the finishedJob
                    // method as well!
                    final long endOfAck = System.currentTimeMillis() + DEFAULT_WAIT_FOR_ACK_IN_MS;
                    this.services.eventAdmin.postEvent(jobEvent);

                    // wait for the ack
                    synchronized ( notifier ) {
                        while ( System.currentTimeMillis() < endOfAck && !notifier.isCalled() ) {
                            try {
                                notifier.wait(endOfAck - System.currentTimeMillis());
                            } catch ( final InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                ignoreException(ie);
                            }
                        }
                        if ( !notifier.isCalled() ) {
                            notifier.markDone();
                        }
                    }
                    if ( !notifier.isCalled() ) {
                        if ( handler.reschedule() ) {
                            this.logger.info("No acknowledge received for job {} stored at {}. Requeueing job.", Utility.toString(handler.getJob()), handler.getJob().getId());
                            handler.getJob().retry();
                            this.requeue(handler);
                        }
                    } else {
                        if ( logger.isDebugEnabled() ) {
                            logger.debug("Received ack for job {}", Utility.toString(job));
                        }
                        this.services.configuration.getAuditLogger().debug("START OK : {}", job.getId());
                        // sanity check for the queued property
                        Calendar queued = job.getProperty(JobImpl.PROPERTY_JOB_QUEUED, Calendar.class);
                        if ( queued == null ) {
                            // we simply use a date of ten seconds ago
                            queued = Calendar.getInstance();
                            queued.setTimeInMillis(System.currentTimeMillis() - 10000);
                        }
                        final long queueTime = handler.started - queued.getTimeInMillis();
                        // update statistics
                        this.services.statisticsManager.jobStarted(this.queueName, job.getTopic(), queueTime);
                        // send notification
                        NotificationUtility.sendNotification(this.services.eventAdmin, NotificationConstants.TOPIC_JOB_STARTED, job, queueTime);

                        synchronized ( this.processingJobsLists ) {
                            this.processingJobsLists.put(job.getId(), handler);
                        }

                        // check for processor
                        final JobProcessor processor = notifier.getProcessor();
                        if ( processor != null ) {
                            boolean result = false;
                            try {
                                result = processor.process(jobEvent);
                            } catch (Throwable t) { //NOSONAR
                                LoggerFactory.getLogger(JobUtil.class).error("Unhandled error occured in job processor " + t.getMessage() + " while processing job " + job, t);
                                // we don't reschedule if an exception occurs
                                result = true;
                            }
                            if ( result ) {
                                this.finishedJob(job.getId(), Job.JobState.SUCCEEDED, false);
                            } else {
                                this.finishedJob(job.getId(), Job.JobState.QUEUED, false);
                            }
                        } else {
                            // async processing
                            final JobExecutionContextImpl ctx = new JobExecutionContextImpl(handler, new JobExecutionContextImpl.ASyncHandler() {

                                @Override
                                public void finished(final JobState state) {
                                    services.jobConsumerManager.unregisterListener(job.getId());
                                    finishedJob(job.getId(), state, true);
                                    asyncCounter.decrementAndGet();
                                }
                            });
                            services.jobConsumerManager.registerListener(job.getId(), handler.getConsumer(), ctx);
                            asyncCounter.incrementAndGet();
                            ctx.markAsync();

                            notifier.setJobExecutionContext(ctx);
                        }
                    }
                }
            } catch (final Exception re) {
                // if an exception occurs, we just log
                this.logger.error("Exception during job processing.", re);
            }
        } finally {
            this.available.release();
        }
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
        this.lock.writeLock().lock();
        try {
            // check if possible
            if ( this.canBeClosed() ) {
                if ( this.closeMarker.get() ) {
                    this.close();
                    return true;
                }
                this.closeMarker.set(true);
            }
        } finally {
            this.lock.writeLock().unlock();
        }
        return false;
    }

    /**
     * Check whether this queue can be closed
     */
    private boolean canBeClosed() {
        return !this.isSuspended()
            && this.asyncCounter.get() == 0
            && this.waitCounter.get() == 0
            && this.available.availablePermits() == this.configuration.getMaxParallel();
    }

    /**
     * Close this queue.
     */
    public void close() {
        this.running = false;
        this.logger.debug("Shutting down job queue {}", queueName);
        this.resume();

        synchronized ( this.processingJobsLists ) {
            this.processingJobsLists.clear();
        }
        if ( this.configuration.getOwnThreadPoolSize() > 0 ) {
            ((EventingThreadPool)this.threadPool).release();
        }

        this.logger.info("Stopped job queue {}", this.queueName);
    }

    /**
     * Periodic maintenance
     */
    public void maintain() {
        // check suspended
        final long since = this.suspendedSince.get();
        if ( since != -1 && since + MAX_SUSPEND_TIME < System.currentTimeMillis() ) {
            logger.info("Waking up suspended queue. It has been suspended for more than {}ms", MAX_SUSPEND_TIME);
            this.resume();
        }

        // set full cache search
        this.doFullCacheSearch.set(true);

        this.startJobs();
    }

    /**
     * Inform the queue about new job for the given topics.
     * @param topics the new topics
     */
    public void wakeUpQueue(final Set<String> topics) {
        this.cache.handleNewTopics(topics);
    }

    /**
     * Put a job back in the queue
     * @param handler The job handler
     */
    private void requeue(final JobHandler handler) {
        this.cache.reschedule(this.queueName, handler, this.services.statisticsManager);
        this.startJobs();
    }

    private static final class RescheduleInfo {
        public boolean      reschedule = false;
        // processing time is only set of state is SUCCEEDED
        public long         processingTime;
        public Job.JobState state;
        public InternalJobState       finalState;
    }

    private RescheduleInfo handleReschedule(final JobHandler handler, final Job.JobState resultState) {
        final RescheduleInfo info = new RescheduleInfo();
        info.state = resultState;
        switch ( resultState ) {
            case SUCCEEDED : // job is finished
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Finished job {}", Utility.toString(handler.getJob()));
                }
                info.processingTime = System.currentTimeMillis() - handler.started;
                info.finalState = InternalJobState.SUCCEEDED;
                break;
            case QUEUED : // check if we exceeded the number of retries
                final int retries = handler.getJob().getProperty(Job.PROPERTY_JOB_RETRIES, 0);
                int retryCount = handler.getJob().getProperty(Job.PROPERTY_JOB_RETRY_COUNT, 0);

                retryCount++;
                if ( retries != -1 && retryCount > retries ) {
                    if ( this.logger.isDebugEnabled() ) {
                        this.logger.debug("Cancelled job {}", Utility.toString(handler.getJob()));
                    }
                    info.finalState = InternalJobState.CANCELLED;
                } else {
                    info.reschedule = true;
                    handler.getJob().retry();
                    if ( this.logger.isDebugEnabled() ) {
                        this.logger.debug("Failed job {}", Utility.toString(handler.getJob()));
                    }
                    info.finalState = InternalJobState.FAILED;
                }
                break;
            default : // consumer cancelled the job (STOPPED, GIVEN_UP, ERROR)
                if ( this.logger.isDebugEnabled() ) {
                    this.logger.debug("Cancelled job {}", Utility.toString(handler.getJob()));
                }
                info.finalState = InternalJobState.CANCELLED;
                break;
        }

        if ( info.state == Job.JobState.QUEUED && !info.reschedule ) {
            info.state = Job.JobState.GIVEN_UP;
        }
        return info;
    }

    /**
     * Handle job finish and determine whether to reschedule or cancel the job
     */
    private boolean finishedJob(final String jobId,
                                final Job.JobState resultState,
                                final boolean isAsync) {
        this.services.configuration.getAuditLogger().debug("FINISHED {} : {}", resultState, jobId);
        this.logger.debug("Received finish for job {}, resultState={}", jobId, resultState);

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
            this.logger.warn("This job has never been started by this queue: {}", jobId);
            return false;
        }

        // handle the rescheduling of the job
        final RescheduleInfo rescheduleInfo = this.handleReschedule(handler, resultState);

        if ( !rescheduleInfo.reschedule ) {
            // we keep cancelled jobs and succeeded jobs if the queue is configured like this.
            final boolean keepJobs = rescheduleInfo.state != Job.JobState.SUCCEEDED || this.configuration.isKeepJobs();
            handler.finished(rescheduleInfo.state, keepJobs, rescheduleInfo.processingTime);
        } else {
            this.reschedule(handler);
        }
        // update statistics
        this.services.statisticsManager.jobEnded(this.queueName,
                handler.getJob().getTopic(),
                rescheduleInfo.finalState,
                rescheduleInfo.processingTime);
        // send notification
        NotificationUtility.sendNotification(this.services.eventAdmin,
                rescheduleInfo.finalState.getTopic(),
                handler.getJob(), rescheduleInfo.processingTime);

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
        properties.put(JobStatusNotifier.CONTEXT_PROPERTY_NAME, new JobStatusNotifierImpl());

        // remove app id and distributable flag
        properties.remove(EventUtil.PROPERTY_DISTRIBUTE);
        properties.remove(EventUtil.PROPERTY_APPLICATION);

        return new Event(eventTopic, properties);
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#resume()
     */
    @Override
    public void resume() {
        if ( this.suspendedSince.getAndSet(-1) != -1 ) {
            this.logger.debug("Waking up suspended queue {}", queueName);
            this.startJobs();
        }
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#suspend()
     */
    @Override
    public void suspend() {
        if ( this.suspendedSince.compareAndSet(-1, System.currentTimeMillis()) ) {
            this.logger.debug("Suspending queue {}", queueName);
        }
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#isSuspended()
     */
    @Override
    public boolean isSuspended() {
        return this.suspendedSince.get() != -1;
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
        if ( this.configuration.getType() == Type.ORDERED ) {
            if ( "isSleepingUntil".equals(key) ) {
                return this.isSleepingUntil;
            }
        }
        return null;
    }

    /**
     * @see org.apache.sling.event.jobs.Queue#getStateInfo()
     */
    @Override
    public String getStateInfo() {
        return "outdated=" + this.isOutdated.get() +
                ", suspendedSince=" + this.suspendedSince.get() +
                ", asyncJobs=" + this.asyncCounter.get() +
                ", waitCount=" + this.waitCounter.get() +
                ", jobCount=" + String.valueOf(this.configuration.getMaxParallel() - this.available.availablePermits() +
                (this.configuration.getType() == Type.ORDERED ? ", isSleepingUntil=" + this.isSleepingUntil : ""));
    }

    /**
     * Get the retry delay for a job.
     * @param handler The job handler.
     * @return The retry delay
     */
    private long getRetryDelay(final JobHandler handler) {
        long delay = this.configuration.getRetryDelayInMs();
        if ( handler.getJob().getProperty(JobImpl.PROPERTY_DELAY_OVERRIDE) != null ) {
            delay = handler.getJob().getProperty(JobImpl.PROPERTY_DELAY_OVERRIDE, Long.class);
        } else  if ( handler.getJob().getProperty(Job.PROPERTY_JOB_RETRY_DELAY) != null ) {
            delay = handler.getJob().getProperty(Job.PROPERTY_JOB_RETRY_DELAY, Long.class);
        }
        return delay;
    }

    /**
     * Helper method which just logs the exception in debug mode.
     * @param e
     */
    private void ignoreException(Exception e) {
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

    private void reschedule(final JobHandler handler) {
        // we delay putting back the job until the retry delay is over
        final long delay = this.getRetryDelay(handler);
        if ( delay > 0 ) {
            if ( this.configuration.getType() == Type.ORDERED ) {
                this.cache.setIsBlocked(true);
            }
            handler.addToRetryList();
            final Date fireDate = new Date();
            fireDate.setTime(System.currentTimeMillis() + delay);
            if ( this.configuration.getType() == Type.ORDERED ) {
                this.isSleepingUntil = fireDate.getTime();
            }

            final String jobName = "Waiting:" + queueName + ":" + handler.hashCode();
            final Runnable t = new Runnable() {
                @Override
                public void run() {
                    try {
                        if ( handler.removeFromRetryList() ) {
                            requeue(handler);
                        }
                        waitCounter.decrementAndGet();
                    } finally {
                        if ( configuration.getType() == Type.ORDERED ) {
                            isSleepingUntil = -1;
                            cache.setIsBlocked(false);
                            startJobs();
                        }
                    }
                }
            };
            this.waitCounter.incrementAndGet();
            if ( !services.scheduler.schedule(t, services.scheduler.AT(fireDate).name(jobName)) ) {
                // if scheduling fails run the thread directly
                t.run();
            }
        } else {
            // put directly into queue
            this.requeue(handler);
        }
    }
}

