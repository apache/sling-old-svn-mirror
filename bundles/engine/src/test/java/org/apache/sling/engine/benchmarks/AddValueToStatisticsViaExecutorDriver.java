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
package org.apache.sling.engine.benchmarks;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math.stat.descriptive.SynchronizedSummaryStatistics;

import com.sun.japex.JapexDriverBase;
import com.sun.japex.TestCase;

public class AddValueToStatisticsViaExecutorDriver extends JapexDriverBase {

    private final Random random = new Random();

    private SynchronizedSummaryStatistics statistics;
    private ExecutorService operationExecutor;

    @Override
    public void prepare(TestCase tc) {
        this.statistics = new SynchronizedSummaryStatistics();
        this.operationExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void run(TestCase tc) {
        final long value = random.nextLong();
        operationExecutor.execute(new Runnable() {

            public void run() {
                statistics.addValue(value);
            }
        });
    }
    /*
    @Override
    public void finish(TestCase testCase) {
        operationExecutor.shutdown();
        try {
            operationExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }*/

}
