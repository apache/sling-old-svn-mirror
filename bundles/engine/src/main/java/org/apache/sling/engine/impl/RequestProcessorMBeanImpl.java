/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.engine.impl;

import javax.management.StandardMBean;

import org.apache.commons.math.stat.descriptive.SynchronizedSummaryStatistics;
import org.apache.sling.engine.jmx.RequestProcessorMBean;

/**
 * This is the implementation of the management interface for the RequestProcessor.
 */
public class RequestProcessorMBeanImpl extends StandardMBean implements RequestProcessorMBean {

    private final SynchronizedSummaryStatistics durationStatistics;

    public RequestProcessorMBeanImpl() {
        super(RequestProcessorMBean.class, false);
        this.durationStatistics = new SynchronizedSummaryStatistics();
    }

    public void addRequestDuration(final long value) {
        durationStatistics.addValue(value);
    }

    public long getRequestsCount() {
        return durationStatistics.getN();
    }

    public double getMaxRequestDurationMsec() {
        return durationStatistics.getMax();
    }

    public double getStandardDeviationDurationMsec() {
        return durationStatistics.getStandardDeviation();
    }
    
    public double getMeanRequestDurationMsec() {
        return durationStatistics.getMean();
    }

    public double getMinRequestDurationMsec() {
        return durationStatistics.getMin();
    }
    
    public void resetStatistics() {
        durationStatistics.clear();
    }

}
