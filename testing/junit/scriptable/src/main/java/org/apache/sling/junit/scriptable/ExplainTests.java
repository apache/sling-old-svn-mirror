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
package org.apache.sling.junit.scriptable;

import static org.junit.Assert.fail;

import org.junit.Test;

/** Test that always fails and explains how to create
 *  scriptable tests.
 */
public class ExplainTests {
    
    @Test
    public void explain() {
        fail(
                "No scriptable tests found."
                + " To create scriptable tests, create nodes with the sling:Test"
                + " mixin under /apps or /libs (*), and setup Sling so that requesting them with .test.txt generates"
                + " a text response containing just TEST_PASSED if the test is successful."
                + " Empty lines and lines starting with # are ignored in the test output."
                + " (*) depends on the JCR resource resolver configuration."
                );
    }
}
