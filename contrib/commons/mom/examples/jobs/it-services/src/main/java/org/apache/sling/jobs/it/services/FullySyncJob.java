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

import org.apache.felix.scr.annotations.*;
import org.apache.sling.jobs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Created by ieb on 11/04/2016.
 */
@Component(immediate = true)
@Properties({
        @Property(name = JobConsumer.JOB_TYPES, cardinality = Integer.MAX_VALUE, value = {
                FullySyncJob.JOB_TYPE
        })
})
@Service(value = JobConsumer.class)
public class FullySyncJob implements JobConsumer {


    public static final String JOB_TYPE = "treadding/inthreadoperation";
    private static final Logger LOGGER = LoggerFactory.getLogger(FullySyncJob.class);

    @Nonnull
    @Override
    public void execute(@Nonnull Job initialState, @Nonnull JobUpdateListener listener, @Nonnull JobCallback callback) {
        LOGGER.info("Got request to start job {} ", initialState);
        initialState.setState(Job.JobState.ACTIVE);
        listener.update(initialState.newJobUpdateBuilder().command(JobUpdate.JobUpdateCommand.UPDATE_JOB).put("processing", "step1").build());

        // DO some work here.

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOGGER.debug(e.getMessage(), e);
        }
        listener.update(initialState.newJobUpdateBuilder().command(JobUpdate.JobUpdateCommand.UPDATE_JOB).put("processing", "step2").build());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOGGER.debug(e.getMessage(),e);
        }
        initialState.setState(Job.JobState.SUCCEEDED);
        listener.update(initialState.newJobUpdateBuilder().command(JobUpdate.JobUpdateCommand.UPDATE_JOB).put("processing", "step3").build());
        callback.callback(initialState);
    }
}
