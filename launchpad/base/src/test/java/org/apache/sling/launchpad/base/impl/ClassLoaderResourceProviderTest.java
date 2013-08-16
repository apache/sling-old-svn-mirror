/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.launchpad.base.impl;

import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import org.junit.Test;

public class ClassLoaderResourceProviderTest {
    
    private final Pattern pattern = ClassLoaderResourceProvider.getResourcePathPattern("resources/bundles");
    
    private void assertMatch(String path, boolean expectMatch) {
        if(expectMatch != pattern.matcher(path).matches()) {
            fail("Expected match=" + expectMatch + " for path '" + path + "', got " + !expectMatch);
        }
    }
    
    @Test
    public void testResourcePathPatterns() {
        assertMatch("resources/bundles/", false);
        
        assertMatch("resources/bundles/0/", true);
        assertMatch("resources/bundles/0", true);
        
        assertMatch("resources/bundles/1234/", true);
        assertMatch("resources/bundles/1234", true);
        
        assertMatch("resources/bundles/12/42", false);
        assertMatch("resources/bundles/12/42", false);
        
        assertMatch("something/else/0/", false);
        assertMatch("something/else/0", false);
        
        /* these fail due to SLING-3022
        assertMatch("resources/bundles.someRunMode/", true);
        assertMatch("resources/bundles.someRunMode/14/", true);
        assertMatch("resources/bundles.someRunMode/15", false);
        
        assertMatch("resources/bundles.runModeA.runModeB/", false);
        assertMatch("resources/bundles.runModeA.runModeB/14/", true);
        assertMatch("resources/bundles.runModeA.runModeB/14", true);
        assertMatch("resources/bundles.runModeA.runModeB/15/16", false);
        */
    }
}
