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
package org.apache.sling.event.impl.jobs.jcr;

import org.apache.sling.event.impl.jobs.JobEvent;
import org.apache.sling.event.jobs.JobUtil;
import org.osgi.service.event.Event;

/**
 * This object encapsulates all information about a job.
 */
public class JCRJobEvent extends JobEvent {

    private final PersistenceHandler handler;

    public JCRJobEvent(final Event e, final PersistenceHandler handler) {
        super(e, (String)e.getProperty(JobUtil.JOB_ID));
        this.handler = handler;
    }

    @Override
    public boolean lock() {
        return this.handler.lock(this);
    }

    @Override
    public void unlock() {
        this.handler.unlock(this);
    }

    @Override
    public void finished() {
        this.handler.finished(this);
    }

    @Override
    public boolean reschedule() {
        return this.handler.reschedule(this);
    }

    @Override
    public boolean remove() {
        return this.handler.remove(this.uniqueId);
    }

    @Override
    public void restart() {
        this.handler.restart(this);
    }
}