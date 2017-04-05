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

import java.util.Date;

import javax.management.StandardMBean;

import org.apache.sling.event.jobs.Statistics;
import org.apache.sling.event.jobs.jmx.StatisticsMBean;

public abstract class AbstractJobStatistics extends StandardMBean implements
        StatisticsMBean {

    public AbstractJobStatistics() {
        super(StatisticsMBean.class, false);
    }

    protected abstract Statistics getStatistics();

    public long getAverageProcessingTime() {
        return getStatistics().getAverageProcessingTime();
    }

    public long getAverageWaitingTime() {
        return getStatistics().getAverageWaitingTime();
    }

    public long getLastActivatedJobTime() {
        return getStatistics().getLastActivatedJobTime();
    }

    public long getLastFinishedJobTime() {
        return getStatistics().getLastFinishedJobTime();
    }

    public long getNumberOfActiveJobs() {
        return getStatistics().getNumberOfActiveJobs();
    }

    public long getNumberOfCancelledJobs() {
        return getStatistics().getNumberOfCancelledJobs();
    }

    public long getStartTime() {
        return getStatistics().getStartTime();
    }

    public Date getStartDate() {
        return new Date(getStartTime());
    }

    public long getNumberOfFinishedJobs() {
        return getStatistics().getNumberOfFinishedJobs();
    }

    public long getNumberOfFailedJobs() {
        return getStatistics().getNumberOfFailedJobs();
    }

    public long getNumberOfProcessedJobs() {
        return getStatistics().getNumberOfProcessedJobs();
    }

    public long getNumberOfQueuedJobs() {
        return getStatistics().getNumberOfQueuedJobs();
    }

    public long getNumberOfJobs() {
        return getStatistics().getNumberOfJobs();
    }

    public void reset() {
        getStatistics().reset();
    }

    public Date getLastActivatedJobDate() {
        return new Date(getStatistics().getLastActivatedJobTime());
    }

    public Date getLastFinishedJobDate() {
        return new Date(getStatistics().getLastFinishedJobTime());
    }

}
