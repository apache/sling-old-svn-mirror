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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.NotificationConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This component receives resource added events and sends a job
 * created event.
 */
@Component
public class NewJobSender implements EventHandler {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The job manager configuration. */
    @Reference
    private JobManagerConfiguration configuration;

    /** The event admin. */
    @Reference
    private EventAdmin eventAdmin;

    /** Service registration for the event handler. */
    private volatile ServiceRegistration eventHandlerRegistration;

    /**
     * Activate this component.
     * Register an event handler.
     */
    @Activate
    protected void activate(final BundleContext bundleContext) {
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Job Topic Manager Event Handler");
        properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        properties.put(EventConstants.EVENT_TOPIC, SlingConstants.TOPIC_RESOURCE_ADDED);
        properties.put(EventConstants.EVENT_FILTER,
                "(" + SlingConstants.PROPERTY_PATH + "=" +
                      this.configuration.getLocalJobsPath() + "/*)");
        this.eventHandlerRegistration = bundleContext.registerService(EventHandler.class.getName(), this, properties);
    }

    /**
     * Deactivate this component.
     * Unregister the event handler.
     */
    @Deactivate
    protected void deactivate() {
        if ( this.eventHandlerRegistration != null ) {
            this.eventHandlerRegistration.unregister();
            this.eventHandlerRegistration = null;
        }
    }

    @Override
    public void handleEvent(final Event event) {
        logger.debug("Received event {}", event);
        final String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
        if ( this.configuration.isLocalJob(path) ) {
            // get topic and id from path
            final int topicStart = this.configuration.getLocalJobsPath().length() + 1;
            final int topicEnd = path.indexOf('/', topicStart);
            if ( topicEnd != -1 ) {
                final String topic = path.substring(topicStart, topicEnd).replace('.', '/');
                final String jobId = path.substring(topicEnd + 1);

                if ( path.indexOf("_", topicEnd + 1) != -1 ) {
                    // only job id and topic are guaranteed
                    final Dictionary<String, Object> properties = new Hashtable<String, Object>();
                    properties.put(NotificationConstants.NOTIFICATION_PROPERTY_JOB_ID, jobId);
                    properties.put(NotificationConstants.NOTIFICATION_PROPERTY_JOB_TOPIC, topic);

                    // we also set internally the queue name
                    final String queueName = this.configuration.getQueueConfigurationManager().getQueueInfo(topic).queueName;
                    properties.put(Job.PROPERTY_JOB_QUEUE_NAME, queueName);

                    final Event jobEvent = new Event(NotificationConstants.TOPIC_JOB_ADDED, properties);
                    // as this is send within handling an event, we do sync call
                    this.eventAdmin.sendEvent(jobEvent);
                }
            }
        }
    }

}
