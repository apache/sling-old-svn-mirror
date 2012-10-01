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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the object that will be returned by the method in which a new
 * PerformanceTestSuite is created
 * 
 */
public class ParameterizedTestList {

	public static final String TEST_CASE_ONLY = "TESTCASEONLY";

	private List<Object> testObjectList = new ArrayList<Object>();
	private String testSuiteTitle = TEST_CASE_ONLY;
	private Map<String, String> parameters = new LinkedHashMap<String, String>();
	private Map<String, Object> parametersObjects = new LinkedHashMap<String, Object>();

	public Map<String, Object> getParametersObjects() {
		return parametersObjects;
	}

	public void addParameterObject(String key, Object parameterObject) {
		this.parametersObjects.put(key, parameterObject);
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void addParameter(String key, String value) {
		parameters.put(key, value);
	}

	public List<Object> getTestObjectList() {
		return testObjectList;
	}

	public void addTestObject(Object testObject) {
		testObjectList.add(testObject);
	}

	public String getTestSuiteName() {
		return testSuiteTitle;
	}

	public void setTestSuiteTitle(String testSuiteTitle) {
		this.testSuiteTitle = testSuiteTitle;
	}

}
