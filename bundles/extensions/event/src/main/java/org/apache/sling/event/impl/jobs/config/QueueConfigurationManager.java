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
package org.apache.sling.event.impl.jobs.config;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.sling.event.impl.jobs.JobEvent;
import org.apache.sling.event.jobs.JobUtil;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;


/**
 * An event handler for special job events.
 *
 * We schedule this event handler to run in the background and clean up
 * obsolete events.
 */
@Component
@Services({
    @Service(value=QueueConfigurationManager.class)
})
public class QueueConfigurationManager {

    /** Configurations - ordered by service ranking. */
    private volatile InternalQueueConfiguration[] orderedConfigs = new InternalQueueConfiguration[0];

    /** Service tracker for the configurations. */
    private ServiceTracker configTracker;

    /** Tracker count to detect changes. */
    private volatile int lastTrackerCount = -1;


    /**
     * Activate this component.
     * Create the service tracker and start it.
     */
    @Activate
    protected void activate(final BundleContext bundleContext) {
        this.configTracker = new ServiceTracker(bundleContext,
                InternalQueueConfiguration.class.getName(), null);
        this.configTracker.open();
    }

    /**
     * Deactivate this component.
     * Stop the service tracker.
     */
    @Deactivate
    protected void deactivate() {
        if ( this.configTracker != null ) {
            this.configTracker.close();
            this.configTracker = null;
        }
    }

    /**
     * Return all configurations.
     */
    public InternalQueueConfiguration[] getConfigurations() {
        final int count = this.configTracker.getTrackingCount();
        InternalQueueConfiguration[] configurations = this.orderedConfigs;
        if ( this.lastTrackerCount < count ) {
            synchronized ( this ) {
                configurations = this.orderedConfigs;
                if ( this.lastTrackerCount < count ) {
                    final Object[] trackedConfigs = this.configTracker.getServices();
                    if ( trackedConfigs == null || trackedConfigs.length == 0 ) {
                        configurations = new InternalQueueConfiguration[0];
                    } else {
                        configurations = new InternalQueueConfiguration[trackedConfigs.length];
                        int i = 0;
                        for(final Object entry : trackedConfigs) {
                            final InternalQueueConfiguration config = (InternalQueueConfiguration)entry;
                            configurations[i] = config;
                            i++;
                        }
                    }
                    this.orderedConfigs = configurations;
                    this.lastTrackerCount = count;
                }
            }
        }
        return configurations;
    }

    /**
     * Find the queue configuration for the job.
     * This method only returns a configuration if one matches.
     */
    public InternalQueueConfiguration getQueueConfiguration(final JobEvent event) {
        final InternalQueueConfiguration[] configurations = this.getConfigurations();
        final String queueName = (String)event.event.getProperty(JobUtil.PROPERTY_JOB_QUEUE_NAME);
        for(final InternalQueueConfiguration config : configurations) {
            if ( config.isValid() ) {
                // check for queue name first
                if ( queueName != null && queueName.equals(config.getName()) ) {
                    event.queueName = queueName;
                    return config;
                }
                if ( config.match(event) ) {
                    return config;
                }
            }
        }
        return null;
    }
}
