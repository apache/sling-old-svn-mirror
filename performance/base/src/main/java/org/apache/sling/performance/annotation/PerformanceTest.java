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
package org.apache.sling.performance.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Performance test annotation to use for the framework to be able to find
 * all the tests in the test class 
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PerformanceTest {
	
	
	// set the warmup time for the test
	int warmuptime() default 1;
	
	// set the run time of the test
	int runtime() default 5;
	
	// set the number of invocations to time 
	// by default the run time is used instead
	int runinvocations() default 0;
	
	// set the number of invocations to run
	// in the warm up phase
	int warmupinvocations() default 0;

	// set the performance threshold
	double threshold() default 0;
}
