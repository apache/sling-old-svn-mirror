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

import org.apache.sling.jobs.Job;
import org.apache.sling.jobs.JobUpdate;
import org.apache.sling.jobs.Types;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by ieb on 06/04/2016.
 */
public class JobUpdateImplTest {

    private String jobId;
    private JobImpl job;

    @Before
    public void setUp() throws Exception {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("job.name", "Jobname");
        jobId = Utils.generateId();
        job = new JobImpl(Types.jobQueue("testtopic"), jobId, Types.jobType("testtype"), properties);
    }

    @Test
    public void test() throws Exception {

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("Update", "update value");
        JobUpdateImpl jobUpdate = new JobUpdateImpl(job, JobUpdate.JobUpdateCommand.START_JOB, properties);

        Map<String, Object> value = Utils.toMapValue(jobUpdate);
        assertNotNull(value);
        System.err.println("Value is " + value);
        assertEquals(14, value.size());

        JobUpdate jobUpdateCopy = new JobUpdateImpl(value);
        assertEquals(jobUpdate.getId(), jobUpdateCopy.getId());
        assertEquals(jobUpdate.expires(), jobUpdateCopy.expires());
        assertEquals(jobUpdate.updateTimestamp(), jobUpdateCopy.updateTimestamp());
        assertEquals(jobUpdate.getCreated(), jobUpdateCopy.getCreated());
        assertEquals(jobUpdate.getStarted(), jobUpdateCopy.getStarted());
        assertEquals(jobUpdate.getCommand(), jobUpdateCopy.getCommand());
        assertEquals(jobUpdate.getFinished(), jobUpdateCopy.getFinished());
        assertEquals(jobUpdate.getResultMessage(), jobUpdateCopy.getResultMessage());
        assertEquals(jobUpdate.getNumberOfRetries(), jobUpdateCopy.getNumberOfRetries());
        assertEquals(jobUpdate.getRetryCount(), jobUpdateCopy.getRetryCount());
        assertEquals(jobUpdate.getState(), jobUpdateCopy.getState());
        assertEquals(jobUpdate.getQueue(), jobUpdateCopy.getQueue());
        Map<String, Object> originalProperties = jobUpdate.getProperties();
        Map<String, Object> copyProperties = jobUpdateCopy.getProperties();
        assertEquals(1, copyProperties.size());
        assertEquals(copyProperties.size(), copyProperties.size());
        assertEquals("update value", originalProperties.get("Update"));
        assertEquals(originalProperties.get("Update"), copyProperties.get("Update"));

    }
}