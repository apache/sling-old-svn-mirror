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

package org.apache.sling.junit.performance.runner;

import org.junit.*;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

public class PerformanceRunnerDynamicsTest {

    @RunWith(PerformanceRunner.class)
    public static class AnnotationOrder {

        public static List<String> executions = new ArrayList<String>();

        @BeforeWarmUpIteration
        public void beforeWarmUpIteration() {
            executions.add("before warm up iteration");
        }

        @AfterWarmUpIteration
        public void afterWarmUpIteration() {
            executions.add("after warm up iteration");
        }

        @BeforeWarmUp
        public void beforeWarmUp() {
            executions.add("before warm up");
        }

        @AfterWarmUp
        public void afterWarmUp() {
            executions.add("after warm up");
        }

        @BeforePerformanceIteration
        public void beforePerformanceIteration() {
            executions.add("before performance iteration");
        }

        @AfterPerformanceIteration
        public void afterPerformanceIteration() {
            executions.add("after performance iteration");
        }

        @BeforePerformanceTest
        public void beforePerformanceTest() {
            executions.add("before performance test");
        }

        @AfterPerformanceTest
        public void afterPerformanceTest() {
            executions.add("after performance test");
        }

        @PerformanceTest(warmUpInvocations = 1, runInvocations = 1)
        public void performanceTest() {
            executions.add("performance test");
        }

    }

    @Test
    public void testAnnotationOrder() {
        List<String> expected = new ArrayList<String>();

        expected.add("before warm up");
        expected.add("before warm up iteration");
        expected.add("performance test");
        expected.add("after warm up iteration");
        expected.add("after warm up");
        expected.add("before performance test");
        expected.add("before performance iteration");
        expected.add("performance test");
        expected.add("after performance iteration");
        expected.add("after performance test");

        JUnitCore.runClasses(AnnotationOrder.class);

        Assert.assertEquals(expected, AnnotationOrder.executions);
    }

    @RunWith(PerformanceRunner.class)
    public static class ExistingJUnitAnnotations {

        public static List<String> executions = new ArrayList<String>();

        @BeforeClass
        public static void beforeClass() {
            executions.add("junit before class");
        }

        @AfterClass
        public static void afterClass() {
            executions.add("junit after class");
        }

        @Before
        public void before() {
            executions.add("junit before");
        }

        @After
        public void after() {
            executions.add("junit after");
        }

        @BeforeWarmUpIteration
        public void beforeWarmUpIteration() {
            executions.add("before warm up iteration");
        }

        @AfterWarmUpIteration
        public void afterWarmUpIteration() {
            executions.add("after warm up iteration");
        }

        @BeforeWarmUp
        public void beforeWarmUp() {
            executions.add("before warm up");
        }

        @AfterWarmUp
        public void afterWarmUp() {
            executions.add("after warm up");
        }

        @BeforePerformanceIteration
        public void beforePerformanceIteration() {
            executions.add("before performance iteration");
        }

        @AfterPerformanceIteration
        public void afterPerformanceIteration() {
            executions.add("after performance iteration");
        }

        @BeforePerformanceTest
        public void beforePerformanceTest() {
            executions.add("before performance test");
        }

        @AfterPerformanceTest
        public void afterPerformanceTest() {
            executions.add("after performance test");
        }

        @PerformanceTest(warmUpInvocations = 1, runInvocations = 1)
        public void performanceTest() {
            executions.add("performance test");
        }

    }

    @Test
    public void testExistingJUnitAnnotations() {
        List<String> expected = new ArrayList<String>();

        expected.add("junit before class");
        expected.add("junit before");
        expected.add("before warm up");
        expected.add("before warm up iteration");
        expected.add("performance test");
        expected.add("after warm up iteration");
        expected.add("after warm up");
        expected.add("before performance test");
        expected.add("before performance iteration");
        expected.add("performance test");
        expected.add("after performance iteration");
        expected.add("after performance test");
        expected.add("junit after");
        expected.add("junit after class");

        JUnitCore.runClasses(ExistingJUnitAnnotations.class);

        Assert.assertEquals(expected, ExistingJUnitAnnotations.executions);
    }

    @RunWith(PerformanceRunner.class)
    public static class PerformanceListeners {

        public static List<String> executions = new ArrayList<String>();

        @Listen
        public static Listener listener = new Listener() {

            @Override
            public void warmUpStarted(String className, String testName) throws Exception {
                executions.add("warm up started event");
            }

            @Override
            public void warmUpFinished(String className, String testName) throws Exception {
                executions.add("warm up finished event");
            }

            @Override
            public void executionStarted(String className, String testName) throws Exception {
                executions.add("execution started event");
            }

            @Override
            public void executionFinished(String className, String testName) throws Exception {
                executions.add("execution finished event");
            }

            @Override
            public void warmUpIterationStarted(String className, String testName) throws Exception {
                executions.add("warm up iteration started event");
            }

            @Override
            public void executionIterationStarted(String className, String testName) throws Exception {
                executions.add("execution iteration started event");
            }

            @Override
            public void warmUpIterationFinished(String className, String testName) throws Exception {
                executions.add("warm up iteration finished event");
            }

            @Override
            public void executionIterationFinished(String className, String testName) throws Exception {
                executions.add("execution iteration finished event");
            }

        };

        @BeforeWarmUpIteration
        public void beforeWarmUpIteration() {
            executions.add("before warm up iteration");
        }

        @AfterWarmUpIteration
        public void afterWarmUpIteration() {
            executions.add("after warm up iteration");
        }

        @BeforeWarmUp
        public void beforeWarmUp() {
            executions.add("before warm up");
        }

        @AfterWarmUp
        public void afterWarmUp() {
            executions.add("after warm up");
        }

        @BeforePerformanceIteration
        public void beforePerformanceIteration() {
            executions.add("before performance iteration");
        }

        @AfterPerformanceIteration
        public void afterPerformanceIteration() {
            executions.add("after performance iteration");
        }

        @BeforePerformanceTest
        public void beforePerformanceTest() {
            executions.add("before performance test");
        }

        @AfterPerformanceTest
        public void afterPerformanceTest() {
            executions.add("after performance test");
        }

        @PerformanceTest(warmUpInvocations = 1, runInvocations = 1)
        public void performanceTest() {
            executions.add("performance test");
        }

    }

    @Test
    public void testPerformanceListeners() {
        List<String> expected = new ArrayList<String>();

        expected.add("before warm up");
        expected.add("warm up started event");
        expected.add("before warm up iteration");
        expected.add("warm up iteration started event");
        expected.add("performance test");
        expected.add("warm up iteration finished event");
        expected.add("after warm up iteration");
        expected.add("warm up finished event");
        expected.add("after warm up");

        expected.add("before performance test");
        expected.add("execution started event");
        expected.add("before performance iteration");
        expected.add("execution iteration started event");
        expected.add("performance test");
        expected.add("execution iteration finished event");
        expected.add("after performance iteration");
        expected.add("execution finished event");
        expected.add("after performance test");

        JUnitCore.runClasses(PerformanceListeners.class);

        Assert.assertEquals(expected, PerformanceListeners.executions);
    }

}
