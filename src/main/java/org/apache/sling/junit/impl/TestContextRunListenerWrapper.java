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
package org.apache.sling.junit.impl;

import org.apache.sling.junit.SlingTestContextProvider;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class TestContextRunListenerWrapper extends RunListener {
    private final RunListener wrapped;
    private long testStartTime;
    
    TestContextRunListenerWrapper(RunListener toWrap) {
        wrapped = toWrap;
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        wrapped.testAssumptionFailure(failure);
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        wrapped.testFailure(failure);
    }

    @Override
    public void testFinished(Description description) throws Exception {
        if(SlingTestContextProvider.hasContext()) {
            SlingTestContextProvider.getContext().output().put("test_execution_time_msec", System.currentTimeMillis() - testStartTime);
        }
        wrapped.testFinished(description);
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        wrapped.testIgnored(description);
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        wrapped.testRunFinished(result);
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        wrapped.testRunStarted(description);
    }

    @Override
    public void testStarted(Description description) throws Exception {
        testStartTime = System.currentTimeMillis();
        wrapped.testStarted(description);
    }
}
