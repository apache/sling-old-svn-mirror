/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.performance;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

/**
 * Wrapper for recorded performance statistics and defined threshold
 */
public class PerformanceRecord {

    private final DescriptiveStatistics statistics;

    private final Number threshold;

    public PerformanceRecord(final DescriptiveStatistics statistics, final Number threshold) {
        this.statistics = statistics;
        this.threshold = threshold;
    }

    public DescriptiveStatistics getStatistics() {
        return this.statistics;
    }

    public Number getThreshold() {
        return this.threshold;
    }

    /**
     * Checks internal statistics against <code>reference</code>. Current implementation looks at 50 percentile.
     *
     * @param reference Reference statistics
     * @return An error string if threshold is exceeded, <code>null</code> if not
     */
    public String checkThreshold(DescriptiveStatistics reference) {
        if (threshold == null || threshold.doubleValue() <= 0) {
            return null;
        }
        double ratio = this.statistics.getPercentile(50) / reference.getPercentile(50);
        if (ratio > threshold.doubleValue()) {
            return String.format("Threshold exceeded! Expected <%6.2f, actual %6.2f", threshold.doubleValue(), ratio);
        }
        return null;
    }
}
