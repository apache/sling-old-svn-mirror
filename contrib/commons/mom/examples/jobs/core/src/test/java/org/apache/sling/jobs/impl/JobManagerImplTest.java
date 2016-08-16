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

import org.apache.sling.jobs.*;
import org.apache.sling.jobs.Types;
import org.apache.sling.jobs.impl.spi.JobStorage;
import org.apache.sling.jobs.impl.storage.InMemoryJobStorage;
import org.apache.sling.mom.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static org.junit.Assert.*;

/**
 * Created by ieb on 06/04/2016.
 */
public class JobManagerImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobManagerImplTest.class);
    private JobManager jobManager;
    private JobStorage jobStorage;
    private OutboundJobUpdateListener messageSender;
    @Mock
    private TopicManager topicManager;
    @Mock
    private QueueManager queueManager;
    private Map<org.apache.sling.mom.Types.TopicName, Queue<QueueEntry>> topicQueues;
    private Map<org.apache.sling.mom.Types.QueueName, Queue<QueueEntry>> messageQueues;

    public JobManagerImplTest() {
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
                LOGGER.info("Topic Manager publish {} {} {} ", new Object[]{ topic, command, properties });
                Queue<QueueEntry> queue = topicQueues.get(topic);
                if ( queue == null) {
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
                LOGGER.info("Queue Manager add {} {} {} ", new Object[]{ topic, properties });
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

    }

    @After
    public void tearDown() throws Exception {
        messageSender.dispose();
        jobStorage.dispose();
    }

    @Test
    public void testCreateJob() throws Exception {
        String testId = "testGetJobById"+System.currentTimeMillis();
        Job job = createJob(testId);

        assertEquals(testId, job.getProperties().get("testid"));
        org.apache.sling.mom.Types.TopicName topicName = org.apache.sling.mom.Types.topicName("testtopic");
        assertNotNull(messageQueues.get(topicName));
        assertEquals(1, messageQueues.get(topicName).size());
        QueueEntry qe = messageQueues.get(topicName).remove();
        assertNotNull(qe);
        Map<String, Object> jobProperties = qe.getProperties();

        // pretend to consume the job message
        JobImpl dequeuedJob = new JobImpl(new JobUpdateImpl(jobProperties));
        assertEquals(job.getId(), dequeuedJob.getId());
        // the operation of the job object is tested in other unit tests, don't repeat that here.
        assertEquals(testId, dequeuedJob.getProperties().get("testid"));

    }

    @Test
    public void testGetJobById() throws Exception {
        String testId = "testGetJobById"+System.currentTimeMillis();
        Job job = createJob(testId);


        Job searchedJob = jobManager.getJobById(job.getId());
        assertNotNull(searchedJob);
        assertEquals(job.getId(), searchedJob.getId());
        assertEquals(testId, searchedJob.getProperties().get("testid"));

    }

    private Job createJob(String testId) {
        Map<String, Object> testProps = new HashMap<String, Object>();
        testProps.put("job.name", "Jobname ");
        testProps.put("testid", testId);
        Job job = jobManager.newJobBuilder(Types.jobQueue("testtopic"), Types.jobType("testtype"))
                .addProperties(testProps)
                .add();
        assertNotNull(job);
        return job;
    }

    @Test
    public void testGetJob() throws Exception {
        String testId = "testGetJobById"+System.currentTimeMillis();
        Job job = createJob(testId);
        Map<String, Object> template = new HashMap<String, Object>();
        template.put("testid", testId);
        try {
            Job searchedJob = jobManager.getJob(job.getQueue(), template);
            assertNotNull(searchedJob);
            assertEquals(job.getId(), searchedJob.getId());
            assertEquals(testId, searchedJob.getProperties().get("testid"));
        } catch ( UnsupportedOperationException e) {
            LOGGER.info("Method getJob to be implemented. Cant be tested, so test passes for the moment.");
        }

    }

    @Test
    public void testFindJobs() throws Exception {
        String testId = "testGetJobById"+System.currentTimeMillis();
        Job job = createJob(testId);
        Map<String, Object> template = new HashMap<String, Object>();
        template.put("testid", testId);
        try {
            @SuppressWarnings("unchecked") Collection<Job> searchedResults = jobManager.findJobs(JobManager.QueryType.ALL, job.getQueue(), 10, template);
            assertNotNull(searchedResults);
            fail("Implement code to test search results");
        } catch ( UnsupportedOperationException e) {
            LOGGER.info("Method findJobs to be implemented. Cant be tested, so test passes for the moment.");
        }

    }

    @Test
    public void testStopJobById() throws Exception {
        String testId = "testGetJobById"+System.currentTimeMillis();
        Job job = createJob(testId);
        Thread.sleep(10);
        jobManager.stopJobById(job.getId());
        Queue<QueueEntry> messageQ = messageQueues.get(org.apache.sling.mom.Types.queueName("testtopic"));
        Queue<QueueEntry> topicQ = topicQueues.get(org.apache.sling.mom.Types.topicName("testtopic"));
        assertEquals(1,messageQ.size());
        QueueEntry qe = messageQ.remove();
        assertNotNull(qe);
        Map<String, Object> jobProperties = qe.getProperties();

        // get the job off the queue.
        JobImpl dequeuedJob = new JobImpl(new JobUpdateImpl(jobProperties));
        assertEquals(job.getId(), dequeuedJob.getId());
        // the operation of the job object is tested in other unit tests, don't repeat that here.
        assertEquals(testId, dequeuedJob.getProperties().get("testid"));


        // get any messages sent to the topic.
        assertEquals(1, topicQ.size());
        QueueEntry stoppedQE = topicQ.remove();
        Map<String, Object> stoppedJobProperties = stoppedQE.getProperties();
        assertNotNull(stoppedJobProperties);
        JobImpl stoppedJob = new JobImpl(new JobUpdateImpl(stoppedJobProperties));
        assertEquals(JobUpdate.JobUpdateCommand.STOP_JOB.asCommandName(), stoppedQE.getCommand());
        assertEquals(job.getId(), stoppedJob.getId());
        // the stop message to the topic wont have any properties.



    }

    @Test
    public void testAbortJob() throws Exception {
        String testId = "testGetJobById"+System.currentTimeMillis();
        Job job = createJob(testId);
        Thread.sleep(10);
        jobManager.abortJob(job.getId());
        LOGGER.info("Message Queues  {}", messageQueues);
        LOGGER.info("Topic Queues  {}", topicQueues);
        Queue<QueueEntry> messageQ = messageQueues.get(org.apache.sling.mom.Types.queueName("testtopic"));
        Queue<QueueEntry> topicQ = topicQueues.get(org.apache.sling.mom.Types.topicName("testtopic"));
        assertEquals(1,messageQ.size());
        QueueEntry qe = messageQ.remove();
        assertNotNull(qe);
        Map<String, Object> jobProperties = qe.getProperties();

        // get the job off the queue.
        JobImpl dequeuedJob = new JobImpl(new JobUpdateImpl(jobProperties));
        assertEquals(job.getId(), dequeuedJob.getId());
        // the operation of the job object is tested in other unit tests, dont repeat that here.
        assertEquals(testId, dequeuedJob.getProperties().get("testid"));


        // get any messages sent to the topic.
        assertEquals(1, topicQ.size());
        QueueEntry stoppedQE = topicQ.remove();
        Map<String, Object> stoppedJobProperties = stoppedQE.getProperties();
        assertNotNull(stoppedJobProperties);
        JobImpl stoppedJob = new JobImpl(new JobUpdateImpl(stoppedJobProperties));
        assertEquals(JobUpdate.JobUpdateCommand.ABORT_JOB.asCommandName(), stoppedQE.getCommand());
        assertEquals(job.getId(), stoppedJob.getId());

    }

    @Test
    public void testRetryJobById() throws Exception {
        String testId = "testGetJobById"+System.currentTimeMillis();
        Job job = createJob(testId);
        Thread.sleep(10);
        jobManager.retryJobById(job.getId());
        Queue<QueueEntry> messageQ = messageQueues.get(org.apache.sling.mom.Types.queueName("testtopic"));
        Queue<QueueEntry> topicQ = topicQueues.get(org.apache.sling.mom.Types.topicName("testtopic"));
        assertEquals(1,messageQ.size());
        QueueEntry qe = messageQ.remove();
        assertNotNull(qe);
        Map<String, Object> jobProperties = qe.getProperties();

        // get the job off the queue.
        JobImpl dequeuedJob = new JobImpl(new JobUpdateImpl(jobProperties));
        assertEquals(job.getId(), dequeuedJob.getId());
        // the operation of the job object is tested in other unit tests, don't repeat that here.
        assertEquals(testId, dequeuedJob.getProperties().get("testid"));


        // get any messages sent to the topic.
        assertEquals(1, topicQ.size());
        QueueEntry stoppedQE = topicQ.remove();
        Map<String, Object> stoppedJobProperties = stoppedQE.getProperties();
        assertNotNull(stoppedJobProperties);
        JobImpl stoppedJob = new JobImpl(new JobUpdateImpl(stoppedJobProperties));
        assertEquals(JobUpdate.JobUpdateCommand.RETRY_JOB.asCommandName(), stoppedQE.getCommand());
        assertEquals(job.getId(), stoppedJob.getId());

    }

}