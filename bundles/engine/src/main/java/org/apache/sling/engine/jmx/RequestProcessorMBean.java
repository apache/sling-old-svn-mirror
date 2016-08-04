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
package org.apache.sling.engine.jmx;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This is the management interface for the SlingRequestProcessor.
 */
@ProviderType
public interface RequestProcessorMBean {

    /**
     * Returns the number of requests collected since last resetting the
     * statistics.
     *
     * @return Number of requests
     * @see #resetStatistics()
     */
    long getRequestsCount();

    /**
     * Returns the time in milliseconds used by the longest request since last
     * resetting the statistics.
     *
     * @return Max request duration
     * @see #resetStatistics()
     */
    long getMaxRequestDurationMsec();

    /**
     * Returns the time in milliseconds used by the shortest request since last
     * resetting the statistics.
     *
     * @return Min request duration
     * @see #resetStatistics()
     */
    long getMinRequestDurationMsec();

    /**
     * Returns the mean request processing time in milliseconds since resetting
     * the statistics.
     *
     * @return Mean request duration
     * @see #resetStatistics()
     */
    double getMeanRequestDurationMsec();

    /**
     * Returns the standard deviation of requests since resetting the
     * statistics. If zero or one requests have been collected only, this method
     * returns zero.
     *
     * @return Standard deviation
     * @see #resetStatistics()
     */
    double getStandardDeviationDurationMsec();

    /**
     * Returns the maximum peak recursive execution depth since last
     * resetting the statistics.
     *
     * @return Max peak recursion depth
     * @see #resetStatistics()
     */
    int getMaxPeakRecursionDepth();


    /**
     * Returns the minimal peak recursive execution depth since last
     * resetting the statistics.
     *
     * @return Min peak recursion depth
     * @see #resetStatistics()
     */
    int getMinPeakRecursionDepth();


    /**
     * Returns the mean peak recursive execution depth since last
     * resetting the statistics.
     *
     * @return Mean peak recursion depth
     * @see #resetStatistics()
     */
    double getMeanPeakRecursionDepth();


    /**
     * Returns the standard deviation of peak recursive execution depth since last
     * resetting the statistics.
     *
     * @return Standard deviation of peak recursion depth
     * @see #resetStatistics()
     */
    double getStandardDeviationPeakRecursionDepth();

    /**
     * Returns the maximum servlet call count since last
     * resetting the statistics.
     *
     * @return Max servlet count call
     * @see #resetStatistics()
     */
    int getMaxServletCallCount();


    /**
     * Returns the minimum servlet call count since last
     * resetting the statistics.
     *
     * @return Min servlet count call
     * @see #resetStatistics()
     */
    int getMinServletCallCount();

    /**
     * Returns the mean servlet call count since last
     * resetting the statistics.
     *
     * @return Mean servlet count call
     * @see #resetStatistics()
     */
    double getMeanServletCallCount();

    /**
     * Returns the standard deviation servlet call counts since last
     * resetting the statistics.
     *
     * @return Mean standard deviation for servlet count call
     * @see #resetStatistics()
     */
    double getStandardDeviationServletCallCount();

    /**
     * Resets all statistics values and restarts from zero.
     */
    void resetStatistics();

}
