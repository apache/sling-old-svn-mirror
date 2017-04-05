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
package org.apache.sling.scripting.javascript.internal;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.sling.scripting.api.ScriptCache;
import org.apache.sling.scripting.javascript.helper.SlingWrapFactory;
import org.mockito.Mockito;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;

import junit.framework.TestCase;

public class RhinoJavaScriptEngineTest extends TestCase {

    private static ScriptCache scriptCache = Mockito.mock(ScriptCache.class);

    public void testPreserveScopeBetweenEvals() throws ScriptException {
        MockRhinoJavaScriptEngineFactory factory = new MockRhinoJavaScriptEngineFactory();
        ScriptEngine engine = factory.getScriptEngine();
        Bindings context = new SimpleBindings();
        engine.eval("var f = 1", context);
        Object result = null;
        try {
            result = engine.eval("f += 1", context);
        } catch (ScriptException e) {
            TestCase.fail(e.getMessage());
        }
        assertTrue(result instanceof Double);
        assertEquals(2.0, result);
    }

    private static class MockRhinoJavaScriptEngineFactory extends RhinoJavaScriptEngineFactory {

        protected SlingWrapFactory wrapFactory;

        @Override
        public ScriptEngine getScriptEngine() {
            final Context rhinoContext = Context.enter();
            Scriptable scope = rhinoContext.initStandardObjects(new ImporterTopLevel(), false);
            return new RhinoJavaScriptEngine(this, scope, scriptCache);
        }

        @Override
        SlingWrapFactory getWrapFactory() {
            if (wrapFactory == null) {
                wrapFactory = new SlingWrapFactory();
            }
            return wrapFactory;
        }
    }

}
