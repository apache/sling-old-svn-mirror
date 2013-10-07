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

import java.util.Calendar;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.jobs.JobUtil;
import org.osgi.service.event.Event;

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

    /** The job topic property.
     * @deprecated Use {@link JobUtil#PROPERTY_JOB_TOPIC}
     */
    @Deprecated
    public static final String PROPERTY_JOB_TOPIC = "event.job.topic";

    /** The property for the unique event id. Value is of type String (This is optional).
     * @deprecated Use {@link JobUtil#PROPERTY_JOB_NAME}
     * */
    @Deprecated
    public static final String PROPERTY_JOB_ID = "event.job.id";

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
     * @deprecated Use {@link JobUtil#PROPERTY_JOB_PARALLEL}
     */
    @Deprecated
    public static final String PROPERTY_JOB_PARALLEL = "event.job.parallel";

    /**
     * The property to set if a job should only be run on the same app it has been created.
     * @deprecated Use {@link JobUtil#PROPERTY_JOB_RUN_LOCAL}
     */
    @Deprecated
    public static final String PROPERTY_JOB_RUN_LOCAL = "event.job.run.local";

    /**
     * The property to track the retry count for jobs. Value is of type Integer.
     * @deprecated Use {@link JobUtil#PROPERTY_JOB_RETRY_COUNT}
     */
    @Deprecated
    public static final String PROPERTY_JOB_RETRY_COUNT = "event.job.retrycount";

    /**
     * The property for setting the maximum number of retries. Value is of type Integer.
     * @deprecated Use {@link JobUtil#PROPERTY_JOB_RETRIES}
     */
    @Deprecated
    public static final String PROPERTY_JOB_RETRIES = "event.job.retries";

    /**
     * The property to set a retry delay. Value is of type Long and specifies milliseconds.
     * @deprecated Use {@link JobUtil#PROPERTY_JOB_RETRY_DELAY}
     */
    @Deprecated
    public static final String PROPERTY_JOB_RETRY_DELAY = "event.job.retrydelay";

    /** The property to set to put the jobs into a separate job queue. This property
     * specifies the name of the job queue. If the job queue does not exists yet
     * a new queue is created.
     * If a ordered job queue is used, the jobs are never executed in parallel
     * from this queue! For non ordered queues the {@link JobUtil#PROPERTY_JOB_PARALLEL}
     * with an integer value higher than 1 can be used to specify the maximum number
     * of parallel jobs for this queue.
     * @deprecated Use {@link JobUtil#PROPERTY_JOB_QUEUE_NAME}
     */
    @Deprecated
    public static final String PROPERTY_JOB_QUEUE_NAME = "event.job.queuename";

    /**
     * If this property is set with any value, the queue processes the jobs in the same
     * order as they have arrived.
     * This property has only an effect if {@link #PROPERTY_JOB_QUEUE_NAME} is specified
     * and the job queue has not been started yet.
     * @deprecated Use {@link JobUtil#PROPERTY_JOB_QUEUE_ORDERED}
     */
    @Deprecated
    public static final String PROPERTY_JOB_QUEUE_ORDERED = "event.job.queueordered";

    /**
     * This property allows to override the priority for the thread used to start this job.
     * The property is evaluated by the {@link JobUtil#processJob(Event, org.apache.sling.event.jobs.JobProcessor)} method.
     * If another way of executing the job is used, it is up to the client to ensure
     * the job priority.
     * For possible values see {@link JobPriority}.
     * @since 2.4
     * @deprecated Use {@link JobUtil#PROPERTY_JOB_PRIORITY}
     */
    @Deprecated
    public static final String PROPERTY_JOB_PRIORITY = "event.job.priority";

    /**
     * The priority for jobs.
     * @since 2.4
     * @deprecated Use {@link JobUtil.JobPriority}
     */
    public enum JobPriority {
        NORM,
        MIN,
        MAX
    }

    /**
     * The topic for jobs.
     * @deprecated Use {@link JobUtil#TOPIC_JOB}
     */
    @Deprecated
    public static final String TOPIC_JOB = "org/apache/sling/event/job";

    /**
     * Timed Events
     */

    /**
     * The topic for timed events.
     * @deprecated Use scheduled jobs instead
     */
    @Deprecated
    public static final String TOPIC_TIMED_EVENT = "org/apache/sling/event/timed";

    /**
     * The real topic of the event.
     * @deprecated Use scheduled jobs instead
     */
    @Deprecated
    public static final String PROPERTY_TIMED_EVENT_TOPIC = "event.topic.timed";

    /**
     * The property for the unique event id.
     * @deprecated Use scheduled jobs instead
     */
    @Deprecated
    public static final String PROPERTY_TIMED_EVENT_ID = "event.timed.id";

    /**
     * The scheduler cron expression for the timed event. Type must be String.
     * @deprecated Use scheduled jobs instead
     */
    @Deprecated
    public static final String PROPERTY_TIMED_EVENT_SCHEDULE = "event.timed.scheduler";

    /**
     * The period in seconds for the timed event. Type must be Long.
     * @deprecated Use scheduled jobs instead
     */
    @Deprecated
    public static final String PROPERTY_TIMED_EVENT_PERIOD = "event.timed.period";

    /**
     * The date for the timed event. Type must be Date.
     * @deprecated Use scheduled jobs instead
     */
    @Deprecated
    public static final String PROPERTY_TIMED_EVENT_DATE = "event.timed.date";

    /**
     * Notification events for jobs.
     */

    /** Asynchronous notification event when a job is started.
     * The property {@link #PROPERTY_NOTIFICATION_JOB} contains the job event and the
     * property {@link org.osgi.service.event.EventConstants#TIMESTAMP} contains the
     * timestamp of the event (as a Long).
     * @since 2.2
     * @deprecated Use {@link JobUtil#TOPIC_JOB_STARTED}
     */
    @Deprecated
    public static final String TOPIC_JOB_STARTED = "org/apache/sling/event/notification/job/START";

    /** Asynchronous notification event when a job is finished.
     * The property {@link #PROPERTY_NOTIFICATION_JOB} contains the job event and the
     * property {@link org.osgi.service.event.EventConstants#TIMESTAMP} contains the
     * timestamp of the event (as a Long).
     * @since 2.2
     * @deprecated Use {@link JobUtil#TOPIC_JOB_FINISHED}
     */
    @Deprecated
    public static final String TOPIC_JOB_FINISHED = "org/apache/sling/event/notification/job/FINISHED";

    /** Asynchronous notification event when a job failed.
     * If a job execution fails, it is rescheduled for another try.
     * The property {@link #PROPERTY_NOTIFICATION_JOB} contains the job event and the
     * property {@link org.osgi.service.event.EventConstants#TIMESTAMP} contains the
     * timestamp of the event (as a Long).
     * @since 2.2
     * @deprecated Use {@link JobUtil#TOPIC_JOB_FAILED}
     */
    @Deprecated
    public static final String TOPIC_JOB_FAILED = "org/apache/sling/event/notification/job/FAILED";

    /** Asynchronous notification event when a job is cancelled.
     * If a job execution is cancelled it is not rescheduled.
     * The property {@link #PROPERTY_NOTIFICATION_JOB} contains the job event and the
     * property {@link org.osgi.service.event.EventConstants#TIMESTAMP} contains the
     * timestamp of the event (as a Long).
     * @since 2.2
     * @deprecated Use {@link JobUtil#TOPIC_JOB_CANCELLED}
     */
    @Deprecated
    public static final String TOPIC_JOB_CANCELLED = "org/apache/sling/event/notification/job/CANCELLED";

    /** Property containing the job event.
     * @since 2.2
     * @deprecated Use {@link JobUtil#PROPERTY_NOTIFICATION_JOB}
     */
    @Deprecated
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
    public static Event createDistributableEvent(final String topic,
                                                 final Dictionary<String, Object> properties) {
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
    public static boolean shouldDistribute(final Event event) {
        return event.getProperty(PROPERTY_DISTRIBUTE) != null;
    }

    /**
     * Is this a local event?
     * @param event
     * @return <code>true</code> if this is a local event
     */
    public static boolean isLocal(final Event event) {
        final String appId = getApplicationId(event);
        return appId == null || appId.equals(Environment.APPLICATION_ID);
    }

    /**
     * Return the application id the event was created at.
     * @param event
     * @return The application id or null if the event has been created locally.
     */
    public static String getApplicationId(final Event event) {
        return (String)event.getProperty(PROPERTY_APPLICATION);
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
                final Object value = e.getProperty(names[i]);
                // the toString() method of Calendar is very verbose
                // therefore we do a toString for these objects based
                // on a date
                if ( value instanceof Calendar ) {
                    buffer.append(value.getClass().getName());
                    buffer.append('(');
                    buffer.append(((Calendar)value).getTime());
                    buffer.append(')');
                } else {
                    buffer.append(value);
                }
            }
        }
        buffer.append("]");
        return buffer.toString();
    }

    /**
     * Is this a job event?
     * This method checks if the event contains the {@link #PROPERTY_JOB_TOPIC}
     * property.
     * @param event The event to check.
     * @return <code>true></code> if this is a job event.
     * @deprecated Use {@link JobUtil#isJobEvent(Event)}
     */
    @Deprecated
    public static boolean isJobEvent(final Event event) {
        return JobUtil.isJobEvent(event);
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
     * @deprecated Use {@link JobUtil#acknowledgeJob(Event)}
     */
    @Deprecated
    public static boolean acknowledgeJob(final Event job) {
        return JobUtil.acknowledgeJob(job);
    }

    /**
     * Notify a finished job.
     * @throws IllegalArgumentException If the event is a job event but does not have a notifier context.
     * @deprecated Use {link {@link JobUtil#finishedJob(Event)}
     */
    @Deprecated
    public static void finishedJob(final Event job) {
        JobUtil.finishedJob(job);
    }

    /**
     * Notify a failed job.
     * @return <code>true</code> if the job has been rescheduled, <code>false</code> otherwise.
     * @throws IllegalArgumentException If the event is a job event but does not have a notifier context.
     * @deprecated Use {@link JobUtil#rescheduleJob(Event)}
     */
    @Deprecated
    public static boolean rescheduleJob(final Event job) {
        return JobUtil.rescheduleJob(job);
    }

    /**
     * Process a job in the background and notify its success.
     * This method also sends an acknowledge message to the job event handler.
     * @throws IllegalArgumentException If the event is a job event but does not have a notifier context.
     * @deprecated Use {@link JobUtil#processJob(Event, org.apache.sling.event.jobs.JobProcessor)}
     */
    @Deprecated
    public static void processJob(final Event job, final JobProcessor processor) {
        JobUtil.processJob(job, processor);
    }
}