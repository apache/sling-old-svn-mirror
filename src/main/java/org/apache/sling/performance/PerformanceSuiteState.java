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

import java.lang.reflect.Method;

public class PerformanceSuiteState {

	public String testSuiteName = ParameterizedTestList.TEST_CASE_ONLY;

	private Method beforeSuiteMethod;
	private Method afterSuiteMethod;
	private int numberOfMethodsInSuite = 0;
	private int numberOfExecutedMethods = 0;
	private Object targetObjectSuite;

	public PerformanceSuiteState(String testSuiteName) {
		this.testSuiteName = testSuiteName;
	}

	public void incrementNumberOfTestMethodsInSuite() {
		numberOfMethodsInSuite++;
	}

	public void incrementNumberOfExecutedTestMethods() {
		numberOfExecutedMethods++;
	}

	public String getTestSuiteName() {
		return testSuiteName;
	}

	public void setTestSuiteName(String testSuiteName) {
		this.testSuiteName = testSuiteName;
	}

	public Method getBeforeSuiteMethod() {
		return beforeSuiteMethod;
	}

	public void setBeforeSuiteMethod(Method beforeSuiteMethod) {
		this.beforeSuiteMethod = beforeSuiteMethod;
	}

	public Method getAfterSuiteMethod() {
		return afterSuiteMethod;
	}

	public void setAfterSuiteMethod(Method afterSuiteMethod) {
		this.afterSuiteMethod = afterSuiteMethod;
	}

	public int getNumberOfMethodsInSuite() {
		return numberOfMethodsInSuite;
	}

	public void setNumberOfMethodsInSuite(int numberOfMethodsInSuite) {
		this.numberOfMethodsInSuite = numberOfMethodsInSuite;
	}

	public int getNumberOfExecutedMethods() {
		return numberOfExecutedMethods;
	}

	public void setNumberOfExecutedMethods(int numberOfExecutedMethods) {
		this.numberOfExecutedMethods = numberOfExecutedMethods;
	}

	public Object getTargetObjectSuite() {
		return targetObjectSuite;
	}

	public void setTargetObjectSuite(Object targetObjectSuite) {
		this.targetObjectSuite = targetObjectSuite;
	}

}
