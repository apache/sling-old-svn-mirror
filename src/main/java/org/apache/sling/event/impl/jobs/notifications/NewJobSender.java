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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.impl.jobs.Utility;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.impl.support.ResourceHelper;
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
        final String rt = (String) event.getProperty(SlingConstants.PROPERTY_RESOURCE_TYPE);
        if ( ResourceHelper.RESOURCE_TYPE_JOB.equals(rt) && this.configuration.isLocalJob(path) ) {
            // read the job
            final ResourceResolver resolver = this.configuration.createResourceResolver();
            try {
                final Resource rsrc = resolver.getResource(path);
                if ( rsrc != null ) {
                    final Job job = Utility.readJob(this.logger, rsrc);
                    if ( job != null ) {
                        logger.debug("Sending job added event for {}", job);
                        NotificationUtility.sendNotification(this.eventAdmin, NotificationConstants.TOPIC_JOB_ADDED, job, null);
                    }
                }
            } finally {
                resolver.close();
            }
        }
    }

}
