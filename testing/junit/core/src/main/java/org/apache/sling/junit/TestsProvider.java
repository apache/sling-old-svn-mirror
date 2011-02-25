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
package org.apache.sling.junit;

import java.util.List;

/** Provides tests, for example by scanning bundles, 
 *  finding test resources in a content repository, etc.
 */
public interface TestsProvider {
    /** Return this service's PID, client might use it later
     *  to instantiate a specific test. 
     */
    String getServicePid();
    
    /** Return the list of available tests */
    List<String> getTestNames();
    
    /** Create a test class to execute the specified test.
     *  The test executes in the same thread that calls
     *  this method, to allow using ThreadLocals to pass
     *  context to the test if needed. */
    Class<?> createTestClass(String testName) throws ClassNotFoundException;
    
    /** Return the timestamp at which our list of tests was last modified */
    long lastModified();
}
