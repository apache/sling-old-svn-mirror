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

import org.apache.sling.performance.annotation.AfterSuite;
import org.apache.sling.performance.annotation.BeforeSuite;
import org.apache.sling.performance.annotation.PerformanceTest;
import org.apache.sling.performance.annotation.PerformanceTestFactory;
import org.apache.sling.performance.annotation.PerformanceTestSuite;
import org.junit.After;
import org.junit.Before;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
		// add normal JUnit tests
		tests.addAll(super.computeTestMethods());

		// add the performance tests
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
		List<FrameworkMethod> tests = new LinkedList<FrameworkMethod>();

		List<Object> testObjects = new ArrayList<Object>();
		List<Object> testObjectsTmp = new ArrayList<Object>();


		ParameterizedTestList testCenter = new ParameterizedTestList();

		// Retrieve the test objects included in the Performance test suite
		for (FrameworkMethod method : getTestClass().getAnnotatedMethods(
				PerformanceTestSuite.class)) {
			Object targetObject = getTestClass().getJavaClass().newInstance();
			if (method.getMethod().getReturnType()
					.equals(ParameterizedTestList.class)) {
				testCenter = (ParameterizedTestList) method.getMethod().invoke(
						targetObject);
				testObjectsTmp = testCenter.getTestObjectList();

                // Iterate through all the test cases and see if they have a factory
                for (Object testObject : testObjectsTmp) {
                    Method[] factoryMethods = getSpecificMethods(
                            testObject.getClass(), PerformanceTestFactory.class);

                    // If we have a factory method, get all the instance objects returned by this factory
                    if (factoryMethods.length > 0) {
                        // Make sure there's only one factory method
                        if (factoryMethods.length > 1) {
                            throw new IllegalStateException(
                                    "There should be at most one PerformanceTestFactory method");
                        }
                        Method factoryMethod = factoryMethods[0];

                        // Execute the method (statically)
                        Object instances = factoryMethod.invoke(testObject);

                        // If the factory returned an array, make a list
                        if (instances.getClass().isArray()) {
                            instances = Arrays.asList((Object[]) instances);
                        }

                        // If the factory returned a single element, put it in a list
                        if (!(instances instanceof Iterable<?>)) {
                            instances = Collections.singletonList(instances);
                        }
                        testObjects.addAll((List) instances);
                    } else {
                        testObjects.add(testObject);
                    }
                }
			} else {
				throw new InitializationError(
						"Wrong signature for the @PerformanceTestSuite method");
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
				newSuite.setBeforeSuiteMethod(beforeSuiteMethods.get(0).getMethod());
			}
			if (afterSuiteMethods.size() == 1) {
				newSuite.setAfterSuiteMethod(afterSuiteMethods.get(0).getMethod());
			}

			current = newSuite;
			newSuite.setTargetObjectSuite(getTestClass().getJavaClass().newInstance());

		}

		// In case there are any objects retrieved from the Performance Suite
		// we should add them to the tests that will be run and increase the
		// number of methods
		// contained in the PerformanceSuite
		if (!testObjects.isEmpty()) {
			for (Object testObject : testObjects) {

				// retrieve the test methods from the test classes
				Method[] testMethods = getSpecificMethods(testObject.getClass(), PerformanceTest.class);

				for (Method method : testMethods) {
					FrameworkPerformanceMethod performaceTestMethod =
                            new FrameworkPerformanceMethod(method, testObject, current, reportLevel);
					tests.add(performaceTestMethod);
				}

                if (!suiteAlreadyRegistered) {
					newSuite.incrementNumberOfTestMethodsInSuite();
				}
			}

			// add the new suite to the list of suites
			suitesState.add(newSuite);
		}

		// Retrieve the performance tests in the case we don't have a
		// performance test suite
		for (FrameworkMethod method : getTestClass().getAnnotatedMethods(PerformanceTest.class)) {
			Object targetObject = getTestClass().getJavaClass().newInstance();
			FrameworkPerformanceMethod performanceTestMethod = new FrameworkPerformanceMethod(
					method.getMethod(), targetObject, current, reportLevel);
			tests.add(performanceTestMethod);
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
