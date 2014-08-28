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

package org.apache.sling.junit.performance.impl;

import org.junit.runners.model.Statement;

public class RunIterations extends Statement {

    private final int invocations;

    private final int seconds;

    private final Statement iteration;

    public RunIterations(int invocations, int seconds, Statement iteration) {
        this.invocations = invocations;
        this.seconds = seconds;
        this.iteration = iteration;
    }

    @Override
    public void evaluate() throws Throwable {
        if (invocations > 0) {
            runByInvocations();
            return;
        }

        if (seconds > 0) {
            runByTime();
            return;
        }

        throw new IllegalArgumentException("Number of invocations or seconds not provided");
    }

    private void runByTime() throws Throwable {
        long end = System.currentTimeMillis() + seconds * 1000;

        while (System.currentTimeMillis() < end) {
            iteration.evaluate();
        }
    }

    private void runByInvocations() throws Throwable {
        for (int i = 0; i < invocations; i++) {
            iteration.evaluate();
        }
    }

}
