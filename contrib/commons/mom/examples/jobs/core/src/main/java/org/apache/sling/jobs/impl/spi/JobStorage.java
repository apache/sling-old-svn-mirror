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

package org.apache.sling.jobs.impl.spi;

import org.apache.sling.jobs.Job;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Created by ieb on 29/03/2016.
 * Provides JobStorage local to the JVM. Implementation may or may not decide to persist over restarts, page, etc
 */
public interface JobStorage {


    /**
     * Get a Job by ID.
     * @param jobId the job ID to get.
     * @return the job or null of the job doesn't exist.
     */
    @Nullable
    Job get(@Nonnull String jobId);

    /**
     * Put a Job into the Job Storage.
     * @param job the job.
     * @return the job just added.
     */
    @Nonnull
    Job put(@Nonnull Job job);

    /**
     * Remove the Job
     * @param jobId
     * @return the job removed or null if not present.
     */
    @Nullable
    Job remove(@Nonnull String jobId);

    /**
     * Remove the Job, returning the job removed.
     * @param job the job to remove.
     * @return the job removed, if the the job was present, otherwise null.
     */
    @Nullable
    Job remove(@Nonnull Job job);

    /**
     * Dispose of the JobStorage.
     */
    void dispose();

}
