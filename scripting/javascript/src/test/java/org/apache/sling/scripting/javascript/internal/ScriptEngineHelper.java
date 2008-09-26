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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Dictionary;
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
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/** Helpers to run javascript code fragments in tests */
public class ScriptEngineHelper {
    private static ScriptEngine engine;

    public static class Data extends HashMap<String, Object> {
    }

    private static ScriptEngine getEngine() {
        if (engine == null) {
            synchronized (ScriptEngineHelper.class) {
                RhinoJavaScriptEngineFactory f = new RhinoJavaScriptEngineFactory();
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
        
        private RhinoMockComponentContext() {
            super(null, null);
        }
        
        @Override
        public BundleContext getBundleContext() {
            return new BundleContext() {

                public void addBundleListener(BundleListener arg0) {
                    // TODO Auto-generated method stub
                    
                }

                public void addFrameworkListener(
                        FrameworkListener arg0) {
                    // TODO Auto-generated method stub
                    
                }

                public void addServiceListener(ServiceListener arg0) {
                    // TODO Auto-generated method stub
                    
                }

                public void addServiceListener(
                        ServiceListener arg0, String arg1)
                        throws InvalidSyntaxException {
                    // TODO Auto-generated method stub
                    
                }

                public Filter createFilter(String arg0)
                        throws InvalidSyntaxException {
                    // TODO Auto-generated method stub
                    return null;
                }

                public ServiceReference[] getAllServiceReferences(
                        String arg0, String arg1)
                        throws InvalidSyntaxException {
                    // TODO Auto-generated method stub
                    return null;
                }

                public Bundle getBundle() {
                    // TODO Auto-generated method stub
                    return null;
                }

                public Bundle getBundle(long arg0) {
                    // TODO Auto-generated method stub
                    return null;
                }

                public Bundle[] getBundles() {
                    // TODO Auto-generated method stub
                    return null;
                }

                public File getDataFile(String arg0) {
                    // TODO Auto-generated method stub
                    return null;
                }

                public String getProperty(String arg0) {
                    // TODO Auto-generated method stub
                    return null;
                }

                public Object getService(ServiceReference arg0) {
                    // TODO Auto-generated method stub
                    return null;
                }

                public ServiceReference getServiceReference(
                        String arg0) {
                    // TODO Auto-generated method stub
                    return null;
                }

                public ServiceReference[] getServiceReferences(
                        String arg0, String arg1)
                        throws InvalidSyntaxException {
                    // TODO Auto-generated method stub
                    return null;
                }

                public Bundle installBundle(String arg0)
                        throws BundleException {
                    // TODO Auto-generated method stub
                    return null;
                }

                public Bundle installBundle(String arg0,
                        InputStream arg1) throws BundleException {
                    // TODO Auto-generated method stub
                    return null;
                }

                public ServiceRegistration registerService(
                        String[] arg0, Object arg1, Dictionary arg2) {
                    // TODO Auto-generated method stub
                    return null;
                }

                public ServiceRegistration registerService(
                        String arg0, Object arg1, Dictionary arg2) {
                    // TODO Auto-generated method stub
                    return null;
                }

                public void removeBundleListener(BundleListener arg0) {
                    // TODO Auto-generated method stub
                    
                }

                public void removeFrameworkListener(
                        FrameworkListener arg0) {
                    // TODO Auto-generated method stub
                    
                }

                public void removeServiceListener(
                        ServiceListener arg0) {
                    // TODO Auto-generated method stub
                    
                }

                public boolean ungetService(ServiceReference arg0) {
                    // TODO Auto-generated method stub
                    return false;
                }
            };
        }
    }
}
