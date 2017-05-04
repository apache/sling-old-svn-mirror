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
import java.util.List;

import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.NotificationConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This component receives resource added events and sends a job
 * created event.
 */
@Component(service = {})
public class NewJobSender implements ResourceChangeListener, ExternalResourceChangeListener {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The job manager configuration. */
    @Reference
    private JobManagerConfiguration configuration;

    /** The event admin. */
    @Reference
    private EventAdmin eventAdmin;

    /** Service registration for the event handler. */
    private volatile ServiceRegistration<ResourceChangeListener> listenerRegistration;

    /**
     * Activate this component.
     * Register an event handler.
     */
    @Activate
    protected void activate(final BundleContext bundleContext) {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_DESCRIPTION, "Apache Sling Job Topic Manager Event Handler");
        properties.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        properties.put(ResourceChangeListener.CHANGES, ChangeType.ADDED.toString());
        properties.put(ResourceChangeListener.PATHS, this.configuration.getLocalJobsPath());

        this.listenerRegistration = bundleContext.registerService(ResourceChangeListener.class, this, properties);
    }

    /**
     * Deactivate this component.
     * Unregister the event handler.
     */
    @Deactivate
    protected void deactivate() {
        if ( this.listenerRegistration != null ) {
            this.listenerRegistration.unregister();
            this.listenerRegistration = null;
        }
    }

    @Override
	public void onChange(final List<ResourceChange> resourceChanges) {
    	for(final ResourceChange resourceChange : resourceChanges) {
    		logger.debug("Received event {}", resourceChange);

    		final String path = resourceChange.getPath();

    		final int topicStart = this.configuration.getLocalJobsPath().length() + 1;
    		final int topicEnd = path.indexOf('/', topicStart);
    		if ( topicEnd != -1 ) {
    			final String topic = path.substring(topicStart, topicEnd).replace('.', '/');
                final String jobId = path.substring(topicEnd + 1);

                if ( path.indexOf("_", topicEnd + 1) != -1 ) {
                	// only job id and topic are guaranteed
                	final Dictionary<String, Object> properties = new Hashtable<>();
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
