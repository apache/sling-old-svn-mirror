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
package org.apache.sling.commons.logservice.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;

public class LogEntryImpl implements LogEntry {

    private final Bundle bundle;

    private final ServiceReference serviceReference;

    private final int level;

    private final String message;

    private final Throwable exception;

    private final long time;

    /* package */LogEntryImpl(Bundle bundle,
            ServiceReference serviceReference, int level, String message,
            Throwable exception) {
        this.bundle = bundle;
        this.serviceReference = serviceReference;
        this.level = level;
        this.message = message;
        this.exception = exception;
        this.time = System.currentTimeMillis();
    }

    public Bundle getBundle() {
        return this.bundle;
    }

    public ServiceReference getServiceReference() {
        return this.serviceReference;
    }

    public int getLevel() {
        return this.level;
    }

    public String getMessage() {
        return this.message;
    }

    public Throwable getException() {
        return this.exception;
    }

    public long getTime() {
        return this.time;
    }
}
