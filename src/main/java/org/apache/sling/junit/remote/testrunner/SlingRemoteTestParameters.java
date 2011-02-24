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
package org.apache.sling.junit.remote.testrunner;

/** Test class "proxies" implement this to indicate where to
 *  run the tests.
 */
public interface SlingRemoteTestParameters {
    /** Return the URL of the JUnit servlet */
    String getJunitServletUrl();
    
    /** Return the optional selector for the test classes to run,
     *  for example "org.apache.sling.testing.samples.sampletests.JUnit4Test"
     */
    String getTestClassesSelector();
    
    /** Return the optional selector for the test methods to run,
     *  for example "someMethodName"
     */
    String getTestMethodSelector();
    
    /** Return the expected number of tests - if zero, no check
     *  is done.
     */
    int getExpectedNumberOfTests();
}
