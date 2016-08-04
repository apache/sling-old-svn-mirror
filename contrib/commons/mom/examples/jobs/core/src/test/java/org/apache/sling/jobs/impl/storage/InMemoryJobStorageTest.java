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
package org.apache.sling.jobs.impl.storage;

import org.apache.sling.jobs.Job;
import org.apache.sling.jobs.impl.Utils;
import org.apache.sling.jobs.impl.spi.JobStorage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

/**
 * Created by ieb on 05/04/2016.
 * Tests in memory storage.
 */
public class InMemoryJobStorageTest {

    private JobStorage jobStorage;

    @Mock
    private Job job;
    private String jobId;

    public InMemoryJobStorageTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void setUp() throws Exception {
        jobId = Utils.generateId();
        Mockito.when(job.getId()).thenReturn(jobId);
        jobStorage = new InMemoryJobStorage();
        jobStorage.put(job);
    }


    @Test
    public void testGet() throws Exception {
        String nextJobId = Utils.generateId();
        assertEquals(jobId, jobStorage.get(jobId).getId());
        assertNull(jobStorage.get(nextJobId));
    }

    @Test
    public void testPut() throws Exception {
        String nextJobId = Utils.generateId();
        Job newJob = Mockito.mock(Job.class);
        Mockito.when(newJob.getId()).thenReturn(nextJobId);
        jobStorage.put(newJob);

        assertEquals(jobId, jobStorage.get(jobId).getId());
        assertEquals(nextJobId, jobStorage.get(nextJobId).getId());
    }

    @Test
    public void testRemove() throws Exception {
        Job removed = jobStorage.remove(job);
        assertNotNull(removed);
        assertEquals(job.getId(), removed.getId());
        assertNull(jobStorage.get(jobId));
    }

    @Test
    public void testRemove1() throws Exception {
        Job removed = jobStorage.remove(job.getId());
        assertNotNull(removed);
        assertEquals(job.getId(), removed.getId());
        assertNull(jobStorage.get(jobId));

    }
}