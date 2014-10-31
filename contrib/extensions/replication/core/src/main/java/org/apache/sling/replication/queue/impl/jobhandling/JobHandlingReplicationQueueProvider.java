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
import org.apache.sling.replication.queue.impl.AbstractReplicationQueueProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(metatype = false, label = "Sling Job handling based Replication Queue Provider")
@Service(value = ReplicationQueueProvider.class)
@Property(name = "name", value = JobHandlingReplicationQueueProvider.NAME)
public class JobHandlingReplicationQueueProvider extends AbstractReplicationQueueProvider
        implements ReplicationQueueProvider {

    public static final String NAME = "sjh";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private JobManager jobManager;


    private final Map<String, ServiceRegistration> jobConsumers = new ConcurrentHashMap<String, ServiceRegistration>();

    private BundleContext context;


    protected JobHandlingReplicationQueueProvider(JobManager jobManager, BundleContext context) {
        this.jobManager = jobManager;
        this.context = context;
    }

    public JobHandlingReplicationQueueProvider() {
    }

    @Override
    protected ReplicationQueue getInternalQueue(String agentName, String queueName)
            throws ReplicationQueueException {
        String name = agentName;
        if (queueName.length() > 0) {
            name += "/" + queueName;
        }
        String topic = JobHandlingReplicationQueue.REPLICATION_QUEUE_TOPIC + '/' + name;
        return new JobHandlingReplicationQueue(name, topic, jobManager);
    }


    public void enableQueueProcessing(@Nonnull String agentName, @Nonnull ReplicationQueueProcessor queueProcessor) {
        // eventually register job consumer for sling job handling based queues
        Dictionary<String, Object> jobProps = new Hashtable<String, Object>();
        String topic = JobHandlingReplicationQueue.REPLICATION_QUEUE_TOPIC + '/' + agentName;
        String childTopic = topic + "/*";
        jobProps.put(JobConsumer.PROPERTY_TOPICS, new String[]{topic, childTopic});
        synchronized (jobConsumers) {
            log.info("registering job consumer for agent {}", agentName);
            ServiceRegistration jobReg = context.registerService(JobConsumer.class.getName(),
                    new ReplicationAgentJobConsumer(queueProcessor), jobProps);
            if (jobReg != null) {
                jobConsumers.put(agentName, jobReg);
            }
            log.info("job consumer for agent {} registered", agentName);
        }
    }

    public void disableQueueProcessing(@Nonnull String agentName) {
        synchronized (jobConsumers) {
            log.info("unregistering job consumer for agent {}", agentName);
            ServiceRegistration jobReg = jobConsumers.remove(agentName);
            if (jobReg != null) {
                jobReg.unregister();
                log.info("job consumer for agent {} unregistered", agentName);
            }
        }
    }

    @Activate
    private void activate(BundleContext context) {
        this.context = context;
    }

    @Deactivate
    private void deactivate() {
        for (ServiceRegistration jobReg : jobConsumers.values()) {
            jobReg.unregister();
        }
        this.context = null;
    }
}
