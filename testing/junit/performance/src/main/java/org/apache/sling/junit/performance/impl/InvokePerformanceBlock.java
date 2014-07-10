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

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

public class InvokePerformanceBlock extends Statement {

    private final PerformanceMethod method;

    private final Statement inner;

    private final Listeners listeners;

    private final TestClass testClass;

    public InvokePerformanceBlock(TestClass testClass, FrameworkMethod method, Statement inner, Listeners listeners) {
        this.testClass = testClass;
        this.method = new PerformanceMethod(method);
        this.inner = inner;
        this.listeners = listeners;
    }

    @Override
    public void evaluate() throws Throwable {

        // Run warm-up invocations

        listeners.warmUpStarted(testClass.getName(), method.getName());
        run(method.getWarmUpInvocations(), method.getWarmUpTime());
        listeners.warmUpFinished(testClass.getName(), method.getName());

        // Run performance invocations

        listeners.executionStarted(testClass.getName(), method.getName());
        run(method.getRunInvocations(), method.getRunTime());
        listeners.executionFinished(testClass.getName(), method.getName());
    }

    private void run(int invocations, int time) throws Throwable {
        if (invocations > 0) {
            runByInvocations(invocations);
            return;
        }

        if (time > 0) {
            runByTime(time);
            return;
        }

        throw new IllegalArgumentException("no time or number of invocations specified");
    }

    private void runByInvocations(int invocations) throws Throwable {
        for (int i = 0; i < invocations; i++) {
            inner.evaluate();
        }
    }

    private void runByTime(int seconds) throws Throwable {
        long end = System.currentTimeMillis() + seconds * 1000;

        while (System.currentTimeMillis() < end) {
            inner.evaluate();
        }
    }

}
