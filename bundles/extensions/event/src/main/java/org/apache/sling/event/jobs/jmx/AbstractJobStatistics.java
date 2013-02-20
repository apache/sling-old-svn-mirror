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
package org.apache.sling.event.jobs.jmx;

import java.util.Date;

import javax.management.StandardMBean;

import org.apache.sling.event.jobs.Statistics;

public abstract class AbstractJobStatistics extends StandardMBean implements
        StatisticsMBean {

    public AbstractJobStatistics() {
        super(StatisticsMBean.class, false);
    }

    protected abstract Statistics getAggregateStatistics();

    public long getAverageProcessingTime() {
        return getAggregateStatistics().getAverageProcessingTime();
    }

    public long getAverageWaitingTime() {
        return getAggregateStatistics().getAverageWaitingTime();
    }

    public long getLastActivatedJobTime() {
        return getAggregateStatistics().getLastActivatedJobTime();
    }

    public long getLastFinishedJobTime() {
        return getAggregateStatistics().getLastFinishedJobTime();
    }

    public long getNumberOfActiveJobs() {
        return getAggregateStatistics().getNumberOfActiveJobs();
    }

    public long getNumberOfCancelledJobs() {
        return getAggregateStatistics().getNumberOfCancelledJobs();
    }

    public long getStartTime() {
        return getAggregateStatistics().getStartTime();
    }

    public long getNumberOfFinishedJobs() {
        return getAggregateStatistics().getNumberOfFinishedJobs();
    }

    public long getNumberOfFailedJobs() {
        return getAggregateStatistics().getNumberOfFailedJobs();
    }

    public long getNumberOfProcessedJobs() {
        return getAggregateStatistics().getNumberOfProcessedJobs();
    }

    public long getNumberOfQueuedJobs() {
        return getAggregateStatistics().getNumberOfQueuedJobs();
    }

    public long getNumberOfJobs() {
        return getAggregateStatistics().getNumberOfJobs();
    }

    public void reset() {
        getAggregateStatistics().reset();
    }

    public Date getLastActivatedJobDate() {
        return new Date(getAggregateStatistics().getLastActivatedJobTime());
    }

    public Date getLastFinishedJobDate() {
        return new Date(getAggregateStatistics().getLastFinishedJobTime());
    }

}
