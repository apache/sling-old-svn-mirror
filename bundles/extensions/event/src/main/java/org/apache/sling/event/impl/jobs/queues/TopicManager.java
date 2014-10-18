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
package org.apache.sling.event.impl.jobs.queues;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.impl.jobs.config.ConfigurationChangeListener;
import org.apache.sling.event.impl.jobs.config.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueInfo;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.NotificationConstants;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Topic manager
 *
 */
@Component(immediate=true)
@Service(value=EventHandler.class)
@Property(name=EventConstants.EVENT_TOPIC, value=NotificationConstants.TOPIC_JOB_ADDED)
public class TopicManager implements EventHandler, ConfigurationChangeListener {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private JobManagerConfiguration configuration;

    @Reference
    private QueueConfigurationManager queueConfigMgr;

    @Reference
    private QueueManager queueManager;

    @Reference
    private JobManager jobManager;

    /** Flag whether the manager is active or suspended. */
    private final AtomicBoolean isActive = new AtomicBoolean(false);

    /**
     * Activate this component.
     */
    @Activate
    protected void activate(final BundleContext bundleContext) {
        this.configuration.addListener(this);
    }

    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate() {
        this.configuration.removeListener(this);
    }

    /**
     * This method is called whenever the topology or queue configurations change.
     * @param caps The new topology capabilities or {@code null} if currently unknown.
     */
    @Override
    public void configurationChanged(final boolean active) {
        logger.debug("Topology changed {}", active);
        if ( active ) {
            final Set<String> topics = this.initialScan();
            final Map<QueueInfo, Set<String>> mapping = this.updateTopicMapping(topics);
            // start queues
            for(final Map.Entry<QueueInfo, Set<String>> entry : mapping.entrySet() ) {
                this.queueManager.start(entry.getKey(), entry.getValue());
            }
            this.isActive.set(true);
        } else {
            this.isActive.set(false);
            this.queueManager.restart();
        }
    }

    /**
     * Scan the resource tree for topics.
     */
    private Set<String> initialScan() {
        logger.debug("Scanning repository for existing topics...");
        final Set<String> topics = new HashSet<String>();

        final ResourceResolver resolver = this.configuration.createResourceResolver();
        try {
            final Resource baseResource = resolver.getResource(this.configuration.getLocalJobsPath());

            // sanity check - should never be null
            if ( baseResource != null ) {
                final Iterator<Resource> topicIter = baseResource.listChildren();
                while ( topicIter.hasNext() ) {
                    final Resource topicResource = topicIter.next();
                    final String topic = topicResource.getName().replace('.', '/');
                    logger.debug("Found topic {}", topic);
                    topics.add(topic);
                }
            }
        } finally {
            resolver.close();
        }
        return topics;
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event) {
        final String topic = (String)event.getProperty(NotificationConstants.NOTIFICATION_PROPERTY_JOB_TOPIC);
        if ( this.isActive.get() && topic != null ) {
            final QueueInfo info = this.queueConfigMgr.getQueueInfo(topic);
            this.queueManager.start(info, Collections.singleton(topic));
        }
    }

    /**
     * Get the latest mapping from queue name to topics
     */
    private Map<QueueInfo, Set<String>> updateTopicMapping(final Set<String> topics) {
        final Map<QueueInfo, Set<String>> mapping = new HashMap<QueueConfigurationManager.QueueInfo, Set<String>>();
        for(final String topic : topics) {
            final QueueInfo queueInfo = this.queueConfigMgr.getQueueInfo(topic);
            Set<String> queueTopics = mapping.get(queueInfo);
            if ( queueTopics == null ) {
                queueTopics = new HashSet<String>();
                mapping.put(queueInfo, queueTopics);
            }
            queueTopics.add(topic);
        }

        this.logger.debug("Established new topic mapping: {}", mapping);
        return mapping;
    }
}
