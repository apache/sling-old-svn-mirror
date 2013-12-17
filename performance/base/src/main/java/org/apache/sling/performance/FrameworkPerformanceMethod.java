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
package org.apache.sling.performance;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.naming.directory.InvalidAttributesException;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.sling.performance.annotation.AfterMethodInvocation;
import org.apache.sling.performance.annotation.BeforeMethodInvocation;
import org.apache.sling.performance.annotation.PerformanceTest;
import org.junit.After;
import org.junit.Before;
import org.junit.runners.model.FrameworkMethod;

class FrameworkPerformanceMethod extends FrameworkMethod {

	private Object target;
	private PerformanceSuiteState performanceSuiteState;
	private PerformanceRunner.ReportLevel reportLevel = PerformanceRunner.ReportLevel.ClassLevel;
    private String testCaseName = "";

	public FrameworkPerformanceMethod(Method method, Object target,
			PerformanceSuiteState performanceSuiteState, PerformanceRunner.ReportLevel reportLevel) {
		super(method);
		this.target = target;
		this.performanceSuiteState = performanceSuiteState;
		this.reportLevel = reportLevel;
        if (target instanceof IdentifiableTestCase) {
            this.testCaseName = ((IdentifiableTestCase) target).testCaseName();
	}
    }

	@Override
	public Object invokeExplosively(Object target, Object... params)
			throws Throwable {
		// Executes the test method on the supplied target

		// Check if this is the first test running from this specific
		// PerformanceSuite
		// and run the BeforeSuite methods
		if ((performanceSuiteState != null)
				&& (performanceSuiteState.getBeforeSuiteMethod() != null)
				&& (performanceSuiteState.getTargetObjectSuite() != null)
				&& (performanceSuiteState.getNumberOfExecutedMethods() == 0)
				&& !performanceSuiteState.testSuiteName
						.equals(ParameterizedTestList.TEST_CASE_ONLY)) {
			performanceSuiteState.getBeforeSuiteMethod().invoke(
					performanceSuiteState.getTargetObjectSuite());
		}

		// In case of a PerformanceSuite we need to run the methods annotated
		// with Before and After
		// ourselves as JUnit can't find them (JUnit is looking for them in the
		// test suite class);
		// in case we don't have to deal with a PerformanceSuite just skip this
		// as JUnit will run the methods itself
		if ((performanceSuiteState != null)
				&& !performanceSuiteState.testSuiteName
						.equals(ParameterizedTestList.TEST_CASE_ONLY)) {

			recursiveCallSpecificMethod(this.target.getClass(), this.target,
					Before.class);
		}

		// Need to count the number of tests run from the PerformanceSuite
		// so that we can call the AfterSuite method after the last test from
		// the suite
		// has run and the AfterSuite needs to run
		performanceSuiteState.incrementNumberOfExecutedTestMethods();

		Object response = null;

		Method testMethodToInvoke = this.getMethod();

		PerformanceTest performanceAnnotation = testMethodToInvoke
				.getAnnotation(PerformanceTest.class);

		// retrieve the test configuration options
		int warmuptime = performanceAnnotation.warmuptime();
		int runtime = performanceAnnotation.runtime();
		int warmupinvocations = performanceAnnotation.warmupinvocations();
		int runinvocations = performanceAnnotation.runinvocations();

		DescriptiveStatistics statistics = new DescriptiveStatistics();

		// System.out.println("Warmup started - test :" +
		// testMethodToInvoke.getName());

		if (warmupinvocations != 0) {
			// Run the number of invocation specified in the annotation
			// for warming up the system
			for (int invocationIndex = 0; invocationIndex < warmupinvocations; invocationIndex++) {

				recursiveCallSpecificMethod(this.target.getClass(),
						this.target, BeforeMethodInvocation.class);

				// TODO: implement the method to run a before a specific test
				// method
				// recursiveCallSpecificMethod(this.target.getClass(),
				// this.target, BeforeSpecificTest.class);

				response = super.invokeExplosively(this.target, params);

				// TODO: implement the method to run a after a specific test
				// method
				// recursiveCallSpecificMethod(this.target.getClass(),
				// this.target, AfterSpecificTest.class);

				recursiveCallSpecificMethod(this.target.getClass(),
						this.target, AfterMethodInvocation.class);
			}
		} else {
			// Run a few iterations to warm up the system
			long warmupEnd = System.currentTimeMillis() + warmuptime * 1000;
			while (System.currentTimeMillis() < warmupEnd) {
				recursiveCallSpecificMethod(this.target.getClass(),
						this.target, BeforeMethodInvocation.class);

				// TODO: implement the method to run a before a specific test
				// method
				// recursiveCallSpecificMethod(this.target.getClass(),
				// this.target, BeforeSpecificTest.class);

				response = super.invokeExplosively(this.target, params);

				// recursiveCallSpecificMethod(this.target.getClass(),
				// this.target, AfterSpecificTest.class);
				// TODO: implement the method to run a after a specific test
				// method

				recursiveCallSpecificMethod(this.target.getClass(),
						this.target, AfterMethodInvocation.class);
			}
		}

		// System.out.println("Warmup ended - test :" +
		// testMethodToInvoke.getName());
		if (runinvocations != 0) {
			// Run the specified number of iterations and capture the execution
			// times
			for (int invocationIndex = 0; invocationIndex < runinvocations; invocationIndex++) {

				response = this.invokeTimedTestMethod(testMethodToInvoke,
						statistics, params);
			}
		} else {
			// Run test iterations and capture the execution times
			long runtimeEnd = System.currentTimeMillis() + runtime * 1000;

			while (System.currentTimeMillis() < runtimeEnd) {

				response = this.invokeTimedTestMethod(testMethodToInvoke,
						statistics, params);

			}
		}

		if (statistics.getN() > 0) {
            ReportLogger.writeReport(this.performanceSuiteState.testSuiteName, testCaseName, this.target.getClass().getName(),
                    getMethod().getName(), statistics, ReportLogger.ReportType.TXT, reportLevel);
		}

		// In case of a PerformanceSuite we need to run the methods annotated
		// with Before and After
		// ourselves as JUnit can't find them; in case we don't have to deal
		// with a PerformanceSuite
		// just skip this as JUnit will run the methods itself
		if ((performanceSuiteState != null)
				&& !performanceSuiteState.testSuiteName
						.equals(ParameterizedTestList.TEST_CASE_ONLY)) {

			recursiveCallSpecificMethod(this.target.getClass(), this.target,
					After.class);
		}

		// Check if this is the last test running from a PerformanceSuite
		// and run the AfterSuite method
		if ((performanceSuiteState != null)
				&& (performanceSuiteState.getAfterSuiteMethod() != null)
				&& (performanceSuiteState.getTargetObjectSuite() != null)
				&& (performanceSuiteState.getNumberOfExecutedMethods() == performanceSuiteState
						.getNumberOfMethodsInSuite())
				&& !performanceSuiteState.testSuiteName
						.equals(ParameterizedTestList.TEST_CASE_ONLY)) {
			performanceSuiteState.getAfterSuiteMethod().invoke(
					performanceSuiteState.getTargetObjectSuite());

		}

		return response;
	}

	/**
	 * Method that runs 1 invocation of the timed test method
	 * 
	 * @param testMethodToInvoke
	 *            the test method to invoke
	 * @param statistics
	 *            the statistics object that collects the results
	 * @param params
	 *            the parameters for the invocation of the test method
	 * @return the response from the method invocation
	 * @throws Throwable
	 */
	private Object invokeTimedTestMethod(Method testMethodToInvoke,
			DescriptiveStatistics statistics, Object... params)
			throws Throwable {

		Object response = null;

		recursiveCallSpecificMethod(this.target.getClass(), this.target,
				BeforeMethodInvocation.class);

		// TODO: implement the method to run a before a specific test method
		// recursiveCallSpecificMethod(this.target.getClass(), this.target,
		// BeforeSpecificTest.class);

		// timing the test method execution
		// System.out.println("Start test: " + testMethodToInvoke.getName());
		long start = System.nanoTime();
		response = super.invokeExplosively(this.target, params);
		long timeMilliseconds = TimeUnit.MILLISECONDS.convert(System.nanoTime()
				- start, TimeUnit.NANOSECONDS);
		statistics.addValue(timeMilliseconds);

		// System.out.println("End test: " + testMethodToInvoke.getName());

		// System.out.println("Test execution time (ms): " + timeMilliseconds);

		// TODO: implement the method to run a after a specific test method
		// recursiveCallSpecificMethod(this.target.getClass(), this.target,
		// AfterSpecificTest.class);

		recursiveCallSpecificMethod(this.target.getClass(), this.target,
				AfterMethodInvocation.class);

		return response;
	}

	/**
	 * Recursively call a specific method annotated with a custom annotation
	 * 
	 * @param test
	 *            the test class that contains the method
	 * @param instance
	 *            the instance on which will run the method
	 * @param methodAnnotation
	 *            the method annotation to look for
	 * @throws InvocationTargetException
	 * @throws InvalidAttributesException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@SuppressWarnings({ "rawtypes" })
	private void recursiveCallSpecificMethod(Class test, Object instance,
			Class<? extends Annotation> methodAnnotation)
			throws InvocationTargetException, InvalidAttributesException,
			IllegalAccessException, InstantiationException {
		if (test.getSuperclass() != null) {
			recursiveCallSpecificMethod(test.getSuperclass(), instance,
					methodAnnotation);
		}

		Method testMethod = getSpecificTestMethod(test, methodAnnotation);
		if (testMethod != null) {
			if (!testMethod.isAccessible()) {
				testMethod.setAccessible(true);
			}
			testMethod.invoke(instance);
		}
	}

	/**
	 * Get the method annotated with the custom annotation
	 * 
	 * @param testClass
	 *            the test class on which to look for the method
	 * @param methodAnnotation
	 *            the method annotation to look for
	 * @return
	 * @throws InvalidAttributesException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@SuppressWarnings({ "rawtypes" })
	private Method getSpecificTestMethod(Class testClass,
			Class<? extends Annotation> methodAnnotation)
			throws InvalidAttributesException, IllegalAccessException,
			InstantiationException {

		Method[] methodsToReturn = getSpecificMethods(testClass,
				methodAnnotation);
		Method methodToReturn = null;
		if (methodsToReturn.length == 1) {
			methodToReturn = methodsToReturn[0];
		} else if (methodsToReturn.length > 1) {
			throw new InvalidAttributesException(
					"Only 1 non parameterized before method accepted");
		}

		return methodToReturn;
	}

	/**
	 * Retrieve all the specific methods from test class
	 * 
	 * @param testClass
	 *            the test class that we need to search in
	 * @param annotation
	 *            the annotation that we should look for
	 * @return the list with the methods that have the specified annotation
	 */
	@SuppressWarnings({ "rawtypes" })
	private Method[] getSpecificMethods(Class testClass,
			Class<? extends Annotation> annotation) {
		Method[] allMethods = testClass.getDeclaredMethods();

		List<Method> methodListResult = new ArrayList<Method>();

		for (Method testMethod : allMethods) {
			if (testMethod.isAnnotationPresent(annotation)) {
				methodListResult.add(testMethod);
			}
		}
		return methodListResult.toArray(new Method[] {});
	}

    @Override
    public String getName() {
        if (testCaseName == null || "".equals(testCaseName.trim())) { return super.getName(); }
        return String.format("%s  [%s.%s]", testCaseName, target.getClass().getSimpleName(),
                getMethod().getName());
}

}
