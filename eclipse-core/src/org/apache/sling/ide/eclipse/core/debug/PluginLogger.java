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
package org.apache.sling.ide.eclipse.core.debug;

import org.eclipse.osgi.util.NLS;

/**
 * The <tt>PluginLogger</tt> is a simple helper class which assists with logging from Eclipse plugins
 *
 */
public interface PluginLogger {

    /**
     * Logs an error message using the platform log facility
     * 
     * @param message message to log
     */
    void error(String message);

    /**
     * Logs an error message using the platform log facility
     * 
     * @param message message to log
     * @param cause the cause
     */
    void error(String message, Throwable cause);

    /**
     * Logs a warning message using the platform log facility
     * 
     * @param message message to log
     */
    void warn(String message);

    /**
     * Logs an warning message using the platform log facility
     * 
     * @param message message to log
     * @param cause the cause
     */
    void warn(String message, Throwable cause);

    /**
     * Sends a trace message using the platform debug facility
     * 
     * <p>
     * By default these trace messages are ignored, and are only logged if debugging is enabled for a specific plug-in.
     * </p>
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
     * @param message A message, using the syntax from {@link NLS#bind(String, Object[])}
     * @param arguments an optional array of arguments
     * 
     * @see <a href="https://wiki.eclipse.org/FAQ_How_do_I_use_the_platform_debug_tracing_facility%3F">How do I use the
     *      platform debug tracing facility?</a>
     */
    void trace(String message, Object... arguments);

}
