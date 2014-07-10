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

import org.apache.sling.junit.performance.runner.PerformanceTest;
import org.junit.runners.model.FrameworkMethod;

public class PerformanceMethod {

    private final FrameworkMethod method;

    public PerformanceMethod(FrameworkMethod method) {
        this.method = method;
    }

    private PerformanceTest getPerformanceTestAnnotation() {
        PerformanceTest performanceTest = method.getAnnotation(PerformanceTest.class);

        if (performanceTest == null) {
            throw new IllegalStateException("a performance method should be annotated with @PerformanceTest");
        }

        return performanceTest;
    }

    public int getWarmUpTime() {
        return getPerformanceTestAnnotation().warmUpTime();
    }

    public int getWarmUpInvocations() {
        return getPerformanceTestAnnotation().warmUpInvocations();
    }

    public int getRunTime() {
        return getPerformanceTestAnnotation().runTime();
    }

    public int getRunInvocations() {
        return getPerformanceTestAnnotation().runInvocations();
    }

    public String getName() {
        return method.getName();
    }

}
