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
package org.apache.sling.junit.healthcheck.impl;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class JUnitTestSelectorTest {
    
    @Test
    public void testSelectorString() {
        assertEquals("com.example.foo", new JUnitTestSelector("com.example.foo",  null, null).getTestSelectorString());
        assertEquals("com.example.testing#someMethod", new JUnitTestSelector("com.example.testing", "someMethod", null).getTestSelectorString());
    }
    
    @Test
    public void testNoMethod() {
        final JUnitTestSelector s = new JUnitTestSelector("com.example.foo",  null, null);
        assertTrue("Expecting package to be selected", s.acceptTestName("com.example.foo"));
        assertTrue("Expecting subpackage to be selected", s.acceptTestName("com.example.foo.bar"));
        assertTrue("Expecting class to be selected", s.acceptTestName("com.example.foo.some.ThisClass"));
        assertFalse("Expecting other package.accept to return false", s.acceptTestName("com.other.foo"));
    }
    
    @Test
    public void testWithMethod() {
        final JUnitTestSelector s = new JUnitTestSelector("com.example.foo",  "someMethod", null);
        assertTrue("Expecting package to be selected", s.acceptTestName("com.example.foo"));
        assertEquals("someMethod", s.getSelectedTestMethodName());
    }
}
