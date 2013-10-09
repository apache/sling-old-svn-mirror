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
 * A job consumer consumes a job.
 *
 * If the job consumer needs more features like providing progress information or adding
 * more information of the processing, {@link JobExecutor} should be implemented instead.
 *
 * A job consumer registers itself with the {@link #PROPERTY_TOPICS} service registration
 * property. The value of this property defines which topics a consumer is able to process.
 * Each string value of this property is either a job topic or a topic category ending
 * with "/*" which means all topics in this category.
 * For example, the value "org/apache/sling/jobs/*" matches the topics
 * "org/apache/sling/jobs/a" and "org/apache/sling/jobs/b" but neither
 * "org/apache/sling/jobs" nor "org/apache/sling/jobs/subcategory/a"
 *
 * If there is more than one job consumer or executor registered for a job topic, the selection is as
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
 * If the consumer decides to process the job asynchronously, the processing must finish
 * within the current lifetime of the job consumer. If the consumer (or the instance
 * of the consumer) dies, the job processing will mark this processing as failed and
 * reschedule.
 *
 * @since 1.0
 */
@ConsumerType
public interface JobConsumer {

    enum JobResult {
        OK,      // processing finished
        FAILED,  // processing failed, can be retried
        CANCEL,  // processing failed permanently
        ASYNC    // processing will be done async
    }

    /** Job property containing an asynchronous handler. */
    String PROPERTY_JOB_ASYNC_HANDLER = ":sling:jobs:asynchandler";

    /**
     * If the consumer decides to process the job asynchronously, this handler
     * interface can be used to notify finished processing. The asynchronous
     * handler can be retried using the property name {@link #PROPERTY_JOB_ASYNC_HANDLER}.
     */
    interface AsyncHandler {

        void failed();

        void ok();

        void cancel();
    }

    /**
     * Service registration property defining the jobs this consumer is able to process.
     * The value is either a string or an array of strings.
     */
    String PROPERTY_TOPICS = "job.topics";


    /**
     * Execute the job.
     *
     * If the job has been processed successfully, {@link JobResult.OK} should be returned.
     * If the job has not been processed completely, but might be rescheduled {@link JobResult.FAILED}
     * should be returned.
     * If the job processing failed and should not be rescheduled, {@link JobResult.CANCEL} should
     * be returned.
     *
     * If the consumer decides to process the job asynchronously it should return {@link JobResult.ASYNC}
     * and notify the job manager by using the {@link AsyncHandler} interface.
     *
     * If the processing fails with throwing an exception/throwable, the process will not be rescheduled
     * and treated like the method would have returned {@link JobResult.CANCEL}.
     *
     * @param job The job
     * @return The job result
     */
    JobResult process(Job job);
}
