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
package org.apache.sling.api.request;

import java.io.PrintWriter;
import java.util.Iterator;

import aQute.bnd.annotation.ProviderType;

/**
 * The <code>RequestProgressTracker</code> class provides the functionality to
 * track the progress of request processing. Instances of this class are
 * provided through the
 * {@link org.apache.sling.api.SlingHttpServletRequest#getRequestProgressTracker()}
 * method.
 * <p>
 * The following functionality is provided:
 * <ol>
 * <li>Track the progress of request processing through the
 * {@link #log(String)} and {@link #log(String, Object...)} methods.
 * <li>Ability to measure and track processing times of parts of request
 * processing through the {@link #startTimer(String)},
 * {@link #logTimer(String)} and {@link #logTimer(String, String, Object...)}
 * methods.
 * <li>Dumping the recording messages through the {@link #dump(PrintWriter)}
 * method.
 * <li>Return the log messages through the {@link #getMessages()} method.
 * </ol>
 * <p>
 * <b>Tracking Request Processing</b>
 * <p>
 * As the request being processed, certain steps may be tracked by calling
 * either of the <code>log</code> methods. A tracking entry consists of a time
 * stamp managed by this class and a tracking message noting the actual step
 * being tracked.
 * <p>
 * <b>Timing Processing Steps</b>
 * </p>
 * Certain steps during request processing may need to be timed in that the time
 * required for processing should be recorded. Instances of this class maintain
 * a map of named timers. Each timer is started (initialized or reset) by
 * calling the {@link #startTimer(String)} method. This method just records the
 * starting time of the named timer and adds a tracking entry with the timer
 * name as the message.
 * <p>
 * To record the number of milliseconds elapsed since a timer has been started,
 * the {@link #logTimer(String)} or {@link #logTimer(String, String, Object...)}
 * method may be called. This method logs a tracking entry with a message
 * consisting of the name of the timer and the number of milliseconds elapsed
 * since the timer was last {@link #startTimer(String) started}. The
 * <code>logTimer</code> methods may be called multiple times to record
 * several timed steps.
 * <p>
 * Calling the {@link #startTimer(String)} method with the name of timer which
 * already exists, resets the start time of the named timer to the current
 * system time.
 * <p>
 * <b>Retrieving Tracking Entries</b>
 * <p>
 * The {@link #dump(PrintWriter)} method may be used to write the tracking
 * entries to the given <code>PrintWriter</code> to for example log them in a
 * HTML comment. Alternatively the tracking entries may be retrieved as an
 * iterator of messages through the {@link #getMessages()} method. The
 * formatting of the tracking entries is implementation specific.
 */
@ProviderType
public interface RequestProgressTracker {

    /** Creates an entry with the given message */
     void log(String message);

    /**
     * Creates an entry with a message constructed from the given
     * <code>MessageFormat</code> format evaluated using the given formatting
     * arguments.
     */
    void log(String format, Object... args);

    /**
     * Starts a named timer. If a timer of the same name already exists, it is
     * reset to the current time.
     */
    void startTimer(String timerName);

    /**
     * Logs an entry with the message set to the name of the timer and the
     * number of milliseconds elapsed since the timer start.
     */
    void logTimer(String timerName);

    /**
     * Logs an entry with the message constructed from the given
     * <code>MessageFormat</code> pattern evaluated using the given arguments
     * and the number of milliseconds elapsed since the timer start.
     */
    void logTimer(String timerName, String format, Object... args);

    /**
     * Returns an <code>Iterator</code> of tracking entries.
     * If there are no messages <code>null</code> is returned.
     */
    Iterator<String> getMessages();

    /**
     * Dumps the process timer entries to the given writer, one entry per line.
     */
    void dump(PrintWriter writer);

    /**
     *  Call this when done processing the request - only the first call of this
     *  method is processed, all further calls to this method are ignored.
     */
    void done();
}
