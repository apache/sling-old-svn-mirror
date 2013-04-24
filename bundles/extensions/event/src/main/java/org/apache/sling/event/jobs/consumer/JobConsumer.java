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



/**
 * A job consumer consumes a job.
 *
 * A job consumer registers itself with the {@link #PROPERTY_TOPICS} service registration
 * property. The value of this property defines which topics a consumer is able to process.
 * Each string value of this property is either a job topic or a topic category ending
 * with "/*" which means all topics in this category.
 * For example, the value "org/apache/sling/jobs/*" matches the topics
 * "org/apache/sling/jobs/a" and "org/apache/sling/jobs/b" but neither
 * "org/apache/sling/jobs" nor "org/apache/sling/jobs/subcategory/a"
 *
 * If there is more than one job consumer registered for a job topic, the selection is as
 * follows:
 * - If there is a single consumer registering for the exact topic, this one is used
 * - If there is more than a single consumer registering for the exact topic, the one
 *   with the highest service ranking is used. If the ranking is equal, the one with
 *   the lowest service ID is used.
 * - If there is a single consumer registered for the category, it is used
 * - If there is more than a single consumer registered for the category, the service
 *   with the highest service ranking is used. If the ranking is equal, the one with
 *   the lowest service ID is used.
 *
 * @since 1.2
 */
public interface JobConsumer {

    enum JobResult {
        OK,
        FAILED,
        CANCEL
    }
    /**
     * Service registration property defining the jobs this consumer is able to process.
     * The value is either a string or an array of strings.
     */
    String PROPERTY_TOPICS = "job.topics";

    /**
     * Execute the job.
     *
     * If the job has been processed successfully, {@link #JobResult.OK} should be returned.
     * If the job has not been processed completely, but might be rescheduled {@link #JobResult.FAILED}
     * should be returned.
     * If the job processing failed and should not be rescheduled, {@link #JobResult.CANCEL} should
     * be returned.
     *
     * If the processing fails with throwing an exception/throwable, the process will not be rescheduled
     * and treated like the method would have returned {@link #JobResult.CANCEL}.
     *
     * @param job The job
     * @return The job result
     */
    JobResult process(Job job);
}
