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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.CoreConstants;

class TracerConfig implements Comparable<TracerConfig> {
    enum MatchResult {
        MATCH_LOG,
        /**
         * Logger category matched but level not. So logging should
         * not be performed and no further TracerConfig should be matched for this
         */
        MATCH_NO_LOG,

        NO_MATCH
    }

    private final String loggerName;
    private final Level level;
    private final int depth;
    private final CallerStackReporter callerReporter;

    public TracerConfig(String loggerName, Level level) {
        this(loggerName, level, null);
    }

    public TracerConfig(String loggerName, Level level,@Nullable CallerStackReporter reporter) {
        this.loggerName = loggerName;
        this.level = level;
        this.depth = getDepth(loggerName);
        this.callerReporter = reporter;
    }

    public boolean match(String loggerName) {
        return loggerName.startsWith(this.loggerName);
    }

    public MatchResult match(String loggerName, Level level) {
        if (loggerName.startsWith(this.loggerName)) {
            if (level.isGreaterOrEqual(this.level)) {
                return MatchResult.MATCH_LOG;
            }
            return MatchResult.MATCH_NO_LOG;
        }
        return MatchResult.NO_MATCH;
    }

    @Override
    public int compareTo(@Nonnull TracerConfig o) {
        int comp = depth > o.depth ? -1 : depth < o.depth ? 1 : 0;
        if (comp == 0) {
            comp = loggerName.compareTo(o.loggerName);
        }
        return comp;
    }

    public int getDepth() {
        return depth;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public Level getLevel() {
        return level;
    }

    public boolean isReportCallerStack(){
        return callerReporter != null;
    }

    public CallerStackReporter getCallerReporter() {
        return callerReporter;
    }

    private static int getDepth(String loggerName) {
        int depth = 0;
        int fromIndex = 0;
        while (true) {
            int index = getSeparatorIndexOf(loggerName, fromIndex);
            depth++;
            if (index == -1) {
                break;
            }
            fromIndex = index + 1;
        }
        return depth;
    }

    /*
     * Taken from LoggerNameUtil. Though its accessible Logback is might not maintain
     * strict backward compatibility for such util classes. So copy the logic
     */
    private static int getSeparatorIndexOf(String name, int fromIndex) {
        int i = name.indexOf(CoreConstants.DOT, fromIndex);
        if (i != -1) {
            return i;
        } else {
            return name.indexOf(CoreConstants.DOLLAR, fromIndex);
        }
    }
}
