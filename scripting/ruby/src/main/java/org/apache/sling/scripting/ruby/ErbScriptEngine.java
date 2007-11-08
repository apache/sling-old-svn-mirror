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
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.HttpStatusCodeException;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptEngine;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A ScriptEngine that uses ruby erb templates to render a Resource.
 *
 * @scr.component
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.property name="service.description" value="Sling Ruby Script Engine"
 * @scr.service interface="org.apache.sling.api.scripting.SlingScriptEngine"
 */
public class ErbScriptEngine implements SlingScriptEngine {

    public static final String RUBY_SCRIPT_EXTENSION = "erb";
    public Ruby runtime;
    RubySymbol bindingSym;
    RubyModule erbModule;

    public ErbScriptEngine() throws SlingException {
        runtime = Ruby.getDefaultInstance();

        runtime.evalScript("require 'java';require 'erb';self.send :include, ERB::Util;class ERB;def get_binding;binding;end;attr_reader :props;def set_props(p);@props = p;"
            + "for name,v in @props;instance_eval \"def #{name}; @props['#{name}'];end\";end;end;end;");

        erbModule = runtime.getClassFromPath("ERB");
        bindingSym = RubySymbol.newSymbol(runtime, "get_binding");
    }

    public String[] getExtensions() {
        return new String[]{RUBY_SCRIPT_EXTENSION};
    }

    public String getEngineName() {
        return "Ruby Erb Script Engine";
    }

    public String getEngineVersion() {
        return "0.9";
    }

    public void eval(SlingScript script, Map<String, Object> props)
        throws SlingException, IOException {
        // ensure get method
        HttpServletRequest request = (HttpServletRequest) props.get(REQUEST);
        if(!"GET".equals(request.getMethod())) {
            throw new HttpStatusCodeException(
                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "Ruby templates only support GET requests");
        }

        try {
            final Writer w = ((HttpServletResponse) props.get(RESPONSE)).getWriter();
            PrintStream stream = new PrintStream(new OutputStream() {
                public void write(int b) {
                    try {
                        w.write(b);
                    } catch(IOException ex) {
                    }
                }
            });

            StringBuffer scriptString = new StringBuffer();
            BufferedReader bufferedScript = (BufferedReader) script.getScriptReader();
            String nextLine = bufferedScript.readLine();
            while(nextLine != null) {
                scriptString.append(nextLine);
                scriptString.append("\n");
                nextLine = bufferedScript.readLine();
            }

            IRubyObject scriptRubyString = JavaEmbedUtils.javaToRuby(runtime, scriptString.toString());
            IRubyObject erb = (IRubyObject) JavaEmbedUtils
                .invokeMethod(runtime, erbModule, "new", new Object[]{scriptRubyString}, IRubyObject.class);

            JavaEmbedUtils.invokeMethod(runtime, erb, "set_props",
                new Object[]{JavaEmbedUtils.javaToRuby(runtime, props)}, IRubyObject.class);

            IRubyObject binding = (IRubyObject) JavaEmbedUtils
                .invokeMethod(runtime, erb, "send", new Object[]{bindingSym}, IRubyObject.class);

            String out = (String) JavaEmbedUtils.invokeMethod(runtime, erb, "result",
                new Object[]{binding}, String.class);

            stream.println(out);
            stream.flush();

        } catch(IOException ioe) {
            throw ioe;
        } catch(Throwable t) {
            throw new SlingException("Failure running Ruby script ", t);
        }
    }
}
