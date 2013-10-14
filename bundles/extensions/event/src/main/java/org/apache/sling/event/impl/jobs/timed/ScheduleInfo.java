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
package org.apache.sling.event.impl.jobs.timed;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.sling.event.EventUtil;
import org.apache.sling.event.impl.support.Environment;
import org.apache.sling.event.impl.support.ResourceHelper;
import org.osgi.service.event.Event;

final class ScheduleInfo implements Serializable {

    private static final long serialVersionUID = 8667701700547811142L;

    public final String topic;
    public final String expression;
    public final Long   period;
    public final Date   date;
    public final String jobId;

    public ScheduleInfo(final Event event)
    throws IllegalArgumentException {
        // let's see if a schedule information is specified or if the job should be stopped
        this.expression = (String) event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_SCHEDULE);
        this.period = (Long) event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_PERIOD);
        this.date = (Date) event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_DATE);
        int count = 0;
        if ( this.expression != null) {
            count++;
        }
        if ( this.period != null ) {
            count++;
        }
        if ( this.date != null ) {
            count++;
        }
        if ( count > 1 ) {
            throw new IllegalArgumentException("Only one configuration property from " + EventUtil.PROPERTY_TIMED_EVENT_SCHEDULE +
                                  ", " + EventUtil.PROPERTY_TIMED_EVENT_PERIOD +
                                  ", or " + EventUtil.PROPERTY_TIMED_EVENT_DATE + " should be used.");
        }
        // we create a job id consisting of the real event topic and an (optional) id
        // if the event contains a timed event id or a job id we'll append that to the name
        this.topic = (String)event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_TOPIC);
        if ( topic == null ) {
            throw new IllegalArgumentException("Timed event does not contain required property " + EventUtil.PROPERTY_TIMED_EVENT_TOPIC + " : " +  EventUtil.toString(event));
        }

        final String id = (String)event.getProperty(EventUtil.PROPERTY_TIMED_EVENT_ID);
        final String jId = (String)event.getProperty(ResourceHelper.PROPERTY_JOB_NAME);

        this.jobId = getJobId(topic, id, jId);
    }

    private ScheduleInfo(final String topic, final String jobId) {
        this.topic = topic;
        this.expression = null;
        this.period = null;
        this.date = null;
        this.jobId = jobId;
    }

    public ScheduleInfo getStopInfo() {
        return new ScheduleInfo(this.topic, this.jobId);
    }

    public boolean isStopEvent() {
        return this.expression == null && this.period == null && this.date == null;
    }

    /** Counter for jobs without an id. */
    private static final AtomicLong eventCounter = new AtomicLong(0);

    public static String getJobId(final String topic, final String timedEventId, final String jobId) {
        final StringBuilder sb = new StringBuilder(topic.replace('/', '.'));
        if ( timedEventId != null ) {
            sb.append('_');
            sb.append(ResourceHelper.filterName(timedEventId));
        }
        if ( jobId != null ) {
            sb.append('_');
            sb.append(ResourceHelper.filterName(jobId));
        }
        if ( timedEventId == null && jobId == null ) {
            sb.append("__");
            sb.append(Environment.APPLICATION_ID);
            sb.append("__");
            sb.append(eventCounter.getAndIncrement());
        }
        return sb.toString();
    }
}