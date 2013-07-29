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
package org.apache.sling.hc.rules.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.sling.hc.api.Rule;
import org.apache.sling.hc.api.RuleBuilder;
import org.apache.sling.hc.rules.scriptable.ScriptableRuleBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ScriptableRuleBuilderTest {
    private RuleBuilder builder;
    private ServiceReference serviceReference;
    private ScriptEngineManager scriptEngineManager; 
    private ScriptEngine scriptEngine; 
    
    public static final String EXT = "test_ext";
    public static final String CODE = "this is the test code";
    public static final String NAME = "The rule name";
    
    @Before
    public void setup() throws ScriptException {
        setupScriptEngine(false);
    }
    
    private void setupScriptEngine(boolean active) throws ScriptException {
        final BundleContext ctx = Mockito.mock(BundleContext.class);
        builder = new ScriptableRuleBuilder(ctx);
        
        if(!active) {
            scriptEngineManager = null;
            serviceReference = null;
            scriptEngine = null;
        } else {
            serviceReference = Mockito.mock(ServiceReference.class);
            
            scriptEngine = Mockito.mock(ScriptEngine.class);
            Mockito.when(scriptEngine.eval(Matchers.same(CODE), Matchers.isA(Bindings.class))).thenReturn("true");
            Mockito.when(scriptEngine.createBindings()).thenReturn(Mockito.mock(Bindings.class));
            
            scriptEngineManager = Mockito.mock(ScriptEngineManager.class);
            Mockito.when(
                    scriptEngineManager.getEngineByExtension(Matchers.same(EXT)))
                    .thenReturn(scriptEngine);
        }
        
        Mockito.when(
                ctx.getServiceReference(Matchers.same(ScriptEngineManager.class.getName())))
                .thenReturn(serviceReference);
        Mockito.when(
                ctx.getService(Matchers.same(serviceReference)))
                .thenReturn(scriptEngineManager);
    }
    
    @Test
    public void testNoServiceReference() {
        final Rule r = builder.buildRule(ScriptableRuleBuilder.NAMESPACE, NAME, EXT, CODE);
        assertTrue(r.evaluate().anythingToReport());
    }
    
    @Test
    public void testNoScriptEngine() throws ScriptException {
        setupScriptEngine(true);
        final Rule r = builder.buildRule(ScriptableRuleBuilder.NAMESPACE, NAME, "bad_extension", CODE);
        assertTrue(r.evaluate().anythingToReport());
    }
    
    @Test
    public void testGoodScript() throws ScriptException {
        setupScriptEngine(true);
        final Rule r = builder.buildRule(ScriptableRuleBuilder.NAMESPACE, NAME, EXT, CODE);
        assertFalse(r.evaluate().anythingToReport());
    }    
}