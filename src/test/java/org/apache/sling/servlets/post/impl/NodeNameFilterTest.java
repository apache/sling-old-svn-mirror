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
package org.apache.sling.servlets.post.impl;

import junit.framework.TestCase;

import org.apache.sling.servlets.post.impl.helper.NodeNameFilter;

public class NodeNameFilterTest extends TestCase {
    private final NodeNameFilter filter = new NodeNameFilter();
    
    protected void runTest(String [] data) {
        for(int i=0; i < data.length; i++) {
            final String input = data[i];
            i++;
            final String expected = data[i];
            final String actual = filter.filter(input);
            assertEquals(expected, actual);
        }
    }
    
    public void testBasicFiltering() {
        final String [] data = {
                "test", "test",
                "t?st", "t_st",
                "t??st", "t_st"
        };
        
        runTest(data);
    }
    
    public void testNoInitialNumber() {
        final String [] data = {
                "1234", "_1234",
                "1", "_1"
        };
        
        runTest(data);
    }
}