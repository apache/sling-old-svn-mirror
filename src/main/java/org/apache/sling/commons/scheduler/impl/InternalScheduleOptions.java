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

import java.io.Serializable;
import java.util.Map;

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

/**
 * Scheduler options provide an extensible way of defining how to schedule a job.
 * @since 2.3
 */
public class InternalScheduleOptions implements ScheduleOptions {

    public String name;

    public boolean canRunConcurrently = false;

    public Map<String, Serializable> configuration;

    public final TriggerBuilder<? extends Trigger> trigger;

    public final IllegalArgumentException argumentException;

    public String[] runOn;

    public InternalScheduleOptions(final TriggerBuilder<? extends Trigger> trigger) {
        this.trigger = trigger;
        this.argumentException = null;
    }

    public InternalScheduleOptions(final IllegalArgumentException iae) {
        this.trigger = null;
        this.argumentException = iae;
    }

    /**
     * @see org.apache.sling.commons.scheduler.ScheduleOptions#config(java.util.Map)
     */
    public ScheduleOptions config(final  Map<String, Serializable> config) {
        this.configuration = config;
        return this;
    }

    /**
     * @see org.apache.sling.commons.scheduler.ScheduleOptions#name(java.lang.String)
     */
    public ScheduleOptions name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @see org.apache.sling.commons.scheduler.ScheduleOptions#canRunConcurrently(boolean)
     */
    public ScheduleOptions canRunConcurrently(final boolean flag) {
        this.canRunConcurrently = flag;
        return this;
    }

    /**
     * @see org.apache.sling.commons.scheduler.ScheduleOptions#onLeaderOnly(boolean)
     */
    public ScheduleOptions onLeaderOnly(final boolean flag) {
        if ( flag ) {
            this.runOn = new String[] {Scheduler.VALUE_RUN_ON_LEADER};
        } else {
            this.runOn = null;
        }
        return this;
    }

    /**
     * @see org.apache.sling.commons.scheduler.ScheduleOptions#onSingleInstanceOnly(boolean)
     */
    public ScheduleOptions onSingleInstanceOnly(final boolean flag) {
        if ( flag ) {
            this.runOn = new String[] {Scheduler.VALUE_RUN_ON_SINGLE};
        } else {
            this.runOn = null;
        }
        return this;
    }

    /**
     * @see org.apache.sling.commons.scheduler.ScheduleOptions#onInstancesOnly(java.lang.String[])
     */
    public ScheduleOptions onInstancesOnly(final String[] slingIds) {
        this.runOn = slingIds;
        return this;
    }
}
