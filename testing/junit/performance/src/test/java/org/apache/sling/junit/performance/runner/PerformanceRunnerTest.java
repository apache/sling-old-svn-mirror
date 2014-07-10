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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;

public class PerformanceRunnerTest {

    private Result runTest(Class<?> testClass) {
        return JUnitCore.runClasses(testClass);
    }

    private void assertTestFails(Class<?> testClass, String message) {
        Result result = runTest(testClass);

        boolean isInitializationError = false;

        for (Failure failure : result.getFailures()) {
            isInitializationError = isInitializationError || failure.getException().getMessage().equals(message);
        }

        Assert.assertEquals(true, isInitializationError);
    }

    @RunWith(PerformanceRunner.class)
    public static class PrivateListenerField {

        @Listen
        private static Listener listener = new Listener();

        @PerformanceTest
        public void test() {

        }

    }

    @Test
    public void testPrivateListenerField() {
        assertTestFails(PrivateListenerField.class, "a @Listen field must be public");
    }

    @RunWith(PerformanceRunner.class)
    public static class InstanceListenerField {

        @Listen
        public Listener listener = new Listener();

        @PerformanceTest
        public void test() {

        }

    }

    @Test
    public void testInstanceListenerField() {
        assertTestFails(InstanceListenerField.class, "a @Listen field must be static");
    }

    @RunWith(PerformanceRunner.class)
    public static class WrongTypeListenerField {

        @Listen
        public static Integer listener = 42;

        @PerformanceTest
        public void test() {

        }

    }

    @Test
    public void testWrongTypeListenerField() {
        assertTestFails(WrongTypeListenerField.class, "a @Listen field must be of type Listener");
    }

    @RunWith(PerformanceRunner.class)
    public static class NullListenerField {

        @Listen
        public static Listener listener = null;

        @PerformanceTest
        public void test() {

        }

    }

    @Test
    public void testNullListenerField() {
        assertTestFails(NullListenerField.class, "a @Listen field must not be null");
    }

    @RunWith(PerformanceRunner.class)
    public static class PrivateListenerMethod {

        @Listen
        private static Listener listener() {
            return new Listener();
        }

        @PerformanceTest
        public void test() {

        }

    }

    @Test
    public void testPrivateListenerMethod() {
        assertTestFails(PrivateListenerMethod.class, "a @Listen method must be public");
    }

    @RunWith(PerformanceRunner.class)
    public static class InstanceListenerMethod {

        @Listen
        public Listener listener() {
            return new Listener();
        }

        @PerformanceTest
        public void test() {

        }

    }

    @Test
    public void testInstanceListenerMethod() {
        assertTestFails(InstanceListenerMethod.class, "a @Listen method must be static");
    }

    @RunWith(PerformanceRunner.class)
    public static class WrongTypeListenerMethod {

        @Listen
        public static Integer listener() {
            return 42;
        }

        @PerformanceTest
        public void test() {

        }

    }

    @Test
    public void testWrongTypeListenerMethod() {
        assertTestFails(WrongTypeListenerMethod.class, "a @Listen method must be of type Listener");
    }

    @RunWith(PerformanceRunner.class)
    public static class BuggyListenerMethod {

        @Listen
        public static Listener listener() {
            throw new RuntimeException();
        }

        @PerformanceTest
        public void test() {

        }

    }

    @Test
    public void testBuggyListenerMethod() {
        assertTestFails(BuggyListenerMethod.class, "error while invoking the @Listen method");
    }

    @RunWith(PerformanceRunner.class)
    public static class NullListenerMethod {

        @Listen
        public static Listener listener() {
            return null;
        }

        @PerformanceTest
        public void test() {

        }

    }

    @Test
    public void testNullListenerMethod() {
        assertTestFails(NullListenerMethod.class, "a @Listen method must return a non-null value");
    }

    @RunWith(PerformanceRunner.class)
    public static class ExecutionStrategyNotSpecified {

        @PerformanceTest(warmUpInvocations = 10)
        public void test() {

        }

    }

    @Test
    public void testExecutionStrategyNotSpecified() {
        assertTestFails(ExecutionStrategyNotSpecified.class, "no time or number of invocations specified");
    }

    @RunWith(PerformanceRunner.class)
    public static class WarmUpStrategyNotSpecified {

        @PerformanceTest(runInvocations = 10)
        public void test() {

        }

    }

    @Test
    public void testWarmUpStrategyNotSpecified() {
        assertTestFails(WarmUpStrategyNotSpecified.class, "no time or number of invocations specified");
    }

}
