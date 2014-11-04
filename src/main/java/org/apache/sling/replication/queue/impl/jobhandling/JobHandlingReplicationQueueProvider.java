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
package org.apache.sling.replication.queue.impl.jobhandling;

import javax.annotation.Nonnull;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueException;
import org.apache.sling.replication.queue.ReplicationQueueProcessor;
import org.apache.sling.replication.queue.ReplicationQueueProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * a queue provider {@link ReplicationQueueProvider} for sling jobs based
 * {@link ReplicationQueue}s
 */
public class JobHandlingReplicationQueueProvider implements ReplicationQueueProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String name;
    private final JobManager jobManager;



    private ServiceRegistration jobConsumer = null;

    private BundleContext context;


    public JobHandlingReplicationQueueProvider(String name, JobManager jobManager, BundleContext context) {
        if (name == null || jobManager == null || context == null) {
            throw new IllegalArgumentException("all arguments are required");
        }
        this.name = name;
        this.jobManager = jobManager;
        this.context = context;
    }

    public ReplicationQueue getQueue(String queueName) throws ReplicationQueueException {
        String topic = JobHandlingReplicationQueue.REPLICATION_QUEUE_TOPIC + '/' + name + "/" + queueName;
        return new JobHandlingReplicationQueue(name, topic, jobManager);
    }


    public void enableQueueProcessing(@Nonnull ReplicationQueueProcessor queueProcessor) throws ReplicationQueueException {
        if (jobConsumer != null) {
            throw new ReplicationQueueException("job already registered");
        }
        // eventually register job consumer for sling job handling based queues
        Dictionary<String, Object> jobProps = new Hashtable<String, Object>();
        String topic = JobHandlingReplicationQueue.REPLICATION_QUEUE_TOPIC + '/' + name;
        String childTopic = topic + "/*";
        jobProps.put(JobConsumer.PROPERTY_TOPICS, new String[]{topic, childTopic});
        log.info("registering job consumer for agent {}", name);
        jobConsumer = context.registerService(JobConsumer.class.getName(), new ReplicationAgentJobConsumer(queueProcessor), jobProps);
        log.info("job consumer for agent {} registered", name);
    }

    public void disableQueueProcessing() {
        if (jobConsumer != null) {
            jobConsumer.unregister();
            log.info("job consumer for agent {} unregistered", name);
            jobConsumer = null;
        }
        log.info("unregistering job consumer for agent {}", name);

    }


}
