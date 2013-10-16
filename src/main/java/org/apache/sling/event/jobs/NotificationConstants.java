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


/**
 * This class contains constants for event notifications
 * @since 1.3
 */
public abstract class NotificationConstants {

    /**
     * Asynchronous notification event when a job is started.
     * The property {@link #NOTIFICATION_PROPERTY_JOB_TOPIC} contains the job topic,
     * the property {@link #NOTIFICATION_PROPERTY_JOB_ID} contains the unique job id.
     * The time stamp of the event (as a Long) is available from the property
     * {@link org.osgi.service.event.EventConstants#TIMESTAMP}.
     * The payload of the job is available as additional job specific properties.
     */
    public static final String TOPIC_JOB_STARTED = "org/apache/sling/event/notification/job/START";

    /**
     * Asynchronous notification event when a job is finished.
     * The property {@link #NOTIFICATION_PROPERTY_JOB_TOPIC} contains the job topic,
     * the property {@link #NOTIFICATION_PROPERTY_JOB_ID} contains the unique job id.
     * The time stamp of the event (as a Long) is available from the property
     * {@link org.osgi.service.event.EventConstants#TIMESTAMP}.
     * The payload of the job is available as additional job specific properties.
     */
    public static final String TOPIC_JOB_FINISHED = "org/apache/sling/event/notification/job/FINISHED";

    /**
     * Asynchronous notification event when a job failed.
     * If a job execution fails, it is rescheduled for another try.
     * The property {@link #NOTIFICATION_PROPERTY_JOB_TOPIC} contains the job topic,
     * the property {@link #NOTIFICATION_PROPERTY_JOB_ID} contains the unique job id.
     * The time stamp of the event (as a Long) is available from the property
     * {@link org.osgi.service.event.EventConstants#TIMESTAMP}.
     * The payload of the job is available as additional job specific properties.
     */
    public static final String TOPIC_JOB_FAILED = "org/apache/sling/event/notification/job/FAILED";

    /**
     * Asynchronous notification event when a job is cancelled.
     * If a job execution is cancelled it is not rescheduled.
     * The property {@link #NOTIFICATION_PROPERTY_JOB_TOPIC} contains the job topic,
     * the property {@link #NOTIFICATION_PROPERTY_JOB_ID} contains the unique job id.
     * The time stamp of the event (as a Long) is available from the property
     * {@link org.osgi.service.event.EventConstants#TIMESTAMP}.
     * The payload of the job is available as additional job specific properties.
     */
    public static final String TOPIC_JOB_CANCELLED = "org/apache/sling/event/notification/job/CANCELLED";

    /**
     * Asynchronous notification event when a job is permanently removed.
     * The property {@link #NOTIFICATION_PROPERTY_JOB_TOPIC} contains the job topic,
     * the property {@link #NOTIFICATION_PROPERTY_JOB_ID} contains the unique job id.
     * The payload of the job is available as additional job specific properties.
     */
    public static final String TOPIC_JOB_REMOVED = "org/apache/sling/event/notification/job/REMOVED";

    /**
     * Property containing the job topic. Value is of type String.
     * @see Job#getTopic()
     */
    public static final String NOTIFICATION_PROPERTY_JOB_TOPIC = "event.job.topic";

    /**
     * Property containing the unique job ID. Value is of type String.
     * @see Job#getId()
     */
    public static final String NOTIFICATION_PROPERTY_JOB_ID = "slingevent:eventId";

   private NotificationConstants() {
        // avoid instantiation
    }
}