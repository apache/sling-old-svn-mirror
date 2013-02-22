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
 * Dummy stats that just returns 0 for all info, used where the queue doesnt
 * implement the Statistics interface.
 */
public class EmptyStatistics implements Statistics {

    public long getStartTime() {
        return 0;
    }

    public long getNumberOfFinishedJobs() {
        return 0;
    }

    public long getNumberOfCancelledJobs() {
        return 0;
    }

    public long getNumberOfFailedJobs() {
        return 0;
    }

    public long getNumberOfProcessedJobs() {
        return 0;
    }

    public long getNumberOfActiveJobs() {
        return 0;
    }

    public long getNumberOfQueuedJobs() {
        return 0;
    }

    public long getNumberOfJobs() {
        return 0;
    }

    public long getLastActivatedJobTime() {
        return 0;
    }

    public long getLastFinishedJobTime() {
        return 0;
    }

    public long getAverageWaitingTime() {
        return 0;
    }

    public long getAverageProcessingTime() {
        return 0;
    }

    public void reset() {
    }

}
