/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.util.poller;

public abstract class AbstractPoller  implements Poller {

    private final long waitInterval;
    private final long waitCount;

    /**
     * Convenience method to execute a generic call and do polling until a condition is met
     * The user must implement the {@link Poller#call()} and {@link Poller#condition()} methods
     * @param waitInterval Number of milliseconds to wait between polls
     * @param waitCount Number of wait intervals
     */
    public AbstractPoller(long waitInterval, long waitCount) {
        this.waitInterval = waitInterval;
        this.waitCount = waitCount;
    }

    /**
     * Calls the {@link Poller#call()} once and then calls {@link Poller#condition()} until it returns true
     * The method waits AbstractPoller#waitInterval milliseconds between calls to {@link Poller#condition()}
     * A maximum of AbstractPoller#waitCount intervals are checked
     * @return true if the condition is met after waiting a maximum of AbstractPoller#waitCount intervals, false otherwise
     * @throws InterruptedException to mark this operation as "waiting"
     */
    public boolean callAndWait() throws InterruptedException {
        if (!call()) return false;
        for (int i=0; i<waitCount; i++) {
            if (condition()) return true;
            Thread.sleep(waitInterval);
        }
        return false;
    }

    /**
     * Calls the @see: Poller#call() and then calls {@link Poller#condition()} until it returns true
     * The Poller#call() method is called in each wait interval, before the Poller#condition().
     * The method waits AbstractPoller#waitInterval milliseconds between calls to {@link Poller#condition()}
     * A maximum of AbstractPoller#waitCount intervals are checked
     * @return true if the condition is met after waiting a maximum of AbstractPoller#waitCount intervals, false otherwise
     * @throws InterruptedException to mark this operation as "waiting"
     */
    public boolean callUntilCondition() throws InterruptedException {
        if (!call()) return false;
        if (condition()) return true;
        for (int i = 0; i < waitCount; i++) {
            Thread.sleep(waitInterval);
            if (!call()) return false;
            if (condition()) return true;
        }
        return false;
    }
}
