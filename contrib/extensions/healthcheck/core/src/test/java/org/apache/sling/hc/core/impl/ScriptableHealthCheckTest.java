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
package org.apache.sling.hc.core.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.sling.hc.api.Result;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.osgi.service.component.ComponentContext;

public class ScriptableHealthCheckTest {
    
    private ScriptableHealthCheck hc;
    private Dictionary<String, String> props;
    private ComponentContext ctx;
    private final JmxScriptBindingsProvider jmxScriptBindingsProvider = new JmxScriptBindingsProvider();

    private void assertExpression(String expression, String languageExtension, boolean expected) throws Exception {
        final ScriptEngine rhino = new ScriptEngineManager().getEngineByExtension("js");
        assertNotNull("With the rhino jar in our classpath, we should get a js script engine", rhino);
        final ScriptEngineManager manager = Mockito.mock(ScriptEngineManager.class);
        Mockito.when(manager.getEngineByExtension(Matchers.same("ecma"))).thenReturn(rhino);
        final Field f = hc.getClass().getDeclaredField("scriptEngineManager");
        f.setAccessible(true);
        f.set(hc, manager);
        
        props.put(ScriptableHealthCheck.PROP_EXPRESSION, expression);
        if(languageExtension != null) {
            props.put(ScriptableHealthCheck.PROP_LANGUAGE_EXTENSION, languageExtension);
        }
        Mockito.when(ctx.getProperties()).thenReturn(props);
        hc.activate(ctx);
        final Result r = hc.execute();
        assertEquals("Expecting result " + expected, expected, r.isOk());
    }
    
    @Before
    public void setup() {
        hc = new ScriptableHealthCheck();
        ctx = Mockito.mock(ComponentContext.class);
        props = new Hashtable<String, String>();
        hc.bindBindingsValuesProvider(jmxScriptBindingsProvider);
    }
    
    @Test
    public void testSimpleExpression() throws Exception {
        assertExpression("2 + 3 == 5", null, true);
    }
    
    @Test
    public void testRemoveBinding() throws Exception {
        final String expr = "jmx.attribute('java.lang:type=ClassLoading', 'LoadedClassCount') > 10"; 
        assertExpression(expr, "ecma", true);
        hc.unbindBindingsValuesProvider(jmxScriptBindingsProvider);
        assertExpression(expr, "ecma", false);
    }
    
    @Test
    public void testJmxExpression() throws Exception {
        assertExpression(
                "jmx.attribute('java.lang:type=ClassLoading', 'LoadedClassCount') > 10"
                + " && jmx.attribute('java.lang:type=Runtime', 'ManagementSpecVersion') > 1", 
                "ecma", true);
    }
    
    @Test
    public void testFalseExpression() throws Exception {
        assertExpression("2 + 3 == 15", null, false);
    }
    
    @Test
    public void testSyntaxError() throws Exception {
        assertExpression("{not [valid ok?", null, false);
    }
    
    @Test
    public void testNoEngine() throws Exception {
        assertExpression("2 + 3 == 5", null, true);
        assertExpression("2 + 3 == 5", "groovy", false);
    }
}
