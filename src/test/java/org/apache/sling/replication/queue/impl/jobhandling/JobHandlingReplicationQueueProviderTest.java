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

import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.Queue;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.replication.queue.ReplicationQueue;
import org.apache.sling.replication.queue.ReplicationQueueProcessor;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testcase for {@link org.apache.sling.replication.queue.impl.jobhandling.JobHandlingReplicationQueueProvider}
 */
public class JobHandlingReplicationQueueProviderTest {

    @Test
    public void testGetOrCreateNamedQueue() throws Exception {
        JobManager jobManager = mock(JobManager.class);
        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);
        Configuration config = mock(Configuration.class);
        when(configAdmin.createFactoryConfiguration(QueueConfiguration.class.getName(), null)).thenReturn(config);
        BundleContext context = mock(BundleContext.class);
        JobHandlingReplicationQueueProvider jobHandlingReplicationQueueProvider = new JobHandlingReplicationQueueProvider(
                jobManager, configAdmin, context);
        ReplicationQueue queue = jobHandlingReplicationQueueProvider.getOrCreateQueue("dummy-agent", "default");
        assertNotNull(queue);
    }

    @Test
    public void testDeleteNonExistingQueue() throws Exception {
        JobManager jobManager = mock(JobManager.class);
        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);
        Configuration config = mock(Configuration.class);
        when(configAdmin.createFactoryConfiguration(QueueConfiguration.class.getName(), null)).thenReturn(config);
        BundleContext context = mock(BundleContext.class);
        JobHandlingReplicationQueueProvider jobHandlingReplicationQueueProvider = new JobHandlingReplicationQueueProvider(
                jobManager, configAdmin, context);
        ReplicationQueue queue = mock(ReplicationQueue.class);
        jobHandlingReplicationQueueProvider.deleteQueue(queue);
    }

    @Test
    public void testDeleteExistingQueue() throws Exception {
        JobManager jobManager = mock(JobManager.class);
        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);
        Configuration config = mock(Configuration.class);
        when(configAdmin.createFactoryConfiguration(QueueConfiguration.class.getName(), null)).thenReturn(config);
        BundleContext context = mock(BundleContext.class);
        JobHandlingReplicationQueueProvider jobHandlingReplicationQueueProvider = new JobHandlingReplicationQueueProvider(
                jobManager, configAdmin, context);
        ReplicationQueue queue = jobHandlingReplicationQueueProvider.getOrCreateQueue("dummy-agent", "default");
        Queue underlyingQueue = mock(Queue.class);
        when(jobManager.getQueue(queue.getName())).thenReturn(underlyingQueue);
        jobHandlingReplicationQueueProvider.deleteQueue(queue);
    }

    @Test
    public void testEnableQueueProcessing() throws Exception {
        JobManager jobManager = mock(JobManager.class);
        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);
        Configuration config = mock(Configuration.class);
        when(configAdmin.createFactoryConfiguration(QueueConfiguration.class.getName(), null)).thenReturn(config);
        BundleContext context = mock(BundleContext.class);
        JobHandlingReplicationQueueProvider jobHandlingReplicationQueueProvider = new JobHandlingReplicationQueueProvider(
                jobManager, configAdmin, context);
        String agentName = "dummy-agent";
        ReplicationQueueProcessor queueProcessor = mock(ReplicationQueueProcessor.class);
        jobHandlingReplicationQueueProvider.enableQueueProcessing(agentName, queueProcessor);
    }

    @Test
    public void testDisableQueueProcessing() throws Exception {
        JobManager jobManager = mock(JobManager.class);
        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);
        Configuration config = mock(Configuration.class);
        when(configAdmin.createFactoryConfiguration(QueueConfiguration.class.getName(), null)).thenReturn(config);
        BundleContext context = mock(BundleContext.class);
        JobHandlingReplicationQueueProvider jobHandlingReplicationQueueProvider = new JobHandlingReplicationQueueProvider(
                jobManager, configAdmin, context);
        String agentName = "dummy-agent";
        jobHandlingReplicationQueueProvider.disableQueueProcessing(agentName);
    }
}