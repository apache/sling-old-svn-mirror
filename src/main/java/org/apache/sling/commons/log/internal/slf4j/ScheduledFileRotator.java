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
package org.apache.sling.commons.log.internal.slf4j;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The <code>ScheduledFileRotator</code> is a {@link FileRotator} which rotates
 * the log file as soon as some configurable time schedule has been passed. Old
 * log files are renamed after the time period that just passed by. No old log
 * files are ever removed by this file rotator.
 * <p>
 * This file rotator implements the same functionality as the
 * <code>DailyFileAppender</code> of the Log4J framework.
 * <p>
 * Code copied and adapted from
 * http://svn.apache.org/repos/asf/logging/log4j/tags/v1_2_15/src
 * /main/java/org/apache/log4j/DailyRollingFileAppender.java from where the
 * following JavaDoc is also copied.
 * <p>
 * The rolling schedule is specified by the <b>datePattern</b> constructor
 * argument. This pattern should follow the {@link SimpleDateFormat}
 * conventions. In particular, you <em>must</em> escape literal text within a
 * pair of single quotes. A formatted version of the date pattern is used as the
 * suffix for the rolled file name.
 * <p>
 * For example, if the log file is configured as <code>/foo/bar.log</code> and
 * the <b>datePattern</b> set to <code>'.'yyyy-MM-dd</code>, on 2001-02-16 at
 * midnight, the logging file <code>/foo/bar.log</code> will be copied to
 * <code>/foo/bar.log.2001-02-16</code> and logging for 2001-02-17 will continue
 * in <code>/foo/bar.log</code> until it rolls over the next day.
 * <p>
 * Is is possible to specify monthly, weekly, half-daily, daily, hourly, or
 * minutely rollover schedules.
 * <p>
 * <table border="1" cellpadding="2">
 * <tr>
 * <th>DatePattern</th>
 * <th>Rollover schedule</th>
 * <th>Example</th>
 * <tr>
 * <td><code>'.'yyyy-MM</code></td>
 * <td>Rollover at the beginning of each month</td>
 * <td>At midnight of May 31st, 2002 <code>/foo/bar.log</code> will be copied to
 * <code>/foo/bar.log.2002-05</code>. Logging for the month of June will be
 * output to <code>/foo/bar.log</code> until it is also rolled over the next
 * month.</td>
 * </tr>
 * <tr>
 * <td><code>'.'yyyy-ww</code></td>
 * <td>Rollover at the first day of each week. The first day of the week depends
 * on the locale.</td>
 * <td>Assuming the first day of the week is Sunday, on Saturday midnight, June
 * 9th 2002, the file <i>/foo/bar.log</i> will be copied to
 * <i>/foo/bar.log.2002-23</i>. Logging for the 24th week of 2002 will be output
 * to <code>/foo/bar.log</code> until it is rolled over the next week.</td>
 * </tr>
 * <tr>
 * <td><code>'.'yyyy-MM-dd</code></td>
 * <td>Rollover at midnight each day.</td>
 * <td>At midnight, on March 8th, 2002, <code>/foo/bar.log</code> will be copied
 * to <code>/foo/bar.log.2002-03-08</code>. Logging for the 9th day of March
 * will be output to <code>/foo/bar.log</code> until it is rolled over the next
 * day.</td>
 * </tr>
 * <tr>
 * <td><code>'.'yyyy-MM-dd-a</code></td>
 * <td>Rollover at midnight and midday of each day.</td>
 * <td>At noon, on March 9th, 2002, <code>/foo/bar.log</code> will be copied to
 * <code>/foo/bar.log.2002-03-09-AM</code>. Logging for the afternoon of the 9th
 * will be output to <code>/foo/bar.log</code> until it is rolled over at
 * midnight.</td>
 * </tr>
 * <tr>
 * <td><code>'.'yyyy-MM-dd-HH</code></td>
 * <td>Rollover at the top of every hour.</td>
 * <td>At approximately 11:00.000 o'clock on March 9th, 2002,
 * <code>/foo/bar.log</code> will be copied to
 * <code>/foo/bar.log.2002-03-09-10</code>. Logging for the 11th hour of the 9th
 * of March will be output to <code>/foo/bar.log</code> until it is rolled over
 * at the beginning of the next hour.</td>
 * </tr>
 * <tr>
 * <td><code>'.'yyyy-MM-dd-HH-mm</code></td>
 * <td>Rollover at the beginning of every minute.</td>
 * <td>At approximately 11:23,000, on March 9th, 2001, <code>/foo/bar.log</code>
 * will be copied to <code>/foo/bar.log.2001-03-09-10-22</code>. Logging for the
 * minute of 11:23 (9th of March) will be output to <code>/foo/bar.log</code>
 * until it is rolled over the next minute.</td>
 * </tr>
 * </table>
 * <p>
 * Do not use the colon ":" character in anywhere in the <b>DatePattern</b>
 * option. The text before the colon is interpeted as the protocol specificaion
 * of a URL which is probably not what you want.
 */
final class ScheduledFileRotator implements FileRotator {

    private static final String DEFAULT_DATE_PATTERN = "'.'yyyy-MM-dd";

    // The code assumes that the following constants are in a increasing
    // sequence.
    private static final int TOP_OF_TROUBLE = -1;

    private static final int TOP_OF_MINUTE = 0;

    private static final int TOP_OF_HOUR = 1;

    private static final int HALF_DAY = 2;

    private static final int TOP_OF_DAY = 3;

    private static final int TOP_OF_WEEK = 4;

    private static final int TOP_OF_MONTH = 5;

    // The gmtTimeZone is used only in computeCheckPeriod() method.
    private static final TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");

    /**
     * The date pattern. By default, the pattern is set to "'.'yyyy-MM-dd"
     * meaning daily rollover.
     */
    private final String datePattern;

    private final Date now;

    private final SimpleDateFormat sdf;

    private final RollingCalendar rc;

    /**
     * The log file will be renamed to the value of the scheduledFilename
     * variable when the next interval is entered. For example, if the rollover
     * period is one hour, the log file will be renamed to the value of
     * "scheduledFilename" at the beginning of the next hour. The precise time
     * when a rollover occurs depends on logging activity.
     */
    private String scheduledFilename;

    /**
     * The next time we estimate a rollover should occur.
     */
    private long nextCheck;


    /**
     * Instantiate a <code>DailyRollingFileAppender</code> and open the file
     * designated by <code>filename</code>. The opened filename will become the
     * ouput destination for this appender.
     *
     * @throws IllegalArgumentException if the <code>datePattern</code> is not a
     *             valid <code>java.text.SimpleDateFormat</code> pattern.
     */
    ScheduledFileRotator(final String datePattern) {
        this.datePattern = (datePattern != null && datePattern.length() > 0)
                ? datePattern
                : DEFAULT_DATE_PATTERN;

        final long currentTime = System.currentTimeMillis();
        final int type = computeCheckPeriod(this.datePattern);

        this.nextCheck = currentTime - 1;
        this.now = new Date(currentTime);
        this.sdf = new SimpleDateFormat(this.datePattern);
        this.rc = new RollingCalendar(type);
    }

    String getDatePattern() {
        return datePattern;
    }

    public boolean isRotationDue(final File file) {
        if (scheduledFilename == null) {
            scheduledFilename = sdf.format(new Date(file.lastModified()));
        }

        final long n = System.currentTimeMillis();
        if (n >= nextCheck) {
            now.setTime(n);
            nextCheck = rc.getNextCheckMillis(now);
            return true;
        }

        return false;
    }

    public void rotate(File file) {

        final String baseFileName = file.getAbsolutePath();
        final String dateSuffix = sdf.format(now);

        // second level guard against rotating too early because the
        // isRotationDue method may to eagerly return true, especially
        // when called the first time
        if (scheduledFilename.equals(dateSuffix)) {
            return;
        }

        final File target = new File(baseFileName + scheduledFilename);
        if (target.exists()) {
            target.delete();
        }

        boolean result = file.renameTo(target);
        if (result) {
            // LogLog.debug(fileName + " -> " + scheduledFilename);
        } else {
            // LogLog.error("Failed to rename [" + fileName + "] to ["
            // + scheduledFilename + "].");
        }

        scheduledFilename = dateSuffix;
    }

    // -----------------------------------------------------------------------

    // This method computes the roll over period by looping over the
    // periods, starting with the shortest, and stopping when the r0 is
    // different from from r1, where r0 is the epoch formatted according
    // the datePattern (supplied by the user) and r1 is the
    // epoch+nextMillis(i) formatted according to datePattern. All date
    // formatting is done in GMT and not local format because the test
    // logic is based on comparisons relative to 1970-01-01 00:00:00
    // GMT (the epoch).

    private static int computeCheckPeriod(String datePattern) {
        if (datePattern != null) {
            final RollingCalendar rollingCalendar = new RollingCalendar(
                gmtTimeZone, Locale.ENGLISH);
            final Date epoch = new Date(0);
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                datePattern);
            simpleDateFormat.setTimeZone(gmtTimeZone);

            for (int i = TOP_OF_MINUTE; i <= TOP_OF_MONTH; i++) {
                // formatting in GMT
                String r0 = simpleDateFormat.format(epoch);
                rollingCalendar.setType(i);
                Date next = new Date(rollingCalendar.getNextCheckMillis(epoch));
                String r1 = simpleDateFormat.format(next);
                // System.out.println("Type = "+i+", r0 = "+r0+", r1 = "+r1);
                if (r0 != null && r1 != null && !r0.equals(r1)) {
                    return i;
                }
            }
        }
        return TOP_OF_TROUBLE; // Deliberately head for trouble...
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": datePattern" + getDatePattern();
    }

    /**
     * RollingCalendar is a helper class to DailyRollingFileAppender. Given a
     * periodicity type and the current time, it computes the start of the next
     * interval.
     */
    private static class RollingCalendar extends GregorianCalendar {
        private static final long serialVersionUID = -3560331770601814177L;

        int type = ScheduledFileRotator.TOP_OF_TROUBLE;

        RollingCalendar(int type) {
            super();
            setType(type);
        }

        RollingCalendar(TimeZone tz, Locale locale) {
            super(tz, locale);
        }

        void setType(int type) {
            this.type = type;
        }

        public long getNextCheckMillis(Date now) {
            return getNextCheckDate(now).getTime();
        }

        public Date getNextCheckDate(Date now) {
            this.setTime(now);

            switch (type) {
                case ScheduledFileRotator.TOP_OF_MINUTE:
                    this.set(Calendar.SECOND, 0);
                    this.set(Calendar.MILLISECOND, 0);
                    this.add(Calendar.MINUTE, 1);
                    break;
                case ScheduledFileRotator.TOP_OF_HOUR:
                    this.set(Calendar.MINUTE, 0);
                    this.set(Calendar.SECOND, 0);
                    this.set(Calendar.MILLISECOND, 0);
                    this.add(Calendar.HOUR_OF_DAY, 1);
                    break;
                case ScheduledFileRotator.HALF_DAY:
                    this.set(Calendar.MINUTE, 0);
                    this.set(Calendar.SECOND, 0);
                    this.set(Calendar.MILLISECOND, 0);
                    int hour = get(Calendar.HOUR_OF_DAY);
                    if (hour < 12) {
                        this.set(Calendar.HOUR_OF_DAY, 12);
                    } else {
                        this.set(Calendar.HOUR_OF_DAY, 0);
                        this.add(Calendar.DAY_OF_MONTH, 1);
                    }
                    break;
                case ScheduledFileRotator.TOP_OF_DAY:
                    this.set(Calendar.HOUR_OF_DAY, 0);
                    this.set(Calendar.MINUTE, 0);
                    this.set(Calendar.SECOND, 0);
                    this.set(Calendar.MILLISECOND, 0);
                    this.add(Calendar.DATE, 1);
                    break;
                case ScheduledFileRotator.TOP_OF_WEEK:
                    this.set(Calendar.DAY_OF_WEEK, getFirstDayOfWeek());
                    this.set(Calendar.HOUR_OF_DAY, 0);
                    this.set(Calendar.MINUTE, 0);
                    this.set(Calendar.SECOND, 0);
                    this.set(Calendar.MILLISECOND, 0);
                    this.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case ScheduledFileRotator.TOP_OF_MONTH:
                    this.set(Calendar.DATE, 1);
                    this.set(Calendar.HOUR_OF_DAY, 0);
                    this.set(Calendar.MINUTE, 0);
                    this.set(Calendar.SECOND, 0);
                    this.set(Calendar.MILLISECOND, 0);
                    this.add(Calendar.MONTH, 1);
                    break;
                default:
                    throw new IllegalStateException("Unknown periodicity type.");
            }
            return getTime();
        }

    }

}