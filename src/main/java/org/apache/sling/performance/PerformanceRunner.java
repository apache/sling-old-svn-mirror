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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.performance.annotation.AfterSuite;
import org.apache.sling.performance.annotation.BeforeSuite;
import org.apache.sling.performance.annotation.PerformanceTest;
import org.apache.sling.performance.annotation.PerformanceTestSuite;
import org.junit.After;
import org.junit.Before;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * The custom JUnit runner that collects the performance tests
 * 
 */



public class PerformanceRunner extends BlockJUnit4ClassRunner {
	protected LinkedList<FrameworkMethod> tests = new LinkedList<FrameworkMethod>();
	private List<PerformanceSuiteState> suitesState = new ArrayList<PerformanceSuiteState>();
	public ReportLevel reportLevel = ReportLevel.ClassLevel;
	
	public static enum ReportLevel{
		ClassLevel,
		MethodLevel
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Parameters {
		public ReportLevel reportLevel() default ReportLevel.ClassLevel;
	}
	
	public PerformanceRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
		
		// set the report level for the tests that are run with the PerformanceRunner
		// by default set to class level for legacy tests compatibility
		if (clazz.getAnnotation(Parameters.class) != null){
			reportLevel = clazz.getAnnotation(Parameters.class).reportLevel();
		}
		
		try {
			computeTests();
		} catch (Exception e) {
			throw new InitializationError(e);
		}
	}

	/**
	 * Compute the tests that will be run
	 * 
	 * @throws Exception
	 */
	protected void computeTests() throws Exception {
		tests.addAll(super.computeTestMethods());

		// count the performance tests
		tests.addAll(computePerformanceTests());

		// This is called here to ensure the test class constructor is called at
		// least
		// once during testing.
		createTest();
	}

	/**
	 * Compute performance tests
	 * 
	 * @return the list containing the performance test methods
	 * @throws Exception
	 */
	protected Collection<? extends FrameworkMethod> computePerformanceTests()
			throws Exception {
		List<FrameworkPerformanceMethod> tests = new LinkedList<FrameworkPerformanceMethod>();

		List<Object> testObjects = new ArrayList<Object>();
		ParameterizedTestList testCenter = new ParameterizedTestList();

		// Retrieve the test objects included in the Performance test suite
		for (FrameworkMethod method : getTestClass().getAnnotatedMethods(
				PerformanceTestSuite.class)) {
			Object targetObject = getTestClass().getJavaClass().newInstance();
			if (method.getMethod().getReturnType()
					.equals(ParameterizedTestList.class)) {
				testCenter = (ParameterizedTestList) method.getMethod().invoke(
						targetObject);
				testObjects = testCenter.getTestObjectList();
			} else {
				throw new InitializationError(
						"Wrong signature for the @PerformanceSuite method");
			}
		}

		// Retrieve the methods before running the methods from the test suite
		List<FrameworkMethod> beforeSuiteMethods = getTestClass()
				.getAnnotatedMethods(BeforeSuite.class);
		if (beforeSuiteMethods.size() > 1) {
			throw new InitializationError(
					"Only one @BeforeSuite method is allowed for a @PerformanceSuite");
		}

		// Retrieve the methods before running the methods from the test suite
		List<FrameworkMethod> afterSuiteMethods = getTestClass()
				.getAnnotatedMethods(AfterSuite.class);
		if (afterSuiteMethods.size() > 1) {
			throw new InitializationError(
					"Only one @AfterSuite method is allowed for a @PerformanceSuite");
		}

		PerformanceSuiteState current = null;
		boolean suiteAlreadyRegistered = false;

		for (PerformanceSuiteState suiteState : suitesState) {
			if (suiteState.testSuiteName.equals(testCenter.getTestSuiteName())) {
				suiteAlreadyRegistered = true;
				suiteState.incrementNumberOfTestMethodsInSuite();
				current = suiteState;
				break;
			}
		}

		// Create a new PerformanceSuiteState object
		PerformanceSuiteState newSuite = new PerformanceSuiteState(
				testCenter.getTestSuiteName());

		if (!suiteAlreadyRegistered) {
			if (beforeSuiteMethods.size() == 1) {
				newSuite.setBeforeSuiteMethod(beforeSuiteMethods.get(0)
						.getMethod());
			}
			if (afterSuiteMethods.size() == 1) {
				newSuite.setAfterSuiteMethod(afterSuiteMethods.get(0)
						.getMethod());
			}

			current = newSuite;
			newSuite.setTargetObjectSuite(getTestClass().getJavaClass()
					.newInstance());

		}

		// In case there are any objects retrieved from the Performance Suite
		// we should add them to the tests that will be run and increase the
		// number of methods
		// contained in the PerformaceSuite
		if (!testObjects.isEmpty()) {
			for (Object testObject : testObjects) {
				// retrieve the test methods from the test classes
				Method[] testMethods = getSpecificMethods(
						testObject.getClass(), PerformanceTest.class);

				if (!suiteAlreadyRegistered) {
					newSuite.incrementNumberOfTestMethodsInSuite();
				}

				for (Method method : testMethods) {
					FrameworkPerformanceMethod performaceTestMethod = new FrameworkPerformanceMethod(
							method, testObject, current, reportLevel);
					tests.add(performaceTestMethod);
				}
			}

			// add the new suite to the list of suites
			suitesState.add(newSuite);
		}

		// Retrieve the performance tests in the case we don't have a
		// performance test suite
		for (FrameworkMethod method : getTestClass().getAnnotatedMethods(
				PerformanceTest.class)) {
			Object targetObject = getTestClass().getJavaClass().newInstance();
			FrameworkPerformanceMethod performaceTestMethod = new FrameworkPerformanceMethod(
					method.getMethod(), targetObject, current, reportLevel);
			tests.add(performaceTestMethod);
		}

		return tests;
	}

	/**
	 * Retrieve specific method from test class
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

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.junit.runners.BlockJUnit4ClassRunner#computeTestMethods()
	 */
	@Override
	protected List<FrameworkMethod> computeTestMethods() {
		return tests;
	}

	/**
	 * Need to override method otherwise the validation will fail because of
	 * some hardcoded conditions in JUnit
	 */
	@Override
	protected void validateInstanceMethods(List<Throwable> errors) {
		validatePublicVoidNoArgMethods(After.class, false, errors);
		validatePublicVoidNoArgMethods(Before.class, false, errors);
		validateTestMethods(errors);
	}

}
