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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.event.impl.jobs.InternalJobState;
import org.apache.sling.event.impl.jobs.JobExecutionResultImpl;
import org.apache.sling.event.impl.jobs.JobHandler;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.impl.jobs.Utility;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.QueueConfiguration;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.slf4j.Logger;

public class JobRunner implements Runnable {

    private final JobImpl job;

    private final JobHandler handler;

    private final QueueConfiguration configuration;

    private final QueueServices services;

    private final Logger logger;

    private final JobExecutor consumer;

    private final StatusNotifier notifier;

    private final AtomicInteger asyncCounter;

    public static interface StatusNotifier {
        boolean finishedJob(final String jobId,
                final Job.JobState resultState,
                final boolean isAsync);

    }
    public JobRunner(
            final Logger logger,
            final JobExecutor consumer,
            final StatusNotifier notifier,
            final JobHandler handler,
            final QueueConfiguration configuration,
            final QueueServices services,
            final AtomicInteger asyncCounter) {
        this.logger = logger;
        this.consumer = consumer;
        this.job = handler.getJob();
        this.handler = handler;
        this.configuration = configuration;
        this.services = services;
        this.asyncCounter = asyncCounter;
        this.notifier = notifier;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        final Object lock = new Object();
        final Thread currentThread = Thread.currentThread();
        // update priority and name
        final String oldName = currentThread.getName();
        final int oldPriority = currentThread.getPriority();

        currentThread.setName(oldName + "-" + job.getQueueName() + "(" + job.getTopic() + ")");
        if ( configuration.getThreadPriority() != null ) {
            switch ( configuration.getThreadPriority() ) {
                case NORM : currentThread.setPriority(Thread.NORM_PRIORITY);
                            break;
                case MIN  : currentThread.setPriority(Thread.MIN_PRIORITY);
                            break;
                case MAX  : currentThread.setPriority(Thread.MAX_PRIORITY);
                            break;
            }
        }
        JobExecutionResultImpl result = JobExecutionResultImpl.CANCELLED;
        Job.JobState resultState = Job.JobState.ERROR;
        final AtomicBoolean isAsync = new AtomicBoolean(false);

        try {
            synchronized ( lock ) {
                final JobExecutionContext ctx = new JobExecutionContext() {

                    private boolean hasInit = false;

                    @Override
                    public void initProgress(final int steps,
                            final long eta) {
                        if ( !hasInit ) {
                            handler.persistJobProperties(job.startProgress(steps, eta));
                            hasInit = true;
                        }
                    }

                    @Override
                    public void incrementProgressCount(final int steps) {
                        if ( hasInit ) {
                            handler.persistJobProperties(job.setProgress(steps));
                        }
                    }

                    @Override
                    public void updateProgress(final long eta) {
                        if ( hasInit ) {
                            handler.persistJobProperties(job.update(eta));
                        }
                    }

                    @Override
                    public void log(final String message, Object... args) {
                        handler.persistJobProperties(job.log(message, args));
                    }

                    @Override
                    public boolean isStopped() {
                        return handler.isStopped();
                    }

                    @Override
                    public void asyncProcessingFinished(final JobExecutionResult result) {
                        synchronized ( lock ) {
                            if ( isAsync.compareAndSet(true, false) ) {
                                services.jobConsumerManager.unregisterListener(job.getId());
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
                                notifier.finishedJob(job.getId(), state, true);
                                asyncCounter.decrementAndGet();
                            } else {
                                throw new IllegalStateException("Job is not processed async " + job.getId());
                            }
                        }
                    }

                    @Override
                    public ResultBuilder result() {
                        return new ResultBuilder() {

                            private String message;

                            private Long retryDelayInMs;

                            @Override
                            public JobExecutionResult failed(final long retryDelayInMs) {
                                this.retryDelayInMs = retryDelayInMs;
                                return new JobExecutionResultImpl(InternalJobState.FAILED, message, retryDelayInMs);
                            }

                            @Override
                            public ResultBuilder message(final String message) {
                                this.message = message;
                                return this;
                            }

                            @Override
                            public JobExecutionResult succeeded() {
                                return new JobExecutionResultImpl(InternalJobState.SUCCEEDED, message, retryDelayInMs);
                            }

                            @Override
                            public JobExecutionResult failed() {
                                return new JobExecutionResultImpl(InternalJobState.FAILED, message, retryDelayInMs);
                            }

                            @Override
                            public JobExecutionResult cancelled() {
                                return new JobExecutionResultImpl(InternalJobState.CANCELLED, message, retryDelayInMs);
                            }
                        };
                    }
                };
                result = (JobExecutionResultImpl)consumer.process(job, ctx);
                if ( result == null ) { // ASYNC processing
                    services.jobConsumerManager.registerListener(job.getId(), consumer, ctx);
                    asyncCounter.incrementAndGet();
                    isAsync.set(true);
                } else {
                    if ( result.succeeded() ) {
                        resultState = Job.JobState.SUCCEEDED;
                    } else if ( result.failed() ) {
                        resultState = Job.JobState.QUEUED;
                    } else if ( result.cancelled() ) {
                        if ( handler.isStopped() ) {
                            resultState = Job.JobState.STOPPED;
                        } else {
                            resultState = Job.JobState.ERROR;
                        }
                    }
                }
            }
        } catch (final Throwable t) { //NOSONAR
            logger.error("Unhandled error occured in job processor " + t.getMessage() + " while processing job " + Utility.toString(job), t);
            // we don't reschedule if an exception occurs
            result = JobExecutionResultImpl.CANCELLED;
            resultState = Job.JobState.ERROR;
        } finally {
            currentThread.setPriority(oldPriority);
            currentThread.setName(oldName);
            if ( result != null ) {
                if ( result.getRetryDelayInMs() != null ) {
                    job.setProperty(JobImpl.PROPERTY_DELAY_OVERRIDE, result.getRetryDelayInMs());
                }
                if ( result.getMessage() != null ) {
                   job.setProperty(Job.PROPERTY_RESULT_MESSAGE, result.getMessage());
                }
                notifier.finishedJob(job.getId(), resultState, false);
            }
        }
    }
}
