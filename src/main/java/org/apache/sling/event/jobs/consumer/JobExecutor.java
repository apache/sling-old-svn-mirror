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
package org.apache.sling.event.jobs.consumer;

import org.apache.sling.event.jobs.Job;

import aQute.bnd.annotation.ConsumerType;



/**
 *
 * @since 1.1
 */
@ConsumerType
public interface JobExecutor {

    /**
     * Service registration property defining the jobs this consumer is able to process.
     * The value is either a string or an array of strings.
     */
    String PROPERTY_TOPICS = "job.topics";


    JobStatus process(Job job, JobExecutionContext context);
}
