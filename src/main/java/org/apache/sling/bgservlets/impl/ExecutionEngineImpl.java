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
package org.apache.sling.bgservlets.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingException;
import org.apache.sling.bgservlets.ExecutionEngine;
import org.apache.sling.bgservlets.JobStatus;
import org.apache.sling.bgservlets.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple ExecutionEngine TODO should use Sling's thread pool, and check
 * synergies with scheduler services
 */
@Component(
        metatype=true,
        label="%ExecutionEngineImpl.label",
        description="%ExecutionEngineImpl.description")
@Service
public class ExecutionEngineImpl implements ExecutionEngine {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private Executor executor;
    private final Map<String, JobStatus> jobs = Collections.synchronizedMap(new HashMap<String, JobStatus>());

    @Property(intValue=10)
    public static final String PROP_CORE_POOL_SIZE = "core.pool.size";
    private int corePoolSize;

    @Property(intValue=20)
    public static final String PROP_MAX_POOL_SIZE = "max.pool.size";
    private int maximumPoolSize;

    @Property(intValue=30)
    public static final String PROP_KEEP_ALIVE_TIME = "keep.alive.time.seconds";
    private int keepAliveTimeSeconds;

    private class RunnableWrapper implements Runnable {
        private final Runnable inputJob;
        private final JobStatus jobStatus;

        RunnableWrapper(Runnable inputJob) {
            this.inputJob = inputJob;
            jobStatus = (inputJob instanceof JobStatus ? (JobStatus) inputJob
                    : null);
        }

        @Override
        public void run() {
            if (jobStatus != null) {
                jobStatus.requestStateChange(JobStatus.State.RUNNING);
            }
            log.info("Starting job {}", inputJob);
            try {
                // TODO save Exceptions in job?
                inputJob.run();
            } finally {
                if (jobStatus != null) {
                    log.debug("Job is done, cleaning up {}", jobStatus.getPath());
                    jobStatus.requestStateChange(JobStatus.State.DONE);
                    jobs.remove(jobStatus.getPath());
                }
            }
            log.info("Done running job {}", inputJob);
        }

        JobStatus getJobStatus() {
            return jobStatus;
        }
    };

    @SuppressWarnings("serial")
    public static class QueueFullException extends SlingException {
        QueueFullException(Runnable r) {
            super("Execution queue is full, cannot execute " + r);
        }
    }

    private int getIntegerProperty(Map<String, Object> props, String name) {
        final Integer value = (Integer)props.get(name);
        if(value == null) {
            throw new IllegalStateException("Missing ComponentContext property: " + name);
        }
        return value.intValue();
    }

    @Activate
    protected void activate(Map<String, Object> props) {
        corePoolSize = getIntegerProperty(props, PROP_CORE_POOL_SIZE);
        maximumPoolSize = getIntegerProperty(props, PROP_MAX_POOL_SIZE);
        keepAliveTimeSeconds = getIntegerProperty(props, PROP_KEEP_ALIVE_TIME);
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(4);
        RejectedExecutionHandler handler = new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r,
                    ThreadPoolExecutor executor) {
                onJobRejected(r);
            }
        };
        log.info("ThreadPoolExecutor configuration: corePoolSize = {}, maxPoolSize={}, keepAliveTimeSeconds={}",
                new Object[] { corePoolSize, maximumPoolSize, keepAliveTimeSeconds });
        executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                keepAliveTimeSeconds, unit, workQueue, handler);
    }

    @Deactivate
    protected void deactivate() {
        // TODO how to shutdown executor?
        executor = null;

        // TODO cleanup jobs??
    }

    private void onJobRejected(Runnable r) {
        final RunnableWrapper w = (RunnableWrapper) r;
        if (w.getJobStatus() != null) {
            w.getJobStatus().requestStateChange(JobStatus.State.REJECTED);
        }
        log.info("Rejected job {}", r);
        throw new QueueFullException(r);
    }

    @Override
    public void queueForExecution(final Runnable inputJob) {
        // Wrap job in our own Runnable to change its state as we execute it
        final RunnableWrapper w = new RunnableWrapper(inputJob);
        if (w.getJobStatus() != null) {
            w.getJobStatus().requestStateChange(JobStatus.State.QUEUED);
            jobs.put(w.getJobStatus().getPath(), w.getJobStatus());
        }
        executor.execute(w);
    }

    @Override
    public JobStatus getJobStatus(String path) {
        return jobs.get(path);
    }

    @Override
    public Iterator<JobStatus> getMatchingJobStatus(Predicate<JobStatus> p) {
        // TODO take predicate into account
        // TODO sort by submission/execution time?
        return jobs.values().iterator();
    }
}