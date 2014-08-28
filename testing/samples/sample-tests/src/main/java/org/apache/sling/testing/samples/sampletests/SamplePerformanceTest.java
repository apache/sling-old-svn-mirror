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
package org.apache.sling.testing.samples.sampletests;

import org.apache.sling.junit.performance.runner.Listen;
import org.apache.sling.junit.performance.runner.Listener;
import org.apache.sling.junit.performance.runner.PerformanceRunner;
import org.apache.sling.junit.performance.runner.PerformanceTest;
import org.junit.runner.RunWith;

import java.util.HashMap;

// Performance tests are run using the PerformanceRunner custom test runner

@RunWith(PerformanceRunner.class)
public class SamplePerformanceTest {

    private static HashMap<String, String> map;

    private static String key;

    private static String value;

    // You can declare one or more listeners. Listeners are subclasses of Listener and are provided by a static field
    // or method annotated by @Listen

    @Listen
    public static Listener createTestDataListener = new Listener() {

        @Override
        public void warmUpStarted(String className, String testName) throws Exception {
            map = new HashMap<String, String>();
        }

        @Override
        public void executionFinished(String className, String testName) throws Exception {
            map = null;
        }

        @Override
        public void warmUpIterationStarted(String className, String testName) throws Exception {
            generateRandomKeyValue();
        }

        @Override
        public void executionIterationStarted(String className, String testName) throws Exception {
            generateRandomKeyValue();
        }

        private void generateRandomKeyValue() {
            long time = System.nanoTime();

            key = Long.toString(time);
            value = Long.toString(time + 1);
        }

    };

    // A performance test must be annotated with @PerformanceTest to be recognized by the custom runner. This
    // performance tests is executed using a strategy based on the number of invocations. In this case, we want to
    // execute the method 10 times to warm up, and 1000 times to measure the real performance.

    @PerformanceTest(warmUpInvocations = 10, runInvocations = 1000)
    public void testSameKeyDifferentValues() {
        map.put("key", value);
    }

    // Another execution strategy is based on the number of seconds. In this case, we execute the warm up for three
    // seconds, then we measure the performance for five seconds.

    @PerformanceTest(warmUpTime = 3, runTime = 5)
    public void testDifferentKeysAndValues() {
        map.put(key, value);
    }

}