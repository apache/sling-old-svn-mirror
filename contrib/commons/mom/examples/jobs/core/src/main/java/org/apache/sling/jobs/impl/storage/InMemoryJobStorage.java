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
import org.apache.sling.jobs.impl.spi.JobStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ieb on 29/03/2016.
 * An unbounded local JVM job store.
 */
public class InMemoryJobStorage implements JobStorage {


    private Map<String, Job> store = new ConcurrentHashMap<String, Job>();

    @Nullable
    @Override
    public Job get(@Nonnull String jobId) {
        check();
        return store.get(jobId);
    }

    @Nonnull
    @Override
    public Job put(@Nonnull Job job) {
        check();
        store.put(job.getId(), job);
        return job;
    }

    @Nullable
    @Override
    public Job remove(@Nonnull String jobId) {
        check();
        Job j = store.get(jobId);
        store.remove(jobId);
        return j;
    }

    @Nullable
    @Override
    public Job remove(@Nonnull Job job) {
        check();
        return remove(job.getId());
    }

    private void check() {
        if ( store == null) {
            throw new IllegalStateException("Job store already closed.");
        }
    }

    @Override
    public void dispose() {
        store.clear();
        store = null;
    }
}
