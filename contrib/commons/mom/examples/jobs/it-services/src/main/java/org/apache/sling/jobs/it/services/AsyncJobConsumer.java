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
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by ieb on 15/04/2016.
 * This job consumer consumes jobs from the job subsystem. It accepts the jobs into a queue and uses a thread pool to drain the queue.
 * If the queue fills up, jobs are returned back to the jobsystem without being accepted. The size of the queue, the number of threads and
 * the maximum number of threads should be tuned for maximum throughput at an acceptable resource usage level. Retuning the consumer
 * will cause the queue to drain and restart.
 *
 * The contract this component makes with the JobSystem is that it will make best efforts to ensure that jobs it accepts into its queue are executed.
 *
 */
@Component(immediate = true)
@Properties({
        @Property(name = JobConsumer.JOB_TYPES, cardinality = Integer.MAX_VALUE, value = {
                AsyncJobConsumer.JOB_TYPE
        })
})
@Service(value = JobConsumer.class)
public class AsyncJobConsumer implements JobConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncJobConsumer.class);


    public static final String JOB_TYPE = "treadding/asyncthreadpoolwithbacklog";

    /**
     * The core number of threads that can be used to run this job. This should be just large enough to ensure
     * throughput without being so large as to impact other operations. Probably 1/2 the number of cores is a good
     * starting point.
     */
    @Property(intValue = 4)
    private static final String CORE_THREAD_POOL_SIZE = "core-thread-pool-size";
    /**
     * The maximum number of threads allocated to running this job. This should not be so large that it can
     * create availability issues for the JVM, but large enough to clear the backlog before it experiences
     * inefficiency due to overflow.
     */
    @Property(intValue = 8)
    private static final String MAC_THREAD_POOL_SIZE = "max-thread-pool-size";

    /**
     * This defines how many messages the component can queue for execution dequeing from the
     * Job queue. This should be just large enough to ensure that the executing threads are kept busy
     * but small enough to ensure that the shutdown is not blocked. Once into the queue there is some
     * impression that the jobs will be executed as they have been dequeued from the message system.
     * The deactivate will wait for the shutdown wait time, and then shut the queue down.
     */
    @Property(intValue = 8)
    private static final String MAX_QUEUED_BACKLOG = "max-queue-backlog";

    /**
     * This is the maximum time allowed to shut the queue down. It should be long enough to ensure that all jobs in
     * the local queue can complete. The longer the local queue set in max-queue-backlog, the higher this value must be.
     */
    @Property(longValue = 30)
    private static final String SHUTDOWN_WAIT_SECONDS = "max-shutdown-wait";

    private ExecutorService executor;
    private LinkedBlockingQueue<Runnable> workQueue;
    private long shutdownWaitSeconds;

    @Activate
    public void activate(Map<String, Object> properites) {
        int corePoolSize = (int) properites.get(CORE_THREAD_POOL_SIZE);
        int maxPoolSize = (int) properites.get(MAC_THREAD_POOL_SIZE);
        int maxBacklog = (int) properites.get(MAX_QUEUED_BACKLOG);
        shutdownWaitSeconds = (long) properites.get(SHUTDOWN_WAIT_SECONDS);
        workQueue = new LinkedBlockingQueue<Runnable>(maxBacklog);
        executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60, TimeUnit.SECONDS, workQueue);
    }

    @Deactivate
    public void deactivate(Map<String, Object> properties) {
        try {
            executor.awaitTermination(shutdownWaitSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for queue to drain ",e);
        }
        executor.shutdown();
    }

    @Nonnull
    @Override
    public void execute(@Nonnull final Job initialState, @Nonnull final JobUpdateListener listener, @Nonnull final JobCallback callback) {
        LOGGER.info("Got request to start job {} ", initialState);
        initialState.setState(Job.JobState.QUEUED);
        listener.update(initialState.newJobUpdateBuilder().command(JobUpdate.JobUpdateCommand.UPDATE_JOB).put("processing", "step1").build());
        // if the Job cant be queued locally, a RejectedExecutionException will be thrown, back to the scheduler and the job message will be put back into the queue to be retried some time later.
        executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
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
                    LOGGER.debug(e.getMessage(), e);
                }
                initialState.setState(Job.JobState.SUCCEEDED);
                listener.update(initialState.newJobUpdateBuilder().command(JobUpdate.JobUpdateCommand.UPDATE_JOB).put("processing", "step3").build());
                callback.callback(initialState);
                return null;
            }
        });
    }
}
