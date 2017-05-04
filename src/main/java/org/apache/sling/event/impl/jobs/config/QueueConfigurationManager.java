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

import org.apache.sling.event.impl.support.ResourceHelper;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;


/**
 * The queue manager manages queue configurations.
 */
@Component(service = QueueConfigurationManager.class,
property = {
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
})
public class QueueConfigurationManager {

    /** Empty configuration array. */
    private static final InternalQueueConfiguration[] EMPTY_CONFIGS = new InternalQueueConfiguration[0];

    /** Configurations - ordered by service ranking. */
    private volatile InternalQueueConfiguration[] orderedConfigs = EMPTY_CONFIGS;

    /** All configurations. */
    private final List<InternalQueueConfiguration> configurations = new ArrayList<>();

    /** The main queue configuration. */
    @Reference
    private MainQueueConfiguration mainQueueConfiguration;

    /**
     * Add a new queue configuration.
     * @param config A new queue configuration.
     */
    @Reference(service=InternalQueueConfiguration.class, policy=ReferencePolicy.DYNAMIC,
            cardinality=ReferenceCardinality.MULTIPLE,
            updated="updateConfig")
    protected void bindConfig(final InternalQueueConfiguration config) {
        synchronized ( configurations ) {
            configurations.add(config);
            this.createConfigurationCache();
        }
    }

    /**
     * Remove a queue configuration.
     * @param config The queue configuration.
     */
    protected void unbindConfig(final InternalQueueConfiguration config) {
        synchronized ( configurations ) {
            configurations.remove(config);
            this.createConfigurationCache();
        }
    }

    /**
     * Update a queue configuration.
     * @param config The queue configuration.
     */
    protected void updateConfig(final InternalQueueConfiguration config) {
        // InternalQueueConfiguration does not implement modified atm,
        // but we handle this case anyway
        synchronized ( configurations ) {
            this.createConfigurationCache();
        }
    }

    /**
     * Create the configurations cache used by clients.
     */
    private void createConfigurationCache() {
        if ( this.configurations.isEmpty() ) {
            this.orderedConfigs = EMPTY_CONFIGS;
        } else {
            Collections.sort(configurations);
            orderedConfigs = configurations.toArray(new InternalQueueConfiguration[configurations.size()]);
        }
    }

    /**
     * Return all configurations.
     * @return An array with all queue configurations except the main queue. Array might be empty.
     */
    public InternalQueueConfiguration[] getConfigurations() {
        return orderedConfigs;
    }

    /**
     * Get the configuration for the main queue.
     * @return The configuration for the main queue.
     */
    public InternalQueueConfiguration getMainQueueConfiguration() {
        return this.mainQueueConfiguration.getMainConfiguration();
    }

    public static final class QueueInfo {
        public InternalQueueConfiguration queueConfiguration;
        public String queueName;
        public String targetId;

        @Override
        public String toString() {
            return queueName;
        }

        @Override
        public int hashCode() {
            return queueName.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if ( obj == this ) {
                return true;
            }
            if ( obj instanceof QueueInfo ) {
                return ((QueueInfo)obj).queueName.equals(this.queueName);
            }
            return false;
        }
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
}
