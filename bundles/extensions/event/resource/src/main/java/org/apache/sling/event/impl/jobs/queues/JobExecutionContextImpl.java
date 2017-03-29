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
package org.apache.sling.event.impl.jobs.queues;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.sling.event.impl.jobs.JobHandler;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;

/**
 * Implementation of the job execution context passed to
 * job executors.
 */
public class JobExecutionContextImpl implements JobExecutionContext {

    /**
     * Call back interface to the queue.
     */
    public interface ASyncHandler {
        void finished(Job.JobState state);
    }

    /**
     * Boolean to check whether init progress has been called.
     */
    private final AtomicBoolean initProgressIsCalled = new AtomicBoolean(false);

    /**
     * Flag to indicate whether this is async processing
     */
    private final AtomicBoolean isAsync = new AtomicBoolean(false);

    private final ASyncHandler asyncHandler;

    private final JobHandler handler;

    public JobExecutionContextImpl(final JobHandler handler,
            final ASyncHandler asyncHandler) {
        this.handler = handler;
        this.asyncHandler = asyncHandler;
    }

    public void markAsync() {
        this.isAsync.set(true);
    }

    @Override
    public void initProgress(final int steps,
            final long eta) {
        if ( initProgressIsCalled.compareAndSet(false, true) ) {
            handler.persistJobProperties(handler.getJob().startProgress(steps, eta));
        }
    }

    @Override
    public void incrementProgressCount(final int steps) {
        if ( initProgressIsCalled.get() ) {
            handler.persistJobProperties(handler.getJob().setProgress(steps));
        }
    }

    @Override
    public void updateProgress(final long eta) {
        if ( initProgressIsCalled.get() ) {
            handler.persistJobProperties(handler.getJob().update(eta));
        }
    }

    @Override
    public void log(final String message, Object... args) {
        handler.persistJobProperties(handler.getJob().log(message, args));
    }

    @Override
    public boolean isStopped() {
        return handler.isStopped();
    }

    @Override
    public void asyncProcessingFinished(final JobExecutionResult result) {
        synchronized ( this ) {
            if ( isAsync.compareAndSet(true, false) ) {
                Job.JobState state = null;
                if ( result.succeeded() ) {
                    state = Job.JobState.SUCCEEDED;
                } else if ( result.failed() ) {
                    state = Job.JobState.QUEUED;
                } else if ( result.cancelled() ) {
                    if ( handler.isStopped() ) {
                        state = Job.JobState.STOPPED;
                    } else {
                        state = Job.JobState.ERROR;
                    }
                }
                asyncHandler.finished(state);
            } else {
                throw new IllegalStateException("Job is not processed async or is already finished: " + handler.getJob().getId());
            }
        }
    }

    @Override
    public ResultBuilder result() {
        return new ResultBuilderImpl();
    }
}
