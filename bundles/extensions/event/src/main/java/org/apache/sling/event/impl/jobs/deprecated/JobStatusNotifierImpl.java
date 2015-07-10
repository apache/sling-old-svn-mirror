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
package org.apache.sling.event.impl.jobs.deprecated;

import org.apache.sling.event.jobs.JobProcessor;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;

/**
 * @deprecated
 */
@Deprecated
public class JobStatusNotifierImpl implements JobStatusNotifier {

    private volatile boolean isCalled = false;

    private volatile boolean isMarked = false;

    private volatile JobProcessor processor;

    private volatile JobExecutionContext context;

    @Override
    public boolean getAcknowledge(final JobProcessor processor) {
        synchronized ( this ) {
            this.isCalled = true;
            this.processor = processor;
            this.notify();
            return !isMarked;
        }
    }

    @Override
    public boolean finishedJob(final boolean reschedule) {
        if ( this.context != null ) {
            this.context.asyncProcessingFinished(reschedule ? context.result().failed() : context.result().succeeded());
        }
        return false;
    }

    public void markDone() {
        this.isMarked = true;
    }

    public boolean isCalled() {
        return this.isCalled;
    }

    public JobProcessor getProcessor() {
        return this.processor;
    }

    public void setJobExecutionContext(final JobExecutionContext context) {
        this.context = context;
    }
}
