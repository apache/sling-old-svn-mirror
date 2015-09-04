/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.impl.vlt;

import org.apache.sling.ide.log.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <tt>Slf4jLogger</tt> is a debugging implementation of the Logger using Slf4j
 * 
 * <p>
 * It's worth noting that the trace methods log their output at INFO level, so that the output is always discoverable
 * </p>
 *
 */
class Slf4jLogger implements Logger {

    private static final long PERF_IGNORE_THRESHOLD = 50;

    private final org.slf4j.Logger wrapped = LoggerFactory.getLogger(Slf4jLogger.class);

    @Override
    public void warn(String message, Throwable cause) {
        wrapped.warn(message, cause);
    }

    @Override
    public void warn(String message) {
        wrapped.warn(message);
    }

    @Override
    public void trace(String message, Throwable error) {
        wrapped.info(message, error);
    }

    @Override
    public void trace(String message, Object... arguments) {

        // this is probably a horribly slow implementation, but it does not matter
        for (int i = 0; i < arguments.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(arguments[i]));
        }

        wrapped.info(message);
    }

    @Override
    public void error(String message, Throwable cause) {
        wrapped.error(message, cause);
    }

    @Override
    public void error(String message) {
        wrapped.error(message);
    }

    @Override
    public void tracePerformance(String message, long duration, Object... arguments) {
        if (duration < PERF_IGNORE_THRESHOLD) {
            return;
        }
        trace(message + " took " + duration + " ms", arguments);
    }
}