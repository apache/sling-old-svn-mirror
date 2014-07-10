/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sling.junit.performance.listener;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.sling.junit.performance.runner.Listener;

import java.util.concurrent.TimeUnit;

/**
 * A performance test listener which computes statistics about the execution of a test and makes them available in form
 * of a {@link org.apache.commons.math.stat.descriptive.DescriptiveStatistics} object.
 * <p/>
 * Clients of this listener are supposed to subclass it and to override the {@link #executionStatistics} method to react
 * when new statistics for a method are available.
 */
public abstract class StatisticsListener extends Listener {

    private DescriptiveStatistics statistics;

    private long begin;

    @Override
    public void executionStarted(String className, String testName) throws Exception {
        statistics = new DescriptiveStatistics();
    }

    @Override
    public void executionIterationStarted(String className, String testName) throws Exception {
        begin = System.nanoTime();
    }

    @Override
    public void executionIterationFinished(String className, String testName) throws Exception {
        statistics.addValue(TimeUnit.MILLISECONDS.convert(System.nanoTime() - begin, TimeUnit.NANOSECONDS));
    }

    @Override
    public void executionFinished(String className, String testName) throws Exception {
        executionStatistics(className, testName, statistics);
    }

    /**
     * This method is called when new statistics are available for a performance method.
     *
     * @param className  Name of the class containing the performance test.
     * @param testName   Name of the method implementing the performance test.
     * @param statistics Statistics about the executions of the performance test.
     * @throws Exception
     */
    protected abstract void executionStatistics(String className, String testName, DescriptiveStatistics statistics) throws Exception;

}
