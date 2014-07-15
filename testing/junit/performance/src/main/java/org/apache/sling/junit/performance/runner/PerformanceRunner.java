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

import org.apache.sling.junit.performance.impl.*;
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
 * <p/>
 * The runner support lifecycle methods which are executed at various stages of the performance test. These methods are
 * annotated with {@link BeforePerformanceIteration}, {@link AfterPerformanceIteration}, {@link BeforePerformanceTest},
 * {@link AfterPerformanceTest}, {@link BeforeWarmUpIteration}, {@link AfterWarmUpIteration}, {@link BeforeWarmUp} and
 * {@link AfterWarmUp}. Every other standard JUnit annotation is also supported.
 */
public class PerformanceRunner extends BlockJUnit4ClassRunner {

    private Listeners listeners;

    public PerformanceRunner(Class<?> testClass) throws InitializationError {
        super(testClass);

        try {
            listeners = new Listeners(getListeners());
        } catch (Throwable e) {
            throw new InitializationError(e);
        }
    }

    @Override
    protected void collectInitializationErrors(List<Throwable> errors) {
        super.collectInitializationErrors(errors);

        validatePublicVoidNoArgMethods(PerformanceTest.class, false, errors);
        validatePublicVoidNoArgMethods(BeforeWarmUpIteration.class, false, errors);
        validatePublicVoidNoArgMethods(AfterWarmUpIteration.class, false, errors);
        validatePublicVoidNoArgMethods(BeforeWarmUp.class, false, errors);
        validatePublicVoidNoArgMethods(AfterWarmUp.class, false, errors);
        validatePublicVoidNoArgMethods(BeforePerformanceIteration.class, false, errors);
        validatePublicVoidNoArgMethods(AfterPerformanceIteration.class, false, errors);
        validatePublicVoidNoArgMethods(BeforePerformanceTest.class, false, errors);
        validatePublicVoidNoArgMethods(AfterPerformanceTest.class, false, errors);
        validatePerformanceTestsExecutionStrategy(errors);
        validateListenMethodsReturnType(errors);
        validateListenMethodsStatic(errors);
        validateListenMethodPublic(errors);
        validateListenFieldsType(errors);
        validateListenFieldsStatic(errors);
        validateListenFieldPublic(errors);
    }

    private void validatePerformanceTestsExecutionStrategy(List<Throwable> errors) {
        for (FrameworkMethod method : getTestClass().getAnnotatedMethods(PerformanceTest.class)) {
            int warmUpInvocations = getWarmUpInvocations(method);
            int warmUpTime = getWarmUpTime(method);

            if (warmUpInvocations <= 0 && warmUpTime <= 0) {
                errors.add(new Error("Method " + method.getName() + "() should provide a valid warmUpInvocations or warmUpTime"));
            }

            if (warmUpInvocations > 0 && warmUpTime > 0) {
                errors.add(new Error("Method " + method.getName() + "() provides both a valid warmUpInvocations and a warmUpTime"));
            }

            int runInvocations = getRunInvocations(method);
            int runTime = getRunTime(method);

            if (runInvocations <= 0 && runTime <= 0) {
                errors.add(new Error("Method " + method.getName() + "() should provide a valid runInvocations or runTime"));
            }

            if (runInvocations > 0 && runTime > 0) {
                errors.add(new Error("Method " + method.getName() + "() provides both a valid runInvocations or runTime"));
            }
        }
    }

    private void validateListenMethodsReturnType(List<Throwable> errors) {
        for (FrameworkMethod method : getTestClass().getAnnotatedMethods(Listen.class)) {
            if (Listener.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            errors.add(new Error("Method " + method.getName() + "() should return an object of type Listener"));
        }
    }

    private void validateListenMethodsStatic(List<Throwable> errors) {
        for (FrameworkMethod method : getTestClass().getAnnotatedMethods(Listen.class)) {
            if (method.isStatic()) {
                continue;
            }

            errors.add(new Error("Method " + method.getName() + "() should be static"));
        }
    }

    private void validateListenMethodPublic(List<Throwable> errors) {
        for (FrameworkMethod method : getTestClass().getAnnotatedMethods(Listen.class)) {
            if (method.isPublic()) {
                continue;
            }

            errors.add(new Error("Method " + method.getName() + "() should be public"));
        }
    }

    private void validateListenFieldsType(List<Throwable> errors) {
        for (FrameworkField field : getTestClass().getAnnotatedFields(Listen.class)) {
            if (Listener.class.isAssignableFrom(field.getType())) {
                continue;
            }

            errors.add(new Error("Field " + field.getName() + " should be of type Listener"));
        }
    }

    private void validateListenFieldsStatic(List<Throwable> errors) {
        for (FrameworkField field : getTestClass().getAnnotatedFields(Listen.class)) {
            if (field.isStatic()) {
                continue;
            }

            errors.add(new Error("Field " + field.getName() + " should be static"));
        }
    }

    private void validateListenFieldPublic(List<Throwable> errors) {
        for (FrameworkField field : getTestClass().getAnnotatedFields(Listen.class)) {
            if (field.isPublic()) {
                continue;
            }

            errors.add(new Error("Field " + field.getName() + " should be public"));
        }
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        return getTestClass().getAnnotatedMethods(PerformanceTest.class);
    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        Statement methodInvoker = super.methodInvoker(method, test);

        Statement invokeWarmUp = methodInvoker;

        invokeWarmUp = withWarmUpIterationStartedEvents(method, test, invokeWarmUp);
        invokeWarmUp = withWarmUpIterationFinishedEvents(method, test, invokeWarmUp);
        invokeWarmUp = withBeforeWarmUpIterations(method, test, invokeWarmUp);
        invokeWarmUp = withAfterWarmUpIterations(method, test, invokeWarmUp);
        invokeWarmUp = withWarmUpIterations(method, test, invokeWarmUp);
        invokeWarmUp = withWarmUpStartedEvents(method, test, invokeWarmUp);
        invokeWarmUp = withWarmUpFinishedEvents(method, test, invokeWarmUp);
        invokeWarmUp = withBeforeWarmUps(method, test, invokeWarmUp);
        invokeWarmUp = withAfterWarmUps(method, test, invokeWarmUp);

        Statement invokePerformanceTest = methodInvoker;

        invokePerformanceTest = withExecutionIterationStartedEvents(method, test, invokePerformanceTest);
        invokePerformanceTest = withExecutionIterationFinishedEvents(method, test, invokePerformanceTest);
        invokePerformanceTest = withBeforePerformanceIterations(method, test, invokePerformanceTest);
        invokePerformanceTest = withAfterPerformanceIterations(method, test, invokePerformanceTest);
        invokePerformanceTest = withPerformanceIterations(method, test, invokePerformanceTest);
        invokePerformanceTest = withExecutionStartedEvents(method, test, invokePerformanceTest);
        invokePerformanceTest = withExecutionFinishedEvents(method, test, invokePerformanceTest);
        invokePerformanceTest = withBeforePerformanceTests(method, test, invokePerformanceTest);
        invokePerformanceTest = withAfterPerformanceTests(method, test, invokePerformanceTest);

        return new RunSerial(invokeWarmUp, invokePerformanceTest);
    }


    protected Statement withBeforeWarmUpIterations(FrameworkMethod method, Object test, Statement next) {
        List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(BeforeWarmUpIteration.class);

        if (methods.size() == 0) {
            return next;
        }

        return new RunBefores(next, methods, test);
    }

    protected Statement withAfterWarmUpIterations(FrameworkMethod method, Object test, Statement next) {
        List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(AfterWarmUpIteration.class);

        if (methods.size() == 0) {
            return next;
        }

        return new RunAfters(next, methods, test);
    }

    protected Statement withWarmUpIterations(FrameworkMethod method, Object test, Statement iteration) {
        return new RunIterations(getWarmUpInvocations(method), getWarmUpTime(method), iteration);
    }

    protected Statement withBeforeWarmUps(FrameworkMethod method, Object test, Statement next) {
        List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(BeforeWarmUp.class);

        if (methods.size() == 0) {
            return next;
        }

        return new RunBefores(next, methods, test);
    }

    protected Statement withAfterWarmUps(FrameworkMethod method, Object test, Statement next) {
        List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(AfterWarmUp.class);

        if (methods.size() == 0) {
            return next;
        }

        return new RunAfters(next, methods, test);
    }

    protected Statement withBeforePerformanceIterations(FrameworkMethod method, Object test, Statement next) {
        List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(BeforePerformanceIteration.class);

        if (methods.size() == 0) {
            return next;
        }

        return new RunBefores(next, methods, test);
    }

    protected Statement withAfterPerformanceIterations(FrameworkMethod method, Object test, Statement next) {
        List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(AfterPerformanceIteration.class);

        if (methods.size() == 0) {
            return next;
        }

        return new RunAfters(next, methods, test);
    }

    protected Statement withPerformanceIterations(FrameworkMethod method, Object test, Statement iteration) {
        return new RunIterations(getRunInvocations(method), getRunTime(method), iteration);
    }

    protected Statement withBeforePerformanceTests(FrameworkMethod method, Object test, Statement next) {
        List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(BeforePerformanceTest.class);

        if (methods.size() == 0) {
            return next;
        }

        return new RunBefores(next, methods, test);
    }

    protected Statement withAfterPerformanceTests(FrameworkMethod method, Object test, Statement next) {
        List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(AfterPerformanceTest.class);

        if (methods.size() == 0) {
            return next;
        }

        return new RunAfters(next, methods, test);
    }

    protected Statement withWarmUpIterationStartedEvents(FrameworkMethod method, Object test, Statement next) {
        return new RunWarmUpIterationStartedEvents(listeners, getTestClass(), method, next);
    }

    protected Statement withWarmUpIterationFinishedEvents(FrameworkMethod method, Object test, Statement next) {
        return new RunWarmUpIterationFinishedEvents(listeners, getTestClass(), method, next);
    }

    protected Statement withWarmUpStartedEvents(FrameworkMethod method, Object test, Statement next) {
        return new RunWarmUpStartedEvents(listeners, getTestClass(), method, next);
    }

    protected Statement withWarmUpFinishedEvents(FrameworkMethod method, Object test, Statement next) {
        return new RunWarmUpFinishedEvents(listeners, getTestClass(), method, next);
    }

    protected Statement withExecutionIterationStartedEvents(FrameworkMethod method, Object test, Statement next) {
        return new RunExecutionIterationStartedEvents(listeners, getTestClass(), method, next);
    }

    protected Statement withExecutionIterationFinishedEvents(FrameworkMethod method, Object test, Statement next) {
        return new RunExecutionIterationFinishedEvents(listeners, getTestClass(), method, next);
    }

    protected Statement withExecutionStartedEvents(FrameworkMethod method, Object test, Statement next) {
        return new RunExecutionStartedEvents(listeners, getTestClass(), method, next);
    }

    protected Statement withExecutionFinishedEvents(FrameworkMethod method, Object test, Statement next) {
        return new RunExecutionFinishedEvents(listeners, getTestClass(), method, next);
    }

    private List<Listener> getListeners() throws Throwable {
        List<Listener> listeners = new ArrayList<Listener>();

        listeners.addAll(readListenersFromStaticFields());
        listeners.addAll(readListenersFromStaticMethods());

        return listeners;
    }

    private List<Listener> readListenersFromStaticMethods() throws Throwable {
        List<Listener> listeners = new ArrayList<Listener>();

        for (FrameworkMethod method : getTestClass().getAnnotatedMethods(Listen.class)) {
            Listener listener = (Listener) method.invokeExplosively(null);

            if (listener == null) {
                throw new IllegalArgumentException("Method " + method.getName() + "() should not return null");
            }

            listeners.add(listener);
        }

        return listeners;
    }

    private List<Listener> readListenersFromStaticFields() throws Exception {
        List<Listener> listeners = new ArrayList<Listener>();

        for (FrameworkField field : getTestClass().getAnnotatedFields(Listen.class)) {
            Listener listener = (Listener) field.get(null);

            if (listener == null) {
                throw new IllegalArgumentException("Field " + field.getName() + " should not be null");
            }

            listeners.add(listener);
        }

        return listeners;
    }

    private int getWarmUpInvocations(FrameworkMethod method) {
        return method.getAnnotation(PerformanceTest.class).warmUpInvocations();
    }

    private int getWarmUpTime(FrameworkMethod method) {
        return method.getAnnotation(PerformanceTest.class).warmUpTime();
    }

    private int getRunInvocations(FrameworkMethod method) {
        return method.getAnnotation(PerformanceTest.class).runInvocations();
    }

    private int getRunTime(FrameworkMethod method) {
        return method.getAnnotation(PerformanceTest.class).runTime();
    }

}

