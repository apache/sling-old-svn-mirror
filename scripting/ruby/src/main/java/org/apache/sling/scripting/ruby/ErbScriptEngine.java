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
package org.apache.sling.scripting.ruby;

import java.io.BufferedReader;
import java.io.Reader;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A ScriptEngine that uses ruby erb templates to render a Resource.
 */
public class ErbScriptEngine extends AbstractSlingScriptEngine {

    private Ruby runtime = null;

    private RubySymbol bindingSym = null;

    private RubyModule erbModule = null;

    public ErbScriptEngine(ErbScriptEngineFactory factory) {
        super(factory);

        runtime = Ruby.getDefaultInstance();
        runtime.evalScript("require 'java';require 'erb';self.send :include, ERB::Util;class ERB;def get_binding;binding;end;attr_reader :props;def set_props(p);@props = p;"
            + "for name,v in @props;instance_eval \"def #{name}; @props['#{name}'];end\";end;end;end;");

        erbModule = runtime.getClassFromPath("ERB");
        bindingSym = RubySymbol.newSymbol(runtime, "get_binding");
    }

    public Object eval(Reader script, ScriptContext scriptContext)
            throws ScriptException {
        Bindings bindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);

        SlingScriptHelper helper = (SlingScriptHelper) bindings.get(SlingBindings.SLING);
        if (helper == null) {
            throw new ScriptException("SlingScriptHelper missing from bindings");
        }

        // ensure GET request
        if (!"GET".equals(helper.getRequest().getMethod())) {
            throw new ScriptException(
                "FreeMarker templates only support GET requests");
        }

        try {
            StringBuffer scriptString = new StringBuffer();
            BufferedReader bufferedScript = new BufferedReader(script);
            String nextLine = bufferedScript.readLine();
            while (nextLine != null) {
                scriptString.append(nextLine);
                scriptString.append("\n");
                nextLine = bufferedScript.readLine();
            }

            IRubyObject scriptRubyString = JavaEmbedUtils.javaToRuby(runtime,
                scriptString.toString());
            IRubyObject erb = (IRubyObject) JavaEmbedUtils.invokeMethod(
                runtime, erbModule, "new", new Object[] { scriptRubyString },
                IRubyObject.class);

            JavaEmbedUtils.invokeMethod(runtime, erb, "set_props",
                new Object[] { JavaEmbedUtils.javaToRuby(runtime, bindings) },
                IRubyObject.class);

            IRubyObject binding = (IRubyObject) JavaEmbedUtils.invokeMethod(
                runtime, erb, "send", new Object[] { bindingSym },
                IRubyObject.class);

            scriptContext.getWriter().write(
                (String) JavaEmbedUtils.invokeMethod(runtime, erb, "result",
                    new Object[] { binding }, String.class));
        } catch (Throwable t) {
            throw new ScriptException("Failure running Ruby script:"
                + t.getMessage());
        }
        return null;
    }
}
