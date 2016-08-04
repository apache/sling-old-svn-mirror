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

import org.osgi.annotation.versioning.ConsumerType;

/**
 * The <code>RequestLog</code> interface defines the API to be implemented by
 * providers of destinations for request log information. To be able to any
 * request log service writing to output type <i>RequestLog Service</i> the
 * respective services must be registered with the OSGi service registry.
 *
 * @deprecated Use the request progress tracker instead.
 */
@Deprecated
@ConsumerType
public interface RequestLog {

    /**
     * The name of the service property which is compared to the name of the
     * RequestLog service to be used to write the log messages (value is
     * "requestlog.name").
     */
    static final String REQUEST_LOG_NAME = "requestlog.name";

    /**
     * Writes the given <code>message</code> to the output destination. For
     * example a file based implementation might write the message into a log
     * file while an implementation supporting Unix <code>syslogd</code> might
     * send the message to a <code>syslogd</code> server.
     *
     * @param message The message to be logged.
     */
    void write(String message);

    /**
     * Closes this request log. This method is called by the request logging
     * infrastructure, when this instance is not used anymore. Hence, this
     * method allows for any cleanup work to be done.
     */
    void close();
}
