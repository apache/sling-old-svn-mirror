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
package org.apache.sling.event.impl.support;

import java.io.Serializable;
import java.util.Date;

import org.apache.sling.event.jobs.ScheduledJobInfo.ScheduleType;

// TODO - implement serializing
public class ScheduleInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public static ScheduleInfo PERIODIC(final int minutes) {
        return new ScheduleInfo(ScheduleType.PERIODICALLY, minutes, -1, -1, -1, null);
    }

    public static ScheduleInfo AT(final Date at) {
        return new ScheduleInfo(ScheduleType.DATE, -1, -1, -1, -1, at);
    }

    public static ScheduleInfo WEEKLY(final int day, final int hour, final int minute) {
        return new ScheduleInfo(ScheduleType.WEEKLY, -1, day, hour, minute, null);
    }

    public static ScheduleInfo DAYLY(final int hour, final int minute) {
        return new ScheduleInfo(ScheduleType.DAILY, -1, -1, hour, minute, null);
    }

    private final ScheduleType scheduleType;

    private final int period;

    private final int dayOfWeek;

    private final int hourOfDay;

    private final int minuteOfHour;

    private final Date at;

    private ScheduleInfo(final ScheduleType scheduleType,
            final int period,
            final int dayOfWeek,
            final int hourOfDay,
            final int minuteOfHour,
            final Date at) {
        this.scheduleType = scheduleType;
        this.period = period;
        this.dayOfWeek = dayOfWeek;
        this.hourOfDay = hourOfDay;
        this.minuteOfHour = minuteOfHour;
        this.at = at;
    }

    public Date getAt() {
        return this.at;
    }

    public ScheduleType getScheduleType() {
        return this.scheduleType;
    }

    public int getDayOfWeek() {
        return this.dayOfWeek;
    }

    public int getHourOfDay() {
        return this.hourOfDay;
    }

    public int getMinuteOfHour() {
        return this.minuteOfHour;
    }

    public int getPeriod() {
        return this.period;
    }
}
