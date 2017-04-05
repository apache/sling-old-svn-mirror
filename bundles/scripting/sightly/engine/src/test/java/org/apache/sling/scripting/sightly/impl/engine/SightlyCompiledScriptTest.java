/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.sightly.java.compiler.RenderUnit;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.reflection.Whitebox;
import org.osgi.framework.BundleContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;


public class SightlyCompiledScriptTest {

    @Rule
    public final SlingContext slingContext = new SlingContext();

    /**
     * Tests that SlingBindings are correctly handled by compiled scripts, by setting them from the script context to the request
     * attributes.
     * @throws ScriptException
     */
    @Test
    public void testEvalSlingBindings() throws ScriptException {
        ScriptEngine scriptEngine = mock(ScriptEngine.class);
        final RenderUnit renderUnit = mock(RenderUnit.class);
        Whitebox.setInternalState(renderUnit, "subTemplates", new HashMap<String, Object>());
        final BundleContext bundleContext = MockOsgi.newBundleContext();
        bundleContext.registerService(ExtensionRegistryService.class.getName(), mock(ExtensionRegistryService.class), new
                Hashtable<String, Object>());
        ResourceResolver resourceResolver = MockSling.newResourceResolver(bundleContext);
        final MockSlingHttpServletRequest request = spy(new MockSlingHttpServletRequest(resourceResolver, bundleContext));
        SightlyCompiledScript compiledScript = spy(new SightlyCompiledScript(scriptEngine, renderUnit));
        ScriptContext scriptContext = mock(ScriptContext.class);
        StringWriter writer = new StringWriter();
        when(scriptContext.getWriter()).thenReturn(writer);
        Bindings scriptContextBindings = new SimpleBindings(){{
            put("test", "testValue");
            put(SlingBindings.REQUEST, request);
            put(SlingBindings.SLING, MockSling.newSlingScriptHelper(bundleContext));
        }};
        SlingBindings oldBindings = new SlingBindings();
        oldBindings.put("old", "oldValue");
        request.setAttribute(SlingBindings.class.getName(), oldBindings);
        when(scriptContext.getBindings(ScriptContext.ENGINE_SCOPE)).thenReturn(scriptContextBindings);
        compiledScript.eval(scriptContext);
        ArgumentCaptor<SlingBindings> slingBindingsArgumentCaptor = ArgumentCaptor.forClass(SlingBindings.class);
        ArgumentCaptor<String> attributeNameArgumentCaptor = ArgumentCaptor.forClass(String.class);

        // request.setAttribute should have been invoked 3 times: once here, twice in the compiled script
        verify(request, times(3)).setAttribute(attributeNameArgumentCaptor.capture(), slingBindingsArgumentCaptor.capture());
        List<SlingBindings> slingBindingsValues = slingBindingsArgumentCaptor.getAllValues();
        int invocation = 1;
        for (SlingBindings bindings : slingBindingsValues) {
            switch (invocation) {
                case 1:
                    assertEquals(oldBindings, bindings);
                    break;
                case 2:
                    assertEquals(3, bindings.size());
                    for (Map.Entry<String, Object> entry : scriptContextBindings.entrySet()) {
                        assertEquals(entry.getValue(), bindings.get(entry.getKey()));
                    }
                    break;
                case 3:
                    assertEquals(oldBindings, bindings);
            }
            invocation++;
        }
        for (String key : attributeNameArgumentCaptor.getAllValues()) {
            assertEquals(SlingBindings.class.getName(), key);
        }
    }
}
