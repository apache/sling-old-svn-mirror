/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.event.impl.jobs.jmx;

import org.apache.sling.event.jobs.Statistics;

/**
 * Dummy statistics for testing purposes.
 */
public class DummyStatistics implements Statistics {

    private long base;

    public DummyStatistics(long base) {
        this.base = base;
        
    }
    public long getStartTime() {
        return base+1;
    }

    public long getNumberOfFinishedJobs() {
        return base+2;
    }

    public long getNumberOfCancelledJobs() {
        return base+3;
    }

    public long getNumberOfFailedJobs() {
        return base+4;
    }

    public long getNumberOfProcessedJobs() {
        return base+5;
    }

    public long getNumberOfActiveJobs() {
        return base+6;
    }

    public long getNumberOfQueuedJobs() {
        return base+7;
    }

    public long getNumberOfJobs() {
        return base+8;
    }

    public long getLastActivatedJobTime() {
        return base+9;
    }

    public long getLastFinishedJobTime() {
        return base+10;
    }

    public long getAverageWaitingTime() {
        return base+11;
    }

    public long getAverageProcessingTime() {
        return base+12;
    }

    public void reset() {
    }

}
