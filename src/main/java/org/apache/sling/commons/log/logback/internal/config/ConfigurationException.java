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
package org.apache.sling.commons.log.logback.internal.config;

public class ConfigurationException extends Exception {

    private static final long serialVersionUID = -9213226340780391070L;

    private final String property;

    private final String reason;

    public ConfigurationException(final String property, final String reason) {
        this(property, reason, null);
    }

    public ConfigurationException(final String property, final String reason, final Throwable cause) {
        super("", cause);
        this.property = property;
        this.reason = reason;
    }

    public String getProperty() {
        return property;
    }

    public String getReason() {
        return reason;
    }
}
