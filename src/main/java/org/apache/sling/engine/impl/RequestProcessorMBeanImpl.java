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

import org.apache.sling.engine.impl.request.RequestData;
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

    private volatile int servletCallCountMin;

    private volatile int servletCallCountMax;

    private volatile double servletCallCountSumX;

    private volatile double servletCallCountSumX2;

    private volatile int peakRecursionDepthMin;

    private volatile int peakRecursionDepthMax;

    private volatile double peakRecursionDepthSumX;

    private volatile double peakRecursionDepthSumX2;

    RequestProcessorMBeanImpl() throws NotCompliantMBeanException {
        super(RequestProcessorMBean.class);
        resetStatistics();
    }

    synchronized void addRequestData(final RequestData data) {
        this.n++;
        
        final long duration = data.getElapsedTimeMsec();
        final int servletCallCount = data.getServletCallCount();
        final int peakRecursionDepth = data.getPeakRecusionDepth();

        if (duration < this.durationMsecMin) {
            this.durationMsecMin = duration;
        }
        if (duration > this.durationMsecMax) {
            this.durationMsecMax = duration;
        }

        this.durationMsecSumX += duration;
        this.durationMsecSumX2 += (duration * duration);
        
        if (servletCallCount < this.servletCallCountMin) {
            this.servletCallCountMin = servletCallCount;
        }
        if (servletCallCount > this.servletCallCountMax) {
            this.servletCallCountMax = servletCallCount;
        }
        this.servletCallCountSumX += servletCallCount;
        this.servletCallCountSumX2 += (servletCallCount * servletCallCount);
        
        if (peakRecursionDepth < this.peakRecursionDepthMin) {
            this.peakRecursionDepthMin = peakRecursionDepth;
        }
        if (peakRecursionDepth > this.peakRecursionDepthMax) {
            this.peakRecursionDepthMax = peakRecursionDepth;
        }
        this.peakRecursionDepthSumX += peakRecursionDepth;
        this.peakRecursionDepthSumX2 += (peakRecursionDepth * peakRecursionDepth);
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
            // "Berechnung fuer auflaufende Messwerte"
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
        this.servletCallCountMin = Integer.MAX_VALUE;
        this.servletCallCountMax = 0;
        this.peakRecursionDepthMin = Integer.MAX_VALUE;
        this.peakRecursionDepthMax = 0;
        this.durationMsecSumX=0d;
        this.durationMsecSumX2=0d;
        this.peakRecursionDepthSumX=0d;
        this.peakRecursionDepthSumX2=0d;
        this.servletCallCountSumX=0d;
        this.servletCallCountSumX2=0d;
        this.n = 0;
    }

    public int getMaxPeakRecursionDepth() {
        return peakRecursionDepthMax;
    }

    public int getMinPeakRecursionDepth() {
        return peakRecursionDepthMin;
    }

    public double getMeanPeakRecursionDepth() {
        if (this.n > 0) {
            return this.peakRecursionDepthSumX / this.n;
        } else {
            return 0;
        }
    }

    public double getStandardDeviationPeakRecursionDepth() {
        if (this.n > 1) {
            return Math.sqrt((this.peakRecursionDepthSumX2 - this.peakRecursionDepthSumX * this.peakRecursionDepthSumX / this.n) / (this.n - 1));
        }

        // single data point has no deviation
        return 0;
    }

    public int getMaxServletCallCount() {
        return this.servletCallCountMax;
    }

    public int getMinServletCallCount() {
        return this.servletCallCountMin;
    }

    public double getMeanServletCallCount() {
        if (this.n > 0) {
            return this.servletCallCountSumX / this.n;
        } else {
            return 0;
        }
    }

    public double getStandardDeviationServletCallCount() {
        if (this.n > 1) {
            return Math.sqrt((this.servletCallCountSumX2 - this.servletCallCountSumX * this.servletCallCountSumX / this.n) / (this.n - 1));
        }

        // single data point has no deviation
        return 0;
    }

}
