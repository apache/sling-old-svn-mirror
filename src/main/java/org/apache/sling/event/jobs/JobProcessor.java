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
package org.apache.sling.event.jobs;

import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.event.Event;

import aQute.bnd.annotation.ConsumerType;

/**
 * A job processor processes a job in the background.
 * It is used by {@link JobUtil#processJob(Event, JobProcessor)}.
 * @since 3.0
 * @deprecated - Use the new {@link JobConsumer} interface instead.
 */
@Deprecated
@ConsumerType
public interface JobProcessor {

    /**
     * Execute the job.
     * If the job fails with a thrown exception/throwable, the process will not be rescheduled.
     *
     * @param job The event containing the job description.
     * @return True if the job could be finished (either successful or by an error).
     *         Return false if the job should be rescheduled.
     */
    boolean process(Event job);
}
