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

package org.apache.sling.jobs.it.services;


import com.google.common.collect.ImmutableMap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.jobs.Job;
import org.apache.sling.jobs.JobManager;
import org.apache.sling.jobs.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by ieb on 11/04/2016.
 */
@Component(immediate = true)
public class JobManagerTestComponent  {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobManagerTestComponent.class);
    public static final String TOPIC = "org/apache/sling/jobs/it/services";
    @Reference
    private JobManager jobManager;


    @Activate
    public void activate(Map<String,Object> props) {
        for( int i = 0; i < 10; i++) {
            Job job = jobManager.newJobBuilder(Types.jobQueue(TOPIC), Types.jobType(AsyncJobConsumer.JOB_TYPE)).addProperties(
                    ImmutableMap.of("jobtest", (Object) "jobtest")).add();
            assertNotNull(job);
            LOGGER.info("Started Job {} ", job.getId());
        }
        // then start 10 sync jobs.
        for( int i = 0; i < 10; i++) {
            Job job = jobManager.newJobBuilder(Types.jobQueue(TOPIC), Types.jobType(FullySyncJob.JOB_TYPE)).addProperties(
                    ImmutableMap.of("jobtest", (Object) "jobtest")).add();
            assertNotNull(job);
            LOGGER.info("Started Job {} ", job.getId());
        }
    }

    @Deactivate
    public void deactivate(Map<String, Object> props) {

    }


}
