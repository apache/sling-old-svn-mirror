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
package org.apache.sling.ide.log;

/**
 * The <tt>Logger</tt> is a simple helper class which assists with logging
 *
 */
public interface Logger {

    /**
     * Logs an error message
     * 
     * @param message message to log
     */
    void error(String message);

    /**
     * Logs an error message
     * 
     * @param message message to log
     * @param cause the cause
     */
    void error(String message, Throwable cause);

    /**
     * Logs a warning message
     * 
     * @param message message to log
     */
    void warn(String message);

    /**
     * Logs an warning message
     * 
     * @param message message to log
     * @param cause the cause
     */
    void warn(String message, Throwable cause);

    /**
     * Sends a trace message
     * 
     * <p>
     * Usage guide:
     * </p>
     * 
     * <pre>
     * logger.trace(&quot;Joining {0} with {1}&quot;, a, b);
     * a.join(b);
     * </pre>
     * 
     * @param message A message, using positional argument syntax
     * @param arguments an optional array of arguments
     * 
     */
    void trace(String message, Object... arguments);

    /**
     * Sends a trace message
     * 
     * <p>
     * Usage guide:
     * </p>
     * 
     * <pre>
     * try {
     *     // code here
     * } catch (RuntimeException e) {
     *     logger.trace(&quot;An unexpected error has occured&quot;, e);
     * }
     * </pre>
     * 
     * @param message A string message
     * @param error the error that occured
     */
    void trace(String message, Throwable error);

    /**
     * Sends a performance trace message
     * 
     * <p>
     * Note that implementations may choose to not display performance entries which take less than a predefined
     * threshold
     * 
     * @param message A string message
     * @param duration The operation's duration in milliseconds
     * @param arguments an optional array of arguments
     */
    void tracePerformance(String message, long duration, Object... arguments);
}
