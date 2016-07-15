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
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.ComponentContext;

/**
 * This is a proxy implementation of the scheduler service.
 * For each using bundle a separate instance is created. The real
 * scheduler implementation, the QuartzScheduler has the same
 * API however in addition each method gets the using bundleId.
 */
@Component
@Service(value=Scheduler.class, serviceFactory=true)
public class SchedulerServiceFactory implements Scheduler {

    private long bundleId;

    @Reference
    private QuartzScheduler scheduler;

    @Activate
    protected void activate(final ComponentContext ctx) {
        this.bundleId = ctx.getUsingBundle().getBundleId();
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#schedule(java.lang.Object, org.apache.sling.commons.scheduler.ScheduleOptions)
     */
    @Override
    public boolean schedule(final Object job, final ScheduleOptions options) {
        return this.scheduler.schedule(this.bundleId, null, job, options);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#unschedule(java.lang.String)
     */
    @Override
    public boolean unschedule(final String jobName) {
        return this.scheduler.unschedule(this.bundleId, jobName);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#NOW()
     */
    @Override
    public ScheduleOptions NOW() {
        return this.scheduler.NOW();
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#NOW(int, long)
     */
    @Override
    public ScheduleOptions NOW(final int times, final long period) {
        return this.scheduler.NOW(times, period);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#AT(java.util.Date)
     */
    @Override
    public ScheduleOptions AT(final Date date) {
        return this.scheduler.AT(date);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#AT(java.util.Date, int, long)
     */
    @Override
    public ScheduleOptions AT(final Date date, final int times, final long period) {
        return this.scheduler.AT(date, times, period);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#EXPR(java.lang.String)
     */
    @Override
    public ScheduleOptions EXPR(final String expression) {
        return this.scheduler.EXPR(expression);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#addJob(java.lang.String, java.lang.Object, java.util.Map, java.lang.String, boolean)
     */
    @Override
    @SuppressWarnings("deprecation")
    public void addJob(final String name, final Object job,
            final Map<String, Serializable> config, final String schedulingExpression,
            final boolean canRunConcurrently) throws Exception {
        this.scheduler.addJob(this.bundleId, null, name, job, config, schedulingExpression, canRunConcurrently);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#addPeriodicJob(java.lang.String, java.lang.Object, java.util.Map, long, boolean)
     */
    @Override
    @SuppressWarnings("deprecation")
    public void addPeriodicJob(final String name, final Object job,
            final Map<String, Serializable> config, final long period,
            final boolean canRunConcurrently) throws Exception {
        this.scheduler.addPeriodicJob(this.bundleId, null, name, job, config, period, canRunConcurrently);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#addPeriodicJob(java.lang.String, java.lang.Object, java.util.Map, long, boolean, boolean)
     */
    @Override
    @SuppressWarnings("deprecation")
    public void addPeriodicJob(final String name, final Object job,
            final Map<String, Serializable> config, final long period,
            final boolean canRunConcurrently, final boolean startImmediate)
            throws Exception {
        this.scheduler.addPeriodicJob(this.bundleId, null, name, job, config, period, canRunConcurrently, startImmediate);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJob(java.lang.Object, java.util.Map)
     */
    @Override
    @SuppressWarnings("deprecation")
    public void fireJob(final Object job, final Map<String, Serializable> config)
            throws Exception {
        this.scheduler.fireJob(this.bundleId, null, job, config);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJob(java.lang.Object, java.util.Map, int, long)
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean fireJob(final Object job, final Map<String, Serializable> config,
            final int times, final long period) {
        return this.scheduler.fireJob(this.bundleId, null, job, config, times, period);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJobAt(java.lang.String, java.lang.Object, java.util.Map, java.util.Date)
     */
    @Override
    @SuppressWarnings("deprecation")
    public void fireJobAt(final String name, final Object job,
            final Map<String, Serializable> config, final Date date) throws Exception {
        this.scheduler.fireJobAt(this.bundleId, null, name, job, config, date);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#fireJobAt(java.lang.String, java.lang.Object, java.util.Map, java.util.Date, int, long)
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean fireJobAt(final String name, final Object job,
            final Map<String, Serializable> config, final Date date, final int times, final long period) {
        return this.scheduler.fireJobAt(this.bundleId, null, name, job, config, date, times, period);
    }

    /**
     * @see org.apache.sling.commons.scheduler.Scheduler#removeJob(java.lang.String)
     */
    @Override
    @SuppressWarnings("deprecation")
    public void removeJob(final String name) throws NoSuchElementException {
        this.scheduler.removeJob(this.bundleId, name);
    }
}
