/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.scripting.core.impl;

import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

/**
 * A dummy script engine factory.
 *
 */
public class DummyScriptEngineFactory implements ScriptEngineFactory {

    class DummyScriptEngine implements ScriptEngine {

        public Bindings createBindings() {
            return new SimpleBindings();
        }

        public Object eval(String arg0) throws ScriptException {
            throw new UnsupportedOperationException();
        }

        public Object eval(Reader arg0) throws ScriptException {
            throw new UnsupportedOperationException();
        }

        public Object eval(String arg0, ScriptContext arg1) throws ScriptException {
            throw new UnsupportedOperationException();
        }

        public Object eval(Reader arg0, ScriptContext arg1) throws ScriptException {
            throw new UnsupportedOperationException();
        }

        public Object eval(String arg0, Bindings arg1) throws ScriptException {
            throw new UnsupportedOperationException();
        }

        public Object eval(Reader arg0, Bindings arg1) throws ScriptException {
            throw new UnsupportedOperationException();
        }

        public Object get(String arg0) {
            throw new UnsupportedOperationException();
        }

        public Bindings getBindings(int arg0) {
            throw new UnsupportedOperationException();
        }

        public ScriptContext getContext() {
            throw new UnsupportedOperationException();
        }

        public ScriptEngineFactory getFactory() {
            return DummyScriptEngineFactory.this;
        }

        public void put(String arg0, Object arg1) {
            // NO-OP
        }

        public void setBindings(Bindings arg0, int arg1) {
            // NO-OP
        }

        public void setContext(ScriptContext arg0) {
            // NO-OP
        }

    }

    public String getEngineName() {
        return "Dummy Scripting Engine";
    }

    public String getEngineVersion() {
        return "1.0";
    }

    public List<String> getExtensions() {
        return Arrays.asList("dum", "dummy");
    }

    public String getLanguageName() {
        return "dummy";
    }

    public String getLanguageVersion() {
        return "2.0";
    }

    public String getMethodCallSyntax(String arg0, String arg1, String... arg2) {
        throw new UnsupportedOperationException();
    }

    public List<String> getMimeTypes() {
        return Collections.singletonList("application/x-dummy");
    }

    public List<String> getNames() {
        return Arrays.asList("Dummy", "dummy");
    }

    public String getOutputStatement(String arg0) {
        throw new UnsupportedOperationException();
    }

    public Object getParameter(String arg0) {
        throw new UnsupportedOperationException();
    }

    public String getProgram(String... arg0) {
        throw new UnsupportedOperationException();
    }

    public ScriptEngine getScriptEngine() {
        return new DummyScriptEngine();
    }

}
