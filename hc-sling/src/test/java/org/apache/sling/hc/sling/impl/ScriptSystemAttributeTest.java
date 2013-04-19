/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.hc.sling.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.hc.api.EvaluationResult;
import org.apache.sling.hc.api.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ScriptSystemAttributeTest {
    
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }
    
    private void assertResult(String info, final String scriptOutput, boolean expectOk) {
        final Answer<?> answer = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final HttpServletResponse response = (HttpServletResponse)invocation.getArguments()[1];
                response.getWriter().write(scriptOutput);
                response.getWriter().flush();
                return null;
            }
        };
        
        final SlingRequestProcessor processor = Mockito.mock(SlingRequestProcessor.class);
        
        try {
            Mockito.doAnswer(answer).when(processor).processRequest(
                    Matchers.any(HttpServletRequest.class), 
                    Matchers.any(HttpServletResponse.class), 
                    Matchers.any(ResourceResolver.class));
        } catch(Exception e) {
            fail("Exception in processRequest: " + e);
        }
        
        final Resource resource = Mockito.mock(Resource.class); 
        final SlingScript script = Mockito.mock(SlingScript.class);
        Mockito.when(script.getScriptResource()).thenReturn(resource);
        final ScriptSystemAttribute a = new ScriptSystemAttribute(processor, script);

        final Rule r = new Rule(a, ScriptSystemAttribute.SUCCESS_STRING);
        final EvaluationResult result = r.evaluate();
        assertEquals("Expecting anythingToReport=" + !expectOk + " for " + info, !expectOk, result.anythingToReport());
    }
    
    @Test
    public void testEmptyScripts() {
        assertResult("Empty script -> error", "", false);
    }
    
    @Test
    public void testOkScript() {
        assertResult("TEST_PASSED script -> ok", "TEST_PASSED", true);
    }
    
    @Test
    public void testComments() {
        assertResult("TEST_PASSED script and blank line -> ok", "\n\nTEST_PASSED\n\n", true);
        assertResult("TEST_PASSED script and comments -> ok", "\n\n#comment\n\t  # comment 2\nTEST_PASSED\n\n", true);
    }
    
    @Test
    public void testDoublePassed() {
        assertResult("Double TEST_PASSED script -> error", "TEST_PASSED\nTEST_PASSED", false);
    }
}
