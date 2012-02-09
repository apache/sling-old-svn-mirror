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

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.sling.engine.jmx.RequestProcessorMBean;

/**
 * This is the implementation of the management interface for the
 * RequestProcessor.
 */
class RequestProcessorMBeanImpl extends StandardMBean implements RequestProcessorMBean {

    // number of requests
    private volatile long n;

    // shortest request
    private volatile long durationMsecMin;

    // longest request
    private volatile long durationMsecMax;

    // sum of request durations
    private volatile double durationMsecSumX;

    // sum of squared request durations
    private volatile double durationMsecSumX2;

    RequestProcessorMBeanImpl() throws NotCompliantMBeanException {
        super(RequestProcessorMBean.class);
        resetStatistics();
    }

    synchronized void addRequestData(final long duration) {
        this.n++;

        if (duration < this.durationMsecMin) {
            this.durationMsecMin = duration;
        }
        if (duration > this.durationMsecMax) {
            this.durationMsecMax = duration;
        }

        this.durationMsecSumX += duration;
        this.durationMsecSumX2 += (duration * duration);
    }

    public long getRequestsCount() {
        return this.n;
    }

    public long getMinRequestDurationMsec() {
        return this.durationMsecMin;
    }

    public long getMaxRequestDurationMsec() {
        return this.durationMsecMax;
    }

    public synchronized double getStandardDeviationDurationMsec() {
        if (this.n > 1) {
            // algorithm taken from
            // http://de.wikipedia.org/wiki/Standardabweichung section
            // "Berechnung für auflaufende Messwerte"
            return Math.sqrt((this.durationMsecSumX2 - this.durationMsecSumX * this.durationMsecSumX / this.n) / (this.n - 1));
        }

        // single data point has no deviation
        return 0;
    }

    public synchronized double getMeanRequestDurationMsec() {
        if (this.n > 0) {
            return this.durationMsecSumX / this.n;
        } else {
            return 0;
        }
    }

    public synchronized void resetStatistics() {
        this.durationMsecMin = Long.MAX_VALUE;
        this.durationMsecMax = 0;
        this.n = 0;
    }

}
