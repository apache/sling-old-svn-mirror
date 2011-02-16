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
package org.apache.sling.junit.testbundle.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Example test using the JUnit4 annotations */
public class JUnit4Test {
    private String title;
    
    @Before
    public void setTitle() {
        title = "FOO";
    }
    
    @After
    public void resetTitle() {
        title = null;
    }
    
    @Test
    public void testPasses() {
    }
    
    @Test
    public void testRequiresBefore() {
        assertNotNull(title);
    }
    
    @Test
    public void testFailsEveryTime() {
        fail("This JUnit4 test fails every time");
    }
    
    @Test
    public void testFailsSometimes() {
        if(Math.random() > 0.5) {
            fail("This JUnit4 test fails 50% of the time");
        }
    }
}