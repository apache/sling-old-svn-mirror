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

public class ScheduleInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Serialization version. */
    private static final String VERSION = "1";

    public static ScheduleInfo HOURLY(final int minutes) {
        return new ScheduleInfo(ScheduleType.HOURLY, -1, -1, minutes, null);
    }

    public static ScheduleInfo AT(final Date at) {
        return new ScheduleInfo(ScheduleType.DATE, -1, -1, -1, at);
    }

    public static ScheduleInfo WEEKLY(final int day, final int hour, final int minute) {
        return new ScheduleInfo(ScheduleType.WEEKLY, day, hour, minute, null);
    }

    public static ScheduleInfo DAYLY(final int hour, final int minute) {
        return new ScheduleInfo(ScheduleType.DAILY, -1, hour, minute, null);
    }

    private final ScheduleType scheduleType;

    private final int dayOfWeek;

    private final int hourOfDay;

    private final int minuteOfHour;

    private final Date at;

    private ScheduleInfo(final ScheduleType scheduleType,
            final int dayOfWeek,
            final int hourOfDay,
            final int minuteOfHour,
            final Date at) {
        this.scheduleType = scheduleType;
        this.dayOfWeek = dayOfWeek;
        this.hourOfDay = hourOfDay;
        this.minuteOfHour = minuteOfHour;
        this.at = at;
    }

    public static ScheduleInfo deserialize(final String s) {
        final String[] parts = s.split(":");
        if ( parts.length == 6 && parts[0].equals(VERSION) ) {
            try {
                return new ScheduleInfo(ScheduleType.valueOf(parts[1]),
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3]),
                        Integer.parseInt(parts[4]),
                        (parts[5].equals("null") ? null : new Date(Long.parseLong(parts[5]))));
            } catch ( final IllegalArgumentException iae) {
                // ignore and return null
            }
        }
        return null;
    }
    public String getSerializedString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(VERSION);
        sb.append(":");
        sb.append(this.scheduleType.name());
        sb.append(":");
        sb.append(String.valueOf(this.dayOfWeek));
        sb.append(":");
        sb.append(String.valueOf(this.hourOfDay));
        sb.append(":");
        sb.append(String.valueOf(this.minuteOfHour));
        sb.append(":");
        if ( at == null ) {
            sb.append("null");
        } else {
            sb.append(String.valueOf(at.getTime()));
        }
        return sb.toString();
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
}
