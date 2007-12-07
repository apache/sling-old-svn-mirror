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

import org.apache.sling.microsling.mocks.MockRequestParameter;
import org.apache.sling.microsling.mocks.MockRequestParameterMap;

/** Test the NodeNameGenerator */
public class NodeNameGeneratorTest extends TestCase {
    
    private final NodeNameGenerator nng = new NodeNameGenerator();
    
    public void testNoParams() {
        final String result = nng.getNodeName(null, null);
        assertTrue("Node name contains a _ at char 1",result.charAt(1) == '_');
    }
    
    public void testTitle() {
        final MockRequestParameterMap map = new MockRequestParameterMap();
        map.addValue("title", new MockRequestParameter("Hello there"));
        final String result = nng.getNodeName(map, null);
        assertEquals("hello_there", result);
    }
    
    public void testPrefix() {
        final MockRequestParameterMap map = new MockRequestParameterMap();
        map.addValue("PR_title", new MockRequestParameter("Hello there"));
        final String result = nng.getNodeName(map, "PR_");
        assertEquals("hello_there", result);
    }
    
    public void testTitlePriority() {
        final MockRequestParameterMap map = new MockRequestParameterMap();
        map.addValue("title", new MockRequestParameter("Hello there"));
        map.addValue("description", new MockRequestParameter("desct"));
        final String result = nng.getNodeName(map, null);
        assertEquals("hello_there", result);
    }
    
    public void testDescriptionAbstract() {
        final MockRequestParameterMap map = new MockRequestParameterMap();
        map.addValue("description", new MockRequestParameter("desc"));
        map.addValue("abstract", new MockRequestParameter("abs"));
        final String result = nng.getNodeName(map, null);
        assertEquals("desc", result);
    }
    
    public void testStandardMaxLength() {
        final MockRequestParameterMap map = new MockRequestParameterMap();
        map.addValue("name", new MockRequestParameter("1234567890123456789012345678901234567890"));
        final String result = nng.getNodeName(map, null);
        assertEquals(NodeNameGenerator.DEFAULT_MAX_NAME_LENGTH,result.length());
    }
    
    public void testCustomMaxLength() {
        final NodeNameGenerator custom = new NodeNameGenerator();
        custom.setMaxLength(2);
        final MockRequestParameterMap map = new MockRequestParameterMap();
        map.addValue("name", new MockRequestParameter("1234567890123456789012345678901234567890"));
        final String result = custom.getNodeName(map, null);
        assertEquals(2,result.length());
    }
}
