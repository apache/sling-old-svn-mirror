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
package org.apache.sling.event;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.sling.event.EventUtil.JobStatusNotifier.NotifierContext;
import org.apache.sling.event.impl.AbstractRepositoryEventHandler;
import org.apache.sling.event.impl.JobEventHandler;
import org.osgi.service.event.Event;
import org.slf4j.LoggerFactory;

/**
 * The <code>EventUtil</code> class is an utility class for
 * clustered environments.
 */
public abstract class EventUtil {

    /** This event property indicates, if the event should be distributed in the cluster (default false). */
    public static final String PROPERTY_DISTRIBUTE = "event.distribute";

    /** This event property specifies the application node. */
    public static final String PROPERTY_APPLICATION = "event.application";

    /**
     * Job Handling
     */

    /** The job topic property. */
    public static final String PROPERTY_JOB_TOPIC = "event.job.topic";

    /** The property for the unique event id. Value is of type String (This is optional). */
    public static final String PROPERTY_JOB_ID = "event.job.id";

    /** The property to set if a job can be run parallel to any other job.
     * The following values are supported:
     * - boolean value <code>true</code> and <code>false</code>
     * - string value <code>true</code> and <code>false</code>
     * - integer value higher than 1 - if this is specified jobs are run in
     * parallel but never more than the specified number.
     *
     * We might want to use different values in the future for enhanced
     * parallel job handling. */
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
     * and starting with version 2.2 this value is only checked in the first job for this
     * queue.
     */
    public static final String PROPERTY_JOB_QUEUE_ORDERED = "event.job.queueordered";

    /** The topic for jobs. */
    public static final String TOPIC_JOB = "org/apache/sling/event/job";

    /**
     * Timed Events
     */

    /** The topic for timed events. */
    public static final String TOPIC_TIMED_EVENT = "org/apache/sling/event/timed";

    /** The real topic of the event. */
    public static final String PROPERTY_TIMED_EVENT_TOPIC = "event.topic.timed";

    /** The property for the unique event id. */
    public static final String PROPERTY_TIMED_EVENT_ID = "event.timed.id";

    /** The scheduler expression for the timed event. */
    public static final String PROPERTY_TIMED_EVENT_SCHEDULE = "event.timed.scheduler";

    /** The period for the timed event. */
    public static final String PROPERTY_TIMED_EVENT_PERIOD = "event.timed.period";

    /** The date for the timed event. */
    public static final String PROPERTY_TIMED_EVENT_DATE = "event.timed.date";

    /**
     * Notification events for jobs.
     */

    /** Asynchronous notification event when a job is started.
     * The property {@link #PROPERTY_NOTIFICATION_JOB} contains the job event and the
     * property {@link org.osgi.service.event.EventConstants#TIMESTAMP} contains the
     * timestamp of the event (as a Long).
     * @since 2.2 */
    public static final String TOPIC_JOB_STARTED = "org/apache/sling/event/notification/job/START";

    /** Asynchronous notification event when a job is finished.
     * The property {@link #PROPERTY_NOTIFICATION_JOB} contains the job event and the
     * property {@link org.osgi.service.event.EventConstants#TIMESTAMP} contains the
     * timestamp of the event (as a Long).
     * @since 2.2 */
    public static final String TOPIC_JOB_FINISHED = "org/apache/sling/event/notification/job/FINISHED";

    /** Asynchronous notification event when a job failed.
     * If a job execution fails, it is rescheduled for another try.
     * The property {@link #PROPERTY_NOTIFICATION_JOB} contains the job event and the
     * property {@link org.osgi.service.event.EventConstants#TIMESTAMP} contains the
     * timestamp of the event (as a Long).
     * @since 2.2 */
    public static final String TOPIC_JOB_FAILED = "org/apache/sling/event/notification/job/FAILED";

    /** Asynchronous notification event when a job is cancelled.
     * If a job execution is cancelled it is not rescheduled.
     * The property {@link #PROPERTY_NOTIFICATION_JOB} contains the job event and the
     * property {@link org.osgi.service.event.EventConstants#TIMESTAMP} contains the
     * timestamp of the event (as a Long).
     * @since 2.2 */
    public static final String TOPIC_JOB_CANCELLED = "org/apache/sling/event/notification/job/CANCELLED";

    /** Property containing the job event.
     * @since 2.2 */
    public static final String PROPERTY_NOTIFICATION_JOB = "event.notification.job";

    /**
     * Utility Methods
     */

    /**
     * Create a distributable event.
     * A distributable event is distributed across the cluster.
     * @param topic
     * @param properties
     * @return An OSGi event.
     */
    public static Event createDistributableEvent(String topic,
                                                 Dictionary<String, Object> properties) {
        final Dictionary<String, Object> newProperties;
        // create a new dictionary
        newProperties = new Hashtable<String, Object>();
        if ( properties != null ) {
            final Enumeration<String> e = properties.keys();
            while ( e.hasMoreElements() ) {
                final String key = e.nextElement();
                newProperties.put(key, properties.get(key));
            }
        }
        // for now the value has no meaning, so we just put an empty string in it.
        newProperties.put(PROPERTY_DISTRIBUTE, "");
        return new Event(topic, newProperties);
    }

    /**
     * Should this event be distributed in the cluster?
     * @param event
     * @return <code>true</code> if the event should be distributed.
     */
    public static boolean shouldDistribute(Event event) {
        return event.getProperty(PROPERTY_DISTRIBUTE) != null;
    }

    /**
     * Is this a local event?
     * @param event
     * @return <code>true</code> if this is a local event
     */
    public static boolean isLocal(Event event) {
        final String appId = getApplicationId(event);
        return appId == null || appId.equals(AbstractRepositoryEventHandler.APPLICATION_ID);
    }

    /**
     * Return the application id the event was created at.
     * @param event
     * @return The application id or null if the event has been created locally.
     */
    public static String getApplicationId(Event event) {
        return (String)event.getProperty(PROPERTY_APPLICATION);
    }

    /**
     * Is this a job event?
     * This method checks if the event contains the {@link #PROPERTY_JOB_TOPIC}
     * property.
     * @param event The event to check.
     * @return <code>true></code> if this is a job event.
     */
    public static boolean isJobEvent(Event event) {
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
        final JobStatusNotifier.NotifierContext ctx = (NotifierContext) job.getProperty(JobStatusNotifier.CONTEXT_PROPERTY_NAME);
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
     * @since 2.3
     */
    public static boolean acknowledgeJob(Event job) {
        final JobStatusNotifier.NotifierContext ctx = getNotifierContext(job);
        if ( ctx != null ) {
            if ( !ctx.notifier.sendAcknowledge(job, ctx.eventNodePath) ) {
                // if we don't get an ack, someone else is already processing this job.
                // we process but do not notify the job event handler.
                LoggerFactory.getLogger(EventUtil.class).info("Someone else is already processing job {}.", job);
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
    public static void finishedJob(Event job) {
        final JobStatusNotifier.NotifierContext ctx = getNotifierContext(job);
        if ( ctx != null ) {
            ctx.notifier.finishedJob(job, ctx.eventNodePath, false);
        }
    }

    /**
     * Notify a failed job.
     * @return <code>true</code> if the job has been rescheduled, <code>false</code> otherwise.
     * @throws IllegalArgumentException If the event is a job event but does not have a notifier context.
     */
    public static boolean rescheduleJob(Event job) {
        final JobStatusNotifier.NotifierContext ctx = getNotifierContext(job);
        if ( ctx != null ) {
           return ctx.notifier.finishedJob(job, ctx.eventNodePath, true);
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
            if ( !ctx.notifier.sendAcknowledge(job, ctx.eventNodePath) ) {
                // if we don't get an ack, someone else is already processing this job.
                // we process but do not notify the job event handler.
                LoggerFactory.getLogger(EventUtil.class).info("Someone else is already processing job {}.", job);
                notify = false;
            }
        }
        final boolean notifyResult = notify;

        final Runnable task = new Runnable() {

            /**
             * @see java.lang.Runnable#run()
             */
            public void run() {
                boolean result = false;
                try {
                    result = processor.process(job);
                } catch (Throwable t) {
                    LoggerFactory.getLogger(EventUtil.class).error("Unhandled error occured in job processor " + t.getMessage() + " while processing job " + job, t);
                    // we don't reschedule if an exception occurs
                    result = true;
                } finally {
                    if ( notifyResult ) {
                        if ( result ) {
                            EventUtil.finishedJob(job);
                        } else {
                            EventUtil.rescheduleJob(job);
                        }
                    }
                }
            }

        };
        // check if the job handler thread pool is available
        if ( JobEventHandler.JOB_THREAD_POOL != null ) {
            JobEventHandler.JOB_THREAD_POOL.execute(task);
        } else {
            // if we don't have a thread pool, we create the thread directly
            // (this should never happen for jobs, but is a safe fallback and
            // allows to call this method for other background processing.
            new Thread(task).start();
        }
    }

    /**
     * This is a private interface which is only public for import reasons.
     */
    public static interface JobStatusNotifier {

        String CONTEXT_PROPERTY_NAME = JobStatusNotifier.class.getName();

        public static final class NotifierContext {
            public final JobStatusNotifier notifier;
            public final String eventNodePath;

            public NotifierContext(JobStatusNotifier n, String path) {
                this.notifier = n;
                this.eventNodePath = path;
            }
        }

        /**
         * Send an acknowledge message that someone is processing the job.
         * @param job The job.
         * @param eventNodePath The storage node in the repository.
         * @return <code>true</code> if the ack is ok, <code>false</code> otherwise (e.g. if
         *   someone else already send an ack for this job.
         */
        boolean sendAcknowledge(Event job, String eventNodePath);

        /**
         * Notify that the job is finished.
         * If the job is not rescheduled, a return value of <code>false</code> indicates an error
         * during the processing. If the job should be rescheduled, <code>true</code> indicates
         * that the job could be rescheduled. If an error occurs or the number of retries is
         * exceeded, <code>false</code> will be returned.
         * @param job The job.
         * @param eventNodePath The storage node in the repository.
         * @param reschedule Should the event be rescheduled?
         * @return <code>true</code> if everything went fine, <code>false</code> otherwise.
         */
        boolean finishedJob(Event job, String eventNodePath, boolean reschedule);
    }

    /**
     * Improved toString method for an Event.
     * This method prints out the event topic and all of the properties.
     */
    public static String toString(final Event e) {
        if ( e == null ) {
            return "<null>";
        }
        final StringBuilder buffer = new StringBuilder(e.getClass().getName());
        buffer.append('(');
        buffer.append(e.hashCode());
        buffer.append(") [topic=");
        buffer.append(e.getTopic());
        buffer.append(", properties=");
        final String[] names = e.getPropertyNames();
        if ( names != null ) {
            for(int i=0;i<names.length;i++) {
                if ( i>0) {
                    buffer.append(",");
                }
                buffer.append(names[i]);
                buffer.append('=');
                buffer.append(e.getProperty(names[i]));
            }
        }
        buffer.append("]");
        return buffer.toString();
    }
}