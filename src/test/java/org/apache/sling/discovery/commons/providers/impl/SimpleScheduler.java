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
package org.apache.sling.discovery.commons.providers.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.sling.commons.scheduler.Scheduler;

public class SimpleScheduler implements Scheduler {

    private boolean failMode;

    @Override
    public void addJob(String name, Object job, Map<String, Serializable> config, String schedulingExpression,
            boolean canRunConcurrently) throws Exception {
        throw new IllegalStateException("not yet impl");
    }

    @Override
    public void addPeriodicJob(String name, Object job, Map<String, Serializable> config, long period, boolean canRunConcurrently)
            throws Exception {
        throw new IllegalStateException("not yet impl");
    }

    @Override
    public void addPeriodicJob(String name, Object job, Map<String, Serializable> config, long period, boolean canRunConcurrently,
            boolean startImmediate) throws Exception {
        throw new IllegalStateException("not yet impl");
    }

    @Override
    public void fireJob(Object job, Map<String, Serializable> config) throws Exception {
        throw new IllegalStateException("not yet impl");
    }

    @Override
    public boolean fireJob(Object job, Map<String, Serializable> config, int times, long period) {
        throw new IllegalStateException("not yet impl");
    }

    @Override
    public void fireJobAt(String name, final Object job, Map<String, Serializable> config, final Date date) throws Exception {
        if (!(job instanceof Runnable)) {
            throw new IllegalArgumentException("only runnables supported for now");
        }
        final Runnable j = (Runnable)job;
        Runnable r = new Runnable() {

            @Override
            public void run() {
                while (System.currentTimeMillis()<date.getTime()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.yield();
                    }
                }
                j.run();
            }
            
        };
        async(r, name);
    }

    private void async(Runnable r, String name) {
        if (failMode) {
            throw new IllegalStateException("failMode");
        }
        Thread th = new Thread(r);
        th.setName("async test thread for "+name);
        th.setDaemon(true);
        th.start();
    }

    @Override
    public boolean fireJobAt(String name, Object job, Map<String, Serializable> config, Date date, int times, long period) {
        throw new IllegalStateException("not yet impl");
    }

    @Override
    public void removeJob(String name) throws NoSuchElementException {
        throw new IllegalStateException("not yet impl");
    }

    public void failMode() {
        failMode = true;
    }

}
