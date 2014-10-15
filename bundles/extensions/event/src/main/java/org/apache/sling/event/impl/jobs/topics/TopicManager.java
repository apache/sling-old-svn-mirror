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
package org.apache.sling.event.impl.jobs.topics;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.impl.jobs.JobHandler;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.JobManagerConfiguration;
import org.apache.sling.event.impl.jobs.JobManagerImpl;
import org.apache.sling.event.impl.jobs.TestLogger;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager;
import org.apache.sling.event.impl.jobs.config.QueueConfigurationManager.QueueInfo;
import org.apache.sling.event.impl.jobs.queues.QueueManager;
import org.apache.sling.event.impl.jobs.topology.TopologyAware;
import org.apache.sling.event.impl.jobs.topology.TopologyCapabilities;
import org.apache.sling.event.impl.jobs.topology.TopologyHandler;
import org.apache.sling.event.impl.support.BatchResourceRemover;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Topic manager
 *
 * TODO - Check syncing of take/update/stop. This might not be 100% correct yet.
 */
@Component
public class TopicManager implements EventHandler, TopologyAware {

    /** Logger. */
    private final Logger logger = new TestLogger(LoggerFactory.getLogger(this.getClass()));

    @Reference
    private JobManagerConfiguration configuration;

    @Reference
    private QueueConfigurationManager queueConfigMgr;

    @Reference
    private TopologyHandler topologyHandler;

    @Reference
    private QueueManager queueManager;

    @Reference
    private JobManager jobManager;

    /** Queue configuration counter to track for changes. */
    private volatile int queueConfigCount = -1;

    /** A set of all topics. Access needs synchronization. */
    private final Set<String> topics = new TreeSet<String>();

    /** Service registration for the event handler. */
    private volatile ServiceRegistration eventHandlerRegistration;

    /** Marker if a new topic has been added. */
    private final AtomicBoolean topicsChanged = new AtomicBoolean(false);

    /** The mapping from a queue name to a cache. */
    private Map<String, QueueJobCache> queueJobCaches = Collections.emptyMap();

    private final AtomicBoolean isActive = new AtomicBoolean(false);

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
        this.topologyHandler.addListener(this);
    }

    /**
     * Deactivate this component.
     * Unregister the event handler.
     */
    @Deactivate
    protected void deactivate() {
        this.topologyHandler.removeListener(this);
        if ( this.eventHandlerRegistration != null ) {
            this.eventHandlerRegistration.unregister();
            this.eventHandlerRegistration = null;
        }
    }

    /**
     * Scan the resource tree for topics.
     */
    private void initialScan() {
        logger.debug("Scanning repository for existing topics...");
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
                    synchronized ( topics ) {
                        topics.add(topic);
                    }
                    topicsChanged.set(true);
                }
            }
        } finally {
            resolver.close();
        }
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(final Event event) {
        final String path = (String)event.getProperty(SlingConstants.PROPERTY_PATH);
        if ( this.configuration.isLocalJob(path) ) {
            final int topicStart = this.configuration.getLocalJobsPath().length() + 1;
            final int topicEnd = path.indexOf("/", topicStart);
            final String topic;
            if ( topicEnd == -1 ) {
                topic = path.substring(topicStart).replace('.', '/');
            } else {
                topic = path.substring(topicStart, topicEnd).replace('.', '/');
            }
            boolean changed = false;
            synchronized ( topics ) {
                final int len = topics.size();
                topics.add(topic);
                changed = topics.size() > len;
            }
            final QueueInfo info = this.queueConfigMgr.getQueueInfo(topic);
            if ( changed ) {
                logger.debug("Adding new topic {}", topic);
                topicsChanged.set(true);
                logger.info("Starting queue {}", info.queueName);

                this.queueManager.start(this, info);
            } else {
                final QueueJobCache cache = this.queueJobCaches.get(info.queueName);
                if ( cache != null ) {
                    cache.handleNewJob(topic);
                    final Object lock = this.queueLocks.get(info.queueName);
                    if ( lock != null ) {
                        synchronized ( lock ) {
                            lock.notify();
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the latest mapping from queue name to topics
     * @return A map from queue names to topics
     */
    private Map<String, QueueJobCache> updateConfiguration() {
        if ( this.queueConfigCount < this.queueConfigMgr.getChangeCount() || this.topicsChanged.get() ) {

            final Map<String, Set<String>> mapping = new HashMap<String, Set<String>>();

            synchronized ( this.topics ) {
                this.queueConfigCount = this.queueConfigMgr.getChangeCount();
                this.topicsChanged.set(false);

                for(final String topic : this.topics) {
                    final QueueInfo queueInfo = this.queueConfigMgr.getQueueInfo(topic);
                    Set<String> names = mapping.get(queueInfo.queueName);
                    if ( names == null ) {
                        names = new TreeSet<String>();
                        mapping.put(queueInfo.queueName, names);
                    }
                    names.add(topic);
                }
            }

            final Map<String, QueueJobCache> cacheMap = new HashMap<String, QueueJobCache>();
            for(final Map.Entry<String, Set<String>> entry : mapping.entrySet()) {
                cacheMap.put(entry.getKey(),
                        new QueueJobCache(this.configuration, this.queueConfigMgr.getQueueInfo(entry.getValue().iterator().next()), entry.getValue()));
            }
            this.queueJobCaches = cacheMap;
            this.logger.debug("Established new topic mapping: {}", mapping);
        }
        return this.queueJobCaches;
    }

    private final Map<String, Object> queueLocks = new ConcurrentHashMap<String, Object>();

    public JobHandler take(final String queueName) {
        Object lock = new Object();
        this.queueLocks.put(queueName, lock);
        JobImpl result = null;
        try {
            boolean isWaiting = true;
            while ( isWaiting ) {
                final Map<String, QueueJobCache> mapping = this.updateConfiguration();
                final QueueJobCache cache = mapping.get(queueName);
                if ( cache != null ) {
                    result = cache.getNextJob();
                    if ( result != null ) {
                        isWaiting = false;
                    }
                }
                if ( isWaiting ) {
                    // block
                    synchronized ( lock ) {
                        try {
                            lock.wait();
                            // refetch lock
                            lock = this.queueLocks.get(queueName);
                            if ( lock == null ) {
                                isWaiting = false;
                            }
                        } catch ( final InterruptedException ignore ) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        } finally {
            this.queueLocks.remove(queueName);
        }
        return (result != null ? new JobHandler( result, (JobManagerImpl)this.jobManager) : null);
    }

    public void stop(final String queueName) {
        final Object lock = queueLocks.remove(queueName);
        if ( lock != null ) {
            synchronized ( lock ) {
                lock.notify();
            }
        }
    }

    /**
     * Remove all jobs for a queue.
     * @param queueName The queue name
     */
    public void removeAll(final String queueName) {
        final Map<String, QueueJobCache> mapping = this.updateConfiguration();
        final QueueJobCache cache = mapping.get(queueName);
        if ( cache != null ) {
            final Set<String> topics = cache.getTopics();
            logger.debug("Removing all jobs for queue {} : {}", queueName, topics);

            if ( topics != null ) {

                final ResourceResolver resolver = this.configuration.createResourceResolver();
                try {
                    final Resource baseResource = resolver.getResource(this.configuration.getLocalJobsPath());

                    // sanity check - should never be null
                    if ( baseResource != null ) {
                        final BatchResourceRemover brr = new BatchResourceRemover();

                        for(final String t : topics) {
                            final Resource topicResource = baseResource.getChild(t.replace('/', '.'));
                            if ( topicResource != null ) {
                                JobTopicTraverser.traverse(logger, topicResource, new JobTopicTraverser.Handler() {

                                    @Override
                                    public boolean handle(final JobImpl job) {
                                        final Resource jobResource = topicResource.getResourceResolver().getResource(job.getResourcePath());
                                        // sanity check
                                        if ( jobResource != null ) {
                                            try {
                                                brr.delete(jobResource);
                                            } catch ( final PersistenceException ignore) {
                                                logger.error("Unable to remove job " + job, ignore);
                                            }
                                        }
                                        return true;
                                    }
                                });
                            }
                        }
                        try {
                            resolver.commit();
                        } catch ( final PersistenceException ignore) {
                            logger.error("Unable to remove jobs", ignore);
                        }
                    }
                } finally {
                    resolver.close();
                }
            }
        }
    }

    @Override
    public void topologyChanged(final TopologyCapabilities caps) {
        this.isActive.set(caps != null);
        if ( this.isActive.get() ) {
            this.initialScan();
            for(final Map.Entry<String, QueueJobCache> entry : this.updateConfiguration().entrySet()) {
                logger.info("Starting queue {}", entry.getKey());

                this.queueManager.start(this, entry.getValue().getQueueInfo());
            }
        }
    }

}
