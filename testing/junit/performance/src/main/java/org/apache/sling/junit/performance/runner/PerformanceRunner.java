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

import org.apache.sling.junit.performance.impl.InvokePerformanceBlock;
import org.apache.sling.junit.performance.impl.InvokePerformanceMethod;
import org.apache.sling.junit.performance.impl.Listeners;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom runner to execute performance tests using JUnit.
 * <p/>
 * For a method to be executed as a performance test, it must be annotated with {@link PerformanceTest}. Every time this
 * annotation is specified, the user must also specify the warm up and execution strategy, because these information are
 * mandatory for the runner to work properly. The warm up and execution strategy can be provided in two ways: by
 * specifying the number of executions to run, or by specifying the amount of time the method should run.
 * <p/>
 * The runner can also invoke one or more {@link Listener}. The listener is specified as a static variable of the test
 * class or as the result of a static method. The listeners are made available to the runner by annotating them with the
 * {@link Listen} annotation.
 */
public class PerformanceRunner extends BlockJUnit4ClassRunner {

    private Listeners listeners;

    public PerformanceRunner(Class<?> testClass) throws InitializationError {
        super(testClass);

        try {
            listeners = new Listeners(readListeners());
        } catch (Exception e) {
            throw new InitializationError(e);
        }
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        return getTestClass().getAnnotatedMethods(PerformanceTest.class);
    }

    @Override
    protected Statement methodBlock(FrameworkMethod method) {
        return new InvokePerformanceBlock(getTestClass(), method, super.methodBlock(method), listeners);
    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        return new InvokePerformanceMethod(getTestClass(), method, super.methodInvoker(method, test), listeners);
    }

    private List<Listener> readListeners() throws Exception {
        List<Listener> listeners = new ArrayList<Listener>();

        listeners.addAll(readListenersFromStaticFields());
        listeners.addAll(readListenersFromStaticMethods());

        return listeners;
    }

    private List<Listener> readListenersFromStaticMethods() throws Exception {
        List<Listener> listeners = new ArrayList<Listener>();

        for (FrameworkMethod method : getTestClass().getAnnotatedMethods(Listen.class)) {
            if (!method.isPublic()) {
                throw new IllegalArgumentException("a @Listen method must be public");
            }

            if (!method.isStatic()) {
                throw new IllegalArgumentException("a @Listen method must be static");
            }

            if (!Listener.class.isAssignableFrom(method.getReturnType())) {
                throw new IllegalArgumentException("a @Listen method must be of type Listener");
            }

            Listener listener = null;

            try {
                listener = (Listener) method.invokeExplosively(null);
            } catch (Throwable throwable) {
                throw new RuntimeException("error while invoking the @Listen method", throwable);
            }

            if (listener == null) {
                throw new IllegalArgumentException("a @Listen method must return a non-null value");
            }

            listeners.add(listener);
        }

        return listeners;
    }

    private List<Listener> readListenersFromStaticFields() throws Exception {
        List<Listener> reporters = new ArrayList<Listener>();

        for (FrameworkField field : getTestClass().getAnnotatedFields(Listen.class)) {
            if (!field.isPublic()) {
                throw new IllegalArgumentException("a @Listen field must be public");
            }

            if (!field.isStatic()) {
                throw new IllegalArgumentException("a @Listen field must be static");
            }

            if (!Listener.class.isAssignableFrom(field.getType())) {
                throw new IllegalArgumentException("a @Listen field must be of type Listener");
            }

            Listener listener = (Listener) field.get(null);

            if (listener == null) {
                throw new IllegalArgumentException("a @Listen field must not be null");
            }

            reporters.add(listener);
        }

        return reporters;
    }

}

