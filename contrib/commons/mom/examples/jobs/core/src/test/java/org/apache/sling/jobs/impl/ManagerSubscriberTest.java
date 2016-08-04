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

package org.apache.sling.jobs.impl;

import aQute.libg.command.Command;
import org.apache.sling.jobs.Job;
import org.apache.sling.jobs.JobUpdate;
import org.apache.sling.jobs.Types;
import org.apache.sling.jobs.impl.spi.JobStorage;
import org.apache.sling.jobs.impl.storage.InMemoryJobStorage;
import org.apache.sling.mom.QueueManager;
import org.apache.sling.mom.TopicManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static org.junit.Assert.*;

/**
 * Created by ieb on 07/04/2016.
 */
public class ManagerSubscriberTest {

    private JobManagerImpl jobManager;
    private JobStorage jobStorage;
    private OutboundJobUpdateListener messageSender;
    @Mock
    private TopicManager topicManager;
    @Mock
    private QueueManager queueManager;
    private HashMap<org.apache.sling.mom.Types.TopicName, Queue<QueueEntry>> topicQueues;
    private HashMap<org.apache.sling.mom.Types.QueueName, Queue<QueueEntry>> messageQueues;
    private ManagerSubscriber managerSubscriber;

    public ManagerSubscriberTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void setUp() throws Exception {
        topicQueues = new HashMap<org.apache.sling.mom.Types.TopicName, Queue<QueueEntry>>();
        messageQueues = new HashMap<org.apache.sling.mom.Types.QueueName, Queue<QueueEntry>>();
        //noinspection unchecked
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                org.apache.sling.mom.Types.TopicName topic = (org.apache.sling.mom.Types.TopicName) invocationOnMock.getArguments()[0];
                org.apache.sling.mom.Types.CommandName command = (org.apache.sling.mom.Types.CommandName) invocationOnMock.getArguments()[1];
                @SuppressWarnings("unchecked") Map<String, Object> properties = (Map<String, Object>) invocationOnMock.getArguments()[2];
                Queue<QueueEntry> queue = topicQueues.get(topic);
                if (queue == null) {
                    queue = new ArrayBlockingQueue<QueueEntry>(100);
                    topicQueues.put(topic, queue);

                }
                queue.add(new QueueEntry(command, properties));
                return null;
            }
        }).when(topicManager)
                .publish(Mockito.any(org.apache.sling.mom.Types.TopicName.class), Mockito.any(org.apache.sling.mom.Types.CommandName.class), Mockito.any(Map.class));

        //noinspection unchecked
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                org.apache.sling.mom.Types.QueueName topic = (org.apache.sling.mom.Types.QueueName) invocationOnMock.getArguments()[0];
                @SuppressWarnings("unchecked") Map<String, Object> properties = (Map<String, Object>) invocationOnMock.getArguments()[1];
                Queue<QueueEntry> queue = messageQueues.get(topic);
                if ( queue == null) {
                    queue = new ArrayBlockingQueue<QueueEntry>(100);
                    messageQueues.put(topic, queue);

                }
                queue.add(new QueueEntry(properties));
                return null;
            }
        }).when(queueManager)
                .add(Mockito.any(org.apache.sling.mom.Types.QueueName.class), Mockito.any(Map.class));

        messageSender = new OutboundJobUpdateListener(topicManager, queueManager);
        jobStorage = new InMemoryJobStorage();
        jobManager = new JobManagerImpl(jobStorage, messageSender);

        managerSubscriber = new ManagerSubscriber();
        Field f = managerSubscriber.getClass().getDeclaredField("jobManager");
        f.setAccessible(true);
        f.set(managerSubscriber,jobManager);


    }

    @After
    public void after() {
        jobManager.dispose();
        jobStorage.dispose();
    }

    @Test
    public void test() {
        // fake up a remote send.
        Map<String, Object> properties = new HashMap<String, Object>();
        String testId = "testGetJobById"+System.currentTimeMillis();
        properties.put("testid", testId);
        properties.put("job.name", "Jobname");
        String jobId = Utils.generateId();
        Job job = new JobImpl(Types.jobQueue("testtopic"), jobId,Types.jobType("testtype"), properties);
        JobUpdateImpl jobUpdate = new JobUpdateImpl(job, JobUpdate.JobUpdateCommand.START_JOB, job.getProperties());

        Map<String, Object> message = Utils.toMapValue(jobUpdate);

        // pump the topic message into the managerSubscriber and check that the job can be found.

        managerSubscriber.onMessage(org.apache.sling.mom.Types.topicName("testtopic"), message);

        Job searchedJob = jobManager.getJobById(jobId);
        assertNotNull(searchedJob);
        assertEquals(job.getId(), searchedJob.getId());
        assertEquals(testId, searchedJob.getProperties().get("testid"));

    }


}