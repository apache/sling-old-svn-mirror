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
package org.apache.sling.microsling.helpers.nodenames;

import junit.framework.TestCase;

/** Test the NodeNameFilter */
public class NodeNameFilterTest extends TestCase {
    
    private final NodeNameFilter nnf = new NodeNameFilter();
    
    public void testNoChange() {
        assertEquals("abcde_12345",nnf.filter("abcde_12345"));
    }
    
    public void testLowercase() {
        assertEquals("abcde_12345",nnf.filter("ABcdE_12345"));
    }
    
    public void testOtherChars() {
        assertEquals("1_2_3_4_5_",nnf.filter("1/2(3)4 5\n"));
    }
}
