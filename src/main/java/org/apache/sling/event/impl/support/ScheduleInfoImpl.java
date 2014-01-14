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
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.sling.event.jobs.ScheduleInfo;
import org.quartz.CronExpression;

public class ScheduleInfoImpl implements ScheduleInfo, Serializable {

    private static final long serialVersionUID = 1L;

    /** Serialization version. */
    private static final String VERSION = "1";

    public static ScheduleInfoImpl HOURLY(final int minutes) {
        return new ScheduleInfoImpl(ScheduleType.HOURLY, -1, -1, minutes, null, -1, null);
    }

    public static ScheduleInfoImpl CRON(final String expr) {
        return new ScheduleInfoImpl(ScheduleType.CRON, -1, -1, -1, null, -1, expr);
    }

    public static ScheduleInfoImpl AT(final Date at) {
        return new ScheduleInfoImpl(ScheduleType.DATE, -1, -1, -1, at, -1, null);
    }

    public static ScheduleInfoImpl YEARLY(final int month, final int day, final int hour, final int minute) {
        return new ScheduleInfoImpl(ScheduleType.YEARLY, day, hour, minute, null, month, null);
    }

    public static ScheduleInfoImpl MONTHLY(final int day, final int hour, final int minute) {
        return new ScheduleInfoImpl(ScheduleType.MONTHLY, day, hour, minute, null, -1, null);
    }

    public static ScheduleInfoImpl WEEKLY(final int day, final int hour, final int minute) {
        return new ScheduleInfoImpl(ScheduleType.WEEKLY, day, hour, minute, null, -1, null);
    }

    public static ScheduleInfoImpl DAILY(final int hour, final int minute) {
        return new ScheduleInfoImpl(ScheduleType.DAILY, -1, hour, minute, null, -1, null);
    }

    private final ScheduleType scheduleType;

    private final int dayOfWeek;

    private final int hourOfDay;

    private final int minuteOfHour;

    private final Date at;

    private final int monthOfYear;

    private final String expression;

    private ScheduleInfoImpl(final ScheduleType scheduleType,
            final int dayOfWeek,
            final int hourOfDay,
            final int minuteOfHour,
            final Date at,
            final int monthOfYear,
            final String expression) {
        this.scheduleType = scheduleType;
        this.dayOfWeek = dayOfWeek;
        this.hourOfDay = hourOfDay;
        this.minuteOfHour = minuteOfHour;
        this.at = at;
        this.monthOfYear = monthOfYear;
        this.expression = expression;
    }

    public static ScheduleInfoImpl deserialize(final ScheduleType scheduleType, final String s) {
        final String[] parts = s.split("|");
        if ( scheduleType == ScheduleType.YEARLY && parts.length == 4 ) {
            try {
                return new ScheduleInfoImpl(scheduleType,
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        null,
                        Integer.parseInt(parts[3]),
                        null);
            } catch ( final IllegalArgumentException iae) {
                // ignore and return null
            }
        } else if ( scheduleType == ScheduleType.MONTHLY && parts.length == 3 ) {
            try {
                return new ScheduleInfoImpl(scheduleType,
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        null,
                        -1,
                        null);
            } catch ( final IllegalArgumentException iae) {
                // ignore and return null
            }
        } else if ( scheduleType == ScheduleType.WEEKLY && parts.length == 3 ) {
            try {
                return new ScheduleInfoImpl(scheduleType,
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        null,
                        -1,
                        null);
            } catch ( final IllegalArgumentException iae) {
                // ignore and return null
            }
        } else if ( scheduleType == ScheduleType.DAILY && parts.length == 2 ) {
            try {
                return new ScheduleInfoImpl(scheduleType,
                        -1,
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        null,
                        -1,
                        null);
            } catch ( final IllegalArgumentException iae) {
                // ignore and return null
            }
        } else if ( scheduleType == ScheduleType.HOURLY && parts.length == 1 ) {
            try {
                return new ScheduleInfoImpl(scheduleType,
                        -1,
                        -1,
                        Integer.parseInt(parts[0]),
                        null,
                        -1,
                        null);
            } catch ( final IllegalArgumentException iae) {
                // ignore and return null
            }
        } else if ( scheduleType == ScheduleType.CRON && parts.length == 1 ) {
            try {
                return new ScheduleInfoImpl(scheduleType,
                        -1,
                        -1,
                        -1,
                        null,
                        -1,
                        parts[0]);
            } catch ( final IllegalArgumentException iae) {
                // ignore and return null
            }
        }

        return null;
    }

    public static ScheduleInfoImpl deserialize(final String s) {
        final String[] parts = s.split("\\|");
        if ( parts.length == 8 && parts[0].equals(VERSION) ) {
            try {
                return new ScheduleInfoImpl(ScheduleType.valueOf(parts[1]),
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3]),
                        Integer.parseInt(parts[4]),
                        (parts[5].equals("null") ? null : new Date(Long.parseLong(parts[5]))),
                        Integer.parseInt(parts[6]),
                        (parts[7].equals("null") ? null : parts[7])
                        );
            } catch ( final IllegalArgumentException iae) {
                // ignore and return null
            }
        }
        return null;
    }

    public String getSerializedString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(VERSION);
        sb.append("|");
        sb.append(this.scheduleType.name());
        sb.append("|");
        sb.append(String.valueOf(this.dayOfWeek));
        sb.append("|");
        sb.append(String.valueOf(this.hourOfDay));
        sb.append("|");
        sb.append(String.valueOf(this.minuteOfHour));
        sb.append("|");
        if ( at == null ) {
            sb.append("null");
        } else {
            sb.append(String.valueOf(at.getTime()));
        }
        sb.append("|");
        sb.append(String.valueOf(this.monthOfYear));
        sb.append("|");
        if ( expression == null ) {
            sb.append("null");
        } else {
            sb.append(String.valueOf(expression));
        }
        return sb.toString();
    }

    @Override
    public ScheduleType getType() {
        return this.scheduleType;
    }

    @Override
    public Date getAt() {
        return this.at;
    }

    @Override
    public int getDayOfWeek() {
        return (this.scheduleType == ScheduleType.WEEKLY ? this.dayOfWeek : -1);
    }

    @Override
    public int getHourOfDay() {
        return this.hourOfDay;
    }

    @Override
    public int getMinuteOfHour() {
        return this.minuteOfHour;
    }

    @Override
    public String getExpression() {
        return this.expression;
    }

    @Override
    public int getMonthOfYear() {
        return this.monthOfYear;
    }

    @Override
    public int getDayOfMonth() {
        return (this.scheduleType == ScheduleType.MONTHLY
                || this.scheduleType == ScheduleType.YEARLY ? this.dayOfWeek : -1);
    }

    public void check(final List<String> errors) {
        switch ( this.scheduleType ) {
        case DAILY : if ( hourOfDay < 0 || hourOfDay > 23 || minuteOfHour < 0 || minuteOfHour > 59 ) {
                         errors.add("Wrong time information : " + minuteOfHour + ":" + minuteOfHour);
                     }
                     break;
        case DATE :  if ( at == null || at.getTime() <= System.currentTimeMillis() + 2000 ) {
                         errors.add("Date must be in the future : " + at);
                     }
                     break;
        case HOURLY : if ( minuteOfHour < 0 || minuteOfHour > 59 ) {
                          errors.add("Minute must be between 0 and 59 : " + minuteOfHour);
                      }
                      break;
        case WEEKLY : if ( hourOfDay < 0 || hourOfDay > 23 || minuteOfHour < 0 || minuteOfHour > 59 ) {
                          errors.add("Wrong time information : " + minuteOfHour + ":" + minuteOfHour);
                      }
                      if ( dayOfWeek < 1 || dayOfWeek > 7 ) {
                          errors.add("Day must be between 1 and 7 : " + dayOfWeek);
                      }
                      break;
        case MONTHLY : if ( hourOfDay < 0 || hourOfDay > 23 || minuteOfHour < 0 || minuteOfHour > 59 ) {
                           errors.add("Wrong time information : " + minuteOfHour + ":" + minuteOfHour);
                       }
                       if ( dayOfWeek < 1 || dayOfWeek > 28 ) {
                           errors.add("Day must be between 1 and 28 : " + dayOfWeek);
                       }
                       break;
        case YEARLY : if ( hourOfDay < 0 || hourOfDay > 23 || minuteOfHour < 0 || minuteOfHour > 59 ) {
                          errors.add("Wrong time information : " + minuteOfHour + ":" + minuteOfHour);
                      }
                      if ( dayOfWeek < 1 || dayOfWeek > 28 ) {
                          errors.add("Day must be between 1 and 28 : " + dayOfWeek);
                      }
                      if ( monthOfYear < 1 || monthOfYear > 12 ) {
                          errors.add("Month must be between 1 and 12 : " + dayOfWeek);
                      }
                      break;
        case CRON : if ( expression == null ) {
                         errors.add("Expression must be specified.");
                    }
                    try {
                        new CronExpression(this.expression);
                    } catch (final ParseException e) {
                        errors.add("Expression must be valid: " + this.expression);
                    }
        }
    }

    public Date getNextScheduledExecution() {
        final Calendar now = Calendar.getInstance();
        switch ( this.scheduleType ) {
            case DATE : return this.at;
            case DAILY : final Calendar next = Calendar.getInstance();
                         next.set(Calendar.HOUR_OF_DAY, this.hourOfDay);
                         next.set(Calendar.MINUTE, this.minuteOfHour);
                         if ( next.before(now) ) {
                             next.add(Calendar.DAY_OF_WEEK, 1);
                         }
                         return next.getTime();
            case WEEKLY : final Calendar nextW = Calendar.getInstance();
                          nextW.set(Calendar.HOUR_OF_DAY, this.hourOfDay);
                          nextW.set(Calendar.MINUTE, this.minuteOfHour);
                          nextW.set(Calendar.DAY_OF_WEEK, this.dayOfWeek);
                          if ( nextW.before(now) ) {
                              nextW.add(Calendar.WEEK_OF_YEAR, 1);
                          }
                          return nextW.getTime();
            case HOURLY : final Calendar nextH = Calendar.getInstance();
                          nextH.set(Calendar.MINUTE, this.minuteOfHour);
                          if ( nextH.before(now) ) {
                              nextH.add(Calendar.HOUR_OF_DAY, 1);
                          }
                          return nextH.getTime();
            case MONTHLY : final Calendar nextM = Calendar.getInstance();
                           nextM.set(Calendar.HOUR_OF_DAY, this.hourOfDay);
                           nextM.set(Calendar.MINUTE, this.minuteOfHour);
                           nextM.set(Calendar.DAY_OF_MONTH, this.dayOfWeek);
                           if ( nextM.before(now) ) {
                               nextM.add(Calendar.MONTH, 1);
                           }
                           return nextM.getTime();
            case YEARLY : final Calendar nextY = Calendar.getInstance();
                          nextY.set(Calendar.HOUR_OF_DAY, this.hourOfDay);
                          nextY.set(Calendar.MINUTE, this.minuteOfHour);
                          nextY.set(Calendar.DAY_OF_MONTH, this.dayOfWeek);
                          nextY.set(Calendar.MONTH, this.monthOfYear - 1);
                          if ( nextY.before(now) ) {
                              nextY.add(Calendar.YEAR, 1);
                          }
                          return nextY.getTime();
            case CRON : try {
                            final CronExpression exp = new CronExpression(this.expression);
                            return exp.getNextValidTimeAfter(new Date());
                        } catch (final ParseException e) {
                            // as we check the expression in check() everything should be fine here
                        }
        }
        return null;
    }

    /**
     * If the job is scheduled daily or weekly, return the cron expression
     */
    public String getCronExpression() {
        if ( this.scheduleType == ScheduleType.DAILY ) {
            final StringBuilder sb = new StringBuilder("0 ");
            sb.append(String.valueOf(this.minuteOfHour));
            sb.append(' ');
            sb.append(String.valueOf(this.hourOfDay));
            sb.append(" * * ?");
            return sb.toString();
        } else if ( this.scheduleType == ScheduleType.WEEKLY ) {
            final StringBuilder sb = new StringBuilder("0 ");
            sb.append(String.valueOf(this.minuteOfHour));
            sb.append(' ');
            sb.append(String.valueOf(this.hourOfDay));
            sb.append(" ? * ");
            sb.append(String.valueOf(this.dayOfWeek));
            return sb.toString();
        } else if ( this.scheduleType == ScheduleType.HOURLY ) {
            final StringBuilder sb = new StringBuilder("0 ");
            sb.append(String.valueOf(this.minuteOfHour));
            sb.append(" * * * ?");
            return sb.toString();
        } else if ( this.scheduleType == ScheduleType.MONTHLY ) {
            final StringBuilder sb = new StringBuilder("0 ");
            sb.append(String.valueOf(this.minuteOfHour));
            sb.append(' ');
            sb.append(String.valueOf(this.hourOfDay));
            sb.append(' ');
            sb.append(String.valueOf(this.dayOfWeek));
            sb.append(" * ?");
            return sb.toString();
        } else if ( this.scheduleType == ScheduleType.YEARLY ) {
            final StringBuilder sb = new StringBuilder("0 ");
            sb.append(String.valueOf(this.minuteOfHour));
            sb.append(' ');
            sb.append(String.valueOf(this.hourOfDay));
            sb.append(' ');
            sb.append(String.valueOf(this.dayOfWeek));
            sb.append(' ');
            sb.append(String.valueOf(this.monthOfYear - 1));
            sb.append(" ?");
            return sb.toString();
        } else if ( this.scheduleType == ScheduleType.CRON ) {
            return this.expression;
        }
        return null;
    }
}
