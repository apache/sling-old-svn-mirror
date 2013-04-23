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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The queue manager manages queue configurations.
 */
@Component
@Service(value=QueueConfigurationManager.class)
public class QueueConfigurationManager {

    /** Configurations - ordered by service ranking. */
    private volatile InternalQueueConfiguration[] orderedConfigs = new InternalQueueConfiguration[0];

    /** Service tracker for the configurations. */
    private ServiceTracker configTracker;

    /** Tracker count to detect changes. */
    private volatile int lastTrackerCount = -1;

    @Reference
    private MainQueueConfiguration mainQueueConfiguration;

    /**
     * Activate this component.
     * Create the service tracker and start it.
     */
    @Activate
    protected void activate(final BundleContext bundleContext)
    throws LoginException, PersistenceException {
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
                        final List<InternalQueueConfiguration> configs = new ArrayList<InternalQueueConfiguration>();
                        for(final Object entry : trackedConfigs) {
                            final InternalQueueConfiguration config = (InternalQueueConfiguration)entry;
                            configs.add(config);
                        }
                        Collections.sort(configs);
                        configurations = configs.toArray(new InternalQueueConfiguration[configs.size()]);
                    }
                    this.orderedConfigs = configurations;
                    this.lastTrackerCount = count;
                }
            }
        }
        return configurations;
    }

    public InternalQueueConfiguration getMainQueueConfiguration() {
        return this.mainQueueConfiguration.getMainConfiguration();
    }

    public static final class QueueInfo {
        public InternalQueueConfiguration queueConfiguration;
        public String queueName;
        public String targetId;
    }

    /**
     * Find the queue configuration for the job.
     * This method only returns a configuration if one matches.
     */
    public QueueInfo getQueueInfo(final String topic) {
        final InternalQueueConfiguration[] configurations = this.getConfigurations();
        for(final InternalQueueConfiguration config : configurations) {
            if ( config.isValid() ) {
                final String qn = config.match(topic);
                if ( qn != null ) {
                    final QueueInfo result = new QueueInfo();
                    result.queueConfiguration = config;
                    result.queueName = ResourceHelper.filterName(qn);

                    return result;
                }
            }
        }
        final QueueInfo result = new QueueInfo();
        result.queueConfiguration = this.mainQueueConfiguration.getMainConfiguration();
        result.queueName = result.queueConfiguration.getName();

        return result;
    }

    public int getChangeCount() {
        return this.configTracker.getTrackingCount();
    }
}
