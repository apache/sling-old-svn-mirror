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
package org.apache.sling.distribution.queue.impl.jobhandling;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.sling.distribution.common.DistributionException;
import org.apache.sling.distribution.queue.DistributionQueue;
import org.apache.sling.distribution.queue.DistributionQueueProcessor;
import org.apache.sling.distribution.queue.DistributionQueueProvider;
import org.apache.sling.distribution.queue.impl.CachingDistributionQueue;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a queue provider {@link org.apache.sling.distribution.queue.DistributionQueueProvider} for sling jobs based
 * {@link org.apache.sling.distribution.queue.DistributionQueue}s
 */
public class JobHandlingDistributionQueueProvider implements DistributionQueueProvider {

    public static final String TYPE = "jobs";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String name;

    private final JobManager jobManager;

    private ServiceRegistration jobConsumer = null;

    private BundleContext context;
    private Set<String> processingQueueNames = null;


    public JobHandlingDistributionQueueProvider(String name, JobManager jobManager, BundleContext context) {
        if (name == null || jobManager == null || context == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        this.name = name;
        this.jobManager = jobManager;
        this.context = context;
    }

    @Nonnull
    public DistributionQueue getQueue(@Nonnull String queueName) {
        String topic = JobHandlingDistributionQueue.DISTRIBUTION_QUEUE_TOPIC + '/' + name + "/" + queueName;
        boolean isActive = jobConsumer != null && (processingQueueNames == null || processingQueueNames.contains(queueName));

        DistributionQueue queue = new JobHandlingDistributionQueue(queueName, topic, jobManager, isActive);
        queue = new CachingDistributionQueue(topic, queue);
        return queue;
    }


    public void enableQueueProcessing(@Nonnull DistributionQueueProcessor queueProcessor, String... queueNames) throws DistributionException {
        if (jobConsumer != null) {
            throw new DistributionException("job already registered");
        }
        // eventually register job consumer for sling job handling based queues
        Dictionary<String, Object> jobProps = new Hashtable<String, Object>();
        String mainTopic = JobHandlingDistributionQueue.DISTRIBUTION_QUEUE_TOPIC + '/' + name;

        List<String> topicList = new ArrayList<String>();
        if (queueNames == null) {
            topicList.add(mainTopic + "/*");
            processingQueueNames = null;
        } else {
            for (String queueName : queueNames) {
                topicList.add(mainTopic + "/" + queueName);
            }
            processingQueueNames = new HashSet<String>(Arrays.asList(queueNames));
        }

        jobProps.put(JobConsumer.PROPERTY_TOPICS, topicList.toArray(new String[topicList.size()]));

        log.info("registering job consumer for agent {}", name);
        log.info("qp: {}, jp: {}", queueProcessor, jobProps);
        jobConsumer = context.registerService(JobConsumer.class.getName(), new DistributionAgentJobConsumer(queueProcessor), jobProps);
        log.info("job consumer for agent {} registered", name);
    }

    public void disableQueueProcessing() {
        if (jobConsumer != null) {
            jobConsumer.unregister();
            log.info("job consumer for agent {} unregistered", name);
            jobConsumer = null;
        }
        processingQueueNames = null;
        log.info("unregistering job consumer for agent {}", name);
    }

}
