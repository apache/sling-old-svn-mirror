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

package org.apache.sling.tracer.internal;

import java.util.Arrays;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.helpers.CyclicBuffer;
import org.apache.sling.api.request.RequestProgressTracker;
import org.slf4j.helpers.MessageFormatter;

class TracerContext {
    static final String QUERY_LOGGER = "org.apache.jackrabbit.oak.query.QueryEngineImpl";

    /**
     * Following queries are internal to Oak and are fired for login/access control
     * etc. They should be ignored. With Oak 1.2+ such queries are logged at trace
     * level (OAK-2304)
     */
    private static final String[] IGNORABLE_QUERIES = {
            "SELECT * FROM [nt:base] WHERE [jcr:uuid] = $id",
            "SELECT * FROM [nt:base] WHERE PROPERTY([rep:members], 'WeakReference') = $uuid",
            "SELECT * FROM [rep:Authorizable]WHERE [rep:principalName] = $principalName",
    };

    private static final int LOG_BUFFER_SIZE = 50;
    /*
     * In memory buffer to store logs till RequestProgressTracker is registered.
     * This would be required for those case where TracerContext is created at
     * normal Filter level which gets invoked before Sling layer is hit.
     *
     * Later when Sling layer is hit and SlingTracerFilter is invoked
     * then it would register the RequestProgressTracker and then these inmemory logs
     * would be dumped there
     */
    private CyclicBuffer<String> buffer;
    private RequestProgressTracker progressTracker;
    private int queryCount;
    private final TracerConfig[] tracers;
    private final Recording recording;

    public TracerContext(TracerConfig[] tracers, Recording recording) {
        this.tracers = tracers;
        this.recording = recording;

        //Say if the list is like com.foo;level=trace,com.foo.bar;level=info.
        // Then first config would result in a match and later config would
        // not be able to suppress the logs from a child category
        //To handle such cases we sort the config. With having more depth i.e. more specific
        //coming first and others later
        Arrays.sort(tracers);
    }

    public boolean shouldLog(String logger, Level level) {
        for (TracerConfig tc : tracers) {
            TracerConfig.MatchResult mr = tc.match(logger, level);
            if (mr == TracerConfig.MatchResult.MATCH_LOG) {
                return true;
            } else if (mr == TracerConfig.MatchResult.MATCH_NO_LOG) {
                return false;
            }
        }
        return false;
    }

    public boolean log(Level level, String logger, String format, Object[] params) {
        recording.log(level, logger, format, params);
        if (QUERY_LOGGER.equals(logger)
                && params != null && params.length == 2) {
            return logQuery((String) params[1]);
        }
        return logWithLoggerName(logger, format, params);
    }

    public void done() {
        if (queryCount > 0) {
            progressTracker.log("JCR Query Count {0}", queryCount);
        }
    }

    /**
     * Registers the progress tracker and also logs all the in memory logs
     * collected so far to the tracker
     */
    public void registerProgressTracker(RequestProgressTracker requestProgressTracker) {
        this.progressTracker = requestProgressTracker;
        if (buffer != null) {
            for (String msg : buffer.asList()) {
                progressTracker.log(msg);
            }
            buffer = null;
        }
    }

    private boolean logWithLoggerName(String loggerName, String format, Object... params) {
        String msg = MessageFormatter.arrayFormat(format, params).getMessage();
        msg = "[" + loggerName + "] " + msg;
        if (progressTracker == null) {
            if (buffer == null) {
                buffer = new CyclicBuffer<String>(LOG_BUFFER_SIZE);
            }
            buffer.add(msg);
        } else {
            progressTracker.log(msg);
        }
        return true;
    }

    private boolean logQuery(String query) {
        if (ignorableQuery(query)) {
            return false;
        }
        queryCount++;
        logWithLoggerName("JCR", " Query {}", query);
        return true;
    }

    private boolean ignorableQuery(String msg) {
        for (String ignorableQuery : IGNORABLE_QUERIES) {
            if (msg.contains(ignorableQuery)) {
                return true;
            }
        }
        return false;
    }
}
