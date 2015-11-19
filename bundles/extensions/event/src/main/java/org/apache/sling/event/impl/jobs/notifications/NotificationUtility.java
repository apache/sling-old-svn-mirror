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
package org.apache.sling.event.impl.jobs.notifications;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.NotificationConstants;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

public abstract class NotificationUtility {

    /** Event property containing the time for job start and job finished events. */
    public static final String PROPERTY_TIME = ":time";

    /**
     * Helper method for sending the notification events.
     */
    public static void sendNotification(final EventAdmin eventAdmin,
            final String eventTopic,
            final Job job,
            final Long time) {
        if ( eventAdmin != null ) {
            // create new copy of job object
            final Job jobCopy = new JobImpl(job.getTopic(), job.getId(), ((JobImpl)job).getProperties());
            sendNotificationInternal(eventAdmin, eventTopic, jobCopy, time);
        }
    }

    /**
     * Helper method for sending the notification events.
     */
    private static void sendNotificationInternal(final EventAdmin eventAdmin,
            final String eventTopic,
            final Job job,
            final Long time) {
        final Dictionary<String, Object> eventProps = new Hashtable<String, Object>();
        // add basic job properties
        eventProps.put(NotificationConstants.NOTIFICATION_PROPERTY_JOB_ID, job.getId());
        eventProps.put(NotificationConstants.NOTIFICATION_PROPERTY_JOB_TOPIC, job.getTopic());
        // copy payload
        for(final String name : job.getPropertyNames()) {
            eventProps.put(name, job.getProperty(name));
        }
        // remove async handler
        eventProps.remove(JobConsumer.PROPERTY_JOB_ASYNC_HANDLER);
        // add timestamp
        eventProps.put(EventConstants.TIMESTAMP, System.currentTimeMillis());
        // add internal time information
        if ( time != null ) {
            eventProps.put(PROPERTY_TIME, time);
        }
        eventAdmin.postEvent(new Event(eventTopic, eventProps));
    }

}
