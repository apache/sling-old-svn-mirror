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

public class InvokePerformanceMethod extends Statement {

    private final TestClass testClass;

    private final PerformanceMethod method;

    private final Statement inner;

    private final Listeners listeners;

    public InvokePerformanceMethod(TestClass testClass, FrameworkMethod method, Statement inner, Listeners listeners) {
        this.testClass = testClass;
        this.method = new PerformanceMethod(method);
        this.inner = inner;
        this.listeners = listeners;
    }

    @Override
    public void evaluate() throws Throwable {
        listeners.iterationStarted(testClass.getName(), method.getName());
        inner.evaluate();
        listeners.iterationFinished(testClass.getName(), method.getName());
    }

}
