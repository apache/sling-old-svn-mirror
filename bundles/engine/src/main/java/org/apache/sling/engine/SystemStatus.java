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
package org.apache.sling.engine;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

/**
 * Provides information on the system status - mostly indicating if the system
 * is ready, or why it is not.
 */
public interface SystemStatus {
    
    @SuppressWarnings("serial")
    static class StatusException extends Exception {
        public StatusException(String reason, Throwable cause) {
            super(reason, cause);
        }
        public StatusException(String reason) {
            super(reason);
        }
    }
    
    /**
     * The SlingMainServlet provides status info if called with this path, and
     * this is also the path under which scripts that define the system status
     * are found.
     */
    String STATUS_PATH = "/system/sling/status";

    /**
     * Throw an exception if the system is not ready to process requests. The
     * readyness state can be cached, and if it is {@link #clear} clears it.
     */
    void checkSystemReady() throws Exception;

    /** Clear any cached state */
    void clear();

    /**
     * Execute the system readyness checking scripts and copy their output to
     * the response
     */
    void doGet(SlingHttpServletRequest req, SlingHttpServletResponse resp)
            throws Exception;
}
