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

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.sling.event.impl.AbstractRepositoryEventHandler.EventInfo;

/**
 * The job blocking queue extends the blocking queue by some
 * functionality for the job event handling.
 */
public final class JobBlockingQueue extends LinkedBlockingQueue<EventInfo> {

    private EventInfo eventInfo;

    private final Object lock = new Object();

    private boolean isWaiting = false;

    private boolean markForCleanUp = false;

    private boolean finished = false;

    private boolean isSleeping = false;

    private String schedulerJobName;
    private Thread sleepingThread;

    public EventInfo waitForFinish() throws InterruptedException {
        this.isWaiting = true;
        this.markForCleanUp = false;
        this.lock.wait();
        this.isWaiting = false;
        final EventInfo object = this.eventInfo;
        this.eventInfo = null;
        return object;
    }

    public void markForCleanUp() {
        if ( !this.isWaiting ) {
            this.markForCleanUp = true;
        }
    }

    public boolean isMarkedForCleanUp() {
        return !this.isWaiting && this.markForCleanUp;
    }

    public void notifyFinish(EventInfo i) {
        this.eventInfo = i;
        this.lock.notify();
    }

    public Object getLock() {
        return lock;
    }

    public boolean isWaiting() {
        return this.isWaiting;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean flag) {
        this.finished = flag;
    }

    public void setSleeping(boolean flag) {
        this.isSleeping = flag;
        if ( !flag ) {
            this.schedulerJobName = null;
            this.sleepingThread = null;
        }
    }

    public void setSleeping(boolean flag, String schedulerJobName) {
        this.schedulerJobName = schedulerJobName;
        this.setSleeping(flag);
    }

    public void setSleeping(boolean flag, Thread sleepingThread) {
        this.sleepingThread = sleepingThread;
        this.setSleeping(flag);
    }

    public String getSchedulerJobName() {
        return this.schedulerJobName;
    }

    public Thread getSleepingThread() {
        return this.sleepingThread;
    }

    public boolean isSleeping() {
        return this.isSleeping;
    }
}

