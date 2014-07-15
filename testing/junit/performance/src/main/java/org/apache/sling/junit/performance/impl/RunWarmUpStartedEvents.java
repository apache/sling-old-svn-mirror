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
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import java.util.List;

public class RunWarmUpStartedEvents extends Statement {

    private final Listeners listeners;

    private final TestClass test;

    private final FrameworkMethod method;

    private final Statement statement;

    public RunWarmUpStartedEvents(Listeners listeners, TestClass test, FrameworkMethod method, Statement statement) {
        this.listeners = listeners;
        this.test = test;
        this.method = method;
        this.statement = statement;
    }

    @Override
    public void evaluate() throws Throwable {
        List<Throwable> errors = listeners.warmUpStarted(test.getName(), method.getName());
        MultipleFailureException.assertEmpty(errors);
        statement.evaluate();
    }

}
