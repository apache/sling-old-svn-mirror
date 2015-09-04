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
package org.apache.sling.junit.tests;

import org.junit.Test;
import org.apache.sling.junit.scriptable.TestAllPaths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestAllPathsTest {

    @Test
    public void checkTestTest() throws Exception {
        final String comment =      "# this is a comment" + "\n";
        final String empty =        "    " + "\n";
        final String testPassed =   "TEST_PASSED" + "\n";
        final String any =          "this is any line" + " \n";
        final String script1 = comment +  empty + testPassed;
        final String script2 = comment +  empty + testPassed + testPassed;
        final String script3 = comment +  empty + testPassed + any;
        final String script4 = comment +  empty;
        assertTrue(TestAllPaths.checkTest(script1));
        assertFalse(TestAllPaths.checkTest(script2));
        assertFalse(TestAllPaths.checkTest(script3));
        assertFalse(TestAllPaths.checkTest(script4));
    }

}
