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
package org.apache.sling.core.impl.request;

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.request.RequestProgressTracker;

/**
 * The <code>SlingRequestProgressTracker</code> class provides the
 * functionality to track the progress of request processing. Instances of this
 * class are provided through the
 * {@link org.apache.sling.api.SlingHttpServletRequest#getRequestProgressTracker()}
 * method.
 * <p>
 * The following functionality is provided:
 * <ol>
 * <li>Track the progress of request processing through the
 * {@link #log(String)} and {@link #log(String, Object...)} methods.
 * <li>Ability to measure and track processing times of parts of request
 * processing through the {@link #startTimer(String)} and
 * {@link #logTimer(String)} methods.
 * <li>Dumping the recording messages through the
 * {@link #dump(PrintWriter)} method.
 * <li>Resetting the tracker through the {@link #reset()} method.
 * </ol>
 * <p>
 * <b>Tracking Request Processing</b>
 * <p>
 * As the request being processed, certain steps may be tracked by calling
 * either of the <code>log</code> methods. A tracking entry consists of a time
 * stamp managed by this class, and a tracking message noting the actual step being
 * tracked.
 * <p>
 * <b>Timing Processing Steps</b>
 * </p>
 * Certain steps during request processing may need to be timed in that the time
 * required for processing should be recorded. Instances of this class maintain
 * a map of named timers. Each timer is started (initialized or reset) by
 * calling the {@link #startTimer(String)} method. This method just records the
 * starting time of the named timer.
 * <p>
 * To record the number of milliseconds ellapsed since a timer has been started,
 * the {@link #logTimer(String)} method may be called. This method logs the
 * tracking entry with message
 * consisting of the name of the timer and the number of milliseconds ellapsed
 * since the timer was last {@link #startTimer(String) started}. The
 * {@link #logTimer(String)} method may be called multiple times to record
 * several timed steps.
 * <p>
 * Additional information can be logged using the {@link #logTimer(String, String, Object...)}
 * method.
 * <p>
 * Calling the {@link #startTimer(String)} method with the name of timer which
 * already exists, resets the start time of the named timer to the current
 * system time.
 * <p>
 * <b>Dumping Tracking Entries</b>
 * <p>
 * The {@link #dump(PrintWriter)} methods writes all tracking entries to the given
 * <code>PrintWriter</code>. Each entry is written on a single line
 * consisting of the following fields:
 * <ol>
 * <li>The number of milliseconds since the last {@link #reset()} (or creation)
 * of this timer.
 * <li>The absolute time of the timer in parenthesis.
 * <li>The entry message
 * </ol>
 */
public class SlingRequestProgressTracker implements RequestProgressTracker {

    /**
     * The <em>printf</em> format to dump a tracking line.
     *
     * @see #dumpText(PrintWriter)
     */
    private static final String DUMP_FORMAT = "%1$7d (%2$tF %2$tT) %3$s%n";
    
    /**
     * The name of the timer tracking the processing time of the complete
     * process.
     */
    private static final String REQUEST_PROCESSING_TIMER = "Request Processing";

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
    
    private boolean done;

    /**
     * Creates a new request progress tracker.
     */
    public SlingRequestProgressTracker() {
        reset();
    }

    /**
     * Resets this timer by removing all current entries and timers and adds an
     * initial timer entry
     */
    public void reset() {
        done = false;
        
        // remove all entries
        entries.clear();
        namedTimerEntries.clear();

        // enter start message
        processingStart = startTimerInternal(REQUEST_PROCESSING_TIMER);
    }

    public Iterator<String> getMessages() {
        return new Iterator<String>() {
            private final Iterator<TrackingEntry> entryIter = entries.iterator();

            public boolean hasNext() {
                return entryIter.hasNext();
            }

            public String next() {
                // throws NoSuchElementException if no entries any more
                TrackingEntry entry = entryIter.next();

                long offset = entry.getTimeStamp() - processingStart;
                return String.format(DUMP_FORMAT, offset, entry.getTimeStamp(),
                    entry.getMessage());
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }

    /**
     * Dumps the process timer entries to the given writer, one entry per line.
     * See the class comments for the rough format of each message line.
     */
    public void dump(PrintWriter writer) {
        logTimer(REQUEST_PROCESSING_TIMER,
            "Dumping SlingRequestProgressTracker Entries");

        Iterator<String> messages = getMessages();
        while (messages.hasNext()) {
            writer.print(messages.next());
        }
    }

    /** Creates an entry with the given message. */
    public void log(String message) {
        entries.add(new TrackingEntry(message));
    }

    /** Creates an entry with the given entry tag and message */
    public void log(String format, Object... args) {
        String message = MessageFormat.format(format, args);
        entries.add(new TrackingEntry(message));
    }

    /**
     * Creates an entry with the given time, entry tag and message. This is a
     * private method, as for users of this API time stamps are managed by this
     * class. For internal uses the timestamp may be generated outside of this
     * method.
     */
    private void log(long timeStamp, String message) {
        entries.add(new TrackingEntry(timeStamp, message));
    }

    /**
     * Starts a named timer. If a timer of the same name already exists, it is
     * reset to the current time.
     */
    public void startTimer(String name) {
        startTimerInternal(name);
    }
    
    /**
     * Actually starts the named timer and returns the start time in
     * milliseconds
     */
    private long startTimerInternal(String name) {
        long timer = System.currentTimeMillis();
        namedTimerEntries.put(name, timer);

        log(timer, "Starting " + name);

        return timer;
    }

    /**
     * Logs an entry with the message set to the name of the timer and the
     * number of milliseconds ellapsed since the timer start.
     */
    public void logTimer(String name) {
        if (namedTimerEntries.containsKey(name)) {
            long timer = namedTimerEntries.get(name);
            long currentTime = System.currentTimeMillis();
            log(currentTime, name + ", elapsed = " + (currentTime - timer)
                + "ms");
        }
    }

    public void logTimer(String name, String format, Object... args) {
        if (namedTimerEntries.containsKey(name)) {
            long timer = namedTimerEntries.get(name);
            long currentTime = System.currentTimeMillis();
            String message = MessageFormat.format(format, args);
            log(currentTime, message + ", elapsed = " + (currentTime - timer)
                + "ms");
        }
    }
    
    public void done() {
        if(done) return;
        logTimer(REQUEST_PROCESSING_TIMER, REQUEST_PROCESSING_TIMER + " ends");
        done = true;
    }


    /** Process tracker entry keeping timestamp, tag and message */
    private static class TrackingEntry {

        // creation time stamp
        private final long timeStamp;

        // tracking message
        private final String message;

        TrackingEntry(String message) {
            this.timeStamp = System.currentTimeMillis();
            this.message = message;
        }

        TrackingEntry(long timeStamp, String message) {
            this.timeStamp = timeStamp;
            this.message = message;
        }

        long getTimeStamp() {
            return timeStamp;
        }

        String getMessage() {
            return message;
        }
    }
}
