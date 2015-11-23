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
package org.apache.sling.discovery.base.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple executor of a background job that periodically
 * invokes a particular runnable - catching RuntimeExceptions
 * that might throw - but not catching Errors (that terminates
 * the BackgroundJob).
 */
public class PeriodicBackgroundJob implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(PeriodicBackgroundJob.class);

    private final long intervalSeconds;
    private final Runnable runnable;

    private final String threadName;
    
    private volatile boolean stopping = false;
    private volatile boolean stopped = false;

    public PeriodicBackgroundJob(long intervalSeconds, String threadName, Runnable runnable) {
        this.intervalSeconds = intervalSeconds;
        this.runnable = runnable;
        this.threadName = threadName;
        Thread th = new Thread(this, threadName);
        th.setDaemon(true);
        th.start();
    }
    
    public void stop() {
        stopping = true;
    }
    
    public boolean isStopping() {
        return stopping;
    }
    
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void run() {
        try{
            while(!stopping) {
                // first sleep
                try {
                    Thread.sleep(intervalSeconds * 1000);
                } catch (InterruptedException e) {
                    logger.info("run: got interrupted: "+e, e);
                }
                if (stopping) {
                    break;
                }
                // then execute if not stopping
                safelyRun(runnable);
            }
        } finally {
            stopped = true;
        }
    }

    private void safelyRun(Runnable r) {
        try{
            r.run();
        } catch(RuntimeException re) {
            // for RuntimeExceptions it's ok to catch them
            // so do so, log and continue
            logger.error("safelyRun: got a RuntimeException executing '"+threadName+"': "+re, re);
        } catch(Error er) {
            // for Errors it is not ok to catch them,
            // so log, but re-throw
            logger.error("safelyRun: got an Error executing '"+threadName+"'. BackgroundJob will terminate! "+er, er);
            throw er;
        }
    }
    
}
