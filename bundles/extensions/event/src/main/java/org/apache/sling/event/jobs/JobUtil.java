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
package org.apache.sling.event.jobs;

import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.event.impl.jobs.JobStatusNotifier;
import org.apache.sling.event.impl.support.Environment;
import org.osgi.service.event.Event;
import org.slf4j.LoggerFactory;

/**
 * The <code>Job</code> class is an utility class for
 * creating and processing jobs.
 * @since 3.0
 */
public abstract class JobUtil {

    /** The job topic property. */
    public static final String PROPERTY_JOB_TOPIC = "event.job.topic";

    /** The property for the unique event name. Value is of type String (This is optional). */
    public static final String PROPERTY_JOB_NAME = "event.job.id";

    /** The property to set if a job can be run parallel to any other job.
     * The following values are supported:
     * - boolean value <code>true</code> and <code>false</code>
     * - string value <code>true</code> and <code>false</code>
     * - integer value higher than 1 - if this is specified jobs are run in
     * parallel but never more than the specified number.
     *
     * We might want to use different values in the future for enhanced
     * parallel job handling.
     *
     * This value is only used, if {@link JobUtil#PROPERTY_JOB_QUEUE_NAME} is
     * specified and the referenced queue is not started yet.
     */
    public static final String PROPERTY_JOB_PARALLEL = "event.job.parallel";

    /** The property to set if a job should only be run on the same app it has been created. */
    public static final String PROPERTY_JOB_RUN_LOCAL = "event.job.run.local";

    /** The property to track the retry count for jobs. Value is of type Integer. */
    public static final String PROPERTY_JOB_RETRY_COUNT = "event.job.retrycount";

    /** The property for setting the maximum number of retries. Value is of type Integer. */
    public static final String PROPERTY_JOB_RETRIES = "event.job.retries";

    /** The property to set a retry delay. Value is of type Long and specifies milliseconds. */
    public static final String PROPERTY_JOB_RETRY_DELAY = "event.job.retrydelay";

    /** The property to set to put the jobs into a separate job queue. This property
     * specifies the name of the job queue. If the job queue does not exists yet
     * a new queue is created.
     * If a ordered job queue is used, the jobs are never executed in parallel
     * from this queue! For non ordered queues the {@link #PROPERTY_JOB_PARALLEL}
     * with an integer value higher than 1 can be used to specify the maximum number
     * of parallel jobs for this queue.
     */
    public static final String PROPERTY_JOB_QUEUE_NAME = "event.job.queuename";

    /** If this property is set with any value, the queue processes the jobs in the same
     * order as they have arrived.
     * This property has only an effect if {@link #PROPERTY_JOB_QUEUE_NAME} is specified
     * and the job queue has not been started yet.
     */
    public static final String PROPERTY_JOB_QUEUE_ORDERED = "event.job.queueordered";

    /** This property allows to override the priority for the thread used to start this job.
     * The property is evaluated by the {@link #processJob(Event, JobProcessor)} method.
     * If another way of executing the job is used, it is up to the client to ensure
     * the job priority.
     * For possible values see {@link JobPriority}.
     */
    public static final String PROPERTY_JOB_PRIORITY = "event.job.priority";

    /**
     * The priority for jobs.
     */
    public enum JobPriority {
        NORM,
        MIN,
        MAX
    }

    /** The topic for jobs. */
    public static final String TOPIC_JOB = "org/apache/sling/event/job";

    /**
     * This is a unique identifer which can be used to cancel the job.
     */
    public static final String JOB_ID = "slingevent:eventId";

    /**
     * Notification events for jobs.
     */

    /** Asynchronous notification event when a job is started.
     * The property {@link #PROPERTY_NOTIFICATION_JOB} contains the job event and the
     * property {@link org.osgi.service.event.EventConstants#TIMESTAMP} contains the
     * timestamp of the event (as a Long).
     */
    public static final String TOPIC_JOB_STARTED = "org/apache/sling/event/notification/job/START";

    /** Asynchronous notification event when a job is finished.
     * The property {@link #PROPERTY_NOTIFICATION_JOB} contains the job event and the
     * property {@link org.osgi.service.event.EventConstants#TIMESTAMP} contains the
     * timestamp of the event (as a Long).
     */
    public static final String TOPIC_JOB_FINISHED = "org/apache/sling/event/notification/job/FINISHED";

    /** Asynchronous notification event when a job failed.
     * If a job execution fails, it is rescheduled for another try.
     * The property {@link #PROPERTY_NOTIFICATION_JOB} contains the job event and the
     * property {@link org.osgi.service.event.EventConstants#TIMESTAMP} contains the
     * timestamp of the event (as a Long).
     */
    public static final String TOPIC_JOB_FAILED = "org/apache/sling/event/notification/job/FAILED";

    /** Asynchronous notification event when a job is cancelled.
     * If a job execution is cancelled it is not rescheduled.
     * The property {@link #PROPERTY_NOTIFICATION_JOB} contains the job event and the
     * property {@link org.osgi.service.event.EventConstants#TIMESTAMP} contains the
     * timestamp of the event (as a Long).
     */
    public static final String TOPIC_JOB_CANCELLED = "org/apache/sling/event/notification/job/CANCELLED";

    /** Property containing the job event. The value is of type org.osgi.service.event.Event. */
    public static final String PROPERTY_NOTIFICATION_JOB = "event.notification.job";

    /**
     * Is this a job event?
     * This method checks if the event contains the {@link #PROPERTY_JOB_TOPIC}
     * property.
     * @param event The event to check.
     * @return <code>true></code> if this is a job event.
     */
    public static boolean isJobEvent(final Event event) {
        return event.getProperty(PROPERTY_JOB_TOPIC) != null;
    }

    /**
     * Check if this a job event and return the notifier context.
     * @throws IllegalArgumentException If the event is a job event but does not have a notifier context.
     */
    private static JobStatusNotifier.NotifierContext getNotifierContext(final Event job) {
        // check if this is a job event
        if ( !isJobEvent(job) ) {
            return null;
        }
        final JobStatusNotifier.NotifierContext ctx = (JobStatusNotifier.NotifierContext) job.getProperty(JobStatusNotifier.CONTEXT_PROPERTY_NAME);
        if ( ctx == null ) {
            throw new IllegalArgumentException("JobStatusNotifier context is not available in event properties.");
        }
        return ctx;
    }

    /**
     * Send an acknowledge.
     * This signals the job handler that someone is starting to process the job. This method
     * should be invoked as a first command during job processing.
     * If this method returns <code>false</code> this means someone else is already
     * processing this job, and the caller should not process the event anymore.
     * @return Returns <code>true</code> if the acknowledge could be sent
     * @throws IllegalArgumentException If the event is a job event but does not have a notifier context.
     */
    public static boolean acknowledgeJob(final Event job) {
        final JobStatusNotifier.NotifierContext ctx = getNotifierContext(job);
        if ( ctx != null ) {
            if ( !ctx.getJobStatusNotifier().sendAcknowledge(job) ) {
                // if we don't get an ack, someone else is already processing this job.
                // we process but do not notify the job event handler.
                LoggerFactory.getLogger(JobUtil.class).info("Someone else is already processing job {}.", job);
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Notify a finished job.
     * @throws IllegalArgumentException If the event is a job event but does not have a notifier context.
     */
    public static void finishedJob(final Event job) {
        final JobStatusNotifier.NotifierContext ctx = getNotifierContext(job);
        if ( ctx != null ) {
            ctx.getJobStatusNotifier().finishedJob(job, false);
        }
    }

    /**
     * Notify a failed job.
     * @return <code>true</code> if the job has been rescheduled, <code>false</code> otherwise.
     * @throws IllegalArgumentException If the event is a job event but does not have a notifier context.
     */
    public static boolean rescheduleJob(final Event job) {
        final JobStatusNotifier.NotifierContext ctx = getNotifierContext(job);
        if ( ctx != null ) {
            return ctx.getJobStatusNotifier().finishedJob(job, true);
        }
        return false;
    }

    /**
     * Process a job in the background and notify its success.
     * This method also sends an acknowledge message to the job event handler.
     * @throws IllegalArgumentException If the event is a job event but does not have a notifier context.
     */
    public static void processJob(final Event job, final JobProcessor processor) {
        // first check for a notifier context to send an acknowledge
        boolean notify = true;
        final JobStatusNotifier.NotifierContext ctx = getNotifierContext(job);
        if ( ctx != null ) {
            if ( !ctx.getJobStatusNotifier().sendAcknowledge(job) ) {
                // if we don't get an ack, someone else is already processing this job.
                // we process but do not notify the job event handler.
                LoggerFactory.getLogger(JobUtil.class).info("Someone else is already processing job {}.", job);
                notify = false;
            }
        }
        final JobPriority priority = (JobPriority) job.getProperty(PROPERTY_JOB_PRIORITY);
        final boolean notifyResult = notify;

        final Runnable task = new Runnable() {

            /**
             * @see java.lang.Runnable#run()
             */
            public void run() {
                final Thread currentThread = Thread.currentThread();
                // update priority and name
                final String oldName = currentThread.getName();
                final int oldPriority = currentThread.getPriority();

                currentThread.setName(oldName + "-" + job.getProperty(PROPERTY_JOB_QUEUE_NAME) + "(" + job.getProperty(PROPERTY_JOB_TOPIC) + ")");
                if ( priority != null ) {
                    switch ( priority ) {
                        case NORM : currentThread.setPriority(Thread.NORM_PRIORITY);
                                    break;
                        case MIN  : currentThread.setPriority(Thread.MIN_PRIORITY);
                                    break;
                        case MAX  : currentThread.setPriority(Thread.MAX_PRIORITY);
                                    break;
                    }
                }
                boolean result = false;
                try {
                    result = processor.process(job);
                } catch (Throwable t) { //NOSONAR
                    LoggerFactory.getLogger(JobUtil.class).error("Unhandled error occured in job processor " + t.getMessage() + " while processing job " + job, t);
                    // we don't reschedule if an exception occurs
                    result = true;
                } finally {
                    currentThread.setPriority(oldPriority);
                    currentThread.setName(oldName);
                    if ( notifyResult ) {
                        if ( result ) {
                            JobUtil.finishedJob(job);
                        } else {
                            JobUtil.rescheduleJob(job);
                        }
                    }
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
    }

    private JobUtil() {
        // avoid instantiation
    }
}