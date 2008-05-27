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
import org.osgi.service.event.Event;
import org.slf4j.Logger;
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

    /** The job topic property. */
    public static final String PROPERTY_JOB_TOPIC = "event.job.topic";

    /** The property for the unique event id. Value is of type String. */
    public static final String PROPERTY_JOB_ID = "event.job.id";

    /** The property to set if a job can be run parallel to any other job. */
    public static final String PROPERTY_JOB_PARALLEL = "event.job.parallel";

    /** The property to track the retry count for jobs. Value is of type Integer. */
    public static final String PROPERTY_JOB_RETRY_COUNT = "event.job.retrycount";

    /** The property to for setting the maximum number of retries. Value is of type Integer. */
    public static final String PROPERTY_JOB_RETRIES = "event.job.retries";

    /** The property to set a retry delay. Value is of type Long and specifies milliseconds. */
    public static final String PROPERTY_JOB_RETRY_DELAY = "event.job.retrydelay";

    /** The topic for jobs. */
    public static final String TOPIC_JOB = "org/apache/sling/event/job";

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

    private final static Logger logger = LoggerFactory.getLogger(EventUtil.class);

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
        return getApplicationId(event) == null;
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
     * Notify a finished job.
     */
    public static void finishedJob(Event job) {
        // check if this is a job event
        if ( !isJobEvent(job) ) {
            return;
        }
        final JobStatusNotifier.NotifierContext ctx = (NotifierContext) job.getProperty(JobStatusNotifier.CONTEXT_PROPERTY_NAME);
        if ( ctx == null ) {
            throw new NullPointerException("JobStatusNotifier context is not available in event properties.");
        }
        ctx.notifier.finishedJob(job, ctx.eventNodePath, false);
    }

    /**
     * Notify a failed job.
     * @return <code>true</code> if the job has been rescheduled, <code>false</code> otherwise.
     */
    public static boolean rescheduleJob(Event job) {
        // check if this is a job event
        if ( !isJobEvent(job) ) {
            return false;
        }
        final JobStatusNotifier.NotifierContext ctx = (NotifierContext) job.getProperty(JobStatusNotifier.CONTEXT_PROPERTY_NAME);
        if ( ctx == null ) {
            throw new NullPointerException("JobStatusNotifier context is not available in event properties.");
        }
        return ctx.notifier.finishedJob(job, ctx.eventNodePath, true);
    }

    /**
     * Process a job in the background and notify its success.
     */
    public static void processJob(final Event job, final JobProcessor processor) {
        final Runnable task = new Runnable() {

            /**
             * @see java.lang.Runnable#run()
             */
            public void run() {
                boolean result = false;
                try {
                    result = processor.process(job);
                } catch (Throwable t) {
                    logger.error("Unhandled error occured in job processor " + t.getMessage(), t);
                    // we don't reschedule if an exception occurs
                    result = true;
                } finally {
                    if ( result ) {
                        EventUtil.finishedJob(job);
                    } else {
                        EventUtil.rescheduleJob(job);
                    }
                }
            }

        };
        final JobStatusNotifier.NotifierContext ctx = (NotifierContext) job.getProperty(JobStatusNotifier.CONTEXT_PROPERTY_NAME);
        if ( ctx != null ) {
            ctx.notifier.execute(task);
        } else {
            // if we don't have a job status notifier, we create the thread directly
            // (this should never happen but is a safe fallback)
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
         * Notify that the job is finished.
         * If the job is not rescheduled, a return value of <code>false</code> indicates an error
         * during the processing. If the job should be rescheduled, <code>true</code> indicates
         * that the job could be rescheduled. If an error occurs or the number of retries is
         * exceeded, <code>false</code> will be returned.
         * @param job The job.
         * @param eventNodePath The storage node in the repository.
         * @param lockToken The lock token locking the node.
         * @param reschedule Should the event be rescheduled?
         * @return <code>true</code> if everything went fine, <code>false</code> otherwise.
         */
        boolean finishedJob(Event job, String eventNodePath, boolean reschedule);

        /**
         * Execute the job in the background
         */
        void execute(Runnable job);
    }
}
