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
package org.apache.sling.event.impl.jobs;

import org.osgi.service.event.Event;

/**
 * This object encapsulates all information about a job.
 */
public abstract class JobEvent {

    public Event event;
    public final String uniqueId;

    public String queueName;

    public long queued = -1;
    public long started = -1;

    public JobEvent(final Event e, final String uniqueId) {
        this.event = e;
        this.uniqueId = uniqueId;
    }

    public abstract boolean lock();
    public abstract void unlock();
    public abstract void finished();
    public abstract boolean reschedule();
    public abstract boolean remove();
    public abstract void restart();

    @Override
    public int hashCode() {
        return this.uniqueId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if ( ! (obj instanceof JobEvent) ) {
            return false;
        }
        return this.uniqueId.equals(((JobEvent)obj).uniqueId);
    }
}