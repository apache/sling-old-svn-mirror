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
package org.apache.sling.scripting.core.impl.helper;

import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

public class ProxyScriptEngineManager extends SlingScriptEngineManager {

    private SlingScriptEngineManager delegatee;

    public ProxyScriptEngineManager() {
        setDelegatee(null);
    }

    public void setDelegatee(SlingScriptEngineManager delegatee) {
        this.delegatee = (delegatee == null)
                ? new SlingScriptEngineManager()
                : delegatee;
    }

    public Object get(String key) {
        return delegatee.get(key);
    }

    public ScriptEngine getEngineByExtension(String extension) {
        return delegatee.getEngineByExtension(extension);
    }

    public ScriptEngine getEngineByMimeType(String mimeType) {
        return delegatee.getEngineByMimeType(mimeType);
    }

    public ScriptEngine getEngineByName(String shortName) {
        return delegatee.getEngineByName(shortName);
    }

    public List<ScriptEngineFactory> getEngineFactories() {
        return delegatee.getEngineFactories();
    }

    public Map<Object, Object> getProperties(ScriptEngineFactory factory) {
        return delegatee.getProperties(factory);
    }

    public void put(String key, Object value) {
        delegatee.put(key, value);
    }

    public void registerEngineExtension(String extension,
            ScriptEngineFactory factory) {
        delegatee.registerEngineExtension(extension, factory);
    }

    public void registerEngineMimeType(String type, ScriptEngineFactory factory) {
        delegatee.registerEngineMimeType(type, factory);
    }

    public void registerEngineName(String name, ScriptEngineFactory factory) {
        delegatee.registerEngineName(name, factory);
    }

    public void registerScriptEngineFactory(ScriptEngineFactory factory,
            Map<Object, Object> props) {
        delegatee.registerScriptEngineFactory(factory, props);
    }

    // Livetribe JSR-223 2.0.6 API
    public Bindings getGlobalScope()
    {
        return getBindings();
    }

    // Livetribe JSR-223 2.0.6 API
    public void setGlobalScope(Bindings globalScope)
    {
        setBindings(globalScope);
    }

    public Bindings getBindings() {
        return delegatee.getBindings();
    }

    public void setBindings(Bindings bindings) {
        delegatee.setBindings(bindings);
    }
}
