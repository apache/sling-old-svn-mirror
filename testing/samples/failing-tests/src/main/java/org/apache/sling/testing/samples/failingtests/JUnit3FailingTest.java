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
package org.apache.sling.testing.samples.failingtests;

import junit.framework.TestCase;

/** Example failing tests using the JUnit3 APIs */
public class JUnit3FailingTest extends TestCase {
    public void testFailsEveryTime() {
        fail("This JUnit3 test fails every time");
    }

    public void testAssertsEveryTime() {
        assertTrue("This JUnit3 test asserts every time", false);
    }
}
