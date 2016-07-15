/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.commons.scheduler.impl;

import java.util.Date;
import java.util.Iterator;

import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.quartz.SchedulerException;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A per thread pool quartz scheduler
 */
public class SchedulerProxy {

    private static final String PREFIX = "Apache Sling Quartz Scheduler ";

    private static final String QUARTZ_SCHEDULER_NAME = "ApacheSling";

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The quartz scheduler. */
    private final org.quartz.Scheduler scheduler;

    private final ThreadPoolManager threadPoolManager;

    private final ThreadPool threadPool;

    private final String poolName;

    public SchedulerProxy(final ThreadPoolManager manager,
            final String pName) throws SchedulerException {
        // sanity null check
        if ( manager == null ) {
            throw new SchedulerException("Thread pool manager missing");
        }
        if ( pName == null ) {
            throw new SchedulerException("Thread pool name missing");
        }

        this.threadPoolManager = manager;
        this.poolName = pName;

        // create the pool
        this.threadPool = this.threadPoolManager.get(poolName);
        final QuartzThreadPool quartzPool = new QuartzThreadPool(this.threadPool);

        boolean succeeded = false;

        try {
            final String name = QUARTZ_SCHEDULER_NAME + this.poolName.replace(' ', '_');
            final DirectSchedulerFactory factory = DirectSchedulerFactory.getInstance();
            // unique run id
            final String runID = new Date().toString().replace(' ', '_') + this.hashCode();

            factory.createScheduler(name, runID, quartzPool, new RAMJobStore());
            // quartz does not provide a way to get the scheduler by name AND runID, so we have to iterate!
            final Iterator<org.quartz.Scheduler> allSchedulersIter = factory.getAllSchedulers().iterator();
            org.quartz.Scheduler s = null;
            while ( s == null && allSchedulersIter.hasNext() ) {
                final org.quartz.Scheduler current = allSchedulersIter.next();
                if ( name.equals(current.getSchedulerName())
                    && runID.equals(current.getSchedulerInstanceId()) ) {
                    s = current;
                }
            }
            if ( s == null ) {
                throw new SchedulerException("Unable to find new scheduler with name " + name + " and run ID " + runID);
            }

            s.start();
            if ( this.logger.isDebugEnabled() ) {
                this.logger.debug("{}for pool {} started.", PREFIX, poolName);
            }
            this.scheduler = s;
            succeeded = true;
        } finally {
            if ( !succeeded) {
                this.threadPoolManager.release(this.threadPool);
            }
        }
    }

    /**
     * Dispose the quartz scheduler
     */
    public void dispose() {
        try {
            this.scheduler.shutdown();
        } catch (SchedulerException e) {
            this.logger.debug("Exception during shutdown of scheduler.", e);
        }
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("{}for pool {} stopped.", PREFIX, poolName);
        }

        this.threadPoolManager.release(this.threadPool);
    }

    public org.quartz.Scheduler getScheduler() {
        return this.scheduler;
    }
}
