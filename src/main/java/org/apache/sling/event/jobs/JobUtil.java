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

import java.util.Calendar;

import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.event.impl.jobs.deprecated.JobStatusNotifier;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.event.Event;
import org.slf4j.LoggerFactory;

/**
 * The <code>Job</code> class is an utility class for
 * creating and processing jobs.
 * @since 3.0
 * @deprecated
 */
@Deprecated
public abstract class JobUtil {

    /**
     * The job topic property.
     * @deprecated - Jobs should be started via {@link JobManager#addJob(String, String, java.util.Map)}
     */
    @Deprecated
    public static final String PROPERTY_JOB_TOPIC = "event.job.topic";

    /**
     * The property for the unique event name. Value is of type String.
     * This property should only be used if it can happen that the exact same
     * job is started on different cluster nodes.
     * By specifying the same id for this job on all cluster nodes,
     * the job handling can detect the duplicates and process the job
     * only once.
     * This is optional - and should only be used for the case mentioned.
     * @deprecated - Jobs should be started via {@link JobManager#addJob(String, String, java.util.Map)}
     */
    @Deprecated
    public static final String PROPERTY_JOB_NAME = "event.job.id";

    /**
     * This property is not supported anymore
     * @deprecated
     */
    @Deprecated
    public static final String PROPERTY_JOB_PARALLEL = "event.job.parallel";

    /**
     * This property is not supported anymore
     * @deprecated
     */
    @Deprecated
    public static final String PROPERTY_JOB_RUN_LOCAL = "event.job.run.local";

    /**
     * The property to track the retry count for jobs. Value is of type Integer.
     * On first execution the value of this property is zero.
     * This property is managed by the job handling.
     * If this property is set by the client creating the job it's value is ignored
     * @deprecated - Use the new {@link Job} interface instead.
     */
    @Deprecated
    public static final String PROPERTY_JOB_RETRY_COUNT = "event.job.retrycount";

    /**
     * The property to track the retry maximum retry count for jobs. Value is of type Integer.
     * This property is managed by the job handling.
     * If this property is set by the client creating the job it's value is ignored
     * @deprecated - Use the new {@link Job} interface instead.
     */
    @Deprecated
    public static final String PROPERTY_JOB_RETRIES = "event.job.retries";

    /**
     * The property to set a retry delay. Value is of type Long and specifies milliseconds.
     * @deprecated - Use the new {@link Job} interface instead.
     */
    @Deprecated
    public static final String PROPERTY_JOB_RETRY_DELAY = "event.job.retrydelay";

    /**
     * The name of the job queue processing this job.
     * This property is set by the job handling when the job is processed.
     * If this property is set by the client creating the job it's value is ignored
     * @deprecated - Use the new {@link Job} interface instead.
     */
    @Deprecated
    public static final String PROPERTY_JOB_QUEUE_NAME = "event.job.queuename";

    /**
     * This property is not supported anymore
     * @deprecated
     */
    @Deprecated
    public static final String PROPERTY_JOB_QUEUE_ORDERED = "event.job.queueordered";

    /**
     * This property is set by the job handling to define the priority of this job
     * execution.
     * The property is evaluated by the {@link #processJob(Event, JobProcessor)} method.
     * If another way of executing the job is used, it is up to the processor to ensure
     * the job priority is taken into account.
     * For possible values see {@link JobPriority}.
     * If this property is set by the client creating the job it's value is ignored
     * @deprecated - Use the new {@link Job} interface instead.
     */
    @Deprecated
    public static final String PROPERTY_JOB_PRIORITY = "event.job.priority";

    /**
     * This property is set by the job handling and contains a calendar object
     * specifying the date and time when this job has been created.
     * If this property is set by the client creating the job it's value is ignored
     * @deprecated - Use the new {@link Job} interface instead.
     */
    @Deprecated
    public static final String PROPERTY_JOB_CREATED = "slingevent:created";

    /**
     * This property is set by the job handling and contains the Sling instance ID
     * of the instance where this job has been created.
     * @deprecated - Use the new {@link Job} interface instead.
     */
    @Deprecated
    public static final String PROPERTY_JOB_CREATED_APPLICATION = "slingevent:application";

    /**
     * This property is set by the job handling and contains the Sling instance ID
     * of the instance where this job should be processed.
     * @deprecated - Use the new {@link Job} interface instead.
     */
    @Deprecated
    public static final String PROPERTY_JOB_APPLICATION = "event.job.application";

    /**
     * The priority for jobs.
     * @deprecated
     */
    public enum JobPriority {
        NORM,
        MIN,
        MAX
    }

    /**
     * The topic for jobs.
     * @deprecated - Use the new {@link JobManager#addJob(String, String, java.util.Map)} method instead.
     */
    @Deprecated
    public static final String TOPIC_JOB = "org/apache/sling/event/job";

    /**
     * This is a unique identifier which can be used to cancel the job.
     * @deprecated - Use the new {@link Job} interface instead.
     */
    @Deprecated
    public static final String JOB_ID = "slingevent:eventId";

    /**
     * Notification events for jobs.
     */

    /**
     * @see NotificationConstants#TOPIC_JOB_STARTED
     * @deprecated Use NotificationConstants#TOPIC_JOB_STARTED
     */
    @Deprecated
    public static final String TOPIC_JOB_STARTED = NotificationConstants.TOPIC_JOB_STARTED;

    /**
     * @see NotificationConstants#TOPIC_JOB_FINISHED
     * @deprecated Use NotificationConstants#TOPIC_JOB_FINISHED
     */
    @Deprecated
    public static final String TOPIC_JOB_FINISHED = NotificationConstants.TOPIC_JOB_FINISHED;

    /**
     * @see NotificationConstants#TOPIC_JOB_FAILED
     * @deprecated Use NotificationConstants#TOPIC_JOB_FAILED
     */
    @Deprecated
    public static final String TOPIC_JOB_FAILED = NotificationConstants.TOPIC_JOB_FAILED;

    /**
     * @see NotificationConstants#TOPIC_JOB_CANCELLED
     * @deprecated Use NotificationConstants#TOPIC_JOB_CANCELLED
     */
    @Deprecated
    public static final String TOPIC_JOB_CANCELLED = NotificationConstants.TOPIC_JOB_CANCELLED;

    /**
     * Property containing the job event. The value is of type org.osgi.service.event.Event.
     * @deprecated
     */
    @Deprecated
    public static final String PROPERTY_NOTIFICATION_JOB = "event.notification.job";

    /**
     * @see NotificationConstants#NOTIFICATION_PROPERTY_JOB_TOPIC
     * @deprecated Use NotificationConstants#NOTIFICATION_PROPERTY_JOB_TOPIC
     */
    @Deprecated
    public static final String NOTIFICATION_PROPERTY_JOB_TOPIC = NotificationConstants.NOTIFICATION_PROPERTY_JOB_TOPIC;

    /**
     * Property containing the optional job name. Value is of type String.
     */
    @Deprecated
    public static final String NOTIFICATION_PROPERTY_JOB_NAME = "event.job.id";

    /**
     * @see NotificationConstants#NOTIFICATION_PROPERTY_JOB_ID
     * @deprecated Use NotificationConstants#NOTIFICATION_PROPERTY_JOB_ID
     */
    @Deprecated
    public static final String NOTIFICATION_PROPERTY_JOB_ID = NotificationConstants.NOTIFICATION_PROPERTY_JOB_ID;

    /**
     * Is this a job event?
     * This method checks if the event contains the {@link #PROPERTY_JOB_TOPIC}
     * property.
     * @param event The event to check.
     * @return <code>true></code> if this is a job event.
     * @deprecated - Use the new {@link Job} interface instead.
     */
    @Deprecated
    public static boolean isJobEvent(final Event event) {
        return event.getProperty(PROPERTY_JOB_TOPIC) != null;
    }

    /**
     * Check if this a job event and return the notifier context.
     * @throws IllegalArgumentException If the event is a job event but does not have a notifier context.
     * @deprecated - Use the new {@link JobConsumer} interface instead.
     */
    @Deprecated
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
     * @deprecated - Use the new {@link JobConsumer} interface instead.
     */
    @Deprecated
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
     * @deprecated - Use the new {@link JobConsumer} interface instead.
     */
    @Deprecated
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
     * @deprecated - Use the new {@link JobConsumer} interface instead.
     */
    @Deprecated
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
     * @deprecated - Use the new {@link JobConsumer} interface instead.
     */
    @Deprecated
    public static void processJob(final Event job, final JobProcessor processor) {
        // first check for a notifier context to send an acknowledge
        final JobStatusNotifier.NotifierContext ctx = getNotifierContext(job);
        boolean notify = ctx != null;
        if ( ctx != null && !ctx.getJobStatusNotifier().sendAcknowledge(job) ) {
            // if we don't get an ack, someone else is already processing this job.
            // we process but do not notify the job event handler.
            LoggerFactory.getLogger(JobUtil.class).info("Someone else is already processing job {}.", job);
            notify = false;
        }
        final JobPriority priority = (JobPriority) job.getProperty(PROPERTY_JOB_PRIORITY);
        final boolean notifyResult = notify;

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

    /**
     * Get the created calendar object.
     * @param job The job event
     * @return The created info or <code>null</code> if this is not a job event.
     * @deprecated - Use the new {@link Job} interface instead.
     */
    @Deprecated
    public static Calendar getJobCreated(final Event job) {
        return (Calendar) job.getProperty(PROPERTY_JOB_CREATED);
    }

    private JobUtil() {
        // avoid instantiation
    }
}