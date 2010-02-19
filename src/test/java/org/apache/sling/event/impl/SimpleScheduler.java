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
package org.apache.sling.event.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.sling.commons.scheduler.Scheduler;

/**
 * Simple scheduler implementation for testing.
 */
public class SimpleScheduler implements Scheduler {

    public void addJob(String name, Object job,
            Map<String, Serializable> config, String schedulingExpression,
            boolean canRunConcurrently) throws Exception {
        throw new IllegalArgumentException();
    }

    public void addPeriodicJob(String name, Object job,
            Map<String, Serializable> config, long period,
            boolean canRunConcurrently) throws Exception {
        throw new IllegalAccessException();
    }

    public boolean fireJob(Object job, Map<String, Serializable> config,
            int times, long period) {
        throw new IllegalArgumentException();
    }

    public void fireJob(Object job, Map<String, Serializable> config)
            throws Exception {
        throw new IllegalAccessException();
    }

    public boolean fireJobAt(String name, Object job,
            Map<String, Serializable> config, Date date, int times, long period) {
        throw new IllegalArgumentException();
    }

    public void fireJobAt(String name, final Object job,
            Map<String, Serializable> config, final Date date) throws Exception {
        new Thread() {
            public void run() {
                final long sleepTime = date.getTime() - System.currentTimeMillis();
                if ( sleepTime > 0 ) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
                ((Runnable)job).run();
            }
        }.start();
    }

    public void removeJob(String name) throws NoSuchElementException {
        // ignore this
    }
}
