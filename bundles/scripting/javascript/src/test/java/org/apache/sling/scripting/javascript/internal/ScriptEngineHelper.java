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

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.apache.sling.commons.testing.osgi.MockBundle;
import org.apache.sling.commons.testing.osgi.MockComponentContext;
import org.apache.sling.scripting.api.ScriptCache;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.osgi.framework.BundleContext;


/** Helpers to run javascript code fragments in tests */
public class ScriptEngineHelper {
    private static ScriptEngine engine;
    private static ScriptCache scriptCache = Mockito.mock(ScriptCache.class);

    public static class Data extends HashMap<String, Object> {
    }

    private static ScriptEngine getEngine() {
        if (engine == null) {
            synchronized (ScriptEngineHelper.class) {
                RhinoJavaScriptEngineFactory f = new RhinoJavaScriptEngineFactory();
                Whitebox.setInternalState(f, "scriptCache", scriptCache);
                f.activate(new RhinoMockComponentContext());
                engine = f.getScriptEngine();
            }
        }
        return engine;
    }

    public String evalToString(String javascriptCode) throws ScriptException {
        return evalToString(javascriptCode, null);
    }

    public Object eval(String javascriptCode, Map<String, Object> data)
            throws ScriptException {
        return eval(javascriptCode, data, new StringWriter());
    }

    public String evalToString(String javascriptCode, Map<String, Object> data)
            throws ScriptException {
        final StringWriter sw = new StringWriter();
        eval(javascriptCode, data, sw);
        return sw.toString();
    }

    public Object eval(String javascriptCode, Map<String, Object> data,
            final StringWriter sw) throws ScriptException {
        final PrintWriter pw = new PrintWriter(sw, true);
        ScriptContext ctx = new SimpleScriptContext();

        final Bindings b = new SimpleBindings();
        b.put("out", pw);
        if (data != null) {
            for (Map.Entry<String, Object> e : data.entrySet()) {
                b.put(e.getKey(), e.getValue());
            }
        }

        ctx.setBindings(b, ScriptContext.ENGINE_SCOPE);
        ctx.setWriter(sw);
        ctx.setErrorWriter(new OutputStreamWriter(System.err));
        Object result = getEngine().eval(javascriptCode, ctx);

        if (result instanceof Wrapper) {
            result = ((Wrapper) result).unwrap();
        }

        if (result instanceof ScriptableObject) {
            Context.enter();
            try {
                result = ((ScriptableObject) result).getDefaultValue(null);
            } finally {
                Context.exit();
            }
        }

        return result;
    }

    private static class RhinoMockComponentContext extends MockComponentContext {

        private BundleContext bundleContext = Mockito.mock(BundleContext.class);

        private RhinoMockComponentContext() {
            super(new MockBundle(0));
        }

        @Override
        public BundleContext getBundleContext() {
            return bundleContext;
        }
    }
}
