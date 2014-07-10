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

/**
 * Base class for Listener classes with empty methods for every possible event. A listener is made available to the
 * {@link PerformanceRunner} using the {@link Listen} annotation.
 */
public class Listener {

    public void warmUpStarted(String className, String testName) throws Exception {

    }

    public void warmUpFinished(String className, String testName) throws Exception {

    }

    public void executionStarted(String className, String testName) throws Exception {

    }

    public void executionFinished(String className, String testName) throws Exception {

    }

    public void warmUpIterationStarted(String className, String testName) throws Exception {

    }

    public void executionIterationStarted(String className, String testName) throws Exception {

    }

    public void warmUpIterationFinished(String className, String testName) throws Exception {

    }

    public void executionIterationFinished(String className, String testName) throws Exception {

    }

}
