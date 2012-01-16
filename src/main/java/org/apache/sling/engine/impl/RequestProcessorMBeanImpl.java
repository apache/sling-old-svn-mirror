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
    private volatile long min;

    // longest request
    private volatile long max;

    // sum of request durations
    private volatile double sumX;

    // sum of squared request durations
    private volatile double sumX2;

    RequestProcessorMBeanImpl() throws NotCompliantMBeanException {
        super(RequestProcessorMBean.class);
        resetStatistics();
    }

    synchronized void addRequestDuration(final long value) {
        this.n++;

        if (value < this.min) {
            this.min = value;
        }
        if (value > this.max) {
            this.max = value;
        }

        this.sumX += value;
        this.sumX2 += (value * value);
    }

    public long getRequestsCount() {
        return this.n;
    }

    public long getMinRequestDurationMsec() {
        return this.min;
    }

    public long getMaxRequestDurationMsec() {
        return this.max;
    }

    public synchronized double getStandardDeviationDurationMsec() {
        if (this.n > 1) {
            // algorithm taken from
            // http://de.wikipedia.org/wiki/Standardabweichung section
            // "Berechnung für auflaufende Messwerte"
            return Math.sqrt((this.sumX2 - this.sumX * this.sumX / this.n) / (this.n - 1));
        }

        // single data point has no deviation
        return 0;
    }

    public synchronized double getMeanRequestDurationMsec() {
        return this.sumX / this.n;
    }

    public synchronized void resetStatistics() {
        this.min = Long.MAX_VALUE;
        this.max = 0;
        this.n = 0;
    }

}
