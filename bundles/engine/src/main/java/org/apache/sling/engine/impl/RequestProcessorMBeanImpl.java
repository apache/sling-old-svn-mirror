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

import java.util.concurrent.atomic.AtomicReference;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.sling.engine.impl.request.RequestData;
import org.apache.sling.engine.jmx.RequestProcessorMBean;

/**
 * This is the implementation of the management interface for the
 * RequestProcessor.
 */
class RequestProcessorMBeanImpl extends StandardMBean implements RequestProcessorMBean {

    private final AtomicReference<Data> dataRef = new AtomicReference<Data>(new Data());

    RequestProcessorMBeanImpl() throws NotCompliantMBeanException {
        super(RequestProcessorMBean.class);
    }

    void addRequestData(final RequestData data) {
        
        // do a non-blocking busy loop and atomically set the new data
        // the advantage of this algorithm is that there is no blocking
        // involved
        // 
        // there might be some memory churn under high contention, but that
        // remains to be seen
        for ( ;; ) {

            Data oldVal = dataRef.get();
            Data newVal = new Data(oldVal, data);

            boolean success = dataRef.compareAndSet(oldVal, newVal);
            if ( success ) {
                break;
            }
        }
    }
    
    public void resetStatistics() {
        dataRef.set(new Data());
    }    

    public long getRequestsCount() {
        return dataRef.get().n;
    }

    public long getMinRequestDurationMsec() {
        return dataRef.get().durationMsecMin;
    }

    public long getMaxRequestDurationMsec() {
        return dataRef.get().durationMsecMax;
    }

    public double getStandardDeviationDurationMsec() {
        return dataRef.get().standardDeviationDurationMsec;
    }

    public double getMeanRequestDurationMsec() {
        return dataRef.get().meanRequestDurationMsec;
    }

    public int getMaxPeakRecursionDepth() {
        return dataRef.get().peakRecursionDepthMax;
    }

    public int getMinPeakRecursionDepth() {
        return dataRef.get().peakRecursionDepthMin;
    }

    public double getMeanPeakRecursionDepth() {
        return dataRef.get().meanPeakRecursionDepth;
    }

    public double getStandardDeviationPeakRecursionDepth() {
        return dataRef.get().standardDeviationPeakRecursionDepth;
    }

    public int getMaxServletCallCount() {
        return dataRef.get().servletCallCountMax;
    }

    public int getMinServletCallCount() {
        return dataRef.get().servletCallCountMin;
    }

    public double getMeanServletCallCount() {
        
        return dataRef.get().meanServletCallCount;
    }

    public double getStandardDeviationServletCallCount() {
        
        return dataRef.get().standardDeviationServletCallCount;
    }
    
    /**
     * Helper class to atomically hold raw data and compute statistics
     */
    private static class Data {
        
        // number of requests
        private final long n;

        // shortest request
        private final long durationMsecMin;

        // longest request
        private final long durationMsecMax;

        // sum of request durations
        private final double durationMsecSumX;

        // sum of squared request durations
        private final double durationMsecSumX2;

        private final int servletCallCountMin;

        private final int servletCallCountMax;

        private final double servletCallCountSumX;

        private final double servletCallCountSumX2;

        private final int peakRecursionDepthMin;

        private final int peakRecursionDepthMax;

        private final double peakRecursionDepthSumX;

        private final double peakRecursionDepthSumX2;

        private final double standardDeviationDurationMsec;

        private final double meanRequestDurationMsec;

        private final double meanPeakRecursionDepth;
        
        private final double standardDeviationPeakRecursionDepth;
        
        private final double meanServletCallCount;

        private final double standardDeviationServletCallCount;
        
        // computed fields
        
        Data() {
            n = 0;
            
            durationMsecMin = Long.MAX_VALUE;
            durationMsecMax = 0;
            durationMsecSumX = 0;
            durationMsecSumX2 = 0;
            
            servletCallCountMin = Integer.MAX_VALUE;
            servletCallCountMax = 0;
            servletCallCountSumX = 0;
            servletCallCountSumX2 = 0;
            
            peakRecursionDepthMin = Integer.MAX_VALUE;
            peakRecursionDepthMax = 0;
            peakRecursionDepthSumX = 0;
            peakRecursionDepthSumX2 = 0;
            
            standardDeviationDurationMsec = computeStandardDeviationDurationMsec();
            meanRequestDurationMsec = computeMeanRequestDurationMsec();
            meanPeakRecursionDepth = computeMeanPeakRecursionDepth();
            standardDeviationPeakRecursionDepth = computeStandardDeviationPeakRecursionDepth();
            meanServletCallCount = computeMeanServletCallCount();
            standardDeviationServletCallCount = computeStandardDeviationServletCallCount();
        }
        
        Data(Data other, RequestData data) {
            if ( other == null || data == null ) {
                throw new IllegalArgumentException("Neither 'other' nor 'data' may be null");
            }
            
            final long duration = data.getElapsedTimeMsec();
            final int servletCallCount = data.getServletCallCount();
            final int peakRecursionDepth = data.getPeakRecusionDepth();

            n = other.n + 1;
            
            durationMsecMin = Math.min(duration, other.durationMsecMin);
            durationMsecMax = Math.max(duration, other.durationMsecMax);
            durationMsecSumX = other.durationMsecSumX + duration;
            durationMsecSumX2 = other.durationMsecSumX2 + (duration * duration);
            
            servletCallCountMin = Math.min(servletCallCount, other.servletCallCountMin);
            servletCallCountMax = Math.max(servletCallCount, other.servletCallCountMax);
            servletCallCountSumX = other.servletCallCountSumX + servletCallCount;
            servletCallCountSumX2 = other.servletCallCountSumX2 + (servletCallCount * servletCallCount);
            
            peakRecursionDepthMin = Math.min(peakRecursionDepth , other.peakRecursionDepthMin);
            peakRecursionDepthMax = Math.max(peakRecursionDepth , other.peakRecursionDepthMax);
            peakRecursionDepthSumX = other.peakRecursionDepthSumX + peakRecursionDepth;
            peakRecursionDepthSumX2 = other.peakRecursionDepthSumX2 + (peakRecursionDepth * peakRecursionDepth);
            
            standardDeviationDurationMsec = computeStandardDeviationDurationMsec();
            meanRequestDurationMsec = computeMeanRequestDurationMsec();
            meanPeakRecursionDepth = computeMeanPeakRecursionDepth();
            standardDeviationPeakRecursionDepth = computeStandardDeviationPeakRecursionDepth();
            meanServletCallCount = computeMeanServletCallCount();
            standardDeviationServletCallCount = computeStandardDeviationServletCallCount();
        }

        private double computeStandardDeviationDurationMsec() {
            if (this.n > 1) {
                // algorithm taken from
                // http://de.wikipedia.org/wiki/Standardabweichung section
                // "Berechnung fuer auflaufende Messwerte"
                return Math.sqrt((this.durationMsecSumX2 - this.durationMsecSumX * this.durationMsecSumX / this.n) / (this.n - 1));
            }

            // single data point has no deviation
            return 0;
        }

        private double computeMeanRequestDurationMsec() {
            if (this.n > 0) {
                return this.durationMsecSumX / this.n;
            } else {
                return 0;
            }
        }
        
        private double computeMeanPeakRecursionDepth() {
            if (this.n > 0) {
                return this.peakRecursionDepthSumX / this.n;
            } else {
                return 0;
            }
        }

        private double computeStandardDeviationPeakRecursionDepth() {
            if (this.n > 1) {
                return Math.sqrt((this.peakRecursionDepthSumX2 - this.peakRecursionDepthSumX * this.peakRecursionDepthSumX / this.n) / (this.n - 1));
            }

            // single data point has no deviation
            return 0;
        }
        
        private double computeMeanServletCallCount() {
            if (this.n > 0) {
                return this.servletCallCountSumX / this.n;
            } else {
                return 0;
            }
        }

        private double computeStandardDeviationServletCallCount() {
            if (this.n > 1) {
                return Math.sqrt((this.servletCallCountSumX2 - this.servletCallCountSumX * this.servletCallCountSumX / this.n) / (this.n - 1));
            }

            // single data point has no deviation
            return 0;
        }
    }

}
