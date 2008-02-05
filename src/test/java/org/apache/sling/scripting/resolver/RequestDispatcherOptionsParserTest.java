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
package org.apache.sling.scripting.resolver;

import junit.framework.TestCase;

import org.apache.sling.api.request.RequestDispatcherOptions;

public class RequestDispatcherOptionsParserTest extends TestCase {
    final RequestDispatcherOptionsParser parser = new RequestDispatcherOptionsParser();
    
    public void testNullString() {
        final RequestDispatcherOptions result = parser.parse(null);
        assertNull(result);
    }
    
    public void testEmptyString() {
        final RequestDispatcherOptions result = parser.parse("");
        assertNull(result);
    }
    
    public void testSingleOption() {
        final RequestDispatcherOptions result = parser.parse("forceResourceType: widget");
        assertNotNull(result);
        assertEquals("Expected option found (" + result + ")",
                "widget",result.get(RequestDispatcherOptions.OPT_FORCE_RESOURCE_TYPE));
    }
    
    public void testResourceTypeShortcut() {
        // a single option with no comma or colon means "forceResourceType"
        final RequestDispatcherOptions result = parser.parse("\t components/widget  ");
        assertNotNull(result);
        assertEquals("Expected option found (" + result + ")",
                "components/widget",result.get(RequestDispatcherOptions.OPT_FORCE_RESOURCE_TYPE));
    }
    
    public void testTwoOptions() {
        final RequestDispatcherOptions result = parser.parse("forceResourceType: true, replaceSelectors : xyz  ,");
        assertNotNull(result);
        assertEquals("Expected option found (" + result + ")",
                "true",result.get(RequestDispatcherOptions.OPT_FORCE_RESOURCE_TYPE));
        assertEquals("Expected option found (" + result + ")",
                "xyz",result.get(RequestDispatcherOptions.OPT_REPLACE_SELECTORS));
    }
}
