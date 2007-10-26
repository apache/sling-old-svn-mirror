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
package org.apache.sling.helpers;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The <code>ProcessTracker</code> class provides the functionality to track
 * the progress of request processing. Instances of this class are provided
 * through the {@link org.apache.sling.SlingHttpServletRequest#getProcessTracker()} method.
 * <p>
 * The following functionality is provided:
 * <ol>
 * <li>Track the progress of request processing through the
 * {@link #log(String)} and {@link #log(String, String)} methods.
 * <li>Ability to measure and track processing times of parts of request
 * processing through the {@link #startTimer(String)} and
 * {@link #checkTimer(String)} methods.
 * <li>Dumping the recording messages through the
 * {@link #dumpText(PrintWriter)} method.
 * <li>Resetting the tracker through the {@link #reset()} method.
 * </ol>
 * <p>
 * <b>Tracking Request Processing</b>
 * <p>
 * As the request being processed, certain steps may be tracked by calling
 * either of the <code>log</code> methods. A tracking entry consists of a time
 * stamp managed by this class, a tracking message noting the actual step being
 * tracked and an optional tracking tag. The value of the tracking tag depends
 * on the application and defaults to {@link #TAG_CHECK} if not specified.
 * <p>
 * <b>Timing Processing Steps</b>
 * </p>
 * Certain steps during request processing may need to be timed in that the time
 * required for processing should be recorded. Instances of this class maintain
 * a map of named timers. Each timer is started (initialized or reset) by
 * calling the {@link #startTimer(String)} method. This method just records the
 * starting time of the named timer and adds a tracking entry with the timer
 * name as the message and the tracking tag {@link #TAG_START}.
 * <p>
 * To record the number of milliseconds ellapsed since a timer has been started,
 * the {@link #checkTimer(String)} method may be called. This method logs s
 * tracking entry with the tracking tag {@link #TAG_CHECK} and a message
 * consisting of the name of the timer and the number of milliseconds ellapsed
 * since the timer was last {@link #startTimer(String) started}. The
 * {@link #checkTimer(String)} method may be called multiple times to record
 * several timed steps.
 * <p>
 * Calling the {@link #startTimer(String)} method with the name of timer which
 * already exists, resets the start time of the named timer to the current
 * system time.
 * <p>
 * <b>Dumping Tracking Entries</b>
 * <p>
 * The {@link #dumpText(PrintWriter)} methods adds a tracking entry with tag
 * {@link #TAG_CHECK} and writes all tracking entries to the given
 * <code>PrintWriter</code>. Each entry is written on a single line
 * consisting of the following fields:
 * <ol>
 * <li>The number of milliseconds since the last {@link #reset()} (or creation)
 * of this timer.
 * <li>The absolute time of the timer in parenthesis.
 * <li>The timer tag enclosed in stars (*)
 * <li>The entry message
 * </ol>
 */
public class ProcessTracker {

    /**
     * The tracking tag used for starting messages in the {@link #reset()} and
     * {@link #startTimer(String)} methods (value is "START").
     */
    public static final String TAG_START = "START";

    /**
     * The default tracking tag used by the {@link #log(String)} and
     * {@link #checkTimer(String)} methods (value is "CHECK").
     */
    public static final String TAG_CHECK = "CHECK";

    /**
     * The <em>printf</em> format to dump a tracking line.
     *
     * @see #dumpText(PrintWriter)
     */
    private static final String DUMP_FORMAT = "%1$7d (%2$tF %2$tT) *%3$5s* %4$s%n";

    /**
     * The system time at creation of this instance or the last {@link #reset()}.
     */
    private long processingStart;

    /**
     * The list of tracking entries.
     */
    private final List<TrackingEntry> entries = new ArrayList<TrackingEntry>();

    /**
     * Map of named timers indexed by timer name storing the sytsem time of
     * start of the respective timer.
     */
    private final Map<String, Long> namedTimerEntries = new HashMap<String, Long>();

    /**
     * Creates a new timer.
     */
    public ProcessTracker() {
        reset();
    }

    /**
     * Resets this timer by removing all current entries and timers and adds an
     * initial timer entry
     */
    public void reset() {
        // remove all entries
        entries.clear();
        namedTimerEntries.clear();

        // enter start message
        processingStart = System.currentTimeMillis();
        log(processingStart, TAG_START, "Request Processing");
    }

    /**
     * Dumps the process timer entries to the given writer, one entry per line.
     * See the class comments for the rough format of each message line.
     */
    public void dumpText(PrintWriter writer) {
        log("Dumping ProcessTracker Entries");

        for (TrackingEntry entry : entries) {
            long offset = entry.getTimeStamp() - processingStart;

            writer.printf(DUMP_FORMAT, offset, entry.getTimeStamp(),
                entry.getTag(), entry.getMessage());
        }
    }

    /** Creates an entry with the given message and entry tag {@link #TAG_CHECK} */
    public void log(String message) {
        entries.add(new TrackingEntry(TAG_CHECK, message));
    }

    /** Creates an entry with the given entry tag and message */
    public void log(String tag, String message) {
        entries.add(new TrackingEntry(tag, message));
    }

    /**
     * Creates an entry with the given time, entry tag and message. This is a
     * private method, as for users of this API time stamps are managed by this
     * class. For internal uses the timestamp may be generated outside of this
     * method.
     */
    private void log(long timeStamp, String tag, String message) {
        entries.add(new TrackingEntry(timeStamp, tag, message));
    }

    /**
     * Starts a named timer. If a timer of the same name already exists, it is
     * reset to the current time.
     */
    public void startTimer(String name) {
        long timer = System.currentTimeMillis();
        namedTimerEntries.put(name, timer);

        log(timer, TAG_START, name);
    }

    /**
     * Logs an entry with entry tag {@link #TAG_CHECK} and the message set to
     * the name of the timer and the number of milliseconds ellapsed since the
     * timer start.
     */
    public void checkTimer(String name) {
        if (namedTimerEntries.containsKey(name)) {
            long timer = namedTimerEntries.get(name);
            long currentTime = System.currentTimeMillis();
            log(currentTime, TAG_CHECK, name + ", elapsed = "
                + (currentTime - timer) + "ms");
        }
    }

    /** Process tracker entry keeping timestamp, tag and message */
    private static class TrackingEntry {

        // creation time stamp
        private final long timeStamp;

        // tracking tag
        private final String tag;

        // tracking message
        private final String message;

        TrackingEntry(String tag, String message) {
            this.timeStamp = System.currentTimeMillis();
            this.tag = tag;
            this.message = message;
        }

        TrackingEntry(long timeStamp, String tag, String message) {
            this.timeStamp = timeStamp;
            this.tag = tag;
            this.message = message;
        }

        long getTimeStamp() {
            return timeStamp;
        }

        String getTag() {
            return tag;
        }

        String getMessage() {
            return message;
        }
    }
}
